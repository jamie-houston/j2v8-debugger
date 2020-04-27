/*
 * Copyright (c) 2020, Salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 *
 */

package com.salesforce.j2v8debugger.utils

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoggerTest {
    val tag = "MyTag"
    val msg = "My message"
    val exception = RuntimeException()

    @BeforeEach
    fun setUp() {
        LogUtils.enabled = true
        mockkStatic(Log::class)
    }

    @AfterEach
    fun cleanUp() {
        LogUtils.enabled = false
        unmockkStatic(Log::class)
    }

    @Test
    fun `Logger i() redirects to Android Log`() {
        every { Log.i(any(), any()) } returns 0

        logger.i(tag, msg)

        verify { Log.i(tag, msg) }
    }

    @Test
    fun `Logger w() redirects to Android Log`() {
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any(), any()) } returns 0

        logger.w(tag, msg)
        logger.w(tag, msg, exception)

        verify { Log.w(tag, msg) }
        verify { Log.w(tag, msg, exception) }
    }


    @Test
    fun `Logger e() redirects to Android Log`() {
        every { Log.e(any(), any(), any()) } returns 0

        logger.e(tag, msg, exception)

        verify { Log.e(tag, msg, exception) }
    }

    @Test
    fun `Logger getStackTraceString() redirects to Android Log`() {
        every { Log.getStackTraceString(any()) } returns ""

        logger.getStackTraceString(exception)

        verify { Log.getStackTraceString(exception) }
    }

    @Test
    fun `Logger disabled doesn't log`(){
        LogUtils.enabled = false
        every { Log.i(any(), any()) } returns 0

        logger.i(tag, msg)

        verify (exactly = 0){ Log.i(tag, msg) }
    }
}