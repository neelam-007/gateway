/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.saml.SamlAssertion;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client-side support for the SAML security assertion.
 */
public class ClientRequestWssSaml extends ClientAssertion {
    private static final Logger logger = Logger.getLogger(ClientRequestWssSaml.class.getName());
    private static final String PROP_SIGN_SAML_SV = "com.l7tech.proxy.signSamlSenderVouchesAssertion";
    private RequireWssSaml data;
    private boolean senderVouches = false;
    private final int samlVersion;

    public ClientRequestWssSaml(RequireWssSaml data) {
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
        this.samlVersion = data.getVersion() == null || data.getVersion() == 1 ? 1 : 2;
    }

    public boolean isSenderVouches() {
        return senderVouches;
    }

    public AssertionStatus decorateRequest(PolicyApplicationContext context)
            throws BadCredentialsException, OperationCanceledException, GeneralSecurityException,
            ClientCertificateException, IOException, SAXException, KeyStoreCorruptException,
            HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException,
            InvalidDocumentFormatException, ConfigurationException
    {
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
                DecorationRequirements wssReqs = context.getWssRequirements(data);

                if ( privateKey != null ) {
                    wssReqs.setSignTimestamp();
                    wssReqs.setSenderMessageSigningPrivateKey(privateKey);
                }

                if( ass.isHolderOfKey() ) {
                    wssReqs.setSenderSamlToken(ass, false);
                } else {
                    final boolean suppress = "true".equals(ssg.getProperties().get("builtinSaml.suppressStrTransform"));
                    wssReqs.setSuppressSamlStrTransform(suppress);
                    if (privateKey != null && certificate != null) {
                        wssReqs.setSenderMessageSigningCertificate(certificate);
                        // only sign the SAML token if the assertion is not signed
                        wssReqs.setSenderSamlToken(ass, !Boolean.getBoolean(PROP_SIGN_SAML_SV));
                    } else {
                        logger.log(Level.INFO, "No private key available to sign assertion -- will include the assertion without a signature.");
                        wssReqs.setSenderSamlToken(ass, false);
                    }
                }
                return AssertionStatus.NONE;
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
        return data.describe();
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }

    private SamlAssertion getSamlAssertion(PolicyApplicationContext context)
            throws BadCredentialsException, OperationCanceledException, GeneralSecurityException,
            ClientCertificateException, IOException, SAXException, KeyStoreCorruptException,
            HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException,
            InvalidDocumentFormatException, ConfigurationException
    {

        return isSenderVouches()
                ? context.getOrCreateSamlSenderVouchesAssertion(samlVersion)
                : context.getOrCreateSamlHolderOfKeyAssertion(samlVersion);
    }
}
