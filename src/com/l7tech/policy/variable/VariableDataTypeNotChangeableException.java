package com.l7tech.policy.variable;

/**
 * Thrown to indicate that the data type of a variable cannot be changed.
 * For example, built-in variables has fixed data type.
 *
 * @since SecureSpan 4.3
 * @author rmak
 */
public class VariableDataTypeNotChangeableException extends RuntimeException {
    private String _variable;

    /**
     * @param   variable    name of variable
     */
    public VariableDataTypeNotChangeableException(String variable) {
        super("The data type of variable \"" + variable + "\" is not changeable.");
        _variable = variable;
    }

    /**
     * @return name of variable
     */
    public String getVariable() {
        return _variable;
    }
}
