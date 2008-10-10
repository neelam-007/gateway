package com.l7tech.policy.variable;

/**
 * Exception thrown if a variable name does not conform to the expected syntax.
 */
public class VariableNameSyntaxException extends IllegalArgumentException {
    public VariableNameSyntaxException() {
    }

    public VariableNameSyntaxException(String s) {
        super(s);
    }

    public VariableNameSyntaxException(String message, Throwable cause) {
        super(message, cause);
    }

    public VariableNameSyntaxException(Throwable cause) {
        super(cause);
    }
}
