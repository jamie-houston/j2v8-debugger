package com.alexii.j2v8debugger

import android.util.Log
import com.alexii.j2v8debugger.V8Helper.releaseV8Debugger
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.debug.DebugHandler.DEBUG_OBJECT_NAME
import com.eclipsesource.v8.inspector.DebuggerConnectionListener
import com.eclipsesource.v8.inspector.V8Inspector
import com.eclipsesource.v8.inspector.V8InspectorDelegate
import com.facebook.stetho.inspector.network.NetworkPeerManager
import org.json.JSONObject
import java.lang.reflect.Field
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

typealias MessageCallback = (JSONObject) -> Unit
/**
 * Debug-related utility functionality for [V8]
 */
object V8Helper {
    private var v8Inspector: V8Inspector? = null
    val nextDispatchId = AtomicInteger(0)

    val chromeMessageQueue = mutableMapOf<String, JSONObject>()
    val v8MessageQueue = mutableMapOf<String, JSONObject>()

    /**
     * Enables V8 debugging. All new runtimes will be created with debugging enabled.
     *
     * Must be enabled before the v8 runtime is created.
     *
     * @see com.eclipsesource.v8.debug.V8DebugServer.configureV8ForDebugging
     * @see com.eclipsesource.v8.debug.DebugHandler
     */
    private fun enableDebugging() {
        V8.setFlags("-expose-debug-as=$DEBUG_OBJECT_NAME")
    }

    /**
     * @return wither debugging was enabled for V8.
     *
     * The condition is necessary, but not sufficient: v8 might be created before flags are set.
     */
    private val isDebuggingEnabled : Boolean
        get() {
            val v8FlagsField: Field = V8::class.java.getDeclaredField("v8Flags")
            v8FlagsField.isAccessible = true
            val v8FlagsValue = v8FlagsField.get(null) as String?

            return v8FlagsValue != null && v8FlagsValue.contains(DEBUG_OBJECT_NAME)
        }

    fun dispatchMessage(method: String, params: String? = null){
        val message = "{\"id\":${nextDispatchId.incrementAndGet()},\"method\":\"$method\", \"params\": $params}"
        Log.i("V8Helper", "dispatching $message")
        v8Inspector?.dispatchProtocolMessage(message)
    }

    val debugV8InspectorDelegate = object: V8InspectorDelegate{
        var initialBreak = true

        override fun waitFrontendMessageOnPause() {
            if (v8MessageQueue.any()) {
                for ((k,v) in v8MessageQueue){
                    Log.i("V8Helper", "*** sending chrome $k with $v")
                    dispatchMessage(k, v.toString())
                }
                v8MessageQueue.clear()
            }
            if (chromeMessageQueue.any()) {
                val networkPeerManager = NetworkPeerManager.getInstanceOrNull()
                for ((k,v) in chromeMessageQueue){
                    Log.i("V8Helper", "*** sending $k with $v")
                    networkPeerManager?.sendNotificationToPeers(k, v)
                }
                chromeMessageQueue.clear()
            }
//            if (initialBreak){
//                dispatchMessage("Debugger.resume")
//                initialBreak = false
//            }
//            Log.i("V8Helper", "*** waitFrontendMessageOnPause")

            // resume Debugger
//            v8Inspector?.dispatchProtocolMessage("{\"id\":9,\"method\":\"Runtime.runIfWaitingForDebugger\"}")
//            dispatchMessage("Debugger.resume")
//            dispatchMessage("Debugger.stepOver")
//            v8Inspector?.dispatchProtocolMessage("{\"id\":${dispatchId.incrementAndGet()},\"method\":\"Debugger.resume\"}")
        }

        override fun onResponse(p0: String?) {
            Log.i("V8Helper", "*** onResponse $p0")
            inspectorResponse = p0
            val message = JSONObject(p0)
            if (message.has("id")) {
                // This is a command response
            } else if (message.has("method")) {
                val params = message.optJSONObject("params")
                // This is an event
                val responseMethod = message.optString("method")
                if (responseMethod == "Debugger.scriptParsed") {
//                    dispatchMessage("Debugger.getPossibleBreakpoints", "{\"start\": {\"scriptId\": \"${params.optString("scriptId")}\", \"lineNumber\": 0, \"columnNumber\": 0}, \"end\": {\"scriptId\": \"${params.optString("scriptId")}\", \"lineNumber\": 10, \"columnNumber\": 0}}")
                    dispatchMessage("Debugger.getScriptSource", "{\"scriptId\": \"${params.get("scriptId")}\"}")
                    if (params.optString("url").isNotEmpty()) {
                        scriptId = params.optString("scriptId")
//                        dispatchMessage("Debugger.getScriptSource", "{\"scriptId\": \"$scriptId\"}")
//                        dispatchMessage("Debugger.getPossibleBreakpoints", "{\"start\": {\"scriptId\": \"$scriptId\", \"lineNumber\": 0, \"columnNumber\": 0}, \"end\": {\"scriptId\": \"$scriptId\", \"lineNumber\": 10, \"columnNumber\": 0}}")
                    }
                } else if (responseMethod == "Debugger.breakpointResolved"){

                    val location = params.getJSONObject("location")
                    location.put("scriptId", "hello-world")
//                    val response = JSONObject().put("breakpointId", params.getString("breakpointId").replace("hello-world", scriptIdToUrl("hello-world"))
//                    ).put("location", location)
                    val response = JSONObject().put("breakpointId", params.getString("breakpointId")).put("location", location)
                    val networkPeerManager = NetworkPeerManager.getInstanceOrNull()
                    Log.i("V8Helper", "*** breakpoint resolved with $response")
                    chromeMessageQueue[responseMethod] = response
//                    networkPeerManager?.sendNotificationToPeers(responseMethod, response)

                } else if (responseMethod == "Debugger.paused") {
                    val updatedScript = params.toString().replace("\"$scriptId\"", "\"hello-world\"")
//                    val networkPeerManager = NetworkPeerManager.getInstanceOrNull()
                    Log.i("V8Helper", "*** debugger.paused $updatedScript")
//                    networkPeerManager?.sendNotificationToPeers(responseMethod, JSONObject(updatedScript))
                    chromeMessageQueue[responseMethod] = JSONObject(updatedScript)
                }
//                dispatchMessage(message.optString("method"), message.optString("params"))
//                if (responseMethod.isNotEmpty() && responseMethod != "Debugger.scriptParsed") {
//                }
//                if (responseMethod == "Debugger.paused") {
//                    var dtoMapper = ObjectMapper()
//                val request = dtoMapper.convertValue(message.optJSONObject("params"), Debugger.PausedEvent::class.java)
//                message.put("id", 200)
//                v8Inspector?.dispatchProtocolMessage(message.toString())
//                v8Inspector?.dispatchProtocolMessage("{\"id\":${dispatchId.incrementAndGet()},\"method\":\"Debugger.resume\"}")
//                val networkPeerManager = NetworkPeerManager.getInstanceOrNull()
//                val pausedEvent = Debugger.PausedEvent(frames)

//                logger.w(Debugger.TAG, "Sending Debugger.paused: $pausedEvent")

//                networkPeerManager.sendNotificationToPeers("Debugger.paused", pausedEvent)
//                }
            }
        }
    }

