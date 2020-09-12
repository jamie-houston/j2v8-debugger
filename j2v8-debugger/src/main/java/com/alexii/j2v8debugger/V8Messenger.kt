package com.alexii.j2v8debugger

import com.alexii.j2v8debugger.model.*
import com.alexii.j2v8debugger.model.BreakpointResolvedEvent
import com.alexii.j2v8debugger.model.LocationResponse
import com.alexii.j2v8debugger.model.ScriptParsedEventRequest
import com.alexii.j2v8debugger.model.V8Response
import com.alexii.j2v8debugger.model.replaceScriptId
import com.alexii.j2v8debugger.utils.logger
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.inspector.V8Inspector
import com.eclipsesource.v8.inspector.V8InspectorDelegate
import com.facebook.stetho.inspector.network.NetworkPeerManager
import com.facebook.stetho.json.ObjectMapper
import org.json.JSONObject
import java.util.Collections
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.LinkedHashMap

class V8Messenger(v8: V8, private val v8Executor: ExecutorService) : V8InspectorDelegate {
    private val dtoMapper: ObjectMapper = ObjectMapper()
    private val chromeMessageQueue =
        Collections.synchronizedMap(LinkedHashMap<String, JSONObject>())
    private val v8ScriptMap = mutableMapOf<String, String>()
    private val v8MessageQueue = LinkedBlockingQueue<PendingResponse>()
    private val pendingMessageQueue = Collections.synchronizedList(mutableListOf<PendingResponse>())
    private val nextDispatchId = AtomicInteger(0)
    private var debuggerState = DebuggerState.Disconnected

    private val v8Inspector by lazy {
        V8Inspector.createV8Inspector(v8, this, TAG)
    }

    val isDebuggerPaused = debuggerState == DebuggerState.Paused

    /**
     * Pass a method and params through to J2V8 to get the response.
     * it's synchronized call even it waits for j2v8 to finish its work in separate thread
     */
    fun getV8Result(method: String, params: JSONObject?): String? {
        val message = PendingResponse(method, nextDispatchId.incrementAndGet(), params)
        pendingMessageQueue.add(message)

        v8MessageQueue.add(message)
        if (debuggerState == DebuggerState.Connected) {
            dispatchMessage(message.messageId, method, params, true)
        }

        while (message.status != MessageState.GotResponse) {
            if (debuggerState == DebuggerState.Connected) {
                Thread.sleep(10)
            }
            // wait for response from server
        }
        pendingMessageQueue.remove(message)
        return message.response
    }

    /**
     * Send messages to J2V8
     * If debugger is paused, they will be queued to send in [waitFrontendMessageOnPause]
     * otherwise we can send now.
     * Some messages are only relevant while paused so ignore them if it's not
     */
    fun sendMessage(
        method: String,
        params: JSONObject? = null,
        crossThread: Boolean,
        runOnlyWhenPaused: Boolean = false
    ) {
        if (debuggerState == DebuggerState.Paused) {
            v8MessageQueue.add(PendingResponse(method, nextDispatchId.incrementAndGet(), params))
        } else if (!runOnlyWhenPaused) {
            dispatchMessage(
                nextDispatchId.incrementAndGet(),
                method = method,
                params = params,
                crossTread = crossThread
            )
        }
    }

    /**
     * This method is called continuously while J2V8 is paused.
     * Any communication must be done inside of this method while debugger is paused.
     */
    override fun waitFrontendMessageOnPause() {
        if (debuggerState != DebuggerState.Paused) {
            // If we haven't attached to chrome yet, resume code (or else we're stuck)
            logger.d(TAG, "Debugger paused without connection.  Resuming J2V8")
            dispatchMessage(nextDispatchId.incrementAndGet(), CdpMethod.Debugger.Resume)
        } else {
            // Check for messages to send to J2V8
            while (v8MessageQueue.any()) {
                val v = v8MessageQueue.remove()
                logger.d(TAG, "Sending v8 ${v.method} with ${v.params}")
                dispatchMessage(v.messageId, v.method, v.params)
            }

            // Check for messages to send to Chrome DevTools
            if (chromeMessageQueue.any()) {
                val networkPeerManager = NetworkPeerManager.getInstanceOrNull()
                if (networkPeerManager?.hasRegisteredPeers() == true) {
                    for ((k, v) in chromeMessageQueue) {
                        logger.d(TAG, "Sending chrome $k with $v")
                        networkPeerManager.sendNotificationToPeers(k, v)
                    }
                } else {
                    // We can't send messages to chrome if it's not attached (networkPeerManager null) so resume debugger
                    dispatchMessage(nextDispatchId.incrementAndGet(), CdpMethod.Debugger.Resume)
                }
                chromeMessageQueue.clear()
            }
        }
    }

