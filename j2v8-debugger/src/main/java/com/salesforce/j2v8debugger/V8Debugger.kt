/*
 * Copyright (c) 2020, Salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 *
 */

package com.salesforce.j2v8debugger

import com.eclipsesource.v8.V8
import com.eclipsesource.v8.inspector.V8Inspector
import com.eclipsesource.v8.inspector.V8InspectorDelegate
import com.facebook.stetho.inspector.network.NetworkPeerManager
import com.salesforce.j2v8debugger.utils.logger
import org.json.JSONObject
import java.util.Collections
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.LinkedHashMap

class V8Debugger: V8InspectorDelegate {

    private var v8Inspector: V8Inspector? = null
    private var debuggerState = DebuggerState.Disconnected
    private val chromeMessageQueue = Collections.synchronizedMap(LinkedHashMap<String, JSONObject>())
    private val v8MessageQueue = Collections.synchronizedMap(LinkedHashMap<String, JSONObject?>())

    private val pendingMessageQueue = Collections.synchronizedList(mutableListOf<PendingResponse>())
    private val nextDispatchId = AtomicInteger(0)
    private var v8ScriptId: String? = null
    private lateinit var chromeScriptName: String

    private fun getOrCreateV8Inspector(v8: V8): V8Inspector {
        if (v8Inspector == null) {
            v8Inspector = V8Inspector.createV8Inspector(v8, this, TAG)
        }

        return v8Inspector ?: throw IllegalStateException("Unable to create V8Inspector")
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
                    v8ScriptId = responseParams.optString("scriptId")
                }
            } else if (responseMethod == Protocol.Debugger.BreakpointResolved) {
                val location = responseParams.getJSONObject("location")
                location.put("scriptId", chromeScriptName)
                val response = JSONObject().put("breakpointId", responseParams.getString("breakpointId")).put("location", location)
                chromeMessageQueue[responseMethod] = response
            } else if (responseMethod == Protocol.Debugger.Paused) {
                debuggerState = DebuggerState.Paused
                val updatedScript = responseParams.toString().replace("\"scriptId\":\"$v8ScriptId\"", "\"scriptId\":\"$chromeScriptName\"")
                chromeMessageQueue[responseMethod] = JSONObject(updatedScript)
            } else if (responseMethod == Protocol.Debugger.Resumed){
                debuggerState = DebuggerState.Connected
            }
        }
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

    /**
     * Utility, which simplifies configuring V8 for debugging support and creation of new instance.
     * Creates V8 runtime, v8 debugger and binds it to Stetho.
     * For releasing resources [releaseDebuggable] should be used.
     *
     * @param v8Executor single-thread executor where v8 will be created
     *  and all debug calls will be performed by Stetho later.
     *
     * NOTE: Should be declared as V8 class extensions when will be allowed (https://youtrack.jetbrains.com/issue/KT-11968)
     */
    fun createDebuggableV8Runtime(v8Executor: ExecutorService, scriptName: String): Future<V8> {
        chromeScriptName = scriptName

        return v8Executor.submit(Callable {
            val runtime = V8.createV8Runtime()
            val inspector = getOrCreateV8Inspector(runtime)

            // Default Chrome DevTool protocol messages
            dispatchMessage(Protocol.Runtime.Enable)
            dispatchMessage(Protocol.Debugger.Enable, "{\"maxScriptsCacheSize\":10000000}")
            dispatchMessage(Protocol.Debugger.SetPauseOnExceptions, "{\"state\": \"none\"}")
            dispatchMessage(Protocol.Debugger.SetAsyncCallStackDepth, "{\"maxDepth\":32}")

            dispatchMessage(Protocol.Runtime.RunIfWaitingForDebugger)


            StethoHelper.initializeWithV8Debugger(inspector, v8Executor)

            runtime
        })
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


    fun releaseV8Debugger() {
        v8Inspector = null
    }

    fun setDebuggerConnected(isConnected: Boolean) {
        debuggerState = if (isConnected) DebuggerState.Connected else DebuggerState.Disconnected
    }

    fun queueV8Message(message: String, params: JSONObject?) {
        if (debuggerState == DebuggerState.Paused) {
            v8MessageQueue[message] = params
        } else {
            dispatchMessage(message, params.toString())
        }
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
        const val TAG = "V8Debugger"
    }
}