package com.l7tech.console.api;

import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.ext.VariableServices;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.Collections;
import java.util.Map;

/**
 * Implementation of <code>VariableServices</code> interface.
 */
public class VariableServicesImpl implements VariableServices {
    private final Assertion assertion;
    private final Assertion previousAssertion;

    public VariableServicesImpl (Assertion assertion, Assertion previousAssertion) {
        this.assertion = assertion;
        this.previousAssertion = previousAssertion;
    }

    @Override
    public String[] getReferencedVarNames(String varName) {
        return Syntax.getReferencedNames(varName);
    }

    @Override
    public Object expandVariable(String varName, Map varMap) {
        // This method should not be called by console code.
        //
        return null;
    }

    @Override
    public Map<String, VariableMetadata> getVariablesSetByPredecessors() {
        if (assertion != null && assertion.getParent() != null) {
            // Editing an assertion that has already been added in policy.
            return SsmPolicyVariableUtils.getVariablesSetByPredecessors(assertion);
        } else if (previousAssertion != null) {
            // Adding a new assertion to policy.
            return SsmPolicyVariableUtils.getVariablesSetByPredecessorsAndSelf(previousAssertion);
        } else {
            return Collections.emptyMap();
        }
    }
}