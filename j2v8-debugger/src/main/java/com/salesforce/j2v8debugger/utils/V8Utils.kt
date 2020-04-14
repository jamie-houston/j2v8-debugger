/*
 * Copyright (c) 2020, Salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 *
 */

package com.salesforce.j2v8debugger.utils

import com.eclipsesource.v8.V8

/**
 * Releases V8 and V8 debugger if any was created.
 *
 * Must be called on V8's thread becuase of the J2V8 limitations.
 *
 * @see V8.release
 * @see releaseV8Debugger
 */
@JvmOverloads
fun V8.releaseDebuggable(reportMemoryLeaks: Boolean = true) {
    // TODO: Implement add rewire releaseV8Debugger
    // releaseV8Debugger()
    this.release(reportMemoryLeaks)
}
