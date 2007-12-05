package com.l7tech.policy.variable;

/**
 * Thrown to indicate that a variable cannot be found.
 */
public class NoSuchVariableException extends Exception {
    private String variable;

    /**
     * @param variable  name of variable
     */
    public NoSuchVariableException(String variable) {
        super("The variable \"" + variable + "\" could not be found.");
        this.variable = variable;
    }

    /**
     * @return name of variable
     */
    public String getVariable() {
        return variable;
    }
}
