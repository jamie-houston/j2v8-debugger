/*
 * Copyright (c) 2020, Salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 *
 */

package com.salesforce.j2v8debugger

import com.facebook.stetho.inspector.console.RuntimeReplFactory
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod
import com.facebook.stetho.inspector.protocol.module.SimpleBooleanResult
import org.json.JSONArray
import org.json.JSONObject
import com.facebook.stetho.inspector.protocol.module.Runtime as FacebookRuntimeBase

/**
 * V8 JS Debugger. Name of the class and methods must match names defined in Chrome Dev Tools protocol.
 *
 * [initialize] must be called before actual debugging (adding breakpoints in Chrome DevTools).
 *  Otherwise setting breakpoint, etc. makes no effect.
 */
@Suppress("UNUSED_PARAMETER", "unused")
class Runtime(private val v8Debugger: V8Debugger, replFactory: RuntimeReplFactory?) : ChromeDevtoolsDomain {
    var adaptee = FacebookRuntimeBase(replFactory)

    @ChromeDevtoolsMethod
    fun getProperties(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult {
        val method = Protocol.Runtime.GetProperties
        val result = v8Debugger.getV8Result(method, params)
        val jsonResult = GetPropertiesResult().put("result", JSONArray(result))
        return jsonResult as JsonRpcResult
    }

    @ChromeDevtoolsMethod
    fun releaseObject(peer: JsonRpcPeer?, params: JSONObject?) = adaptee.releaseObject(peer, params)

    @ChromeDevtoolsMethod
    fun releaseObjectGroup(peer: JsonRpcPeer?, params: JSONObject?) = adaptee.releaseObjectGroup(peer, params)

    /**
     * Replaces [FacebookRuntimeBase.callFunctionOn] as override can't be used: CallFunctionOnResponse is private (return type)
     */
    @ChromeDevtoolsMethod
    fun callFunctionOn(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult? = adaptee.callFunctionOn(peer, params)

    @ChromeDevtoolsMethod
    fun evaluate(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult = adaptee.evaluate(peer, params)

    @ChromeDevtoolsMethod
    fun enable(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult {
        return SimpleBooleanResult(true)
    }
}
