package com.l7tech.policy.validator;

import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.PolicyVariableUtils;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.AssertionPath;
import com.l7tech.common.xml.Wsdl;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * Variable validator for use with any assertions that use variables.
 *
 * @author steve
 */
public class VariableUseValidator implements AssertionValidator {

    //- PUBLIC

    public void validate( AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result ) {
        for ( String warnString : warnStrings )
            result.addWarning(new PolicyValidatorResult.Warning(assertion, path, warnString, null));
        if (errString != null)
            result.addError(new PolicyValidatorResult.Error(assertion, path, errString, null));
    }

    //- PACKAGE

    VariableUseValidator( final Assertion a  ) {
        List<String> warningStrings = new ArrayList<String>();
        String errorString = null;

        if (a instanceof UsesVariables) {
            UsesVariables ua = (UsesVariables) a;
            try {
                final String[] vars = ua.getVariablesUsed();
                final Set<String> predecessorVariables = PolicyVariableUtils.getVariablesSetByPredecessors(a).keySet();
                for (String var : vars) {
                    if (!BuiltinVariables.isPredefined(var) &&
                        Syntax.getMatchingName(var, predecessorVariables) == null) {
                        warningStrings.add(
                                "This assertion refers to the variable '" +
                                var +
                                "', which is neither predefined " +
                                "nor set in the policy so far." );
                    }
                }
            } catch (IllegalArgumentException iae) {
                errorString = "This assertion uses invalid variable syntax.";
            }
        }

        this.assertion = a;
        this.warnStrings = warningStrings;
        this.errString = errorString;
    }

    //- PRIVATE

    private final Assertion assertion;
    private final List<String> warnStrings;
    private final String errString;
}
