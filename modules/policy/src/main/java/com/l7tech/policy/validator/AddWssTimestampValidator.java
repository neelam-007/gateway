package com.l7tech.policy.validator;

import com.l7tech.policy.assertion.xmlsec.AddWssTimestamp;
import com.l7tech.policy.assertion.xmlsec.WsSecurity;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.AssertionPath;
import com.l7tech.wsdl.Wsdl;

/**
 * Policy validation for AddWssTimestamp.
 */
public class AddWssTimestampValidator implements AssertionValidator {
    private final AddWssTimestamp assertion;
    private final boolean requiresDecoration;

    public AddWssTimestampValidator( final AddWssTimestamp addWssTimestamp ) {
        assertion = addWssTimestamp;
        requiresDecoration = !Assertion.isResponse( assertion );
    }

    @Override
    public void validate( final AssertionPath path,
                          final Wsdl wsdl,
                          final boolean soap,
                          final PolicyValidatorResult result) {
        if ( requiresDecoration ) {
            boolean seenSelf = false;
            boolean seenDecoration = false;
            Assertion[] assertionPath = path.getPath();
            for (int i = assertionPath.length - 1; i >= 0; i--) {
                Assertion a = assertionPath[i];
                if ( a == assertion ) {
                    seenSelf = true;
                }
                if ( seenSelf && a instanceof WsSecurity && ((WsSecurity)a).isApplyWsSecurity() && AssertionUtils.isSameTargetMessage( assertion, a ) ) {
                    seenDecoration = true;
                }
            }

            if ( !seenDecoration ) {
                result.addWarning(new PolicyValidatorResult.Warning(assertion, path, "This assertion will be ignored. An \"Add or remove WS-Security\" assertion should be used to apply security.", null));
            }
        }
    }
}
