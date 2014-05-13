package com.l7tech.policy.assertion.ext;

import com.l7tech.policy.variable.VariableMetadata;

import java.util.Map;

/**
 * Use this service to access context variables.
 */
public interface VariableServices {

    /**
     * The key for getting {@link VariableServices} from the Console Context.<p>
     * See {@link com.l7tech.policy.assertion.ext.cei.UsesConsoleContext UsesConsoleContext} for more details.
     */
    static final String CONSOLE_CONTEXT_KEY = "variableServices";

    /**
     * Use {@link com.l7tech.policy.variable.ContextVariablesUtils#getReferencedNames(String)} instead.
     */
    @Deprecated
    public String[] getReferencedVarNames(String varName);

    /**
     * Use {@link com.l7tech.policy.assertion.ext.message.CustomPolicyContext#expandVariable(String, java.util.Map)} instead.
     */
    @Deprecated
    public Object expandVariable(String varName, Map varMap);

    /**
     * Get the variables set before this Assertion in the Policy.  These declarations are made by Assertions implementing {@link com.l7tech.policy.assertion.SetsVariables#getVariablesSet()}.
     *
     * <p>The returned Map keys are in the correct case, and the Map is case insensitive.</p>
     *
     * @return The Map of names to VariableMetadata, may be empty but never null.
     * @see VariableMetadata
     */
    Map<String, VariableMetadata> getVariablesSetByPredecessors();
}
