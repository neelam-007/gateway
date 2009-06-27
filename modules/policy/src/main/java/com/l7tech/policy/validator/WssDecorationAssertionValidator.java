package com.l7tech.policy.validator;

import com.l7tech.policy.assertion.xmlsec.WsSecurity;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.wsdl.Wsdl;

/**
 * AssertionValidator for assertions used with the WsSecurity assertion.
 */
public class WssDecorationAssertionValidator implements AssertionValidator {

    //- PUBLIC

    public WssDecorationAssertionValidator( final Assertion assertion ) {
        this.assertion = assertion;
        this.requiresDecoration = !Assertion.isResponse( assertion );
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
            for (Assertion a : assertionPath) {
                if (!a.isEnabled()) continue;
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

    //- PROTECTED

    protected final Assertion assertion;
    protected final boolean requiresDecoration;
    
}
