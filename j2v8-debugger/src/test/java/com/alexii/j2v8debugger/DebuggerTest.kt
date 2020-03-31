//package com.alexii.j2v8debugger
//
//import com.alexii.j2v8debugger.utils.logger
//import com.eclipsesource.v8.debug.DebugHandler
//import com.eclipsesource.v8.inspector.V8Inspector
//import com.facebook.stetho.json.ObjectMapper
//import com.google.common.util.concurrent.MoreExecutors
//import io.mockk.mockk
//import io.mockk.verify
//import org.json.JSONObject
//import org.junit.Assert.assertEquals
//import org.junit.Assert.assertTrue
//import org.junit.BeforeClass
//import org.junit.Test
//
//class DebuggerTest {
//    companion object {
//        @BeforeClass
//        @JvmStatic
//        fun setUpClass() {
//            logger = mockk()
//        }
//    }
//
//    @Test
//    fun `on enable all scripts retrieved`() {
//        val scriptSourceProviderMock = mockk<ScriptSourceProvider> {}
//        val debugger = Debugger(scriptSourceProviderMock)
//
//        debugger.enable(mockk(), null)
//
//        verify(exactly = 1) {
//            scriptSourceProviderMock.allScriptIds
//        }
//    }
//
//    //todo test all @ChromeDevtoolsMethod
//
//    @Test
//    fun `works when V8 initialized`() {
//        val v8DebugHandlerMock = mockk<V8Inspector>()
//        val scriptSourceProviderMock = mockk<ScriptSourceProvider> {}
//        val directExecutor = MoreExecutors.newDirectExecutorService()
//
//        val debugger = Debugger(scriptSourceProviderMock)
//        debugger.initialize(v8DebugHandlerMock, directExecutor)
//
//        verify(v8DebugHandlerMock).addBreakHandler(any())
//
//
//        val requestStub = Debugger.SetBreakpointByUrlRequest()
//        requestStub.url = "testUrl"
//        requestStub.lineNumber = 0;
//        requestStub.columnNumber = 0
//
//        val jsonParamsMock = mock<JSONObject>()
//        val mapperMock = mock<ObjectMapper> {
//            on { convertValue(eq(jsonParamsMock), eq(requestStub::class.java)) } doReturn requestStub
//        }
//        debugger.dtoMapper = mapperMock
//
//        val response = debugger.setBreakpointByUrl(mock(), jsonParamsMock)
//
//        verify(mapperMock, times(1)).convertValue(eq(jsonParamsMock), eq(requestStub::class.java))
//        verifyNoMoreInteractions(mapperMock)
//
//        verify(v8DebugHandlerMock).setScriptBreakpoint(eq(requestStub.scriptId), eq(requestStub.lineNumber!!))
//        verifyNoMoreInteractions(v8DebugHandlerMock)
//
//        assertTrue(response is Debugger.SetBreakpointByUrlResponse)
//        val responseLocation: Debugger.Location = (response as Debugger.SetBreakpointByUrlResponse).locations[0]
//
//        assertEquals(requestStub.scriptId, responseLocation.scriptId)
//        assertEquals(requestStub.lineNumber, responseLocation.lineNumber)
//        assertEquals(requestStub.columnNumber, responseLocation.columnNumber)
//    }
//
//    @Test
//    fun `No exceptions thrown when V8 not initialized`() {
//        val scriptSourceProviderMock = mock<ScriptSourceProvider> {}
//        val debugger = Debugger(scriptSourceProviderMock)
//
//
//        val requestMock = mock<Debugger.SetBreakpointByUrlRequest>()
//        val jsonParamsMock = mock<JSONObject>()
//        val mapperMock = mock<ObjectMapper> {
//            on { convertValue(eq(jsonParamsMock), eq(requestMock::class.java)) } doReturn requestMock
//        }
//        debugger.dtoMapper = mapperMock
//
//        val response = debugger.setBreakpointByUrl(mock(), jsonParamsMock)
//
//        verifyZeroInteractions(mapperMock)
//        verifyZeroInteractions(requestMock)
//        verifyZeroInteractions(jsonParamsMock)
//
//        assertTrue(response == null)
//    }
//
//}