package com.l7tech.external.assertions.js.features;

public class JavaScriptExecutorOptions {

    private String scriptName;
    private String script;
    private boolean strictModeEnabled;
    private int executionTimeout;

    public JavaScriptExecutorOptions(final String scriptName, final String script, final boolean strictModeEnabled,
                                     final int executionTimeout) {
        this.scriptName = scriptName;
        this.script = script;
        this.strictModeEnabled = strictModeEnabled;
        this.executionTimeout = executionTimeout;
    }

    public String getScriptName() {
        return scriptName;
    }

    public String getScript() {
        return script;
    }

    public boolean isStrictModeEnabled() {
        return strictModeEnabled;
    }

    public int getExecutionTimeout() {
        return executionTimeout;
    }
}
