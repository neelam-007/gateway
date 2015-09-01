package com.l7tech.gateway.common.solutionkit;

/**
 * A Solution Kit Exception that is categorized as a forbidden request.
 */
public class ForbiddenException extends SolutionKitException {
    public ForbiddenException(String message) {
        super(message);
    }
}
