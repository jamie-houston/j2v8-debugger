package com.salesforce.j2v8inspector.model

object CdpMethod {
    /***
     * Methods in https://chromedevtools.github.io/devtools-protocol/tot/Debugger/
     */
    object Debugger {
        private val domain = "Debugger"

        // cdt methods
        val ContinueToLocation = "$domain.continueToLocation"
        val EvaluateOnCallFrame = "$domain.evaluateOnCallFrame"
        val GetPossibleBreakpoints = "$domain.getPossibleBreakpoints"
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
        val SearchInContent = "$domain.searchInContent"
        val GetFunctionDetails = "$domain.getFunctionDetails"
        val SetOverlayMessage = "$domain.setOverlayMessage"

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
        val CallFunctionOn = "$domain.callFunctionOn"
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

    /**
     * Methods in https://chromedevtools.github.io/devtools-protocol/tot/Console
     * doc says it's deprecated, however cdt still sends the messages
     */
    object Console {
        private val domain = "Console"
        val ClearMessages = "$domain.clearMessages"
    }
}