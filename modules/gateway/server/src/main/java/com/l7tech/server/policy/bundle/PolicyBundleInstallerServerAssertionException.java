package com.l7tech.server.policy.bundle;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;

/**
 * Exception thrown by implementation classes of PolicyBundleInstallerAbstractServerAssertion
 */
public class PolicyBundleInstallerServerAssertionException extends PolicyAssertionException {
    private final int httpStatusCode;

    public PolicyBundleInstallerServerAssertionException(Assertion ass, String message, int httpStatusCode) {
        super(ass, message);
        this.httpStatusCode = httpStatusCode;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }
}
