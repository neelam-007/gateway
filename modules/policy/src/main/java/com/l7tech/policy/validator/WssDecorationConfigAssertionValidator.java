package com.l7tech.policy.validator;

import com.l7tech.policy.assertion.xmlsec.WssDecorationConfig;
import com.l7tech.policy.assertion.xmlsec.WsSecurity;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.PrivateKeyable;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.wsdl.Wsdl;

import java.util.List;
import java.util.ArrayList;

/**
 * Policy validator for WssDecorationConfig assertions.
 */
public class WssDecorationConfigAssertionValidator implements AssertionValidator {

    //- PUBLIC

    public WssDecorationConfigAssertionValidator( final Assertion wssDecorationConfigAssertion ) {
        this.assertion = wssDecorationConfigAssertion;
    }

    @Override
    public void validate( final AssertionPath path,
                          final Wsdl wsdl,
                          final boolean soap,
                          final PolicyValidatorResult result ) {
        List<Assertion> shouldMatch = new ArrayList<Assertion>();

        boolean seenSelf = false;
        for ( Assertion pathAssertion : path.getPath() ) {
            if (!pathAssertion.isEnabled()) continue;

            if ( assertion == pathAssertion ) {
                seenSelf = true;                
            }

            if ( pathAssertion instanceof WsSecurity &&
                 AssertionUtils.isSameTargetMessage( assertion, pathAssertion )) {
                WsSecurity wsSecurity = (WsSecurity) pathAssertion;
                if ( wsSecurity.isApplyWsSecurity() ) {
                    if ( seenSelf ) {
                        break; // Settings after this don't need to match
                    } else {
                        shouldMatch.clear(); // Settings before this don't need to match
                    }
                }
            }

            if ( pathAssertion instanceof WssDecorationConfig &&
                 AssertionUtils.isSameTargetMessage( assertion, pathAssertion ) &&
                 AssertionUtils.isSameTargetRecipient( assertion, pathAssertion )) {
                shouldMatch.add( pathAssertion );
            }
        }

        for ( Assertion pathAssertion : shouldMatch ) {
            if ( pathAssertion instanceof PrivateKeyable &&
                 assertion instanceof PrivateKeyable ) {
                PrivateKeyable pk1 = (PrivateKeyable) pathAssertion;
                PrivateKeyable pk2 = (PrivateKeyable) assertion;
                if ( pk1.isUsesDefaultKeyStore() != pk2.isUsesDefaultKeyStore() ||
                     (!pk1.isUsesDefaultKeyStore() && !isSamePrivateKey(pk1, pk2) ) ) {
                        result.addWarning(new PolicyValidatorResult.Warning(pathAssertion, path, "Multiple signing assertions present with different \"Private Key\" selections. The same \"Private Key\" should be used for these assertions.", null));                                        
                }
            }

            if ( pathAssertion instanceof WssDecorationConfig &&
                 assertion instanceof WssDecorationConfig ) {
                WssDecorationConfig wdc1 = (WssDecorationConfig) pathAssertion;
                WssDecorationConfig wdc2 = (WssDecorationConfig) assertion;

                // Check for the same key reference types (BST, Issuer/Serial, etc)
                if ( (wdc1.getKeyReference() == null && wdc2.getKeyReference() != null) ||
                     (wdc1.getKeyReference() != null && !wdc1.getKeyReference().equals(wdc2.getKeyReference()))) {
                    String message = "Multiple signing assertions present with different \"Key Reference/Certificate Inclusion\" selections. The same \"Key Reference/Certificate Inclusion\" type should be used for these assertions.";
                    result.addWarning(new PolicyValidatorResult.Warning(pathAssertion, path, message, null));                                        
                }

                // Check for the same token protection settings
                if ( wdc1.isProtectTokens() != wdc2.isProtectTokens() ) {
                    String message = "Multiple signing assertions present with different token signature requirements";
                    result.addWarning(new PolicyValidatorResult.Warning(pathAssertion, path, message, null));
                }
            }
        }
    }

    //- PRIVATE

    private final Assertion assertion;

    private boolean isSamePrivateKey( final PrivateKeyable pk1,
                                      final PrivateKeyable pk2 ) {
        return (pk1.getNonDefaultKeystoreId() == pk2.getNonDefaultKeystoreId() || pk1.getNonDefaultKeystoreId()==-1 ) &&
               (pk1.getKeyAlias()!=null && pk1.getKeyAlias().equalsIgnoreCase( pk2.getKeyAlias() ));
    }
}

