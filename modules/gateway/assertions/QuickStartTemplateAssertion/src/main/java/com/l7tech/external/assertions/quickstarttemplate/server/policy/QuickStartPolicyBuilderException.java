package com.l7tech.external.assertions.quickstarttemplate.server.policy;

/**
 * Quick start service builder exception.
 */
public class QuickStartPolicyBuilderException extends Exception {
    public QuickStartPolicyBuilderException(String message) {
        super(message);
    }
    public QuickStartPolicyBuilderException(String message, Throwable cause) {
        super(message, cause);
    }
}
