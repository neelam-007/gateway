package com.l7tech.gateway.common.solutionkit;

/**
 * A Solution Kit Exception that is categorized as a bad request.
 */
public class BadRequestException extends SolutionKitException {
    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
