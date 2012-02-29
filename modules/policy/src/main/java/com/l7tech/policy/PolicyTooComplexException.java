package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;

/**
 * Exception thrown if a policy is too complex to be processed in the requested way, such as if an attempt
 * is made to enumerate all policy paths on a policy with more than the current maximum number of paths.
 */
public class PolicyTooComplexException extends PolicyAssertionException {
    public PolicyTooComplexException(Assertion ass) {
        super(ass);
    }

    public PolicyTooComplexException(Assertion ass, String message) {
        super(ass, message);
    }

    public PolicyTooComplexException(Assertion ass, Throwable cause) {
        super(ass, cause);
    }

    public PolicyTooComplexException(Assertion ass, String message, Throwable cause) {
        super(ass, message, cause);
    }
}
