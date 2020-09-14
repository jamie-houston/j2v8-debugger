package com.salesforce.j2v8inspector

import com.salesforce.j2v8inspector.model.CdpMethod
import com.salesforce.j2v8inspector.utils.logger
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod
import org.json.JSONObject

internal class Console : BaseCdtDomain(), ChromeDevtoolsDomain {

    @Suppress("unused", "UNUSED_PARAMETER")
    @ChromeDevtoolsMethod
    fun clearMessages(peer: JsonRpcPeer, params: JSONObject?) {
        val method = CdpMethod.Console.ClearMessages
        logger.d(TAG, "$method: $params")
        v8Messenger?.sendMessage(method, params, crossThread = true)
    }

    companion object {
        const val TAG = "Console"
    }
}