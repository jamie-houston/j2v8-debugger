package com.alexii.j2v8debugger

import com.alexii.j2v8debugger.model.StethoJsonRpcResult
import com.alexii.j2v8debugger.utils.logger
import com.facebook.stetho.inspector.jsonrpc.JsonRpcException
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult
import com.facebook.stetho.inspector.jsonrpc.protocol.JsonRpcError
import org.json.JSONObject

open class BaseCdtDomain {

    protected var v8Messenger: V8Messenger? = null

    fun initialize(v8Messenger: V8Messenger) {
        this.v8Messenger = v8Messenger
    }

    fun getV8ResultAsJsonRpcResult(method: String, params: JSONObject?): JsonRpcResult {
        logger.d(Runtime.TAG, "$method $params")
        var resultStr = getV8Result(method, params)
        return if (resultStr.isNullOrEmpty()) StethoJsonRpcResult() else StethoJsonRpcResult(resultStr)
    }

    fun sendMessage(
        method: String,
        params: JSONObject? = null,
        crossThread: Boolean,
        runOnlyWhenPaused: Boolean = false
    ) {
        logger.d(Runtime.TAG, "$method $params")
        v8Messenger?.sendMessage(method, params, crossThread, runOnlyWhenPaused)
    }

    private fun getV8Result(method: String, params: JSONObject?): String? {
        return v8Messenger?.getV8Result(method, params)
    }
}
