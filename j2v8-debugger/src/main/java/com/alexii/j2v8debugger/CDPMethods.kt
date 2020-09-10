package com.alexii.j2v8debugger

object Protocol {
    /***
     * Methods in https://chromedevtools.github.io/devtools-protocol/tot/Debugger/
     */
    object Debugger {
        private val domain = "Debugger"

        // cdt methods
        val ContinueToLocation = "$domain.continueToLocation"
        val EvaluateOnCallFrame = "$domain.evaluateOnCallFrame"
        val Enable = "$domain.enable"
        val Pause = "$domain.pause"
        val RemoveBreakpoint = "$domain.removeBreakpoint"
        val Resume = "$domain.resume"
        val SetAsyncCallStackDepth = "$domain.setAsyncCallStackDepth"
        val SetBreakpointsActive = "$domain.setBreakpointsActive"
        val SetBreakpointByUrl = "$domain.setBreakpointByUrl"
        val SetPauseOnExceptions = "$domain.setPauseOnExceptions"
        val SetSkipAllPauses = "$domain.setSkipAllPauses"
        val SetVariableValue = "$domain.setVariableValue"
        val StepInto = "$domain.stepInto"
        val StepOut = "$domain.stepOut"
        val StepOver = "$domain.stepOver"
        val GetFunctionDetails = "$domain.getFunctionDetails"

        // events
        val BreakpointResolved = "$domain.breakpointResolved"
        val Paused = "$domain.paused"
        val Resumed = "$domain.resumed"
        val scriptFailedToParse = "$domain.scriptFailedToParse"
        val ScriptParsed = "$domain.scriptParsed"

    }

    /***
     * Methods in https://chromedevtools.github.io/devtools-protocol/tot/Runtime/
     */
    object Runtime{
        private val domain = "Runtime"
        val AwaitPromise = "$domain.awaitPromise"
        val callFunctionOn = "$domain.callFunctionOn"
        val CompileScript = "$domain.compileScript"
        val Disable = "$domain.disable"
        val DiscardConsoleEntries = "$domain.discardConsoleEntries"
        val Enable = "$domain.enable"
        val Evaluate = "$domain.evaluate"
        val GetProperties = "$domain.getProperties"
        val GlobalLexicalScopeNames = "$domain.globalLexicalScopeNames"
        val QueryObjects = "$domain.queryObjects"
        val ReleaseObject = "$domain.releaseObject"
        val ReleaseObjectGroup = "$domain.releaseObjectGroup"
        val RunScript = "$domain.runScript"
        val SetAsyncCallStackDepth = "$domain.setAsyncCallStackDepth"
        val RunIfWaitingForDebugger = "$domain.runIfWaitingForDebugger"
    }
}