package com.alexii.j2v8debugger.utils

import android.util.Log
import androidx.annotation.VisibleForTesting

internal var logger = Logger()
    @VisibleForTesting get
    @VisibleForTesting set

class Logger {

    fun i(tag: String, msg: String) = Log.i(tag, msg)

    fun w(tag: String, msg: String) = Log.w(tag, msg)

    fun w(tag: String, msg: String, tr: Throwable) = Log.w(tag, msg, tr)

    fun e(tag: String, msg: String, tr: Throwable) = Log.e(tag, msg, tr)

    fun getStackTraceString(tr: Throwable) = Log.getStackTraceString(tr)
}