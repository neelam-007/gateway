/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.validator;

import com.l7tech.security.saml.SamlConstants;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Validates the SAML statement constraints in the context of the policy
 * and the service.
 *
 * @author Emil Marceta
 */
public class SamlStatementValidator implements AssertionValidator {
    /** @noinspection UnusedDeclaration*/
    private static final Logger logger = Logger.getLogger(SamlStatementValidator.class.getName());
    private final RequireWssSaml requestWssSaml;
    private final boolean hasHok;
    private final boolean hasSv;
    final private boolean hasBearer;

    public SamlStatementValidator(final RequireWssSaml sa) {
        requestWssSaml = sa;
        List confirmations = Arrays.asList(requestWssSaml.getSubjectConfirmations());
        hasHok = confirmations.contains(SamlConstants.CONFIRMATION_HOLDER_OF_KEY);
        hasSv = confirmations.contains(SamlConstants.CONFIRMATION_SENDER_VOUCHES);
        hasBearer = confirmations.contains(SamlConstants.CONFIRMATION_BEARER);
    }

    @Override
    public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
        if ( Assertion.isRequest(requestWssSaml) ) {
            final SslAssertion[]  sslAssertions = getSslAssertions(path);
            
            if (requestWssSaml.isNoSubjectConfirmation()) {
                if (sslAssertions.length == 0) {
                    String message = "SSL is recommended with Require SAML Token Profile assertions with No Subject Confirmation";
                    result.addWarning((new PolicyValidatorResult.Warning(requestWssSaml, path, message, null)));
                }
            }

            if (hasBearer) {
                if (sslAssertions.length == 0) {
                    String message = "SSL is recommended with Require SAML Token Profile assertions with Bearer Subject Confirmation";
                    result.addWarning((new PolicyValidatorResult.Warning(requestWssSaml, path, message, null)));
                }
            }

            if (hasHok) {
                if (!requestWssSaml.isRequireHolderOfKeyWithMessageSignature()) {
                    boolean hasSslAsCrendentialSource = false;
                    for (SslAssertion sslAssertion : sslAssertions) {
                        if (sslAssertion.isCredentialSource()) {
                            hasSslAsCrendentialSource = true;
                            break;
                        }
                    }
                    if (!hasSslAsCrendentialSource) {
                        String message = "Require SSL or TLS Transport assertion with Client Certificates configured must be used when No Proof Of Possession specified for Holder-Of-Key.";
                        result.addError((new PolicyValidatorResult.Error(requestWssSaml, path, message, null)));
                    }
                }
            }
            if (hasSv) {
                if (!requestWssSaml.isRequireSenderVouchesWithMessageSignature()) {
                    boolean hasSslAsCrendentialSource = false;
                    for (SslAssertion sslAssertion : sslAssertions) {
                        if (sslAssertion.isCredentialSource()) {
                            hasSslAsCrendentialSource = true;
                            break;
                        }
                    }
                    if (!hasSslAsCrendentialSource) {
                        String message = "SSL with Client Certificates must be used when No Proof Of Possession specified for Sender-Vouches.";
                        result.addError((new PolicyValidatorResult.Error(requestWssSaml, path, message, null)));
                    }
                }
            }
        } else {
            if (hasHok && !requestWssSaml.isRequireHolderOfKeyWithMessageSignature()) {
                String message = "\"Require Message Signature\" should be used for Proof Of Possession with Holder-Of-Key.";
                result.addWarning((new PolicyValidatorResult.Warning(requestWssSaml, path, message, null)));
            }
            if (hasSv && !requestWssSaml.isRequireSenderVouchesWithMessageSignature()) {
                String message = "\"Require Message Signature\" should be used for Proof Of Possession with Sender-Vouches.";
                result.addWarning((new PolicyValidatorResult.Warning(requestWssSaml, path, message, null)));
            }
        }

        if (!requestWssSaml.isCheckAssertionValidity()) {
            String message = "SAML assertion validity period checking is disabled.";
            result.addWarning((new PolicyValidatorResult.Warning(requestWssSaml, path, message, null)));
        }
    }

    private SslAssertion[] getSslAssertions( final AssertionPath path ) {
        Collection<SslAssertion> sslAssertions = new ArrayList<SslAssertion>();

        Assertion[] pathArray = path.getPath();
        for (Assertion assertion : pathArray) {
            if (!assertion.isEnabled()) continue;
            if (assertion instanceof SslAssertion) {
                sslAssertions.add((SslAssertion)assertion);
            }
        }

        return sslAssertions.toArray(new SslAssertion[sslAssertions.size()]);
    }
}
