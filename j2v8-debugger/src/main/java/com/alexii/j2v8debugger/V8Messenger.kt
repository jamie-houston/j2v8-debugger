/*
 * Copyright (c) 2020, Salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 *
 */

package com.alexii.j2v8debugger

import com.alexii.j2v8debugger.utils.logger
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.inspector.V8Inspector
import com.eclipsesource.v8.inspector.V8InspectorDelegate
import com.facebook.stetho.inspector.network.NetworkPeerManager
import org.json.JSONObject
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.LinkedHashMap

class V8Messenger(v8: V8): V8InspectorDelegate {
    private val chromeMessageQueue = Collections.synchronizedMap(LinkedHashMap<String, JSONObject>())
    private val v8ScriptMap = mutableMapOf<String, String>()
    private val v8MessageQueue = Collections.synchronizedMap(LinkedHashMap<String, JSONObject?>())

    private val pendingMessageQueue = Collections.synchronizedList(mutableListOf<PendingResponse>())
    private val nextDispatchId = AtomicInteger(0)
    private var debuggerState = DebuggerState.Disconnected
    private val v8Inspector by lazy {
        V8Inspector.createV8Inspector(v8, this, TAG)
    }

    fun getV8Result(method: String, params: JSONObject?): String? {
        val pendingMessage = PendingResponse(method, nextDispatchId.incrementAndGet())
        pendingMessageQueue.add(pendingMessage)

        v8MessageQueue[method] = params ?: JSONObject()
        while (pendingMessage.response.isNullOrBlank()) {
            // wait for response from server
        }
        pendingMessageQueue.remove(pendingMessage)
        return pendingMessage.response
    }

    override fun waitFrontendMessageOnPause() {
        if (debuggerState != DebuggerState.Paused) {
            // If we haven't attached to chrome yet, resume code (or else we're stuck)
            logger.d(V8Debugger.TAG, "Debugger paused without connection.  Resuming J2V8")
            dispatchMessage(Protocol.Debugger.Resume)
        } else {
            if (v8MessageQueue.any()) {
                for ((k, v) in v8MessageQueue) {
                    logger.d(V8Debugger.TAG, "Sending v8 $k with $v")
                    dispatchMessage(k, v.toString())
                }
                v8MessageQueue.clear()
            }
            if (chromeMessageQueue.any()) {
                val networkPeerManager = NetworkPeerManager.getInstanceOrNull()
                for ((k, v) in chromeMessageQueue) {
                    logger.d(V8Debugger.TAG, "Sending chrome $k with $v")
                    networkPeerManager?.sendNotificationToPeers(k, v)
                }
                chromeMessageQueue.clear()
            }
        }
    }

    override fun onResponse(p0: String?) {
        logger.d(V8Debugger.TAG, "onResponse $p0")
        val message = JSONObject(p0)
        if (message.has("id")) {
            // This is a command response
            val pendingMessage = pendingMessageQueue.firstOrNull { msg -> msg.pending && msg.messageId == message.getInt("id") }
            if (pendingMessage != null) {
                pendingMessage.response = message.optJSONObject("result")?.optString("result")
            }
        } else if (message.has("method")) {
            // This is an event
            val responseParams = message.optJSONObject("params")
            val responseMethod = message.optString("method")
            if (responseMethod == Protocol.Debugger.ScriptParsed) {
                if (responseParams.optString("url").isNotEmpty()) {
                    // Get the V8 Script ID to map to the Chrome ScipeId
                    v8ScriptMap[responseParams.optString("scriptId")] = responseParams.getString("url")
                }
            } else if (responseMethod == Protocol.Debugger.BreakpointResolved) {
                val location = responseParams.getJSONObject("location")
                location.put("scriptId", v8ScriptMap[location.getString("scriptId")])
                val response = JSONObject().put("breakpointId", responseParams.getString("breakpointId")).put("location", location)
                chromeMessageQueue[responseMethod] = response
            } else if (responseMethod == Protocol.Debugger.Paused) {
                debuggerState = DebuggerState.Paused
                val regex = "\"scriptId\":\"(\\d+)\"".toRegex()
                val updatedScript = responseParams.toString().replace(regex) {
                    "\"scriptId\":\"${v8ScriptMap[it.groups[1]?.value]}\""
                }
                chromeMessageQueue[responseMethod] = JSONObject(updatedScript)
            } else if (responseMethod == Protocol.Debugger.Resumed) {
                debuggerState = DebuggerState.Connected
            }
        }
    }

    fun sendMessage(message: String, params: JSONObject? = null, runOnlyWhenPaused: Boolean = false) {
        if (debuggerState == DebuggerState.Paused) {
            v8MessageQueue[message] = params
        } else if (!runOnlyWhenPaused) {
            dispatchMessage(message, params.toString())
        }
    }

    fun setDebuggerConnected(isConnected: Boolean) {
        debuggerState = if (isConnected) DebuggerState.Connected else DebuggerState.Disconnected
    }


    private fun dispatchMessage(method: String, params: String? = null) {
        val messageId: Int
        val pendingMessage = pendingMessageQueue.firstOrNull { msg -> msg.method == method && !msg.pending }
        if (pendingMessage != null) {
            pendingMessage.pending = true
            messageId = pendingMessage.messageId
        } else {
            messageId = nextDispatchId.incrementAndGet()
        }
        val message = "{\"id\":$messageId,\"method\":\"$method\", \"params\": ${params ?: "{}"}}"
        logger.d(V8Debugger.TAG, "dispatching $message")
        v8Inspector?.dispatchProtocolMessage(message)
    }

    private data class PendingResponse(val method: String, var messageId: Int) {
        var response: String? = null
        var pending = false
    }

    private enum class DebuggerState {
        Disconnected,
        Paused,
        Connected
    }


    companion object {
        const val TAG = "V8Messenger"
    }
}