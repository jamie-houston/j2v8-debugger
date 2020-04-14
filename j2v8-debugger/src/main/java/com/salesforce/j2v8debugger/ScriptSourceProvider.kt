/*
 * Copyright (c) 2020, Salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 *
 */

package com.salesforce.j2v8debugger

interface ScriptSourceProvider {

    val allScriptIds: Collection<String>

    /**
     * @param scriptId id or name of the script
     *
     * @return source code of the script.
     */
    fun getSource(scriptId: String): String
}
