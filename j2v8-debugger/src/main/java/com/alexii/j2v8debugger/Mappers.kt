package com.alexii.j2v8debugger

import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult
import com.facebook.stetho.json.annotation.JsonProperty
import org.json.JSONObject

class EvaluateOnCallFrameResult(
    @field:JsonProperty
    @JvmField
    val result: JSONObject? = null
) : JsonRpcResult

/**
 * Fired as the result of [Debugger.enable]
 */
class ScriptParsedEvent(
    @field:JsonProperty @JvmField
    val scriptId: String,

    @field:JsonProperty @JvmField
    val url: String = scriptIdToUrl(scriptId)
)

class ScriptParsedEventRequest : JsonRpcResult{
    @field:JsonProperty
    @JvmField
    var scriptId: String = ""

    @field:JsonProperty
    @JvmField
    var url: String = ""
}

class GetScriptSourceRequest : JsonRpcResult {
    @field:JsonProperty
    @JvmField
    var scriptId: String? = null
}

class GetScriptSourceResponse(
    @field:JsonProperty @JvmField
    val scriptSource: String
) : JsonRpcResult

class SetBreakpointByUrlRequest : JsonRpcResult {
    @field:JsonProperty
    @JvmField
    var url: String? = null

    @field:JsonProperty
    @JvmField
    var lineNumber: Int? = null

    //unused for now
    @field:JsonProperty
    @JvmField
    var columnNumber: Int? = null

    //unused for now
    @field:JsonProperty
    @JvmField
    var condition: String? = null

    val scriptId get() = urlToScriptId(url)
}

class SetBreakpointByUrlResponse(
    request: SetBreakpointByUrlRequest) : JsonRpcResult {
    @field:JsonProperty @JvmField
    val breakpointId = "1:${request.lineNumber}:${request.columnNumber}:${request.scriptId}"

    @field:JsonProperty
    @JvmField
    val locations: List<Location> = listOf(Location(request.scriptId!!, request.lineNumber!!, request.columnNumber!!))
}

data class Location(
    @field:JsonProperty @JvmField
    val scriptId: String,

    @field:JsonProperty @JvmField
    val lineNumber: Int,

    @field:JsonProperty @JvmField
    val columnNumber: Int
)

class LocationResponse {
    @field:JsonProperty
    @JvmField
    var scriptId: String? = null

    @field:JsonProperty
    @JvmField
    var lineNumber: Int? = null

    @field:JsonProperty
    @JvmField
    var columnNumber: Int? = null
}

class V8Response : JsonRpcResult {
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
    var params: JSONObject? = null
}

class BreakpointResolvedEvent: JsonRpcResult {
    @field:JsonProperty
    @JvmField
    var breakpointId: String? = null

    @field:JsonProperty
    @JvmField
    var location: LocationResponse? = null
}

class GetPropertiesResult : JSONObject(), JsonRpcResult

//users of the lib can change this value
private val scriptsDomain = "http://app/"
private val scriptsUrlBase get() = scriptsDomain + StethoHelper.scriptsPathPrefix

//move to separate mapper class if conversion logic become complicated and used in many places
private fun scriptIdToUrl(scriptId: String?) = scriptsUrlBase + scriptId
private fun urlToScriptId(url: String?) = url?.removePrefix(scriptsUrlBase)
