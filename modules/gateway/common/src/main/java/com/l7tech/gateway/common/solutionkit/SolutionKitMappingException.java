package com.l7tech.gateway.common.solutionkit;

/**
 * Exception for when unable to install Solution Kit bundle due to mapping errors.
 */
public class SolutionKitMappingException extends SolutionKitException {
    public SolutionKitMappingException(final String message) {
        super(message);
    }

    public SolutionKitMappingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
