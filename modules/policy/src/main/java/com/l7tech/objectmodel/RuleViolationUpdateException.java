package com.l7tech.objectmodel;

/**
 * Update exception for business rule violations.
 */
public class RuleViolationUpdateException extends UpdateException {

    /**
     * Create a rule violation exception with the given rule (message)
     *
     * @param message The rule violation message.
     */
    public RuleViolationUpdateException( final String message ) {
        super( message );
    }

    /**
     * Create a rule violation exception with the given rule (message)
     *
     * @param message The rule violation message.
     * @param cause The underlying cause
     */
    public RuleViolationUpdateException( final String message, final Throwable cause ) {
        super( message, cause );
    }
}
