package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Variable validator for use with any assertions that set variables.
 */
public class VariableSetValidator implements AssertionValidator {

    //- PUBLIC

    @Override
    public void validate( AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result ) {
        for ( String warnString : warnStrings )
            result.addWarning(new PolicyValidatorResult.Warning(assertion, warnString, null));
        if (errString != null)
            result.addError(new PolicyValidatorResult.Error(assertion, errString, null));
    }

    //- PACKAGE

    VariableSetValidator( final Assertion a  ) {
        List<String> warningStrings = new ArrayList<String>();
        String errorString = null;

        if (a instanceof SetsVariables) {
            SetsVariables sa = (SetsVariables) a;
            try {
                final VariableMetadata[] vars = sa.getVariablesSet();
                for (VariableMetadata var : vars) {
                     final VariableMetadata meta = BuiltinVariables.getMetadata(var.getName());
                    if (meta != null) {
                        if(!meta.isSettable()) {
                            warningStrings.add(
                                   "Target Variable:" + " '" + var.getName() + "' is not settable.");
                        }
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
