package com.alexii.j2v8debugger

import android.util.Log
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod
import org.json.JSONObject
import com.facebook.stetho.inspector.protocol.module.Inspector as FacebookInspectorStub

class Inspector : FacebookInspectorStub() {
    @ChromeDevtoolsMethod
    override fun enable(peer: JsonRpcPeer?, params: JSONObject?) {
        Log.i("Inspector", "enabled $params")
        super.enable(peer, params)
    }

    @ChromeDevtoolsMethod
    override fun disable(peer: JsonRpcPeer?, params: JSONObject?) {
        Log.i("Inspector", "disabled $params")
        super.disable(peer, params)
    }
}