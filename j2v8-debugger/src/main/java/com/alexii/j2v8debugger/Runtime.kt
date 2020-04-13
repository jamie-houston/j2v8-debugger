package com.alexii.j2v8debugger

import androidx.annotation.VisibleForTesting
import com.facebook.stetho.inspector.console.RuntimeReplFactory
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod
import com.facebook.stetho.inspector.protocol.module.SimpleBooleanResult
import kotlinx.coroutines.runBlocking
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
class Runtime(replFactory: RuntimeReplFactory?) : ChromeDevtoolsDomain {
    @VisibleForTesting
    var adaptee = FacebookRuntimeBase(replFactory)

    @ChromeDevtoolsMethod
    fun getProperties(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult {

        val method = Protocol.Runtime.GetProperties

        var result: String? = null
        runBlocking {
            result = V8Helper.getV8Result(method, params)
        }
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
