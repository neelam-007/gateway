package com.l7tech.identity;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Jul 17, 2008
 * Time: 12:44:05 PM
 * To change this template use File | Settings | File Templates.
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
