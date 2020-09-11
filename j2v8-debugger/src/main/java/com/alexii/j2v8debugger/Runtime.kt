package com.alexii.j2v8debugger

import com.alexii.j2v8debugger.model.EvaluateOnCallFrameResult
import com.alexii.j2v8debugger.model.CdpMethod
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
class Runtime : BaseCdtDomain(), ChromeDevtoolsDomain {
    var dtoMapper: ObjectMapper = ObjectMapper()

    @ChromeDevtoolsMethod
    fun getProperties(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult {
        val method = CdpMethod.Runtime.GetProperties
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
        val method = CdpMethod.Runtime.ReleaseObject

        logger.d(TAG, "$method $params")
        v8Messenger?.sendMessage(method, params,crossThread = true)
    }

    @ChromeDevtoolsMethod
    fun releaseObjectGroup(peer: JsonRpcPeer?, params: JSONObject?) {
        val method = CdpMethod.Runtime.ReleaseObjectGroup
        logger.d(TAG, "$method $params")
        v8Messenger?.sendMessage(method, params,crossThread = true)
    }

    @ChromeDevtoolsMethod
    fun callFunctionOn(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult {
        val method = CdpMethod.Runtime.CallFunctionOn
        logger.d(TAG, "$method $params")
        return getV8ResultAsJsonRpcResult(method, params)
    }

    @ChromeDevtoolsMethod
    fun evaluate(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult {
        val method = CdpMethod.Runtime.Evaluate
        logger.d(TAG, "$method $params")
        return getV8ResultAsJsonRpcResult(method, params)
    }

    @ChromeDevtoolsMethod
    fun compileScript(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult {
        val method = CdpMethod.Runtime.CompileScript
        logger.d(TAG, "$method $params")
        return getV8ResultAsJsonRpcResult(method, params)
    }

    @ChromeDevtoolsMethod
    fun awaitPromise(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult {
        val method = CdpMethod.Runtime.AwaitPromise
        logger.d(TAG, "$method $params")
        return getV8ResultAsJsonRpcResult(method, params)
    }

    @ChromeDevtoolsMethod
    fun runScript(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult {
        val method = CdpMethod.Runtime.RunScript
        logger.d(TAG, "$method $params")
        return getV8ResultAsJsonRpcResult(method, params)
    }

    @ChromeDevtoolsMethod
    fun queryObjects(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult {
        val method = CdpMethod.Runtime.QueryObjects
        logger.d(TAG, "$method $params")
        return getV8ResultAsJsonRpcResult(method, params)
    }

    companion object {
        const val TAG = "Runtime"
    }
}
