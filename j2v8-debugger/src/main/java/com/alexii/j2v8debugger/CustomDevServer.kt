package com.alexii.j2v8debugger

import com.facebook.stetho.inspector.ChromeDevtoolsServer
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain
import com.facebook.stetho.websocket.SimpleSession

public class CustomDevServer(domainModules: MutableIterable<ChromeDevtoolsDomain>?) : ChromeDevtoolsServer(domainModules) {
    override fun onMessage(session: SimpleSession?, message: String?) {
        super.onMessage(session, message)
    }
}