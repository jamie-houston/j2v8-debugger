package com.alexii.j2v8debugger

import androidx.annotation.VisibleForTesting
import com.alexii.j2v8debugger.utils.LogUtils
import com.alexii.j2v8debugger.utils.logger
import com.eclipsesource.v8.inspector.V8Inspector
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod
import com.facebook.stetho.inspector.protocol.module.Runtime.RemoteObject
import com.facebook.stetho.json.ObjectMapper
import com.facebook.stetho.json.annotation.JsonProperty
import com.facebook.stetho.websocket.CloseCodes
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import com.facebook.stetho.inspector.protocol.module.Debugger as FacebookDebuggerStub

//users of the lib can change this value
val scriptsDomain = "http://app/"
val scriptsUrlBase get() = scriptsDomain + StethoHelper.scriptsPathPrefix

//move to separate mapper class if conversion logic become complicated and used in many places
fun scriptIdToUrl(scriptId: String?) = scriptsUrlBase + scriptId
fun urlToScriptId(url: String?) = url?.removePrefix(scriptsUrlBase)

/**
 * V8 JS Debugger. Name of the class and methods must match names defined in Chrome Dev Tools protocol.
 *
 * [initialize] must be called before actual debugging (adding breakpoints in Chrome DevTools).
 *  Otherwise setting breakpoint, etc. makes no effect.
 *  Above is also true for the case when debugger is paused due to pause/resume implementation as thread suspension.
 */
