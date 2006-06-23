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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client-side support for the SAML security assertion.
 */
public class ClientRequestWssSaml extends ClientAssertion {
    private static final Logger logger = Logger.getLogger(ClientRequestWssSaml.class.getName());
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
        final PrivateKey privateKey;
        if (!ssg.isFederatedGateway() || ssg.getTrustedGateway() != null) {
            context.prepareClientCertificate();
            privateKey = ssg.getClientCertificatePrivateKey();
        } else
            privateKey = null;

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

        context.getPendingDecorations().put(this, new ClientDecorator() {
            public AssertionStatus decorateRequest(PolicyApplicationContext context) throws PolicyAssertionException {
                try {
                    DecorationRequirements wssReqs;
                    if (data.getRecipientContext().localRecipient()) {
                        wssReqs = context.getDefaultWssRequirements();
                    } else {
                        wssReqs = context.getAlternateWssRequirements(data.getRecipientContext());
                    }
                    if (privateKey != null) {
                        wssReqs.setSenderSamlToken(ass.asElement(), false);
                        wssReqs.setSignTimestamp();
                        wssReqs.setSenderMessageSigningPrivateKey(privateKey);
                    } else {
                        wssReqs.setSenderSamlToken(ass.asElement(), false); // can't sign the assertion
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
}
