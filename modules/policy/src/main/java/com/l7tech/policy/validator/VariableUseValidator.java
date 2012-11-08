package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.PolicyVariableUtils;
import com.l7tech.policy.variable.Syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Variable validator for use with any assertions that use variables.
 *
 * @author steve
 */
public class VariableUseValidator implements AssertionValidator {

    //- PUBLIC

    @Override
    public void validate( AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result ) {
        for ( String warnString : warnStrings )
            result.addWarning(new PolicyValidatorResult.Warning(assertion, warnString, null));
        if (errString != null)
            result.addError(new PolicyValidatorResult.Error(assertion, errString, null));
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
                    if(var == null){
                        errorString = "This assertion uses context variable that needs to be specified.";
                        break;
                    }
                    if (var.startsWith(BuiltinVariables.PREFIX_SERVICE + "." + BuiltinVariables.SERVICE_SUFFIX_URL)) {
                        warningStrings.add("The context variable \"service.url\" has been deprecated.  Now HTTP routing " +
                            "assertions use a new context variable \"httpRouting.url\" instead of \"service.url\".");
                    }

                    if (!BuiltinVariables.isPredefined(var) &&
                        Syntax.getMatchingName(var, predecessorVariables) == null) {
                        warningStrings.add(
                                "This assertion refers to the variable '" +
                                var +
                                "', which is neither predefined " +
                                "nor set in the policy so far." );
                    }

                    if (BuiltinVariables.isDeprecated(var)) {
                        warningStrings.add("Deprecated variable '" + var + "' should be replaced by '" + BuiltinVariables.getMetadata(var).getReplacedBy() + BuiltinVariables.getUnmatchedName(var) + "'.");
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
