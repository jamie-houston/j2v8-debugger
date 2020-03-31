package com.alexii.j2v8debugger

import android.app.Application
import com.eclipsesource.v8.inspector.V8Inspector
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ExecutorService
import com.facebook.stetho.inspector.protocol.module.Debugger as FacebookDebuggerStub
import com.facebook.stetho.inspector.protocol.module.Runtime as FacebookRuntimeBase

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class StethoHelperTest {

    @Test
    fun `returns custom Debugger and no Stetho Debugger Stub`() {
        val scriptSourceProviderMock = mockk<ScriptSourceProvider> {}
        val contextMock = mockk<Application> {}
        every {
            contextMock.applicationContext
        } returns contextMock

        val domains = StethoHelper.getDefaultInspectorModulesWithDebugger(contextMock, scriptSourceProviderMock)

        assertTrue("No Debugger present", domains.any { it.javaClass == Debugger::class.java })
        assertFalse("Stetho Debugger present", domains.any { it.javaClass == FacebookDebuggerStub::class.java })
    }

    @Test
    fun `returns custom Runtime and no Stetho base Runtime`() {
        val scriptSourceProviderMock = mockk<ScriptSourceProvider> {}
        val contextMock = mockk<Application> {}
        every { contextMock.applicationContext } returns contextMock

        val domains = StethoHelper.getDefaultInspectorModulesWithDebugger(contextMock, scriptSourceProviderMock)

        assertTrue("No Debugger present", domains.any { it.javaClass == Runtime::class.java })
        assertFalse("Stetho Debugger present", domains.any { it.javaClass == FacebookRuntimeBase::class.java })
    }

    @Test
    fun `initialized when Stetho created before v8`() {
        val scriptSourceProviderMock = mockk<ScriptSourceProvider> {}
        val contextMock = mockk<Application> {}
        every { contextMock.applicationContext } returns contextMock
        StethoHelper.getDefaultInspectorModulesWithDebugger(contextMock, scriptSourceProviderMock)

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

        StethoHelper.getDefaultInspectorModulesWithDebugger(contextMock, scriptSourceProviderMock)

        verify(exactly = 1) { v8ExecutorServiceMock.execute(any()) }
        assertTrue(StethoHelper.isStethoAndV8DebuggerFullyInitialized)
    }


    val StethoHelper.isStethoAndV8DebuggerFullyInitialized
        get() = this.debugger?.v8Inspector != null
}