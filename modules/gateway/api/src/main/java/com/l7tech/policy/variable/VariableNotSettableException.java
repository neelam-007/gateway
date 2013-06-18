package com.l7tech.policy.variable;

/**
 * Thrown to indicate that a variable cannot be set by user.
 * For example, some built-in variables are read-only by user.
 */
public class VariableNotSettableException extends RuntimeException {
    private String variable;

    /**
     * @param variable  name of variable
     */
    public VariableNotSettableException(String variable) {
        super("The variable \"" + variable + "\" is not settable.");
        this.variable = variable;
    }

    /**
     * @return name of variable
     */
    public String getVariable() {
        return variable;
    }
}
