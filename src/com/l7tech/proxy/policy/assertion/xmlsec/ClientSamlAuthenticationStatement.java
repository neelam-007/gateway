/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
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
 * Unimplemented on the client-side.
 * @author mike
 * @version 1.0
 */
public class ClientSamlAuthenticationStatement extends ClientAssertion {
    private static final Logger logger = Logger.getLogger(ClientSamlAuthenticationStatement.class.getName());
    private SamlAuthenticationStatement data;

    public ClientSamlAuthenticationStatement(SamlAuthenticationStatement data) {
        this.data = data;
    }

    public AssertionStatus decorateRequest(PolicyApplicationContext context)
            throws BadCredentialsException, OperationCanceledException, GeneralSecurityException,
                   ClientCertificateException, IOException, SAXException, KeyStoreCorruptException,
                   HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException,
                   InvalidDocumentFormatException
    {
        final Ssg ssg = context.getSsg();

        // If we are capable of having client cert, then get one
        final PrivateKey privateKey;
        if (!ssg.isFederatedGateway() || ssg.getTrustedGateway() != null) {
            context.prepareClientCertificate();
            privateKey = ssg.getClientCertificatePrivateKey();
        } else
            privateKey = null;

        // Look up or apply for SAML ticket
        final SamlAssertion ass = context.getOrCreateSamlAssertion();

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
                        wssReqs.setSenderSamlToken(ass.asElement(), true); // sign the assertion into the msg   
                        wssReqs.setSignTimestamp();
                        wssReqs.setSenderMessageSigningPrivateKey(privateKey);
                    } else {
                        wssReqs.setSenderSamlToken(ass.asElement(), false); // can't sign the assertion
                    }
                    return AssertionStatus.NONE;
                } catch (IOException e) {
                    String msg = "Cannot initialize the recipient's  DecorationRequirements";
                    logger.log(Level.WARNING, msg, e);
                    throw new PolicyAssertionException(msg, e);
                } catch (CertificateException e) {
                    String msg = "Cannot initialize the recipient's  DecorationRequirements";
                    logger.log(Level.WARNING, msg, e);
                    throw new PolicyAssertionException(msg, e);
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
        return "SAML Authentication Statement";
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }
}
