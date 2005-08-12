package com.l7tech.policy.assertion;

/**
 * Superclass of {@link RequestXpathAssertion} and {@link ResponseXpathAssertion} that
 * adds variable prefix support.
 */
public abstract class SimpleXpathAssertion extends XpathBasedAssertion {
    protected String variablePrefix;
    public static final String TRUE = "true";
    public static final String FALSE = "false";
    public static final String VAR_SUFFIX_FOUND = "found";
    public static final String VAR_SUFFIX_RESULT = "result";
    public static final String VAR_SUFFIX_COUNT = "count";

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
    }
}
