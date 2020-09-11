package com.alexii.j2v8debugger

import com.alexii.j2v8debugger.model.StethoJsonRpcResult
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
class Runtime(replFactory: RuntimeReplFactory?) : BaseCdtDomain(), ChromeDevtoolsDomain {
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
        logger.d(TAG, "$method $params")

        var resultStr: String? = getV8Result(method, params)

        val jsonArray = if (resultStr.isNullOrEmpty()) JSONArray() else JSONArray(resultStr)
        return StethoJsonRpcResult(jsonArray)
    }

    /**
     * Pass through the needed [FacebookRuntimeBase] methods
     */
    @ChromeDevtoolsMethod
    fun releaseObject(peer: JsonRpcPeer?, params: JSONObject?) {
        val method = Protocol.Runtime.ReleaseObject

        logger.d(TAG, "$method $params")
        v8Messenger?.sendMessage(method, params,crossThread = true)
    }

    @ChromeDevtoolsMethod
    fun releaseObjectGroup(peer: JsonRpcPeer?, params: JSONObject?) {
        val method = Protocol.Runtime.ReleaseObjectGroup
        logger.d(TAG, "$method $params")
        v8Messenger?.sendMessage(method, params,crossThread = true)
    }

    @ChromeDevtoolsMethod
    fun callFunctionOn(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult {
        val method = Protocol.Runtime.CallFunctionOn
        logger.d(TAG, "$method $params")
        return getV8ResultAsJsonRpcResult(method, params)
    }

    @ChromeDevtoolsMethod
    fun evaluate(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult {
        logger.d(TAG, "evaluate: $params")
        var result: String? = getV8Result(Protocol.Runtime.Evaluate, params)
        return EvaluateOnCallFrameResult(JSONObject(result))
    }

    @ChromeDevtoolsMethod
    fun CompileScript(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult {
        val method = Protocol.Runtime.CallFunctionOn
        logger.d(TAG, "$method $params")
        return getV8ResultAsJsonRpcResult(method, params)
    }

    companion object {
        const val TAG = "Runtime"
    }
}
