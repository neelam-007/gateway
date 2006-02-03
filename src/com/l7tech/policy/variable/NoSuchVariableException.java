package com.l7tech.policy.variable;

/**
 * Signals that the Exception could not be found
 */
public class NoSuchVariableException extends Exception {
    private String variable;
    /**
     * @param   variable   the variable that was not found and caused the exception
     */
    public NoSuchVariableException(String variable) {
        super("The variable '"+variable+"' could not be found");
        this.variable = variable;
    }

    /**
     * @return the variable name that was not found
     */
    public String getVariable() {
        return variable;
    }
}
