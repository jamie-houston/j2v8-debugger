package com.alexii.j2v8debugger

import com.alexii.j2v8debugger.model.CdpMethod
import com.alexii.j2v8debugger.utils.logger
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod
import org.json.JSONObject

internal class Console(
) : BaseCdtDomain(), ChromeDevtoolsDomain {

    @Suppress("unused", "UNUSED_PARAMETER")
    @ChromeDevtoolsMethod
    fun clearMessages(peer: JsonRpcPeer, params: JSONObject?) {
        val method = CdpMethod.Console.ClearMessages
        logger.d(Runtime.TAG, "$method: $params")
        v8Messenger?.sendMessage(method, params, crossThread = true)
    }

    companion object {
        const val TAG = "Console"
    }
}