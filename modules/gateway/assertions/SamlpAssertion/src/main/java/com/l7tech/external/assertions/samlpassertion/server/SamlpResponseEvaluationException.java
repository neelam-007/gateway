package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.policy.assertion.AssertionStatus;

/**
 * User: vchan
 */
public class SamlpResponseEvaluationException extends SamlpAssertionException {

    public SamlpResponseEvaluationException() {
    }

    public SamlpResponseEvaluationException(String message) {
        super(message);
    }

    public SamlpResponseEvaluationException(AssertionStatus status) {
        super(status);
    }

    public SamlpResponseEvaluationException(String message, AssertionStatus status) {
        super(message, status);
    }

    public SamlpResponseEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SamlpResponseEvaluationException(Throwable cause) {
        super(cause);
    }
}
