package com.l7tech.console.event;

/**
 * Exception used to stop policy change from happening.
 *
 * @version 1.0
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class PolicyChangeVetoException extends Exception {
    private PolicyEvent policyEvent;

    /**
     * Create the <code>PolicyChangeVetoException</code> instance
     * with the event that caused the
     * @param e
     */
    public PolicyChangeVetoException(PolicyEvent e) {
        policyEvent = e;
    }

    /**
     * @return the poly event associated with the exception
     */
    public PolicyEvent getPolicyEvent() {
        return policyEvent;
    }

}
