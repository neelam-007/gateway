package com.l7tech.policy.assertion;

import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.DataType;

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
    public static final String VAR_SUFFIX_MULTIPLE_RESULTS = "results";
    public static final String VAR_SUFFIX_COUNT = "count";
    public static final String VAR_SUFFIX_ELEMENT = "element";
    public static final String VAR_SUFFIX_MULTIPLE_ELEMENTS = "elements";

    private transient String foundVariable;
    private transient String resultVariable;
    private transient String multipleResultsVariable;
    private transient String countVariable;
    private transient String elementVariable;
    private transient String multipleElementsVariable;

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
        multipleResultsVariable = prefix + "." + VAR_SUFFIX_MULTIPLE_RESULTS;
        countVariable = prefix + "." + VAR_SUFFIX_COUNT;
        elementVariable = prefix + "." + VAR_SUFFIX_ELEMENT;
        multipleElementsVariable = prefix + "." + VAR_SUFFIX_MULTIPLE_ELEMENTS;
    }

    public String foundVariable() {
        if (foundVariable == null) doVarNames();
        return foundVariable;
    }

    public String resultVariable() {
        if (resultVariable == null) doVarNames();
        return resultVariable;
    }

    public String multipleResultsVariable() {
        if (multipleResultsVariable == null) doVarNames();
        return multipleResultsVariable;
    }

    public String countVariable() {
        if (countVariable == null) doVarNames();
        return countVariable;
    }

    public String elementVariable() {
        if (elementVariable == null) doVarNames();
        return elementVariable;
    }

    public String multipleElementsVariable() {
        if (multipleElementsVariable == null) doVarNames();
        return multipleElementsVariable;
    }

    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
            // Note default prefixes are used here for property lookup purposes
            new VariableMetadata(foundVariable(), false, false, defaultVariablePrefix() + "." + VAR_SUFFIX_FOUND, false, DataType.BOOLEAN),
            new VariableMetadata(countVariable(), false, false, defaultVariablePrefix() + "." + VAR_SUFFIX_COUNT, false, DataType.INTEGER),
            new VariableMetadata(resultVariable(), false, false, defaultVariablePrefix() + "." + VAR_SUFFIX_RESULT, false, DataType.STRING),
            new VariableMetadata(multipleResultsVariable(), false, false, defaultVariablePrefix() + "." + VAR_SUFFIX_MULTIPLE_RESULTS, false, DataType.STRING),
            new VariableMetadata(elementVariable(), false, false, defaultVariablePrefix() + "." + VAR_SUFFIX_ELEMENT, false, DataType.ELEMENT),
            new VariableMetadata(multipleElementsVariable(), false, false, defaultVariablePrefix() + "." + VAR_SUFFIX_MULTIPLE_ELEMENTS, false, DataType.ELEMENT),
        };
    }

    protected abstract String defaultVariablePrefix();
}
