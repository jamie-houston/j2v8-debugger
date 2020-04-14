package com.salesforce.j2v8debugger.utils

import android.util.Log

internal var logger = Logger()

class Logger {

    fun i(tag: String, msg: String) = Log.i(tag, msg)

    fun w(tag: String, msg: String) = Log.w(tag, msg)

    fun w(tag: String, msg: String, tr: Throwable) = Log.w(tag, msg, tr)

    fun e(tag: String, msg: String, tr: Throwable) = Log.e(tag, msg, tr)

    fun getStackTraceString(tr: Throwable): String = Log.getStackTraceString(tr)
}