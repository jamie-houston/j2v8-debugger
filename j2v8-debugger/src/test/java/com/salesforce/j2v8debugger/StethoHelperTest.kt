package com.salesforce.j2v8debugger

import android.app.Application
import com.eclipsesource.v8.inspector.V8Inspector
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.ExecutorService
import com.facebook.stetho.inspector.protocol.module.Debugger as FacebookDebuggerStub
import com.facebook.stetho.inspector.protocol.module.Runtime as FacebookRuntimeBase

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StethoHelperTest {

    @Test
    fun `returns custom Debugger and no Stetho Debugger Stub`() {
        val scriptSourceProviderMock = mockk<ScriptSourceProvider> {}
        val contextMock = mockk<Application> {}
        every {
            contextMock.applicationContext
        } returns contextMock

        val domains = StethoHelper.getDefaultInspectorModulesWithDebugger(contextMock, scriptSourceProviderMock, mockk())

        assertTrue(domains.any { it.javaClass == Debugger::class.java }, "No Debugger present")
        assertFalse(domains.any { it.javaClass == FacebookDebuggerStub::class.java }, "Stetho Debugger present")
    }

    @Test
    fun `returns custom Runtime and no Stetho base Runtime`() {
        val scriptSourceProviderMock = mockk<ScriptSourceProvider> {}
        val contextMock = mockk<Application> {}
        every { contextMock.applicationContext } returns contextMock

        val domains = StethoHelper.getDefaultInspectorModulesWithDebugger(contextMock, scriptSourceProviderMock, mockk())

        assertTrue(domains.any { it.javaClass == Runtime::class.java }, "No Debugger present")
        assertFalse(domains.any { it.javaClass == FacebookRuntimeBase::class.java }, "Stetho Debugger present")
    }

    @Test
    fun `initialized when Stetho created before v8`() {
        val scriptSourceProviderMock = mockk<ScriptSourceProvider> {}
        val contextMock = mockk<Application> {}
        every { contextMock.applicationContext } returns contextMock
        StethoHelper.getDefaultInspectorModulesWithDebugger(contextMock, scriptSourceProviderMock, mockk())

        val v8InspectorMock = mockk<V8Inspector>()
        val v8ExecutorServiceMock = mockk<ExecutorService> {
            every {
                execute(any())
            } answers { arg<Runnable>(0).run() }
        }
        StethoHelper.initializeWithV8Debugger(v8InspectorMock, v8ExecutorServiceMock)

        verify(exactly = 1) { v8ExecutorServiceMock.execute(any()) }
        assertTrue(StethoHelper.isStethoAndV8DebuggerFullyInitialized)
    }

    //xxx: check why test is failing if run together with other, but ok when run separately
    @Test
    fun `initialized when v8 created before Stetho`() {
        val v8InspectorMock = mockk<V8Inspector>()
        val v8ExecutorServiceMock = mockk<ExecutorService> {
            every { execute(any()) } answers { arg<Runnable>(0).run() }
        }
        StethoHelper.initializeWithV8Debugger(v8InspectorMock, v8ExecutorServiceMock)

        val scriptSourceProviderMock = mockk<ScriptSourceProvider> {}
        val contextMock = mockk<Application> {}
        every { contextMock.applicationContext } returns contextMock

        StethoHelper.getDefaultInspectorModulesWithDebugger(contextMock, scriptSourceProviderMock, mockk())

        verify(exactly = 1) { v8ExecutorServiceMock.execute(any()) }
        assertTrue(StethoHelper.isStethoAndV8DebuggerFullyInitialized)
    }


    val StethoHelper.isStethoAndV8DebuggerFullyInitialized
        get() = this.debugger?.v8Inspector != null
}