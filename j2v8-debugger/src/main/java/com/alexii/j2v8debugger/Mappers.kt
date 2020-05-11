package com.alexii.j2v8debugger

import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult
import com.facebook.stetho.inspector.protocol.module.Runtime
import com.facebook.stetho.json.annotation.JsonProperty
import org.json.JSONObject

internal class EvaluateOnCallFrameResult(
        @field:JsonProperty
        @JvmField
        val result: JSONObject? = null
) : JsonRpcResult

/**
 * Fired as the result of [Debugger.enable]
 */
internal class ScriptParsedEvent(
        @field:JsonProperty @JvmField
        val scriptId: String,

        @field:JsonProperty @JvmField
        val url: String = scriptIdToUrl(scriptId)
)

internal class ScriptParsedEventRequest : JsonRpcResult {
    @field:JsonProperty
    @JvmField
    var scriptId: String = ""

    @field:JsonProperty
    @JvmField
    var url: String = ""
}

internal class GetScriptSourceRequest : JsonRpcResult {
    @field:JsonProperty
    @JvmField
    var scriptId: String? = null
}

internal class GetScriptSourceResponse(
        @field:JsonProperty @JvmField
        val scriptSource: String
) : JsonRpcResult

internal class SetBreakpointByUrlRequest : JsonRpcResult {
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

internal class SetBreakpointByUrlResponse(
        request: SetBreakpointByUrlRequest) : JsonRpcResult {
    @field:JsonProperty
    @JvmField
    val breakpointId = "1:${request.lineNumber}:${request.columnNumber}:${request.scriptId}"

    @field:JsonProperty
    @JvmField
    val locations: List<Location> = listOf(Location(request.scriptId!!, request.lineNumber!!, request.columnNumber!!))
}

internal data class Location(
        @field:JsonProperty @JvmField
        val scriptId: String,

        @field:JsonProperty @JvmField
        val lineNumber: Int,

        @field:JsonProperty @JvmField
        val columnNumber: Int
)

internal class LocationResponse {
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
    var params: JSONObject? = null
}

internal class BreakpointResolvedEvent : JsonRpcResult {
    @field:JsonProperty
    @JvmField
    var breakpointId: String? = null

    @field:JsonProperty
    @JvmField
    var location: LocationResponse? = null
}

internal class GetPropertiesResult : JSONObject(), JsonRpcResult

internal data class PausedEvent @JvmOverloads constructor(
        @field:JsonProperty @JvmField
        val callFrames: List<CallFrame>,

        @field:JsonProperty @JvmField
        val reason: String = "other"
)

internal data class CallFrame @JvmOverloads constructor(
        @field:JsonProperty @JvmField
        val callFrameId: String,

        @field:JsonProperty @JvmField
        val functionName: String,

        @field:JsonProperty @JvmField
        val location: LocationResponse,

        /** JavaScript script name or url. */
        @field:JsonProperty @JvmField
        val url: String,

        @field:JsonProperty @JvmField
        val scopeChain: List<Scope>,

        //xxx: check how and whether it's wotking with this
        @field:JsonProperty @JvmField
        val `this`: Runtime.RemoteObject? = null
)

internal data class Scope(
        /** one of: global, local, with, closure, catch, block, script, eval, module. */
        @field:JsonProperty @JvmField
        val type: String,
        /**
         * Object representing the scope.
         * For global and with scopes it represents the actual object;
         * for the rest of the scopes, it is artificial transient object enumerating scope variables as its properties.
         */
        @field:JsonProperty @JvmField
        val `object`: Runtime.RemoteObject
)

//users of the lib can change this value
private val scriptsDomain = "http://app/"
private val scriptsUrlBase get() = scriptsDomain + StethoHelper.scriptsPathPrefix

//move to separate mapper class if conversion logic become complicated and used in many places
private fun scriptIdToUrl(scriptId: String?) = scriptsUrlBase + scriptId
private fun urlToScriptId(url: String?) = url?.removePrefix(scriptsUrlBase)
