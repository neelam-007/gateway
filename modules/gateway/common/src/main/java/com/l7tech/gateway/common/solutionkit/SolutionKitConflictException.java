package com.l7tech.gateway.common.solutionkit;

/**
 * Exception tracks solution kit conflicts such as entity conflict and instance modifier conflict.
 */
public class SolutionKitConflictException extends SolutionKitException {
    public SolutionKitConflictException(String message) {
        super(message);
    }
}
