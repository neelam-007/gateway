package com.l7tech.external.assertions.quickstarttemplate.server.policy;

/**
 * Quick start service builder exception.
 */
class QuickStartPolicyBuilderException extends Exception {
    QuickStartPolicyBuilderException(String message) {
        super(message);
    }
    QuickStartPolicyBuilderException(String message, Throwable cause) {
        super(message, cause);
    }
}
