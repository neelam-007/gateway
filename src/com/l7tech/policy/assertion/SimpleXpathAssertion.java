package com.l7tech.policy.assertion;

import com.l7tech.policy.variable.VariableMetadata;

/**
 * Superclass of {@link RequestXpathAssertion} and {@link ResponseXpathAssertion} that
 * adds variable prefix support.
 */
public abstract class SimpleXpathAssertion
        extends XpathBasedAssertion
        implements SetsVariables
{
    protected String variablePrefix;
    public static final String TRUE = "true";
    public static final String FALSE = "false";

    public static final String VAR_SUFFIX_FOUND = "found";
    public static final String VAR_SUFFIX_RESULT = "result";
    public static final String VAR_SUFFIX_COUNT = "count";

    private transient String foundVariable;
    private transient String resultVariable;
    private transient String countVariable;

    /**
     * @return the prefix to be used in front of context variable names set by the assertion
     */
    public String getVariablePrefix() {
        return variablePrefix;
    }

    /**
     * @param variablePrefix the prefix to be used in front of context variable names set by the assertion
     */
    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
        doVarNames();
    }

    private void doVarNames() {
        String prefix = variablePrefix;
        if (prefix == null || prefix.length() == 0) {
            prefix = defaultVariablePrefix();
        }
        foundVariable = prefix + "." + VAR_SUFFIX_FOUND;
        resultVariable = prefix + "." + VAR_SUFFIX_RESULT;
        countVariable = prefix + "." + VAR_SUFFIX_COUNT;
    }

    public String foundVariable() {
        if (foundVariable == null) doVarNames();
        return foundVariable;
    }

    public String resultVariable() {
        if (resultVariable == null) doVarNames();
        return resultVariable;
    }

    public String countVariable() {
        if (countVariable == null) doVarNames();
        return countVariable;
    }

    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
            // Note default prefixes are used here for property lookup purposes
            new VariableMetadata(foundVariable(), false, false, defaultVariablePrefix() + "." + VAR_SUFFIX_FOUND, false),
            new VariableMetadata(countVariable(), false, false, defaultVariablePrefix() + "." + VAR_SUFFIX_COUNT, false),
            new VariableMetadata(resultVariable(), false, false, defaultVariablePrefix() + "." + VAR_SUFFIX_RESULT, false),
        };
    }

    protected abstract String defaultVariablePrefix();
}
