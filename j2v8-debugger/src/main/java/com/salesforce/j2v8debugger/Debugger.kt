/*
 * Copyright (c) 2020, Salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 *
 */

package com.salesforce.j2v8debugger

import com.eclipsesource.v8.inspector.V8Inspector
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult
import com.facebook.stetho.inspector.network.NetworkPeerManager
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod
import com.facebook.stetho.json.ObjectMapper
import com.salesforce.j2v8debugger.utils.LogUtils
import com.salesforce.j2v8debugger.utils.logger
import org.json.JSONObject
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import com.facebook.stetho.inspector.protocol.module.Debugger as FacebookDebuggerStub

/**
 * V8 JS Debugger. Name of the class and methods must match names defined in Chrome Dev Tools protocol.
 *
 * [initialize] must be called before actual debugging (adding breakpoints in Chrome DevTools).
 *  Otherwise setting breakpoint, etc. makes no effect.
 *  Above is also true for the case when debugger is paused due to pause/resume implementation as thread suspension.
 */ @Suppress("UNUSED_PARAMETER", "unused")
class Debugger(
    private val scriptSourceProvider: ScriptSourceProvider,
    private val v8Debugger: V8Debugger
) : FacebookDebuggerStub() {
    var dtoMapper: ObjectMapper = ObjectMapper()

    //xxx: consider using WeakReference
    /** Must be called on [v8Executor]]. */
    var v8Inspector: V8Inspector? = null
        private set

    /**
     * Needed as @ChromeDevtoolsMethod methods are called on Stetho threads, but not v8 thread.
     *
     * XXX: consider using ThreadBound from Facebook with an implementation, which uses Executor.
     */
    private var v8Executor: ExecutorService? = null

    private var connectedPeer: JsonRpcPeer? = null

    private val breakpointsAdded = mutableListOf<String>()

    companion object {
        const val TAG = "j2v8-debugger"
    }

    fun initialize(v8Inspector: V8Inspector, v8Executor: ExecutorService) {
        this.v8Executor = v8Executor
        this.v8Inspector = v8Inspector
    }

    private fun validateV8Initialized() {
        if (v8Executor == null || v8Inspector == null) {
            throw IllegalStateException("Unable to set breakpoint when v8 was not initialized yet")
        }
    }

    @ChromeDevtoolsMethod
    override fun enable(peer: JsonRpcPeer, params: JSONObject?) {
        runStethoSafely {
            connectedPeer = peer

            scriptSourceProvider.allScriptIds
                .map { ScriptParsedEvent(it) }
                .forEach { peer.invokeMethod(Protocol.Debugger.ScriptParsed, it, null) }

            peer.registerDisconnectReceiver(::onDisconnect)
        }
        v8Debugger.setDebuggerConnected(true)
    }

    @ChromeDevtoolsMethod
    fun setOverlayMessage(peer: JsonRpcPeer, params: JSONObject?) {
        // Ignore
    }

    @ChromeDevtoolsMethod
    fun evaluateOnCallFrame(peer: JsonRpcPeer, params: JSONObject?): JsonRpcResult? {
        val method = Protocol.Debugger.EvaluateOnCallFrame
        val result = v8Debugger.getV8Result(method, params)
        return EvaluateOnCallFrameResult(JSONObject(result))
    }

    @ChromeDevtoolsMethod
    fun setSkipAllPauses(peer: JsonRpcPeer, params: JSONObject?) {
        // This was changed from skipped to skip
        // https://chromium.googlesource.com/chromium/src/third_party/WebKit/Source/platform/v8_inspector/+/e7a781c04b7822a46e7de465623152ff1b45bdac%5E%21/
        v8Debugger.queueV8Message(Protocol.Debugger.SetSkipAllPauses, JSONObject().put("skip", params?.getBoolean("skipped")))
    }

    private fun onDisconnect() {
        logger.d(TAG, "Disconnecting from Chrome")
        runStethoSafely {
            breakpointsAdded.forEach { breakpointId ->
                v8Executor?.execute {
                    v8Debugger.dispatchMessage(
                        Protocol.Debugger.RemoveBreakpoint,
                        "{\"breakpointId\": \"$breakpointId\"}"
                    )
                }
            }
            breakpointsAdded.clear()

            NetworkPeerManager.getInstanceOrNull()?.removePeer(connectedPeer)
            connectedPeer = null

            //avoid app being freezed when no debugging happening anymore
            v8Debugger.setDebuggerConnected(false)
        }
    }

    /**
     * Invoked when scripts are changed. Currently closes Chrome DevTools.
     */
    internal fun onScriptsChanged() {
        scriptSourceProvider.allScriptIds
            .map { ScriptParsedEvent(it) }
            .forEach { connectedPeer?.invokeMethod(Protocol.Debugger.ScriptParsed, it, null) }
    }

    @ChromeDevtoolsMethod
    override fun disable(peer: JsonRpcPeer, params: JSONObject?) {
        v8Debugger.setDebuggerConnected(false)
    }

    @ChromeDevtoolsMethod
    fun getScriptSource(peer: JsonRpcPeer, params: JSONObject): JsonRpcResult? {
        return runStethoAndV8Safely {
            try {
                val request = dtoMapper.convertValue(params, GetScriptSourceRequest::class.java)
                val scriptSource = scriptSourceProvider.getSource(request.scriptId!!)
                GetScriptSourceResponse(scriptSource)
            } catch (e: Exception) {
                // Send exception as source code for debugging.
                // Otherwise If error is thrown - Stetho reports broken I/O pipe and disconnects
                GetScriptSourceResponse(logger.getStackTraceString(e))
            }
        }
    }

    @ChromeDevtoolsMethod
    fun resume(peer: JsonRpcPeer, params: JSONObject?) {
        v8Debugger.queueV8Message(Protocol.Debugger.Resume, params)
    }

    @ChromeDevtoolsMethod
    fun pause(peer: JsonRpcPeer, params: JSONObject?) {
        v8Debugger.queueV8Message(Protocol.Debugger.Pause, params)
    }

    @ChromeDevtoolsMethod
    fun stepOver(peer: JsonRpcPeer, params: JSONObject?) {
        v8Debugger.queueV8Message(Protocol.Debugger.StepOver, params)
    }

    @ChromeDevtoolsMethod
    fun stepInto(peer: JsonRpcPeer, params: JSONObject?) {
        v8Debugger.queueV8Message(Protocol.Debugger.StepInto, params)
    }

    @ChromeDevtoolsMethod
    fun stepOut(peer: JsonRpcPeer, params: JSONObject?) {
        v8Debugger.queueV8Message(Protocol.Debugger.StepOut, params)
    }

    @ChromeDevtoolsMethod
    fun setBreakpoint(peer: JsonRpcPeer?, params: JSONObject?): JsonRpcResult? {
        //Looks like this method should not be called at all.
        val action: () -> JsonRpcResult? = {
            throw IllegalArgumentException("Unexpected Debugger.setBreakpoint() is called by Chrome DevTools: $params")
        }
        return runStethoSafely(action)
    }

    @ChromeDevtoolsMethod
    fun setBreakpointByUrl(peer: JsonRpcPeer, params: JSONObject): JsonRpcResult? {
        return runStethoAndV8Safely {
            val responseFuture = v8Executor!!.submit(Callable {
                val request = dtoMapper.convertValue(params, SetBreakpointByUrlRequest::class.java)
                val breakpointParams =
                    JSONObject().put("lineNumber", request.lineNumber).put("url", request.scriptId)
                        .put("columnNumber", request.columnNumber)
                v8Debugger.dispatchMessage(
                    Protocol.Debugger.SetBreakpointByUrl,
                    breakpointParams.toString()
                )
                val breakpointId =
                    "1:${request.lineNumber}:${request.columnNumber}:${request.scriptId}"
                breakpointsAdded.add(breakpointId)
                SetBreakpointByUrlResponse(
                    breakpointId,
                    Location(request.scriptId!!, request.lineNumber!!, request.columnNumber!!)
                )
            })

            responseFuture.get()
        }
    }

    @ChromeDevtoolsMethod
    fun removeBreakpoint(peer: JsonRpcPeer, params: JSONObject) {
        //Chrome DevTools are removing breakpoint from UI regardless of the response (unlike setting breakpoint):
        // -> do best effort to remove breakpoint when executor is free
        runStethoAndV8Safely {
            v8Executor?.execute {
                v8Debugger.dispatchMessage(
                    Protocol.Debugger.RemoveBreakpoint,
                    params.toString()
                )
            }
        }
        breakpointsAdded.remove(params.getString("breakpointId"))
    }

    @ChromeDevtoolsMethod
    fun setAsyncCallStackDepth(peer: JsonRpcPeer, params: JSONObject): JsonRpcResult {
        return SimpleIntegerResult(32)
    }

    @ChromeDevtoolsMethod
    fun setBreakpointsActive(peer: JsonRpcPeer, params: JSONObject) {
        runStethoAndV8Safely {
            v8Executor?.execute { v8Debugger.dispatchMessage(Protocol.Debugger.SetBreakpointsActive, params.toString()) }
        }
    }

    /**
     *  Safe for Stetho - makes sure that no exception is thrown.
     *  Safe for V8 - makes sure, that v8 initialized and v8 thread is not not paused in debugger.
     */
    private fun <T> runStethoSafely(action: () -> T): T? {
        LogUtils.logChromeDevToolsCalled()

        return try {
            action()
        } catch (e: Throwable) { //not Exception as V8 throws Error
            // Otherwise If error is thrown - Stetho reports broken I/O pipe and disconnects
            logger.w(TAG, "Unable to perform " + LogUtils.getChromeDevToolsMethodName(), e)
            null
        }
    }

    /**
     * Safe for Stetho - makes sure that no exception is thrown.
     * If any exception then [JsonRpcError] is thrown from method annotated with @ChromeDevtoolsMethod-
     * Stetho reports broken I/O pipe and Chrome DevTools disconnects.
     */
    private fun <T> runStethoAndV8Safely(action: () -> T): T? {
        return runStethoSafely {
            validateV8Initialized()

            action()
        }
    }
}
