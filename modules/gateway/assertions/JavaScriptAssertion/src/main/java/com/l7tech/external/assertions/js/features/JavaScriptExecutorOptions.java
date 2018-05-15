package com.l7tech.external.assertions.js.features;

public class JavaScriptExecutorOptions {

    private String script;
    private boolean strictModeEnabled;
    private int executionTimeout;

    public JavaScriptExecutorOptions(final String script, final boolean strictModeEnabled, final int executionTimeout) {
        this.script = script;
        this.strictModeEnabled = strictModeEnabled;
        this.executionTimeout = executionTimeout;
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
