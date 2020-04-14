package com.salesforce.j2v8debugger

import android.content.Context
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.inspector.V8Inspector
import com.facebook.stetho.InspectorModulesProvider
import com.facebook.stetho.Stetho
import com.facebook.stetho.inspector.console.RuntimeReplFactory
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain
import com.salesforce.j2v8debugger.utils.logger
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ExecutorService
import com.facebook.stetho.inspector.protocol.module.Debugger as FacebookDebuggerStub
import com.facebook.stetho.inspector.protocol.module.Runtime as FacebookRuntimeBase


object StethoHelper {
    var debugger: Debugger? = null
        private set

    private var v8InspectorRef: WeakReference<V8Inspector>? = null
    private var v8ExecutorRef: WeakReference<ExecutorService>? = null

    /**
     * Changing this prefix lead to changing the path of exposed to Chrome DevTools scripts.
     * It results in "collapsing" all further parsed scripts under path specified.
     * For multiple collapsed elements '/' could be used inside the string path.
     *
     * E.g. If set to "user1" or "user2" - scripts will be collapsed under "user1" or "user2" segment
     *  in Chrome DevTools UI.
     */
    var scriptsPathPrefix = ""
        set(value) {
            field = "/" + value + "/"
        }

    /**
     * @return Similar to [Stetho.defaultInspectorModulesProvider] but contains [Debugger] for [V8]
     */
    @JvmStatic
    fun defaultInspectorModulesProvider(
        context: Context,
        scriptSourceProvider: ScriptSourceProvider,
        v8Debugger: V8Debugger
    ): InspectorModulesProvider {
        return InspectorModulesProvider { getInspectorModules(context, scriptSourceProvider, v8Debugger) }
    }

    @JvmOverloads
    fun getInspectorModules(
        context: Context,
        scriptSourceProvider: ScriptSourceProvider,
        v8Debugger: V8Debugger,
        factory: RuntimeReplFactory? = null
    ): Iterable<ChromeDevtoolsDomain> {
        return try {
            getDefaultInspectorModulesWithDebugger(context, scriptSourceProvider, v8Debugger, factory)
        } catch (e: Throwable) { //v8 throws Error instead of Exception on wrong thread access, etc.
            logger.e(
                Debugger.TAG,
                "Unable to init Stetho with V8 Debugger. Default set-up will be used",
                e
            )

            getDefaultInspectorModules(context, factory)
        }
    }

    fun getDefaultInspectorModulesWithDebugger(
        context: Context,
        scriptSourceProvider: ScriptSourceProvider,
        v8Debugger: V8Debugger,
        factory: RuntimeReplFactory? = null
    ): Iterable<ChromeDevtoolsDomain> {
        val defaultInspectorModules = getDefaultInspectorModules(context, factory)

        //remove work-around when https://github.com/facebook/stetho/pull/600 is merged
        val inspectorModules = ArrayList<ChromeDevtoolsDomain>()
        for (defaultModule in defaultInspectorModules) {
            if (FacebookDebuggerStub::class != defaultModule::class
                && FacebookRuntimeBase::class != defaultModule::class
            ) {
                inspectorModules.add(defaultModule)
            }
        }

        debugger = Debugger(scriptSourceProvider, v8Debugger)
        inspectorModules.add(debugger!!)
        inspectorModules.add(Runtime(v8Debugger, factory))

        bindV8ToChromeDebuggerIfReady()

        return inspectorModules
    }

    /**
     * @param v8Executor executor, where V8 should be previously initialized and further will be called on.
     */
    fun initializeWithV8Debugger(v8Inspector: V8Inspector, v8Executor: ExecutorService) {
        v8InspectorRef = WeakReference(v8Inspector)
        v8ExecutorRef = WeakReference(v8Executor)

        bindV8ToChromeDebuggerIfReady()
    }

    /**
     * Inform Chrome DevTools, that scripts are changed. Currently closes Chrome DevTools.
     * New content will be displayed when it will be opened again.
     */
    fun notifyScriptsChanged() {
        //todo: check if we can "update" scripts already reported with "Debugger.scriptParsed"
        debugger?.onScriptsChanged()
    }

    private fun bindV8ToChromeDebuggerIfReady() {
        val chromeDebuggerAttached = debugger != null

        val v8Inspector = v8InspectorRef?.get()
        val v8Executor = v8ExecutorRef?.get()
        val v8DebuggerInitialized = v8Inspector != null && v8Executor != null

        if (v8DebuggerInitialized && chromeDebuggerAttached) {
            v8Executor!!.execute {
                bindV8DebuggerToChromeDebugger(
                    debugger!!,
                    v8Inspector!!,
                    v8Executor
                )
            }
        }
    }

    /**
     * Shoulds be called when both Chrome debugger and V8 debugger is ready
     *  (When Chrome DevTools UI is open and V8 is created in debug mode with debugger object).
     */
    private fun bindV8DebuggerToChromeDebugger(
        chromeDebugger: Debugger,
        v8Inspector: V8Inspector,
        v8Executor: ExecutorService
    ) {
        chromeDebugger.initialize(v8Inspector, v8Executor)
    }

    /**
     * @return default Stetho.DefaultInspectorModulesBuilder
     *
     * @param context Android context, which is required to access android resources by Stetho.
     * @param factory copies behaviour of [Stetho.DefaultInspectorModulesBuilder.runtimeRepl] using [Runtime]
     */
    private fun getDefaultInspectorModules(
        context: Context,
        factory: RuntimeReplFactory?
    ): Iterable<ChromeDevtoolsDomain> {
        return Stetho.DefaultInspectorModulesBuilder(context).runtimeRepl(factory)
            .finish()
    }
}
