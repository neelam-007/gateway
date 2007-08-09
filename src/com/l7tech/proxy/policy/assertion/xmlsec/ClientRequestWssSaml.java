/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client-side support for the SAML security assertion.
 */
public class ClientRequestWssSaml extends ClientAssertion {
    private static final Logger logger = Logger.getLogger(ClientRequestWssSaml.class.getName());
    private static final String PROP_SIGN_SAML_SV = "com.l7tech.proxy.signSamlSenderVouchesAssertion";
    private RequestWssSaml data;
    private boolean senderVouches = false;

    public ClientRequestWssSaml(RequestWssSaml data) {
        this.data = data;
        for (int i = 0; i < data.getSubjectConfirmations().length; i++) {
            String subjconf = data.getSubjectConfirmations()[i];
            if (SamlConstants.CONFIRMATION_SENDER_VOUCHES.equals(subjconf)) {
                senderVouches = true;
            } else if (SamlConstants.CONFIRMATION_HOLDER_OF_KEY.equals(subjconf)) {
                senderVouches = false;
                // If HoK is acceptable, don't try SV
                break;
            }
        }
    }

    public boolean isSenderVouches() {
        return senderVouches;
    }

    public AssertionStatus decorateRequest(PolicyApplicationContext context)
            throws BadCredentialsException, OperationCanceledException, GeneralSecurityException,
            ClientCertificateException, IOException, SAXException, KeyStoreCorruptException,
            HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException,
            InvalidDocumentFormatException, ConfigurationException {
        final Ssg ssg = context.getSsg();

        // If we are capable of having client cert, then get one
        X509Certificate cert = null;
        PrivateKey key = null;
        if (!ssg.isFederatedGateway() || ssg.getTrustedGateway() != null) {
            context.prepareClientCertificate();
            cert = ssg.getClientCertificate();
            key = ssg.getClientCertificatePrivateKey();
        }

        SamlAssertion samlAssertion;
        try {
            samlAssertion = getSamlAssertion(context);
        } catch (ClientCertificateRevokedException cce) {
            if (ssg.isFederatedGateway()) {
                Ssg trustedSsg = ssg.getTrustedGateway();
                if (trustedSsg != null) {
                    try {
                        trustedSsg.getRuntime().getSsgKeyStoreManager().obtainClientCertificate(context.getFederatedCredentials());
                    } catch (ServerFeatureUnavailableException sfue) {
                        throw new ConfigurationException(ExceptionUtils.getMessage(sfue), sfue);
                    }
                    // cert/key are updated
                    cert = ssg.getClientCertificate();
                    key = ssg.getClientCertificatePrivateKey();
                    samlAssertion = getSamlAssertion(context);
                } else {
                    throw cce; // rethrow
                }
            } else {
                throw cce; // rethrow and handle higher up  
            }
        }
        final SamlAssertion ass = samlAssertion;

        final X509Certificate certificate = cert;
        final PrivateKey privateKey = key;
        context.getPendingDecorations().put(this, new ClientDecorator() {
            public AssertionStatus decorateRequest(PolicyApplicationContext context) throws PolicyAssertionException {
                try {
                    DecorationRequirements wssReqs;
                    if (data.getRecipientContext().localRecipient()) {
                        wssReqs = context.getDefaultWssRequirements();
                    } else {
                        wssReqs = context.getAlternateWssRequirements(data.getRecipientContext());
                    }

                    if ( privateKey != null ) {
                        wssReqs.setSignTimestamp();
                        wssReqs.setSenderMessageSigningPrivateKey(privateKey);
                    }

                    if( ass.isHolderOfKey() ) {
                        wssReqs.setSenderSamlToken(ass.asElement(), false);
                    } else {
                        if (privateKey != null && certificate != null) {
                            wssReqs.setSenderMessageSigningCertificate(certificate);
                            // only sign the SAML token if the assertion is not signed
                            wssReqs.setSenderSamlToken(ass.asElement(), !Boolean.getBoolean(PROP_SIGN_SAML_SV));
                        } else {
                            logger.log(Level.WARNING, "Cannot use SAML Sender Vouches, no private key available to sign assertion.");
                            return AssertionStatus.FALSIFIED;
                        }
                    }
                    return AssertionStatus.NONE;
                } catch (IOException e) {
                    String msg = "Cannot initialize the recipient's  DecorationRequirements";
                    logger.log(Level.WARNING, msg, e);
                    throw new PolicyAssertionException(data, msg, e);
                } catch (CertificateException e) {
                    String msg = "Cannot initialize the recipient's  DecorationRequirements";
                    logger.log(Level.WARNING, msg, e);
                    throw new PolicyAssertionException(data, msg, e);
                }
            }
        });

        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context)
            throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, IOException,
                   SAXException, ResponseValidationException, KeyStoreCorruptException, PolicyAssertionException,
                   InvalidDocumentFormatException
    {
        // No action required on response
        return AssertionStatus.NONE;
    }

    public String getName() {
        return isSenderVouches() ? "SAML v1 Sender-Vouches Authentication Statement" : "SAML v1 Holder-of-Key Authentication Statement";
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }

    private SamlAssertion getSamlAssertion(PolicyApplicationContext context)
            throws BadCredentialsException, OperationCanceledException, GeneralSecurityException,
            ClientCertificateException, IOException, SAXException, KeyStoreCorruptException,
            HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException,
            InvalidDocumentFormatException, ConfigurationException {


        final SamlAssertion ass;
        if (isSenderVouches()) {
            if (data.getVersion()==null || data.getVersion().intValue()==1) {
                ass = context.getOrCreateSamlSenderVouchesAssertion(1);
            }
            else {
                ass = context.getOrCreateSamlSenderVouchesAssertion(2);
            }
        } else {
            // Look up or apply for SAML ticket
            if (data.getVersion()==null || data.getVersion().intValue()==1) {
                ass = context.getOrCreateSamlHolderOfKeyAssertion(1);
            }
            else {
                ass = context.getOrCreateSamlHolderOfKeyAssertion(2);
            }
        }
        return ass;
    }
}
