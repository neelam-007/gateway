package com.l7tech.policy;

/**
 * Exception that indicates an invalid policy.
 *
 * @author steve
 */
public class InvalidPolicyException extends Exception {

    public InvalidPolicyException( Throwable cause ) {
        super( cause );
    }

    public InvalidPolicyException( String message ) {
        super( message );
    }

    public InvalidPolicyException( String message, Throwable cause ) {
        super( message, cause );
    }
}
