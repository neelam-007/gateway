package com.l7tech.server.processcontroller.patching;

/**
 * Thrown by the Patch Service and its subcomponents
 * to indicate a error condition while performing a patch related operation.
 *
 * @author jbufu
 */
public class PatchException extends Exception {

    public PatchException() { }

    public PatchException( String message ) {
        super( message );
    }

    public PatchException(Throwable cause) {
        super(cause);
    }

    public PatchException( String message, Throwable cause ) {
        super( message, cause );
    }
}
