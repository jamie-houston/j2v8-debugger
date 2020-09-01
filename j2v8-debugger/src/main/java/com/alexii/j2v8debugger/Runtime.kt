package com.alexii.j2v8debugger

import com.facebook.stetho.inspector.console.RuntimeReplFactory
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import com.facebook.stetho.inspector.protocol.module.Runtime as FacebookRuntimeBase

/**
 * Runtime Domain. Name of the class and methods must match names defined in Chrome Dev Tools protocol.
 */
@Suppress("UNUSED_PARAMETER", "unused")
class Runtime(replFactory: RuntimeReplFactory?) : ChromeDevtoolsDomain {
    private var v8Messenger: V8Messenger? = null
    private val adaptee = FacebookRuntimeBase(replFactory)
    private var v8Executor: ExecutorService? = null

    fun initialize(v8Messenger: V8Messenger, v8Executor: ExecutorService) {
        this.v8Messenger = v8Messenger
        this.v8Executor = v8Executor
    }

    @ChromeDevtoolsMethod
    fun getProperties(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult {
        val method = Protocol.Runtime.GetProperties
        var result: String? = ""

        if (v8Messenger?.isDebuggerPaused == true) {
            result = v8Messenger?.getV8Result(method, params)
        } else {
            v8Executor?.execute {
                result = v8Messenger?.getV8Result(method, params)
            }
        }

        val jsonResult = GetPropertiesResult().put("result", JSONArray(result))
        return jsonResult as JsonRpcResult
    }

    /**
     * Pass through the needed [FacebookRuntimeBase] methods
     */
    @ChromeDevtoolsMethod
    fun releaseObject(peer: JsonRpcPeer?, params: JSONObject?) = adaptee.releaseObject(peer, params)

    @ChromeDevtoolsMethod
    fun releaseObjectGroup(peer: JsonRpcPeer?, params: JSONObject?) = adaptee.releaseObjectGroup(peer, params)

    @ChromeDevtoolsMethod
    fun callFunctionOn(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult? = adaptee.callFunctionOn(peer, params)

    @ChromeDevtoolsMethod
    fun evaluate(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult {
        val method = Protocol.Runtime.Evaluate
        var result: String? = null
        v8Executor?.execute {
            result = v8Messenger?.getV8Result(method, params)
        }
        return EvaluateOnCallFrameResult(JSONObject(result))
    }

    @ChromeDevtoolsMethod
    fun compileScript(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult? {
        return adaptee.callFunctionOn(peer, params)
    }
}
