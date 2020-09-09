package com.alexii.j2v8debugger

import com.alexii.j2v8debugger.utils.LogUtils
import com.alexii.j2v8debugger.utils.logger
import com.facebook.stetho.inspector.console.RuntimeReplFactory
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod
import com.facebook.stetho.json.ObjectMapper
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import com.facebook.stetho.inspector.protocol.module.Runtime as FacebookRuntimeBase

/**
 * Runtime Domain. Name of the class and methods must match names defined in Chrome Dev Tools protocol.
 */
@Suppress("UNUSED_PARAMETER", "unused")
class Runtime(replFactory: RuntimeReplFactory?) : ChromeDevtoolsDomain {
    private var v8Messenger: V8Messenger? = null
    private val adaptee = FacebookRuntimeBase(replFactory)
    private var v8Executor: ExecutorService? = null
    var dtoMapper: ObjectMapper = ObjectMapper()

    fun initialize(v8Messenger: V8Messenger, v8Executor: ExecutorService) {
        this.v8Messenger = v8Messenger
        this.v8Executor = v8Executor
    }

    @ChromeDevtoolsMethod
    fun getProperties(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult {
        val method = Protocol.Runtime.GetProperties
        var result: String? = ""

        if (v8Messenger?.isDebuggerPaused == true) {
            result = v8Messenger?.getV8Result(method, params)
        } else {
            v8Executor?.execute {
                result = v8Messenger?.getV8Result(method, params)
            }
        }

        val jsonResult = GetPropertiesResult().put("result", JSONArray(result))
        return jsonResult as JsonRpcResult
    }

    /**
     * Pass through the needed [FacebookRuntimeBase] methods
     */
    @ChromeDevtoolsMethod
    fun releaseObject(peer: JsonRpcPeer?, params: JSONObject?) = adaptee.releaseObject(peer, params)

    @ChromeDevtoolsMethod
    fun releaseObjectGroup(peer: JsonRpcPeer?, params: JSONObject?) = adaptee.releaseObjectGroup(peer, params)

    @ChromeDevtoolsMethod
    fun callFunctionOn(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult? = adaptee.callFunctionOn(peer, params)

    @ChromeDevtoolsMethod
    fun evaluate(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult? {
        return runStethoAndV8Safely {
            try {
                val method = Protocol.Runtime.Evaluate
                val request = dtoMapper.convertValue(params, EvaluateRequest::class.java)

                if (!request.objectGroup.equals("console")) {
                    println("Runtime.evaluate 1")
                    EvaluateOnCallFrameResult(JSONObject("{'wasThrown': true, 'exceptionDetails': 'Not supported by FAB'}"))
                }
                var result: String? = null
                println("Runtime.evaluate 2")
//                v8Executor?.execute {
                    println("Runtime.evaluate 3")
                    result = v8Messenger?.getV8Result(method, params)
                    println("Runtime.evaluate 4. result: $result")
//                }
                println("Runtime.evaluate 5. result: $result")
                EvaluateOnCallFrameResult(JSONObject(result))

            } catch (e: Exception) {
                // Send exception as source code for debugging.
                // Otherwise If error is thrown - Stetho reports broken I/O pipe and disconnects
                println("Runtime.evaluate 6. error: ${e.message}")
                EvaluateOnCallFrameResult(JSONObject("{'wasThrown': true, 'exceptionDetails': '${logger.getStackTraceString(e)}'}"))
            }
        }
    }

    @ChromeDevtoolsMethod
    fun compileScript(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult? {
        return adaptee.callFunctionOn(peer, params)
    }
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

    private fun validateV8Initialized() {
        if (v8Executor == null) {
            throw IllegalStateException("Unable to call method before v8 has been initialized")
        }
    }

    companion object {
        const val TAG = "Runtime"
    }
}
