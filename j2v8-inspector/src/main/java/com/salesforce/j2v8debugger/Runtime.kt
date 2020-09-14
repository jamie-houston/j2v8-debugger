package com.salesforce.j2v8inspector

import com.salesforce.j2v8inspector.model.CdpMethod
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod
import com.facebook.stetho.json.ObjectMapper
import org.json.JSONObject
import com.facebook.stetho.inspector.protocol.module.Runtime as FacebookRuntimeBase

/**
 * Runtime Domain. Name of the class and methods must match names defined in Chrome Dev Tools protocol.
 */
@Suppress("UNUSED_PARAMETER", "unused")
class Runtime : BaseCdtDomain(), ChromeDevtoolsDomain {
    var dtoMapper: ObjectMapper = ObjectMapper()

    @ChromeDevtoolsMethod
    fun getProperties(peer: JsonRpcPeer?, params: JSONObject?) =
        getV8ResultAsJsonRpcResult(CdpMethod.Runtime.GetProperties, params)

    /**
     * Pass through the needed [FacebookRuntimeBase] methods
     */
    @ChromeDevtoolsMethod
    fun releaseObject(peer: JsonRpcPeer?, params: JSONObject?) =
        sendMessage(CdpMethod.Runtime.ReleaseObject, params, crossThread = true)

    @ChromeDevtoolsMethod
    fun releaseObjectGroup(peer: JsonRpcPeer?, params: JSONObject?) =
        sendMessage(CdpMethod.Runtime.ReleaseObjectGroup, params, crossThread = true)

    @ChromeDevtoolsMethod
    fun callFunctionOn(peer: JsonRpcPeer?, params: JSONObject?) =
        getV8ResultAsJsonRpcResult(CdpMethod.Runtime.CallFunctionOn, params)

    @ChromeDevtoolsMethod
    fun evaluate(peer: JsonRpcPeer?, params: JSONObject?) =
        getV8ResultAsJsonRpcResult(CdpMethod.Runtime.Evaluate, params)

    @ChromeDevtoolsMethod
    fun compileScript(peer: JsonRpcPeer?, params: JSONObject?) =
        getV8ResultAsJsonRpcResult(CdpMethod.Runtime.CompileScript, params)

    @ChromeDevtoolsMethod
    fun awaitPromise(peer: JsonRpcPeer?, params: JSONObject?) =
        getV8ResultAsJsonRpcResult(CdpMethod.Runtime.AwaitPromise, params)

    @ChromeDevtoolsMethod
    fun runScript(peer: JsonRpcPeer?, params: JSONObject?) =
        getV8ResultAsJsonRpcResult(CdpMethod.Runtime.RunScript, params)

    @ChromeDevtoolsMethod
    fun queryObjects(peer: JsonRpcPeer?, params: JSONObject?) =
        getV8ResultAsJsonRpcResult(CdpMethod.Runtime.QueryObjects, params)

}
