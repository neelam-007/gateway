package com.l7tech.policy.variable;

/**
 * Exception thrown if a variable cannot be found.
 */
public class NoSuchVariableException extends Exception {
    private final String variable;

    /**
     * @param variable  name of variable
     */
    public NoSuchVariableException(final String variable, final String message) {
        super(message);
        this.variable = variable;
    }

    /**
     * @return name of variable
     */
    public String getVariable() {
        return variable;
    }
}
