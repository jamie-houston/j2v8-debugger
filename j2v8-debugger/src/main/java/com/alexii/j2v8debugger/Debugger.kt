package com.alexii.j2v8debugger

import com.alexii.j2v8debugger.model.CdpMethod
import com.alexii.j2v8debugger.model.GetScriptSourceRequest
import com.alexii.j2v8debugger.model.GetScriptSourceResponse
import com.alexii.j2v8debugger.model.ScriptParsedEvent
import com.alexii.j2v8debugger.model.SetBreakpointByUrlRequest
import com.alexii.j2v8debugger.model.SetBreakpointByUrlResponse
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult
import com.facebook.stetho.inspector.network.NetworkPeerManager
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod
import com.facebook.stetho.json.ObjectMapper
import com.alexii.j2v8debugger.utils.LogUtils
import com.alexii.j2v8debugger.utils.logger
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain
import org.json.JSONObject
import java.util.concurrent.ExecutorService

/**
 * Debugger Domain. Name of the class and methods must match names defined in Chrome Dev Tools protocol.
 */
internal class Debugger(
    private val scriptSourceProvider: ScriptSourceProvider
) : BaseCdtDomain(), ChromeDevtoolsDomain {
    var dtoMapper: ObjectMapper = ObjectMapper()

    /**
     * Needed as @ChromeDevtoolsMethod methods are called on Stetho threads, but not v8 thread.
     *
     * XXX: consider using ThreadBound from Facebook with an implementation, which uses Executor.
     */
    private var v8Executor: ExecutorService? = null
    private var connectedPeer: JsonRpcPeer? = null
    private val breakpointsAdded = mutableListOf<String>()

    fun initialize(v8Executor: ExecutorService, v8Messenger: V8Messenger) {
        this.v8Executor = v8Executor
        initialize(v8Messenger)
    }

    private fun validateV8Initialized() {
        if (v8Executor == null) {
            throw IllegalStateException("Unable to call method before v8 has been initialized")
        }
    }

    internal fun onScriptsChanged() {
        scriptSourceProvider.allScriptIds
            .map { ScriptParsedEvent(it) }
            .forEach { connectedPeer?.invokeMethod(CdpMethod.Debugger.ScriptParsed, it, null) }
    }

    @ChromeDevtoolsMethod
    fun enable(peer: JsonRpcPeer, params: JSONObject?) {
        runStethoSafely {
            connectedPeer = peer

            // Notify DevTools of scripts we want to display/debug
            onScriptsChanged()

            peer.registerDisconnectReceiver(::onDisconnect)
        }
        v8Messenger?.setDebuggerConnected(true)
    }

    private fun onDisconnect() {
        logger.d(TAG, "Disconnecting from Chrome")
        runStethoSafely {
            // Remove added breakpoints
            breakpointsAdded.forEach { breakpointId ->
                v8Executor?.execute {
                    v8Messenger?.sendMessage(
                        method = CdpMethod.Debugger.RemoveBreakpoint,
                        params = JSONObject().put("breakpointId", breakpointId),
                        crossThread = false
                    )
                }
            }
            breakpointsAdded.clear()

            NetworkPeerManager.getInstanceOrNull()?.removePeer(connectedPeer)
            connectedPeer = null

            // avoid app being freezed when no debugging happening anymore
            v8Messenger?.setDebuggerConnected(false)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @ChromeDevtoolsMethod
    fun evaluateOnCallFrame(peer: JsonRpcPeer, params: JSONObject?) =
        getV8ResultAsJsonRpcResult(CdpMethod.Debugger.EvaluateOnCallFrame, params)

    @Suppress("UNUSED_PARAMETER")
    @ChromeDevtoolsMethod
    fun getPossibleBreakpoints(peer: JsonRpcPeer, params: JSONObject?) =
        getV8ResultAsJsonRpcResult(CdpMethod.Debugger.GetPossibleBreakpoints, params)

    @Suppress("UNUSED_PARAMETER")
    @ChromeDevtoolsMethod
    fun searchInContent(peer: JsonRpcPeer, params: JSONObject?) =
        getV8ResultAsJsonRpcResult(CdpMethod.Debugger.SearchInContent, params)

//    @Suppress("UNUSED_PARAMETER")
//    @ChromeDevtoolsMethod
//    fun getFunctionDetails(peer: JsonRpcPeer, params: JSONObject?) =
//        getV8ResultAsJsonRpcResult(CdpMethod.Debugger.GetFunctionDetails, params)

    @Suppress("UNUSED_PARAMETER")
    @ChromeDevtoolsMethod
    fun setSkipAllPauses(peer: JsonRpcPeer, params: JSONObject?) =
    // This was changed from skipped to skip
        // https://chromium.googlesource.com/chromium/src/third_party/WebKit/Source/platform/v8_inspector/+/e7a781c04b7822a46e7de465623152ff1b45bdac%5E%21/
        sendMessage(
            CdpMethod.Debugger.SetSkipAllPauses,
            JSONObject().put("skip", params?.getBoolean("skipped")),
            crossThread = true,
            runOnlyWhenPaused = true
        )


    @Suppress("UNUSED_PARAMETER")
    @ChromeDevtoolsMethod
    fun disable(peer: JsonRpcPeer, params: JSONObject?) {
        v8Messenger?.setDebuggerConnected(false)
    }

    @Suppress("UNUSED_PARAMETER")
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

    @Suppress("UNUSED_PARAMETER")
    @ChromeDevtoolsMethod
    fun setBreakpointByUrl(peer: JsonRpcPeer, params: JSONObject): SetBreakpointByUrlResponse? {
        val request = dtoMapper.convertValue(params, SetBreakpointByUrlRequest::class.java)
        request.url = request.scriptId
        runStethoAndV8Safely {
            v8Executor?.execute {
                sendMessage(
                    CdpMethod.Debugger.SetBreakpointByUrl,
                    dtoMapper.convertValue(request, JSONObject::class.java),
                    crossThread = false
                )
            }
        }
        val response = SetBreakpointByUrlResponse(request)
        // Save breakpoint to remove on disconnect
        breakpointsAdded.add(response.breakpointId)
        return response
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @ChromeDevtoolsMethod
    fun removeBreakpoint(peer: JsonRpcPeer, params: JSONObject) {
        sendMessage(CdpMethod.Debugger.RemoveBreakpoint, params, crossThread = true)
        breakpointsAdded.remove(params.getString("breakpointId"))
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @ChromeDevtoolsMethod
    fun setBreakpointsActive(peer: JsonRpcPeer, params: JSONObject) =
        sendMessage(CdpMethod.Debugger.SetBreakpointsActive, params, crossThread = true)

    /**
     * Pass through to J2V8 methods
     */
    @Suppress("unused", "UNUSED_PARAMETER")
    @ChromeDevtoolsMethod
    fun resume(peer: JsonRpcPeer, params: JSONObject?) = sendMessage(
        CdpMethod.Debugger.Resume,
        params,
        crossThread = true,
        runOnlyWhenPaused = true
    )


    @Suppress("unused", "UNUSED_PARAMETER")
    @ChromeDevtoolsMethod
    fun pause(peer: JsonRpcPeer, params: JSONObject?) =
        sendMessage(
            CdpMethod.Debugger.Pause,
            params,
            crossThread = true,
            runOnlyWhenPaused = true
        )


    @Suppress("unused", "UNUSED_PARAMETER")
    @ChromeDevtoolsMethod
    fun stepOver(peer: JsonRpcPeer, params: JSONObject?) =
        sendMessage(
            CdpMethod.Debugger.StepOver,
            params,
            crossThread = true,
            runOnlyWhenPaused = true
        )


    @Suppress("unused", "UNUSED_PARAMETER")
    @ChromeDevtoolsMethod
    fun stepInto(peer: JsonRpcPeer, params: JSONObject?) =
        sendMessage(
            CdpMethod.Debugger.StepInto, params,
            crossThread = true, runOnlyWhenPaused = true
        )


    @Suppress("unused", "UNUSED_PARAMETER")
    @ChromeDevtoolsMethod
    fun stepOut(peer: JsonRpcPeer, params: JSONObject?) =
        sendMessage(
            CdpMethod.Debugger.StepOut,
            params,
            crossThread = true,
            runOnlyWhenPaused = true
        )


    @Suppress("unused", "UNUSED_PARAMETER")
    @ChromeDevtoolsMethod
    fun setOverlayMessage(peer: JsonRpcPeer, params: JSONObject?) {
        // developer tool seems still send this event to show a overlay message, v8 doesn't have it.
        // do nothing here
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @ChromeDevtoolsMethod
    fun setPauseOnExceptions(peer: JsonRpcPeer, params: JSONObject?) =
        sendMessage(CdpMethod.Debugger.SetPauseOnExceptions, params, crossThread = true)


    @Suppress("unused", "UNUSED_PARAMETER")
    @ChromeDevtoolsMethod
    fun continueToLocation(peer: JsonRpcPeer, params: JSONObject?) = sendMessage(
        CdpMethod.Debugger.ContinueToLocation, params, crossThread = true, runOnlyWhenPaused = true
    )


    @Suppress("unused", "UNUSED_PARAMETER")
    @ChromeDevtoolsMethod
    fun setVariableValue(peer: JsonRpcPeer, params: JSONObject?) = sendMessage(
        CdpMethod.Debugger.SetVariableValue, params, crossThread = true, runOnlyWhenPaused = true
    )

    /**
     *  Safe for Stetho - makes sure that no exception is thrown.
     * If any exception then [JsonRpcError] is thrown from method annotated with @ChromeDevtoolsMethod-
     * Stetho reports broken I/O pipe and Chrome DevTools disconnects.
     */
    private fun <T> runStethoSafely(action: () -> T): T? {
        LogUtils.logChromeDevToolsCalled()

        return try {
            action()
        } catch (e: Throwable) { //not Exception as V8 throws Error
            // Otherwise If error is thrown - Stetho reports broken I/O pipe and disconnects
            logger.w(TAG, "Unable to perform " + LogUtils.getChromeDevToolsMethodName(), e)
            null
        }
    }

    /**
     * Safe for Stetho - makes sure that no exception is thrown.
     * Safe for V8 - makes sure, that v8 initialized and v8 thread is not not paused in debugger.
     */
    private fun <T> runStethoAndV8Safely(action: () -> T): T? {
        return runStethoSafely {
            validateV8Initialized()

            action()
        }
    }

    companion object {
        const val TAG = "Debugger"
    }
}
