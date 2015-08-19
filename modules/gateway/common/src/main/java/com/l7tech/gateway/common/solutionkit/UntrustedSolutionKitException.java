package com.l7tech.gateway.common.solutionkit;

/**
 * Indicates untrusted SKAR file; either SKAR file is unsigned, signature cannot be verified or signed with not trusted signer.
 */
public class UntrustedSolutionKitException extends SolutionKitException {
    public UntrustedSolutionKitException(final String message) {
        super(message);
    }

    public UntrustedSolutionKitException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
