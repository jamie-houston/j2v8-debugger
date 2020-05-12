/*
 * Copyright (c) 2020, Salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 *
 */

package com.alexii.j2v8debugger

import com.facebook.stetho.json.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.json.JSONObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class V8MessengerTests {
    @Test
    fun `debugger script parsed sets scriptId`(){
        val v8Messenger = spyk(V8Messenger(mockk(relaxed = true)), recordPrivateCalls = true)
        val scriptId = "12345"
        val url = "98765"
        val parsedEvent = JSONObject("""{"method":"Debugger.scriptParsed","params":{"scriptId": "$scriptId", "url": "$url"}}""")
        v8Messenger.onResponse(parsedEvent.toString())
    }

    @Test
    fun `debugger paused sets debuggerState to paused`(){
        val v8Messenger = spyk(V8Messenger(mockk(relaxed = true)), recordPrivateCalls = true)
//        val response = V8Response()
//        val mapperMock = mockk<ObjectMapper>(relaxed = true) {
//            every { convertValue(any(), eq(V8Response::class.java)) } returns  response
//        }
//        every {
//            v8Messenger.getProperty("dtoMapper")
//        } returns mapperMock
//
        val pausedEvent = JSONObject("""{"method":"Debugger.paused","params":{"callFrames":[{"callFrameId":"{\"ordinal\":0,\"injectedScriptId\":1}","functionName":"","functionLocation":{"scriptId":"9","lineNumber":0,"columnNumber":0},"location":{"scriptId":"9","lineNumber":0,"columnNumber":15},"url":"hello-world1","scopeChain":[{"type":"global","object":{"type":"object","className":"global","description":"global","objectId":"{\"injectedScriptId\":1,\"id\":1}"}}],"this":{"type":"object","className":"global","description":"global","objectId":"{\"injectedScriptId\":1,\"id\":2}"}}],"reason":"","hitBreakpoints":[]}}""")
        v8Messenger.onResponse(pausedEvent.toString())
//        assertEquals(v8Messenger.getDebuggerState(), V8Messenger.DebuggerState.Paused)
    }

    @Test
    fun `ScriptIds are replaced`(){
        val v8ScriptMap = mutableMapOf<String, String>()
        v8ScriptMap["9"] = "hello-world"
        val regex = "\"scriptId\":\"(\\d+)\"".toRegex()
        val incomingParams = """{"method":"Debugger.paused","params":{"callFrames":[{"callFrameId":"{\"ordinal\":0,\"injectedScriptId\":1}","functionName":"","functionLocation":{"scriptId":"9","lineNumber":0,"columnNumber":0},"location":{"scriptId":"9","lineNumber":0,"columnNumber":15},"url":"hello-world1","scopeChain":[{"type":"global","object":{"type":"object","className":"global","description":"global","objectId":"{\"injectedScriptId\":1,\"id\":1}"}}],"this":{"type":"object","className":"global","description":"global","objectId":"{\"injectedScriptId\":1,\"id\":2}"}}],"reason":"","hitBreakpoints":[]}}"""
        val updatedScript = incomingParams.toString().replace(regex) {
            "\"scriptId\":\"${v8ScriptMap[it.groups[1]?.value]}\""
        }
        val result = """{"method":"Debugger.paused","params":{"callFrames":[{"callFrameId":"{\"ordinal\":0,\"injectedScriptId\":1}","functionName":"","functionLocation":{"scriptId":"hello-world","lineNumber":0,"columnNumber":0},"location":{"scriptId":"hello-world","lineNumber":0,"columnNumber":15},"url":"hello-world1","scopeChain":[{"type":"global","object":{"type":"object","className":"global","description":"global","objectId":"{\"injectedScriptId\":1,\"id\":1}"}}],"this":{"type":"object","className":"global","description":"global","objectId":"{\"injectedScriptId\":1,\"id\":2}"}}],"reason":"","hitBreakpoints":[]}}"""

        assertEquals(updatedScript, result)
    }
}