    /**
     * Responses from J2V8 come through here.
     */
    override fun onResponse(p0: String?) {
        logger.d(TAG, "onResponse $p0")
        val message = dtoMapper.convertValue(JSONObject(p0), V8Response::class.java)
        if (message.isResponse) {
            // This is a command response
            pendingMessageQueue.firstOrNull { msg -> msg.status == MessageState.SentToJ2v8 && msg.messageId == message.id }
                ?.apply {
                    response =
                        if (message.error != null) p0 else message.result?.toString()
                    status = MessageState.GotResponse
                }
        } else {
            val responseParams = message.params

            when (val responseMethod = message.method) {
                CdpMethod.Debugger.ScriptParsed -> handleScriptParsedEvent(responseParams)
                CdpMethod.Debugger.BreakpointResolved -> handleBreakpointResolvedEvent(
                    responseParams,
                    responseMethod
                )
                CdpMethod.Debugger.Paused -> handleDebuggerPausedEvent(
                    responseParams,
                    responseMethod
                )
                CdpMethod.Debugger.Resumed -> handleDebuggerResumedEvent()
            }
        }
    }

    private fun handleDebuggerResumedEvent() {
        if (debuggerState == DebuggerState.Paused) {
            debuggerState = DebuggerState.Connected
        }
    }

    private fun handleDebuggerPausedEvent(responseParams: JSONObject?, responseMethod: String?) {
        if (debuggerState == DebuggerState.Disconnected) {
            dispatchMessage(nextDispatchId.incrementAndGet(), CdpMethod.Debugger.Resume)
        } else {
            if (responseParams != null) {
                debuggerState = DebuggerState.Paused
                val updatedScript = replaceScriptId(responseParams, v8ScriptMap)
                chromeMessageQueue[responseMethod] = updatedScript
            }
        }
    }

    private fun handleScriptParsedEvent(responseParams: JSONObject?) {
        val scriptParsedEvent =
            dtoMapper.convertValue(responseParams, ScriptParsedEventRequest::class.java)
        if (scriptParsedEvent.url.isNotEmpty()) {
            // Get the V8 Script ID to map to the Chrome ScriptId (stored in url)
            v8ScriptMap[scriptParsedEvent.scriptId] = scriptParsedEvent.url
        }
    }

    /**
     * For BreakpointResolved events, we need to convert the scriptId from the J2V8 scriptId
     * to the Chrome DevTools scriptId before passing it through
     */
    private fun handleBreakpointResolvedEvent(
        responseParams: JSONObject?,
        responseMethod: String?
    ) {
        val breakpointResolvedEvent =
            dtoMapper.convertValue(responseParams, BreakpointResolvedEvent::class.java)
        val location = breakpointResolvedEvent.location
        val response = BreakpointResolvedEvent().also { resolvedEvent ->
            resolvedEvent.breakpointId = breakpointResolvedEvent.breakpointId
            resolvedEvent.location = LocationResponse().also { locationResponse ->
                locationResponse.scriptId = v8ScriptMap[location?.scriptId]
                locationResponse.lineNumber = location?.lineNumber
                locationResponse.columnNumber = location?.columnNumber
            }
        }
        chromeMessageQueue[responseMethod] =
            dtoMapper.convertValue(response, JSONObject::class.java)
    }


    /**
     * Change debugger state when DevTools connects and disconnects
     */
    fun setDebuggerConnected(isConnected: Boolean) {
        debuggerState = if (isConnected) DebuggerState.Connected else DebuggerState.Disconnected
    }

    /**
     * Pass message to J2V8
     * If we're awaiting a response in the pendingMessageQueue, use the Id and set to pending
     *
     * Note: this method must be run under the same thread as v8
     */
    private fun dispatchMessage(
        messageId: Int,
        method: String,
        params: JSONObject? = null,
        crossTread: Boolean = false
    ) {
        val pendingMessage =
            pendingMessageQueue.firstOrNull { msg -> msg.method == method && msg.status == MessageState.Pending }
        pendingMessage?.status = MessageState.SentToJ2v8

        val message = JSONObject()
            .put("id", messageId)
            .put("method", method)
            .put("params", params)

        logger.d(TAG, "dispatching $message")
        if (crossTread) {
            v8Executor?.execute { submitMessageToJ2v8(message) }
        } else {
            submitMessageToJ2v8(message)
        }
    }

    private fun submitMessageToJ2v8(message: JSONObject) {
        logger.d(TAG, "submitMessageToJ2v8: $message")
        v8Inspector?.dispatchProtocolMessage(message.toString())
    }

    /**
     * Track messages waiting for responses.
     * These Ids are set when the message is created so the response can be tied back to the request
     */
    private data class PendingResponse(
        val method: String,
        val messageId: Int,
        val params: JSONObject?
    ) {
        var response: String? = null
        var status: MessageState = MessageState.Pending
    }

    internal enum class MessageState {
        Pending,
        SentToJ2v8,
        GotResponse,
    }

    internal enum class DebuggerState {
        Disconnected,
        Connected,
        Paused,
    }


    companion object {
        const val TAG = "V8Messenger"
    }
}