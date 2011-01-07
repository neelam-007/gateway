package com.l7tech.policy.validator;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.security.xml.WsSecurityVersion;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.xmlsec.WsSecurity;

/**
 * AssertionValidator for assertions used with the WsSecurity assertion.
 */
public class WssDecorationAssertionValidator implements AssertionValidator {

    //- PUBLIC

    /**
     * Required default constructor.
     */
    public WssDecorationAssertionValidator( final Assertion assertion ) {
        this(assertion, false);
    }

    public WssDecorationAssertionValidator( final Assertion assertion, final boolean isWss11 ) {
        this.assertion = assertion;
        this.requiresDecoration = !Assertion.isResponse( assertion );
        this.isWss11 = isWss11;
    }

    @Override
    public void validate( final AssertionPath path,
                          final PolicyValidationContext pvc,
                          final PolicyValidatorResult result ) {
        if ( requiresDecoration || isWss11 ) {
            boolean seenWss10 = false;
            boolean seenSelf = false;
            boolean seenDecoration = false;
            Assertion[] assertionPath = path.getPath();
            for (Assertion a : assertionPath) {
                if (!a.isEnabled()) continue;
                if ( a == assertion ) {
                    seenSelf = true;
                }
                if ( a instanceof WsSecurity && ((WsSecurity)a).isApplyWsSecurity() && AssertionUtils.isSameTargetMessage( assertion, a ) ) {
                    if ( seenSelf  ) seenDecoration = true;
                    if ( ((WsSecurity)a).getWsSecurityVersion() == WsSecurityVersion.WSS10 ) seenWss10 = true;
                }
            }

            if ( requiresDecoration && !seenDecoration ) {
                final String wsSecurityAssName = new WsSecurity().meta().get(AssertionMetadata.SHORT_NAME).toString();
                result.addWarning(new PolicyValidatorResult.Warning(assertion, path, "This assertion will be ignored as an " +
                        "\"" + wsSecurityAssName + "\" assertion configured to apply WS-Security with the same\n" +
                        "message target is required to apply security.", null));
            }

            if ( isWss11 && seenWss10 ) {
                result.addWarning(new PolicyValidatorResult.Warning(assertion, path, "This assertion uses WS-Security 1.1 features, but the WS-Security version is set to 1.0.", null));
            }
        }
    }

    //- PROTECTED

    protected final Assertion assertion;
    protected final boolean requiresDecoration;
    protected final boolean isWss11;
    
}
