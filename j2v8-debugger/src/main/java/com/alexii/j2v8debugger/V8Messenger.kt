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
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult
import com.facebook.stetho.inspector.network.NetworkPeerManager
import com.facebook.stetho.json.ObjectMapper
import com.facebook.stetho.json.annotation.JsonProperty
import org.json.JSONObject
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.LinkedHashMap

class V8Messenger(v8: V8): V8InspectorDelegate {
    private val dtoMapper: ObjectMapper = ObjectMapper()
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
            logger.d(TAG, "Debugger paused without connection.  Resuming J2V8")
            dispatchMessage(Protocol.Debugger.Resume)
        } else {
            if (v8MessageQueue.any()) {
                for ((k, v) in v8MessageQueue) {
                    logger.d(TAG, "Sending v8 $k with $v")
                    dispatchMessage(k, v.toString())
                }
                v8MessageQueue.clear()
            }
            if (chromeMessageQueue.any()) {
                val networkPeerManager = NetworkPeerManager.getInstanceOrNull()
                for ((k, v) in chromeMessageQueue) {
                    logger.d(TAG, "Sending chrome $k with $v")
                    networkPeerManager?.sendNotificationToPeers(k, v)
                }
                chromeMessageQueue.clear()
            }
        }
    }

    override fun onResponse(p0: String?) {
        logger.d(TAG, "onResponse $p0")
        val message = dtoMapper.convertValue(JSONObject(p0), V8Response::class.java)
        if (message.isResponse) {
            // This is a command response
            val pendingMessage = pendingMessageQueue.firstOrNull { msg -> msg.pending && msg.messageId == message.id }
            if (pendingMessage != null) {
                pendingMessage.response = message.result?.optString("result")
            }
        } else {
            val responseParams = message.params
            val responseMethod = message.method

            val functionMap = mapOf<String, (JSONObject?, String?) -> Unit>(
                Pair(Protocol.Debugger.ScriptParsed, ::handleScriptParsedEvent),
                Pair(Protocol.Debugger.BreakpointResolved, ::handleBreakpointResolvedEvent),
                Pair(Protocol.Debugger.Paused, ::handleDebuggerPausedEvent),
                Pair(Protocol.Debugger.Resumed, ::handleDebuggerResumedEvent)
            )

            functionMap[responseMethod]?.invoke(responseParams, responseMethod)
        }
    }

    private fun handleDebuggerResumedEvent(responseParams: JSONObject?, responseMethod: String?) {
        debuggerState = DebuggerState.Connected
    }

    private fun handleDebuggerPausedEvent(responseParams: JSONObject?, responseMethod: String?) {

        debuggerState = DebuggerState.Paused
        val regex = "\"scriptId\":\"(\\d+)\"".toRegex()
        val updatedScript = responseParams.toString().replace(regex) {
            "\"scriptId\":\"${v8ScriptMap[it.groups[1]?.value]}\""
        }
        chromeMessageQueue[responseMethod] = JSONObject(updatedScript)
    }

    private fun handleScriptParsedEvent(responseParams: JSONObject?, responseMethod: String?) {

        val scriptParsedEvent = dtoMapper.convertValue(responseParams, ScriptParsedEventRequest::class.java)
        if (scriptParsedEvent.url.isNotEmpty()) {
            // Get the V8 Script ID to map to the Chrome ScipeId
            v8ScriptMap[scriptParsedEvent.scriptId] = scriptParsedEvent.url
        }
    }

    private fun handleBreakpointResolvedEvent(responseParams: JSONObject?, responseMethod: String?) {
        val breakpointResolvedEvent = dtoMapper.convertValue(responseParams, BreakpointResolvedEvent::class.java)
        val location = breakpointResolvedEvent.location
        val response = BreakpointResolvedEvent().also {
            it.breakpointId = breakpointResolvedEvent.breakpointId
            it.location = LocationResponse().also {
                it.scriptId = v8ScriptMap[location?.scriptId]
                it.lineNumber = location?.lineNumber
                it.columnNumber = location?.columnNumber
            }
        }
        chromeMessageQueue[responseMethod] = dtoMapper.convertValue(response, JSONObject::class.java)
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
        logger.d(TAG, "dispatching $message")
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