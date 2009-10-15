package com.l7tech.policy.variable;

/**
 * Exception thrown if a variable name does not conform to the expected syntax.
 */
public class VariableNameSyntaxException extends IllegalArgumentException {

    public VariableNameSyntaxException(String message) {
        super(message);
    }

    public VariableNameSyntaxException(String message, Throwable cause) {
        super(message, cause);
    }

}
