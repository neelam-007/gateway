package com.l7tech.objectmodel;

/**
 * Exception thrown if an integrity constraint is violated.
 *
 * @author steve
 */
public class ConstraintViolationException extends ObjectModelException {

    public ConstraintViolationException() {
        super();
    }

    public ConstraintViolationException( String message ) {
        super( message );
    }

    public ConstraintViolationException( String message, Throwable cause ) {
        super( message, cause );
    }

    public ConstraintViolationException(Throwable cause) {
        super(cause);
    }
}
