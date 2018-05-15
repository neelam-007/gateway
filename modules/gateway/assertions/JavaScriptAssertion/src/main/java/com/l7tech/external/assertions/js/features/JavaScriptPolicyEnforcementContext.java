package com.l7tech.external.assertions.js.features;

import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNotSettableException;

import java.io.IOException;

/**
 * Context interface to be available to the Javascript to access context variables.
 */
public interface JavaScriptPolicyEnforcementContext {
    /**
     * Gets the variable from the PolicyEnforcementContext. This method is accessed from the Javascript to fetch
     * context variables in the script.
     * @param name name of the context variable
     * @return If the context object is of type Message, return the JSON object if it is JSON or return the string
     * format of the complete payload.
     * @throws NoSuchVariableException
     */
    public Object getVariable(String name) throws NoSuchVariableException;

    /**
     * Sets the value of the context variables. This method is accessed from the Javascript to update the context
     * variable.
     * @param name of the variable
     * @param scriptObj value to set
     * @throws NoSuchVariableException
     * @throws IOException
     * @throws VariableNotSettableException
     */
    public void setVariable(String name, Object value) throws NoSuchVariableException, IOException, JavaScriptException;

}
