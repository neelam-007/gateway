package com.l7tech.server.util.xml;

import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.xpath.NoSuchXpathVariableException;
import com.l7tech.xml.xpath.XpathVariableFinder;

/**
 * An XpathVariableFinder that finds unprefixed variables from a specified PolicyEnforcementContext.
 */
public class PolicyEnforcementContextXpathVariableFinder implements XpathVariableFinder {
    private final PolicyEnforcementContext pec;

    /**
     * Create a finder that will find variables in the specified PEC.
     *
     * @param pec  the context whose variables to make available to xpath.  Required.
     */
    public PolicyEnforcementContextXpathVariableFinder(PolicyEnforcementContext pec) {
        this.pec = pec;
    }

    public Object getVariableValue(String namespaceUri, String variableName) throws NoSuchXpathVariableException {
        if (namespaceUri != null && namespaceUri.length() > 0)
            throw new NoSuchXpathVariableException("Unsupported XPath variable namespace: " + namespaceUri);
        try {
            return pec.getVariable(variableName);
        } catch (NoSuchVariableException e) {
            throw new NoSuchXpathVariableException(ExceptionUtils.getMessage(e), e);
        }
    }
}
