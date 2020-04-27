/*
 * Copyright (c) 2020, Salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 *
 */

package com.salesforce.j2v8debugger

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
    val scriptId: String?,

    @field:JsonProperty @JvmField
    val url: String? = scriptIdToUrl(scriptId)
)

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
    //script id
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
    @field:JsonProperty @JvmField
    val breakpointId: String,

    location: Location
) : JsonRpcResult {
    @field:JsonProperty
    @JvmField
    val locations: List<Location> = listOf(location)
}

class RemoveBreakpointRequest : JsonRpcResult {
    //script id
    @field:JsonProperty
    @JvmField
    var breakpointId: String? = null
}


data class Location(
    @field:JsonProperty @JvmField
    val scriptId: String,

    @field:JsonProperty @JvmField
    val lineNumber: Int,

    @field:JsonProperty @JvmField
    val columnNumber: Int
)

class SimpleIntegerResult(@JsonProperty(required = true) var result: Int) : JsonRpcResult

class GetPropertiesResult : JSONObject(), JsonRpcResult

//users of the lib can change this value
private val scriptsDomain = "http://app/"
private val scriptsUrlBase get() = scriptsDomain + StethoHelper.scriptsPathPrefix

//move to separate mapper class if conversion logic become complicated and used in many places
private fun scriptIdToUrl(scriptId: String?) = scriptsUrlBase + scriptId
private fun urlToScriptId(url: String?) = url?.removePrefix(scriptsUrlBase)
