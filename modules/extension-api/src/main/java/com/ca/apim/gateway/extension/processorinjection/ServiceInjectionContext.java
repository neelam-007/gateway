package com.ca.apim.gateway.extension.processorinjection;

/**
 * Allows Service Injection Extensions to modify the policy execution context for the service that will be/has been executed.
 */
public interface ServiceInjectionContext {
    /**
     * Return a variable that is set in the Execution Context. Returns null if there is no variable set with that name
     *
     * @param name The name of the variable
     * @return The variable value or null if no such variable is set.
     */
    Object getVariable(final String name);

    /**
     * Sets a variable in the execution context. This will override existing variables if the are already set.
     *
     * @param name  The name of the variable to set
     * @param value The value of the variable
     */
    void setVariable(final String name, final Object value);
}
