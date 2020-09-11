package com.alexii.j2v8debugger

import com.alexii.j2v8debugger.model.StethoJsonRpcResult
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult
import org.json.JSONObject

open class BaseCdtDomain {

    protected var v8Messenger: V8Messenger? = null

    fun getV8Result(method: String, params: JSONObject?): String? {
        return v8Messenger?.getV8Result(method, params)
    }

    fun getV8ResultAsJsonRpcResult(method: String, params: JSONObject?): JsonRpcResult {
        var result: String? = getV8Result(method, params)
        return if (result.isNullOrEmpty()) StethoJsonRpcResult() else StethoJsonRpcResult(result)
    }
}