package com.salesforce.j2v8inspector

import com.salesforce.j2v8inspector.model.CdpMethod
import com.salesforce.j2v8inspector.utils.LogUtils
import com.eclipsesource.v8.V8
import org.json.JSONObject
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

object V8Debugger {
    private const val MAX_SCRIPT_CACHE_SIZE = 10000000
    private const val MAX_DEPTH = 32

    /**
     * Utility, which simplifies configuring V8 for debugging support and creation of new instance.
     * Creates V8 runtime, v8 debugger and binds it to Stetho.
     *
     * @param v8Executor single-thread executor where v8 will be created
     *  and all debug calls will be performed by Stetho later.
     *
     * NOTE: Should be declared as V8 class extensions when will be allowed (https://youtrack.jetbrains.com/issue/KT-11968)
     */
    fun createDebuggableV8Runtime(
        v8Executor: ExecutorService,
        globalAlias: String = "global",
        enableLogging: Boolean = true
    ): Future<V8> {
        LogUtils.enabled = enableLogging
        return v8Executor.submit(Callable {
            val runtime = V8.createV8Runtime(globalAlias)
            val messenger = V8Messenger(runtime, v8Executor)
            with(messenger) {
                // Default Chrome DevTool protocol messages
                sendMessage(CdpMethod.Runtime.Enable, crossThread = false)

                sendMessage(
                    CdpMethod.Debugger.Enable,
                    JSONObject().put("maxScriptsCacheSize", MAX_SCRIPT_CACHE_SIZE),
                    crossThread = false
                )
                sendMessage(
                    CdpMethod.Debugger.SetAsyncCallStackDepth,
                    JSONObject().put("maxDepth", MAX_DEPTH),
                    crossThread = false
                )
                sendMessage(CdpMethod.Runtime.RunIfWaitingForDebugger, crossThread = false)
            }

            StethoHelper.initializeWithV8Messenger(messenger, v8Executor)

            runtime
        })
    }
}