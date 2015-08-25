package com.l7tech.gateway.common.solutionkit;

/**
 * Indicates a top level error with the Solution Kit subsystem.
 */
public class SolutionKitException extends Exception {
    public SolutionKitException(String message) {
        super(message);
    }

    public SolutionKitException(String message, Throwable cause) {
        super(message, cause);
    }

    public SolutionKitException(Throwable cause) {
        super(cause);
    }
}