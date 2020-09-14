package com.salesforce.j2v8inspector

interface ScriptSourceProvider {

    val allScriptIds: Collection<String>

    /**
     * @param scriptId id or name of the script
     *
     * @return source code of the script.
     */
    fun getSource(scriptId: String): String
}
