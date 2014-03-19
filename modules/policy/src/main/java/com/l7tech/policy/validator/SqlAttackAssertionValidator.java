package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.*;

/**
 * Policy validator for SQL Attack Assertion
 */
public class SqlAttackAssertionValidator implements AssertionValidator {

    //- PUBLIC

    public SqlAttackAssertionValidator( final SqlAttackAssertion sqlAttackAssertion ) {
        this.sqlAttackAssertion = sqlAttackAssertion;
    }

    @Override
    public void validate( final AssertionPath assertionPath,
                          final PolicyValidationContext pvc,
                          final PolicyValidatorResult result) {
        if (sqlAttackAssertion.getProtections().isEmpty()) {
            result.addWarning(new PolicyValidatorResult.Warning(sqlAttackAssertion,
                    "No SQL protections have been specified", null));
        }

        if (sqlAttackAssertion.isIncludeUrl() && sqlAttackAssertion.getTarget() != TargetMessageType.REQUEST) {
            result.addWarning(new PolicyValidatorResult.Warning(sqlAttackAssertion,
                    "URL cannot be checked if the message is not targeted to Request.", null));
        }

        if (!sqlAttackAssertion.isIncludeUrl() && !sqlAttackAssertion.isIncludeBody()) {
            result.addError(new PolicyValidatorResult.Error(sqlAttackAssertion,
                    "Neither the URL nor Body has been selected to be protected.", null));
        }

        // Check if any WSS Token Assertions violate the option "Invasive SQL Injection Attack Protection" or not.
        if (sqlAttackAssertion.isSqlMetaEnabled()) {
            if ( hasWssAssertion( assertionPath.getPath(), sqlAttackAssertion ) ) {
                result.addWarning(new PolicyValidatorResult.Warning(sqlAttackAssertion,
                        "WS-Security message decoration violates the selected \"Invasive SQL Injection Attack Protection\".", null));
            }
        }
    }

    //- PRIVATE

    private final SqlAttackAssertion sqlAttackAssertion;

    private boolean hasWssAssertion( final Assertion[] path, final MessageTargetable messageTargetable  ) {
        boolean hasWss = false;

        for ( Assertion assertion : path ) {
            if (!assertion.isEnabled()) continue;
            if ( AssertionUtils.isSameTargetMessage( assertion, messageTargetable ) &&
                 Assertion.isWSSecurity(assertion) ) {
                hasWss = true;
                break;
            }
        }

        return hasWss;
    }
}
