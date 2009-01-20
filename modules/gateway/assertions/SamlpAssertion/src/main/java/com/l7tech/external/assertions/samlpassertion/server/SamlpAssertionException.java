package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.policy.assertion.AssertionStatus;

/**
 * Base exception used by all SAMLP assertion related classes.
 *
 * @author: vchan
 */
public class SamlpAssertionException extends Exception {

    AssertionStatus status;

    public SamlpAssertionException() {
    }

    public SamlpAssertionException(String message) {
        super(message);
    }

    public SamlpAssertionException(AssertionStatus status) {
        this.status = status;
    }

    public SamlpAssertionException(String message, AssertionStatus status) {
        super(message);
        this.status = status;
    }

    public SamlpAssertionException(String message, Throwable cause) {
        super(message, cause);
    }

    public SamlpAssertionException(Throwable cause) {
        super(cause);
    }

    public AssertionStatus getStatus() {
        return status;
    }
}
