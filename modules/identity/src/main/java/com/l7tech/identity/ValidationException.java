package com.l7tech.identity;

/**
 * @author darmstrong
 * Date: Jul 17, 2008
 */
public class ValidationException extends Exception{
    public ValidationException() {
        super();
    }

    public ValidationException( String message ) {
        super( message );
    }

    public ValidationException( String message, Throwable cause ) {
        super( message, cause );
    }
}
