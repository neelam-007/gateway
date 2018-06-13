package com.l7tech.external.assertions.js.features;

/**
 * Exception thrown if JavaScriptAssertion fails because of invalid scenarios.
 */
public class JavaScriptException extends RuntimeException {

    public JavaScriptException(String message) {
        super(message);
    }

    public JavaScriptException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
}