    var scriptId: String? = null

    private val debuggerConnectionListener = object: DebuggerConnectionListener{
        override fun onDebuggerDisconnected() {
            Log.i("V8Helper", "*** onDebuggerDisconnected")
        }

        override fun onDebuggerConnected() {
            Log.i("V8Helper", "*** onDebuggerConnected")
        }
    }

    private var inspectorResponse: String? = null

    /**
     * @return new or existing v8 debugger object.
     * Must be released before [V8.release] is called.
     */
//    fun getOrCreateV8Debugger(v8: V8): DebugHandler {
    fun getOrCreateV8Debugger(v8: V8) : V8Inspector {
        if (v8Inspector == null) {
            if (!isDebuggingEnabled) {
                throw IllegalStateException("V8 Debugging is not enabled. "
                    + "Call V8Helper.enableV8Debugging() before creation of V8 runtime!")
            }

            v8Inspector = V8Inspector.createV8Inspector(v8, debugV8InspectorDelegate, "test")
            v8Inspector?.addDebuggerConnectionListener(debuggerConnectionListener)

        }
        return v8Inspector!!
    }

    fun releaseV8Debugger() {
//        v8Debugger?.release()
//        v8Debugger = null
    }

    /**
     * Utility, which simplifies configuring V8 for debugging support and creation of new instance.
     * Creates V8 runtime, v8 debugger and binds it to Stetho.
     * For releasing resources [releaseDebuggable] should be used.
     *
     * @param v8Executor sigle-thread executor where v8 will be created
     *  and all debug calls will be performed by Stetho later.
     *
     * NOTE: Should be declared as V8 class extensions when will be allowed (https://youtrack.jetbrains.com/issue/KT-11968)
     */
    @JvmStatic
    fun createDebuggableV8Runtime(v8Executor: ExecutorService): Future<V8> {
        enableDebugging()

        val v8Future: Future<V8> = v8Executor.submit(Callable {
            val runtime = V8.createV8Runtime()
            val inspector = getOrCreateV8Debugger(runtime)

            // Default Chrome DevTool protocol messages
            dispatchMessage("Runtime.enable")
            dispatchMessage("Debugger.enable", "{\"maxScriptsCacheSize\":10000000}")
            dispatchMessage("Debugger.setPauseOnExceptions", "{\"state\": \"none\"}")
            dispatchMessage("Debugger.setAsyncCallStackDepth", "{\"maxDepth\":32}")
            // Target Doamin?  V8 S/B only target, correct?
            // Necessary?
            dispatchMessage("Runtime.getIsolateId")
            dispatchMessage("Debugger.setBlackboxPatterns","{\"patterns\":[]}")

            dispatchMessage("Runtime.runIfWaitingForDebugger")

//            dispatchMessage("Debugger.setBreakpointsActive", "{\"active\": false}")
//            inspector.dispatchProtocolMessage("{\"id\":${dispatchId.incrementAndGet()},\"method\":\"Runtime.runIfWaitingForDebugger\"}");

//            runtime.schedulePauseOnNextStatement(inspector)

//            StethoHelper.initializeWithV8Debugger(v8Debugger, v8Executor)
            StethoHelper.initializeWithV8Debugger(inspector, v8Executor)

            runtime
        })

        return v8Future;
    }

}


/**
 * Releases V8 and V8 debugger if any was created.
 *
 * Must be called on V8's thread becuase of the J2V8 limitations.
 *
 * @see V8.release
 * @see releaseV8Debugger
 */
@JvmOverloads
fun V8.releaseDebuggable(reportMemoryLeaks: Boolean = true) {
    V8Helper.releaseV8Debugger()
    this.release(reportMemoryLeaks)
}
