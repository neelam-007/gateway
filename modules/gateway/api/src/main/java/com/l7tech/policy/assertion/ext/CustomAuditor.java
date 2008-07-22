package com.l7tech.policy.assertion.ext;

/**
 * An auditor for Custom Assertions.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public interface CustomAuditor {

    /**
     * Audit information at info level for the given assertion.
     *
     * @param customAssertion the assertion
     * @param message the message
     */
    void auditInfo(CustomAssertion customAssertion, String message);

    /**
     * Audit information at warning level for the given assertion.
     *
     * @param customAssertion the assertion
     * @param message the message
     */
    void auditWarning(CustomAssertion customAssertion, String message);
}
