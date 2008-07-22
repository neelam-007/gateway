package com.l7tech.console.tree.policy;

/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class PolicyException extends Exception {
    public PolicyException() {
        super();
    }

    public PolicyException(Throwable cause) {
        super(cause);
    }

    public PolicyException(String message) {
        super(message);
    }

    public PolicyException(String message, Throwable cause) {
        super(message, cause);
    }
}
