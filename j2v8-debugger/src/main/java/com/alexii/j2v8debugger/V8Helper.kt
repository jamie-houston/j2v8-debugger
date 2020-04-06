package com.alexii.j2v8debugger

import android.util.Log
import com.alexii.j2v8debugger.V8Helper.releaseV8Debugger
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.debug.DebugHandler
import com.eclipsesource.v8.debug.DebugHandler.DEBUG_OBJECT_NAME
import com.eclipsesource.v8.inspector.DebuggerConnectionListener
import com.eclipsesource.v8.inspector.V8Inspector
import com.eclipsesource.v8.inspector.V8InspectorDelegate
import org.json.JSONObject
import java.lang.reflect.Field
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

/**
 * Debug-related utility functionality for [V8]
 */
object V8Helper {
//    private var v8Debugger: DebugHandler? = null
    private var v8Inspector: V8Inspector? = null

    /**
     * Enables V8 debugging. All new runtimes will be created with debugging enabled.
     *
     * Must be enabled before the v8 runtime is created.
     *
     * @see com.eclipsesource.v8.debug.V8DebugServer.configureV8ForDebugging
     * @see com.eclipsesource.v8.debug.DebugHandler
     */
    fun enableDebugging() {
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

    private val debugV8InspectorDelegate = object: V8InspectorDelegate{
        override fun waitFrontendMessageOnPause() {
            Log.i("V8Helper", "*** waitFrontendMessageOnPause")

            // resume Debugger
//            v8Inspector?.dispatchProtocolMessage("{\"id\":9,\"method\":\"Runtime.runIfWaitingForDebugger\"}")
            v8Inspector?.dispatchProtocolMessage("{\"id\":9,\"method\":\"Debugger.resume\"}")
        }

        override fun onResponse(p0: String?) {
            Log.i("V8Helper", "*** onResponse $p0")
            inspectorResponse = p0
            val message = JSONObject(p0)
            if (message.optString("method") == "Debugger.paused") {
//                message.put("id", 200)
//                v8Inspector?.dispatchProtocolMessage(message.toString())
                v8Inspector?.dispatchProtocolMessage("{\"id\":200,\"method\":\"Debugger.resume\"}")
            }
            if (message.optString("method") == "Debugger.scriptParsed"){
                scriptId = message.getJSONObject("params").getString("scriptId")
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
//            inspector.dispatchProtocolMessage("{\"id\":1,\"method\":\"Profiler.enable\"}")
            inspector.dispatchProtocolMessage("{\"id\":2,\"method\":\"Runtime.enable\"}")
            inspector.dispatchProtocolMessage("{\"id\":3,\"method\":\"Debugger.enable\",\"params\":{\"maxScriptsCacheSize\":10000000}}")
//            inspector.dispatchProtocolMessage("{\"id\":4,\"method\":\"Debugger.setPauseOnExceptions\",\"params\":{\"state\":\"uncaught\"}}")
//            inspector.dispatchProtocolMessage("{\"id\":5,\"method\":\"Debugger.setAsyncCallStackDepth\",\"params\":{\"maxDepth\":32}}");
//            inspector.dispatchProtocolMessage("{\"id\":6,\"method\":\"Runtime.getIsolateId\"}");
//            inspector.dispatchProtocolMessage("{\"id\":7,\"method\":\"Debugger.setBlackboxPatterns\",\"params\":{\"patterns\":[]}}");
            inspector.dispatchProtocolMessage("{\"id\":8,\"method\":\"Runtime.runIfWaitingForDebugger\"}");

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
