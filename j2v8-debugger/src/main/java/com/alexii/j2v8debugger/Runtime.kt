package com.alexii.j2v8debugger

import com.alexii.j2v8debugger.utils.logger
import com.facebook.stetho.inspector.console.RuntimeReplFactory
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod
import com.facebook.stetho.json.ObjectMapper
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Callable
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
        logger.d(TAG, "getProperties $params")

        val method = Protocol.Runtime.GetProperties

        var result: String? = v8Messenger?.getV8Result(method, params)

        val jsonArray = if (result.isNullOrEmpty()) JSONArray() else JSONArray(result)
        val jsonResult = GetPropertiesResult().put("result", jsonArray)

        return jsonResult as JsonRpcResult
    }

    /**
     * Pass through the needed [FacebookRuntimeBase] methods
     */
    @ChromeDevtoolsMethod
    fun releaseObject(peer: JsonRpcPeer?, params: JSONObject?) = adaptee.releaseObject(peer, params)

    @ChromeDevtoolsMethod
    fun releaseObjectGroup(peer: JsonRpcPeer?, params: JSONObject?) =
        adaptee.releaseObjectGroup(peer, params)

    @ChromeDevtoolsMethod
    fun callFunctionOn(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult? =
        adaptee.callFunctionOn(peer, params)

    @ChromeDevtoolsMethod
    fun evaluate(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult {
        val request = dtoMapper.convertValue(params, EvaluateRequest::class.java)

        if (!request.objectGroup.equals("console")) {
            return EvaluateOnCallFrameResult(JSONObject("{'wasThrown': true, 'exceptionDetails': 'Not supported by FAB'}"))
        }

        logger.d(TAG, "evaluate: $params")
        val result: String? = v8Messenger?.getV8Result(Protocol.Runtime.Evaluate, params)
        logger.d(TAG, "result: $result")
        return EvaluateOnCallFrameResult(JSONObject(result))
    }

    @ChromeDevtoolsMethod
    fun compileScript(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult? {
        return adaptee.callFunctionOn(peer, params)
    }

    companion object {
        const val TAG = "Runtime"
    }
}
