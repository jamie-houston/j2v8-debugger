package com.salesforce.j2v8inspector.model

import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult
import org.json.JSONArray
import org.json.JSONObject

class StethoJsonRpcResult: JSONObject, JsonRpcResult {
    constructor(): super()

    constructor(result: String): super(result)

    constructor(result: JSONObject): super() {
        this.put("result", result)
    }

    constructor(result: JSONArray): super() {
        this.put("result", result)
    }
}