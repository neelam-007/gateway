package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.policy.assertion.xmlsec.WsSecurity;
import com.l7tech.policy.assertion.xmlsec.WssConfigurationAssertion;
import com.l7tech.policy.assertion.xmlsec.WssDecorationConfig;
import com.l7tech.policy.assertion.xmlsec.WssVersionAssertion;
import com.l7tech.security.xml.WsSecurityVersion;

import java.util.ArrayList;
import java.util.List;

/**
 * Validator for assertions that specify a WS-Security version
 */
public class WssVersionAssertionValidator implements AssertionValidator {

    //- PUBLIC

    public WssVersionAssertionValidator( final Assertion assertion ) {
        this.assertion = assertion;
        this.targetAssertion = getTargetAssertion( assertion );
        this.expectedVersion = getWsSecurityVersion( assertion );
    }

    @Override
    public void validate( final AssertionPath path,
                          final PolicyValidationContext pvc,
                          final PolicyValidatorResult result ) {
        if ( expectedVersion == null ) return;

        final boolean isSecurityHeaderAddressable = assertion instanceof SecurityHeaderAddressable;
        final List<Assertion> shouldMatch = new ArrayList<Assertion>();
        boolean seenSelf = false;
        for ( final Assertion pathAssertion : path.getPath() ) {
            if (!pathAssertion.isEnabled()) continue;

            if ( assertion == pathAssertion ) {
                seenSelf = true;
            }

            if ( !(assertion instanceof WssVersionAssertion) &&
                 pathAssertion instanceof WsSecurity &&
                 AssertionUtils.isSameTargetMessage( targetAssertion, pathAssertion )) {
                final WsSecurity wsSecurity = (WsSecurity) pathAssertion;
                if ( wsSecurity.isApplyWsSecurity() ) {
                    if ( seenSelf ) {
                        shouldMatch.add( pathAssertion );
                        break; // Settings after this don't need to match
                    } else {
                        shouldMatch.clear(); // Settings before this don't need to match
                    }
                }
            }

            if ( pathAssertion instanceof WssVersionAssertion && Assertion.isResponse(targetAssertion) ) {
                shouldMatch.add( pathAssertion );
            }

            if ( pathAssertion instanceof WssDecorationConfig &&
                 AssertionUtils.isSameTargetMessage( targetAssertion, pathAssertion ) &&
                 (!isSecurityHeaderAddressable || AssertionUtils.isSameTargetRecipient( assertion, pathAssertion ))) {
                shouldMatch.add( pathAssertion );
            }

            if ( pathAssertion instanceof WsSecurity &&
                 AssertionUtils.isSameTargetMessage( targetAssertion, pathAssertion ) ) {
                shouldMatch.add( pathAssertion );
            }
        }

        for ( final Assertion pathAssertion : shouldMatch ) {
            final WsSecurityVersion pathWsSecurityVersion = getWsSecurityVersion( pathAssertion );
            if ( pathWsSecurityVersion != null && expectedVersion != pathWsSecurityVersion ) {
                String message = "Multiple assertions present with different \"WS-Security version\" settings.  If one assertion specifies a version, the other assertions in the same path should either specify the same version or not specify a version.";
                result.addWarning(new PolicyValidatorResult.Warning(pathAssertion, path, message, null));
            }
        }
    }

    //- PRIVATE

    private final Assertion assertion;
    private final Assertion targetAssertion;
    private final WsSecurityVersion expectedVersion;

    private static Assertion getTargetAssertion( final Assertion assertion ) {
        return assertion instanceof WssVersionAssertion ?
            new MessageTargetableAssertion(TargetMessageType.RESPONSE){}  :
            assertion;
    }

    private static WsSecurityVersion getWsSecurityVersion( final Assertion assertion ) {
        WsSecurityVersion version = null;

        if ( assertion instanceof WssVersionAssertion ) {
            version = WsSecurityVersion.WSS11;
        }

        if ( assertion instanceof WsSecurity ) {
            final WsSecurity wsSecurity = (WsSecurity) assertion;
            if ( wsSecurity.isApplyWsSecurity() ) {
                version = wsSecurity.getWsSecurityVersion();
            }
        }

        if ( assertion instanceof WssConfigurationAssertion ) {
            final WssConfigurationAssertion wssConfigurationAssertion = (WssConfigurationAssertion) assertion;
            version = wssConfigurationAssertion.getWssVersion();
        }

        return version;
    }
}
