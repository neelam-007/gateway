package com.l7tech.policy.variable;

/**
 * Signals that the Exception could not be found
 */
public class VariableNotSettableException extends RuntimeException {
    private String variable;
    /**
     * @param   variable   the variable that was not found and caused the exception
     */
    public VariableNotSettableException(String variable) {
        super("The variable '"+variable+"' is not settable");
        this.variable = variable;
    }

    /**
     * @return the variable name that was not found
     */
    public String getVariable() {
        return variable;
    }
}
