package com.alexii.j2v8debugger.model

import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult
import org.json.JSONArray
import org.json.JSONObject

class StethoJsonRpcResult: JSONObject, JsonRpcResult {
    constructor(): super()

    constructor(result: String): super() {
        this.put("result", JSONObject(result))
    }

    constructor(result: JSONObject): super() {
        this.put("result", result)
    }

    constructor(result: JSONArray): super() {
        this.put("result", result)
    }
}