class Debugger(
    private val scriptSourceProvider: ScriptSourceProvider
) : FacebookDebuggerStub() {
    var dtoMapper: ObjectMapper = ObjectMapper()
        @VisibleForTesting set
        @VisibleForTesting get

    //xxx: consider using WeakReference
    /** Must be called on [v8Executor]]. */
    var v8Inspector: V8Inspector? = null
        private set
        @VisibleForTesting get


    /**
     * Needed as @ChromeDevtoolsMethod methods are called on Stetho threads, but not v8 thread.
     *
     * XXX: consider using ThreadBound from Facebook with an implementation, which uses Executor.
     */
    private var v8Executor: ExecutorService? = null

    private var connectedPeer: JsonRpcPeer? = null

    companion object {
        const val TAG = "j2v8-debugger"
    }

    fun initialize(v8Inspector: V8Inspector, v8Executor: ExecutorService) {
        this.v8Executor = v8Executor
        this.v8Inspector = v8Inspector
    }

    private fun validateV8Initialized() {
        if (v8Executor == null || v8Inspector == null) {
            throw IllegalStateException("Unable to set breakpoint when v8 was not initialized yet")
        }
    }

    @ChromeDevtoolsMethod
    override fun enable(peer: JsonRpcPeer, params: JSONObject?) {
        runStethoSafely {
            connectedPeer = peer

            scriptSourceProvider.allScriptIds
                .map { ScriptParsedEvent(it) }
                .forEach { peer.invokeMethod(Protocol.Debugger.ScriptParsed, it, null) }

            peer.registerDisconnectReceiver(::onDisconnect)
        }
    }

    @ChromeDevtoolsMethod
    fun setOverlayMessage(peer: JsonRpcPeer, params: JSONObject?){
        // Ignore
    }

    @ChromeDevtoolsMethod
    fun evaluateOnCallFrame(peer: JsonRpcPeer, params: JSONObject?) : JsonRpcResult? {
        val method = Protocol.Debugger.EvaluateOnCallFrame

        var result: String? = null
        runBlocking {
            result = V8Helper.getV8Result(method, params)
        }
        return EvaluateOnCallFrameResult(JSONObject(result))
    }

    @ChromeDevtoolsMethod
    fun setSkipAllPauses(peer: JsonRpcPeer, params: JSONObject?){
        // This was changed from skipped to skip
        // https://chromium.googlesource.com/chromium/src/third_party/WebKit/Source/platform/v8_inspector/+/e7a781c04b7822a46e7de465623152ff1b45bdac%5E%21/
        V8Helper.v8MessageQueue[Protocol.Debugger.SetSkipAllPauses] = JSONObject().put("skip", params?.getBoolean("skipped"))
    }

    private fun onDisconnect() {
        runStethoSafely {
            connectedPeer = null
            //avoid app being freezed when no debugging happening anymore
            v8Executor?.execute {
                V8Helper.dispatchMessage(Protocol.Debugger.Resume)
            }
            // TODO: Remove all breakpoints (so next launch doesn't have them
            //xxx: check if something else is needed to be done here
        }
    }

    /**
     * Invoked when scripts are changed. Currently closes Chrome DevTools.
     */
    internal fun onScriptsChanged() {
        //todo: check if we can "update" scripts already reported with "Debugger.scriptParsed"
        connectedPeer?.webSocket?.close(CloseCodes.NORMAL_CLOSURE, "on scripts changed");
    }

    @ChromeDevtoolsMethod
    override fun disable(peer: JsonRpcPeer, params: JSONObject?) {
        //xxx: figure-out why and when this method could be called
    }

    @ChromeDevtoolsMethod
    fun getScriptSource(peer: JsonRpcPeer, params: JSONObject): JsonRpcResult? {
        return runStethoAndV8Safely {
            try {
                val request = dtoMapper.convertValue(params, GetScriptSourceRequest::class.java)
                val scriptSource = scriptSourceProvider.getSource(request.scriptId!!)
                GetScriptSourceResponse(scriptSource)
            } catch (e: Exception) {
                // Send exception as source code for debugging.
                // Otherwise If error is thrown - Stetho reports broken I/O pipe and disconnects
                GetScriptSourceResponse(logger.getStackTraceString(e))
            }
        }
    }

    @ChromeDevtoolsMethod
    fun resume(peer: JsonRpcPeer, params: JSONObject?) {
        V8Helper.v8MessageQueue[Protocol.Debugger.Resume] = params
    }

    @ChromeDevtoolsMethod
    fun pause(peer: JsonRpcPeer, params: JSONObject?) {
        V8Helper.v8MessageQueue[Protocol.Debugger.Pause] = params

        //check what's needed here
    }

    @ChromeDevtoolsMethod
    fun stepOver(peer: JsonRpcPeer, params: JSONObject?) {
        V8Helper.v8MessageQueue[Protocol.Debugger.StepOver] = params
    }

    @ChromeDevtoolsMethod
    fun stepInto(peer: JsonRpcPeer, params: JSONObject?) {
        V8Helper.v8MessageQueue[Protocol.Debugger.StepInto] = params
    }

    @ChromeDevtoolsMethod
    fun stepOut(peer: JsonRpcPeer, params: JSONObject?) {
        V8Helper.v8MessageQueue[Protocol.Debugger.StepOut] = params
    }

    @ChromeDevtoolsMethod
    fun setBreakpoint(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult? {
        //Looks like this method should not be called at all.
        val action: () -> JsonRpcResult? = {
            throw IllegalArgumentException("Unexpected Debugger.setBreakpoint() is called by Chrome DevTools: $params")
        }
        return runStethoSafely(action)
    }

    @ChromeDevtoolsMethod
    fun setBreakpointByUrl(peer: JsonRpcPeer, params: JSONObject): JsonRpcResult? {
        return runStethoAndV8Safely {
            val responseFuture = v8Executor!!.submit(Callable {
                val request = dtoMapper.convertValue(params, SetBreakpointByUrlRequest::class.java)
                val breakpointParams = JSONObject().put("lineNumber", request.lineNumber).put("url", request.scriptId).put("columnNumber", request.columnNumber)
                V8Helper.dispatchMessage(Protocol.Debugger.SetBreakpointByUrl,  breakpointParams.toString())
                SetBreakpointByUrlResponse("1:${request.lineNumber}:${request.columnNumber}:${request.scriptId}", Location(request.scriptId!!, request.lineNumber!!, request.columnNumber!!))
            })

            responseFuture.get()
        }
    }

    @ChromeDevtoolsMethod
    fun removeBreakpoint(peer: JsonRpcPeer, params: JSONObject) {
        //Chrome DevTools are removing breakpoint from UI regardless of the response (unlike setting breakpoint):
        // -> do best effort to remove breakpoint when executor is free
        runStethoAndV8Safely {
            v8Executor!!.execute {V8Helper.dispatchMessage(Protocol.Debugger.RemoveBreakpoint, params.toString())}
        }
    }

    @ChromeDevtoolsMethod
    fun setAsyncCallStackDepth(peer: JsonRpcPeer, params: JSONObject): JsonRpcResult{
        return SimpleIntegerResult(32)
    }

    @ChromeDevtoolsMethod
    fun setBreakpointsActive(peer: JsonRpcPeer, params: JSONObject){
        runStethoAndV8Safely {
            v8Executor?.execute {V8Helper.dispatchMessage(Protocol.Debugger.SetBreakpointsActive, params.toString()) }
        }
    }

    /**
     *  Safe for Stetho - makes sure that no exception is thrown.
     *  Safe for V8 - makes sure, that v8 initialized and v8 thread is not not paused in debugger.
     */
    private fun <T> runStethoSafely(action: () -> T): T? {
        LogUtils.logChromeDevToolsCalled()

        try {
            return action()
        } catch (e: Throwable) { //not Exception as V8 throws Error
            // Otherwise If error is thrown - Stetho reports broken I/O pipe and disconnects
            logger.w(TAG, "Unable to perform " + LogUtils.getChromeDevToolsMethodName(), e)

            return null
        }
    }

    /**
     * Safe for Stetho - makes sure that no exception is thrown.
     * If any exception then [JsonRpcError] is thrown from method annotated with @ChromeDevtoolsMethod-
     * Stetho reports broken I/O pipe and Chrome DevTools disconnects.
     */
    private fun <T> runStethoAndV8Safely(action: () -> T): T? {
        return runStethoSafely {
            validateV8Initialized()

            action()
        }
    }

    class EvaluateOnCallFrameResult(
        @field:JsonProperty
        @JvmField
        val result: JSONObject? = null
    ): JsonRpcResult

    /**
     * Fired as the result of [Debugger.enable]
     */
    class ScriptParsedEvent(
        @field:JsonProperty @JvmField
        val scriptId: String?,

        @field:JsonProperty @JvmField
        val url: String? = scriptIdToUrl(scriptId)
    )

    class GetScriptSourceRequest : JsonRpcResult {
        @field:JsonProperty
        @JvmField
        var scriptId: String? = null
    }

    class GetScriptSourceResponse(
        @field:JsonProperty @JvmField
        val scriptSource: String
    ) : JsonRpcResult

    class SetBreakpointByUrlRequest : JsonRpcResult {
        //script id
        @field:JsonProperty
        @JvmField
        var url: String? = null

        @field:JsonProperty
        @JvmField
        var lineNumber: Int? = null

        //unused for now
        @field:JsonProperty
        @JvmField
        var columnNumber: Int? = null

        //unused for now
        @field:JsonProperty
        @JvmField
        var condition: String? = null

        val scriptId get() = urlToScriptId(url)
    }


    class SetBreakpointByUrlResponse(
        @field:JsonProperty @JvmField
        val breakpointId: String,

        location: Location
    ) : JsonRpcResult {
        @field:JsonProperty
        @JvmField
        val locations: List<Location> = listOf(location)
    }

    class RemoveBreakpointRequest : JsonRpcResult {
        //script id
        @field:JsonProperty
        @JvmField
        var breakpointId: String? = null
    }


    data class Location(
        @field:JsonProperty @JvmField
        val scriptId: String,

        @field:JsonProperty @JvmField
        val lineNumber: Int,

        @field:JsonProperty @JvmField
        val columnNumber: Int
    )

    data class CallFrame @JvmOverloads constructor(
        @field:JsonProperty @JvmField
        val callFrameId: String,

        @field:JsonProperty @JvmField
        val functionName: String,

        @field:JsonProperty @JvmField
        val location: Location,

        /** JavaScript script name or url. */
        @field:JsonProperty @JvmField
        val url: String,

        @field:JsonProperty @JvmField
        val scopeChain: List<Scope>,

        //xxx: check how and whether it's wotking with this
        @field:JsonProperty @JvmField
        val `this`: RemoteObject? = null
    )

    data class Scope(
        /** one of: global, local, with, closure, catch, block, script, eval, module. */
        @field:JsonProperty @JvmField
        val type: String,
        /**
         * Object representing the scope.
         * For global and with scopes it represents the actual object;
         * for the rest of the scopes, it is artificial transient object enumerating scope variables as its properties.
         */
        @field:JsonProperty @JvmField
        val `object`: RemoteObject
    )
}

class SimpleIntegerResult(@JsonProperty(required = true) var result: Int) : JsonRpcResult {

}
