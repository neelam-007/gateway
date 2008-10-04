package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;

/**
 * Utility exception that can be used as an alternative to returning AssertionStatus.FAILED from a server assertion.
 * <p/>
 * This style can result in simpler code for assertions with deeply nested code that fails only in exceptional situations.
 * <p/>
 * Failure conditions that may occur frequently, like a failed ComparisonAssertion, should not be reported with
 * this mechanism since it is much less expensive to just return AssertionStatus.FAILED instead.
 */
public class AssertionStatusException extends RuntimeException {
    private AssertionStatus assertionStatus = AssertionStatus.FAILED;

    public AssertionStatusException() {
    }

    public AssertionStatusException(String message) {
        super(message);
    }

    public AssertionStatusException(String message, Throwable cause) {
        super(message, cause);
    }

    public AssertionStatusException(Throwable cause) {
        super(cause);
    }

    public AssertionStatusException(AssertionStatus stat) {
        assertionStatus = stat;
    }

    public AssertionStatusException(AssertionStatus stat, String message) {
        super(message);
        assertionStatus = stat;
    }

    public AssertionStatusException(AssertionStatus stat, String message, Throwable cause) {
        super(message, cause);
        assertionStatus = stat;
    }

    public AssertionStatusException(AssertionStatus stat, Throwable cause) {
        super(cause);
        assertionStatus = stat;
    }

    /**
     * @param assertionStatus the assertion status to report with this exception.  If null, AssertionStatus.FAILED will be assumed.
     */
    public void setAssertionStatus(AssertionStatus assertionStatus) {
        if (assertionStatus == null)
            assertionStatus = AssertionStatus.FAILED;
        this.assertionStatus = assertionStatus;
    }

    /**
     * @return the assertion status to return.  Never null.
     */
    public AssertionStatus getAssertionStatus() {
        return assertionStatus;
    }
}
