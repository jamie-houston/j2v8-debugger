package com.alexii.j2v8debugger.model

import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult
import com.facebook.stetho.json.annotation.JsonProperty
import org.json.JSONObject

/**
 * Any response from J2V8
 * If it contains an id, it's a response from a request sent
 * Otherwise it's an event
 */
internal class V8Response : JsonRpcResult {
    val isResponse by lazy { (id != null) }

    @field:JsonProperty
    @JvmField
    var id: Int? = null

    @field:JsonProperty
    @JvmField
    var method: String? = null

    @field:JsonProperty
    @JvmField
    var result: JSONObject? = null

    @field:JsonProperty
    @JvmField
    var error: JSONObject? = null


    @field:JsonProperty
    @JvmField
    var params: JSONObject? = null
}

internal class V8Error