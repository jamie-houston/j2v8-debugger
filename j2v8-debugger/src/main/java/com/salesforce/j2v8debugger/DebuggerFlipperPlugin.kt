package com.salesforce.j2v8debugger

import com.facebook.flipper.core.FlipperConnection
import com.facebook.flipper.core.FlipperObject
import com.facebook.flipper.core.FlipperPlugin
import org.json.JSONObject

class DebuggerFlipperPlugin : FlipperPlugin{
    private var flipperConnection: FlipperConnection? = null

    override fun onConnect(connection: FlipperConnection?) {
        flipperConnection = connection
        sendMessage(Protocol.Debugger.Enable)
    }

    override fun runInBackground(): Boolean = false

    override fun getId(): String = "j2v8Plugin"

    override fun onDisconnect() {
        flipperConnection = null
    }

    public fun sendMessage(method: String, params: JSONObject? = null){
        val flipperObject = FlipperObject(params)
        flipperConnection?.send(method, flipperObject)
    }
}