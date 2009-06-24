package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.SqlAttackAssertion;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.wsdl.Wsdl;

/**
 * Policy validator for SQL Attack Assertion
 */
public class SqlAttackAssertionValidator implements AssertionValidator {

    //- PUBLIC

    public SqlAttackAssertionValidator( final SqlAttackAssertion sqlAttackAssertion ) {
        this.sqlAttackAssertion = sqlAttackAssertion;
        if ( sqlAttackAssertion.getProtections().isEmpty() ) {
            warningStr = "No SQL protections have been specified";
        } else {
            warningStr = null;
        }
    }

    @Override
    public void validate( final AssertionPath assertionPath,
                          final Wsdl wsdl,
                          final boolean soap,
                          final PolicyValidatorResult result) {
            if ( warningStr != null ) {
                result.addWarning(new PolicyValidatorResult.Warning(sqlAttackAssertion, assertionPath, warningStr, null));
            }
            // Check if any WSS Token Assertions violate the option "Invasive SQL Injection Attack Protection" or not.
            if (sqlAttackAssertion.isSqlMetaEnabled()) {
                if ( hasWssAssertion( assertionPath.getPath(), sqlAttackAssertion ) ) {
                    result.addWarning(new PolicyValidatorResult.Warning(sqlAttackAssertion, assertionPath,
                        "WS-Security message decoration violates the selected \"Invasive SQL Injection Attack Protection\".", null));
                }
            }
    }

    //- PRIVATE

    private final SqlAttackAssertion sqlAttackAssertion;
    private final String warningStr;

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
