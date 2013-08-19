package com.l7tech.policy.validator;

import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.PrivateKeyable;
import com.l7tech.policy.assertion.xmlsec.WsSecurity;
import com.l7tech.policy.assertion.xmlsec.WssDecorationConfig;
import com.l7tech.security.xml.KeyReference;

import java.util.ArrayList;
import java.util.List;

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
                          final PolicyValidationContext pvc,
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
                        result.addWarning(new PolicyValidatorResult.Warning(pathAssertion, "Multiple signing assertions present with different \"Private Key\" selections. The same \"Private Key\" should be used for these assertions.", null));
                }
            }

            if ( pathAssertion instanceof WssDecorationConfig &&
                 assertion instanceof WssDecorationConfig ) {
                WssDecorationConfig wdc1 = (WssDecorationConfig) pathAssertion;
                WssDecorationConfig wdc2 = (WssDecorationConfig) assertion;

                // Check for the same key reference types (BST, Issuer/Serial, etc)
                if ( (wdc1.getKeyReference() != null && wdc2.getKeyReference() != null) &&
                     (!wdc1.getKeyReference().equals(wdc2.getKeyReference())))
                {
                    String finalVal = assertion.getOrdinal() > pathAssertion.getOrdinal() ? wdc2.getKeyReference() : wdc1.getKeyReference();
                    if (KeyReference.THUMBPRINT_SHA1.getName().equals(finalVal)) {
                        // Assertions other than WSS Config don't currently expose this option,
                        // so this isn't an error as long as the bottommost assertion is the one calling for THUMBPRINT_SHA1.
                        // It will be executed last and so will override any previous setting.
                    } else {
                        String message = "Multiple signing assertions present with different \"Key Reference/Certificate Inclusion\" selections. The same \"Key Reference/Certificate Inclusion\" type should be used for these assertions.";
                        result.addWarning(new PolicyValidatorResult.Warning(pathAssertion, message, null));
                    }
                }

                // Check for the same token protection settings
                if ( wdc1.isUsingProtectTokens() && wdc2.isUsingProtectTokens() && wdc1.isProtectTokens() != wdc2.isProtectTokens() ) {
                    String message = "Multiple signing assertions present with different token signature requirements";
                    result.addWarning(new PolicyValidatorResult.Warning(pathAssertion, message, null));
                }

                // Check for the same signature digest
                if ( wdc1.getDigestAlgorithmName() != null && wdc2.getDigestAlgorithmName() != null && !wdc1.getDigestAlgorithmName().equals(wdc2.getDigestAlgorithmName()) ) {
                    String message = "Multiple signing assertions present with different explicit \"Signature Digest Algorithm\" selections.  If one assertion specifies a digest, the other assertions in the same path should either specify the same digest or else leave it as \"Automatic\".";
                    result.addWarning(new PolicyValidatorResult.Warning(pathAssertion, message, null));
                }
            }
        }
    }

    //- PRIVATE

    private final Assertion assertion;

    private boolean isSamePrivateKey( final PrivateKeyable pk1,
                                      final PrivateKeyable pk2 ) {
        return (Goid.equals(pk1.getNonDefaultKeystoreId(), pk2.getNonDefaultKeystoreId()) || Goid.isDefault(pk1.getNonDefaultKeystoreId()) ) &&
               (pk1.getKeyAlias()!=null && pk1.getKeyAlias().equalsIgnoreCase( pk2.getKeyAlias() ));
    }
}

