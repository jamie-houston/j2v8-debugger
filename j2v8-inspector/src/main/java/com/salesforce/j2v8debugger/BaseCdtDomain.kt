package com.salesforce.j2v8inspector

import com.salesforce.j2v8inspector.model.StethoJsonRpcResult
import com.salesforce.j2v8inspector.utils.logger
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult
import org.json.JSONObject

open class BaseCdtDomain {

    protected var v8Messenger: V8Messenger? = null

    fun initialize(v8Messenger: V8Messenger) {
        this.v8Messenger = v8Messenger
    }

    fun getV8ResultAsJsonRpcResult(method: String, params: JSONObject?): JsonRpcResult {
        logger.d(TAG, "$method $params")
        val resultStr = getV8Result(method, params)
        return if (resultStr.isNullOrEmpty()) StethoJsonRpcResult() else StethoJsonRpcResult(resultStr)
    }

    fun sendMessage(
        method: String,
        params: JSONObject? = null,
        crossThread: Boolean,
        runOnlyWhenPaused: Boolean = false
    ) {
        logger.d(TAG, "$method $params")
        v8Messenger?.sendMessage(method, params, crossThread, runOnlyWhenPaused)
    }

    private fun getV8Result(method: String, params: JSONObject?): String? {
        return v8Messenger?.getV8Result(method, params)
    }

    companion object {
        const val TAG = "CdtDomain"
    }
}