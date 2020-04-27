package com.alexii.j2v8debugger

import com.facebook.stetho.json.ObjectMapper
import org.json.JSONObject
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.Random
import kotlin.test.assertEquals

class MapperTests {
    @Test
    fun `setBreakpointByUrl gets scriptId from url`(){
        val dtoMapper= ObjectMapper()
        val prefix = Random().nextInt(100).toString()
        StethoHelper.scriptsPathPrefix = prefix
        val url = UUID.randomUUID().toString()

        val params = JSONObject("""{"lineNumber":6,"url":"http:\/\/app\/\/$prefix\/$url","columnNumber":0,"condition":""}""")
        val request = dtoMapper.convertValue(params, SetBreakpointByUrlRequest::class.java)
        assertEquals(request.scriptId, url)
    }
}