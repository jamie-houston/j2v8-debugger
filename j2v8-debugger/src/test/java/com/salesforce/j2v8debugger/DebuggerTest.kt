package com.salesforce.j2v8debugger

import com.salesforce.j2v8debugger.utils.logger
import com.eclipsesource.v8.inspector.V8Inspector
import com.facebook.stetho.json.ObjectMapper
import com.google.common.util.concurrent.MoreExecutors
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.random.Random

class DebuggerTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun setUpClass() {
            logger = mockk(relaxed = true)
        }
    }

    @Test
    fun `on enable all scripts retrieved`() {
        val scriptSourceProviderMock = mockk<ScriptSourceProvider> (relaxed = true)
        val debugger = Debugger(scriptSourceProviderMock, mockk(relaxed = true))

        debugger.enable(mockk(relaxed = true), null)

        verify(exactly = 1) {
            scriptSourceProviderMock.allScriptIds
        }
    }

    //todo test all @ChromeDevtoolsMethod

    @Test
    fun `works when V8 initialized`() {
        val v8InspectorMock = mockk<V8Inspector>(relaxed = true)
        val scriptSourceProviderMock = mockk<ScriptSourceProvider> (relaxed = true)
        val directExecutor = MoreExecutors.newDirectExecutorService()

        val v8Debugger = mockk<V8Debugger>(relaxed = true)
        val debugger = Debugger(scriptSourceProviderMock, v8Debugger)
        debugger.initialize(v8InspectorMock, directExecutor)

        val requestStub = SetBreakpointByUrlRequest()
        requestStub.url = "testUrl"
        requestStub.lineNumber = Random(100).nextInt()
        requestStub.columnNumber = Random(100).nextInt()

        val jsonParamsMock = mockk<JSONObject>()
        val mapperMock = mockk<ObjectMapper> {
            every { convertValue(eq(jsonParamsMock), eq(requestStub::class.java)) } returns  requestStub
        }
        debugger.dtoMapper = mapperMock

        val response = debugger.setBreakpointByUrl(mockk(), jsonParamsMock)

        verify (exactly = 1){mapperMock.convertValue(eq(jsonParamsMock), eq(requestStub::class.java))}

        verify { v8Debugger.dispatchMessage(Protocol.Debugger.SetBreakpointByUrl, any()) }

        assertTrue(response is SetBreakpointByUrlResponse)
        val responseLocation: Location = (response as SetBreakpointByUrlResponse).locations[0]

        assertEquals(requestStub.scriptId, responseLocation.scriptId)
        val lineNumber = requestStub.lineNumber
        val columnNumber = requestStub.columnNumber
        assertEquals(lineNumber, responseLocation.lineNumber)
        assertEquals(columnNumber, responseLocation.columnNumber)
    }

    @Test
    fun `No exceptions thrown when V8 not initialized`() {
        val scriptSourceProviderMock = mockk<ScriptSourceProvider> {}
        val debugger = Debugger(scriptSourceProviderMock, mockk())


        val requestMock = mockk<SetBreakpointByUrlRequest>()
        val jsonParamsMock = mockk<JSONObject>()
        val mapperMock = mockk<ObjectMapper> {
            every { convertValue(eq(jsonParamsMock), eq(requestMock::class.java)) } returns requestMock
        }
        debugger.dtoMapper = mapperMock

        val response = debugger.setBreakpointByUrl(mockk(), jsonParamsMock)

        verify {mapperMock wasNot called}
        verify {requestMock wasNot called}
        verify {jsonParamsMock wasNot called}

        assertTrue(response == null)
    }

}