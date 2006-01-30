/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.security.saml.SamlAssertionGenerator;
import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.security.saml.SubjectStatement;
import com.l7tech.common.security.xml.CertificateResolver;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.SimpleCertificateResolver;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.UnknownHostException;
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
    private RequestWssSaml data;
    private final InetAddress localHost;

    public ClientRequestWssSaml(RequestWssSaml data) {
        this.data = data;
        try {
            localHost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
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

        boolean sv = false;
        for (int i = 0; i < data.getSubjectConfirmations().length; i++) {
            String subjconf = data.getSubjectConfirmations()[i];
            if (SamlConstants.CONFIRMATION_SENDER_VOUCHES.equals(subjconf)) {
                sv = true;
            } else if (SamlConstants.CONFIRMATION_HOLDER_OF_KEY.equals(subjconf)) {
                sv = false;
                // If HoK is acceptable, don't try SV
                break;
            }
        }

        final SamlAssertion ass;
        if (sv) {
            LoginCredentials creds = context.getRequestCredentials();
            creds.setCredentialSourceAssertion(HttpBasic.class);

            if (creds == null) {
                if (ssg.isChainCredentialsFromClient())
                    throw new PolicyAssertionException("Can't create Sender-Vouches without chained credentials!");

                PasswordAuthentication pw = ssg.getRuntime().getCredentials();
                creds = LoginCredentials.makePasswordCredentials(pw.getUserName(), null, HttpBasic.class);
            }

            SignerInfo si = new SignerInfo(privateKey, new X509Certificate[] { ssg.getClientCertificate() });

            SamlAssertionGenerator.Options opts = new SamlAssertionGenerator.Options();
            opts.setClientAddress(localHost);       // TODO allow override from API caller (i.e. portal) 
            opts.setExpiryMinutes(5);                // TODO configurable?
            opts.setId(SamlAssertionGenerator.generateAssertionId("SSB-SamlAssertion"));
            opts.setSignAssertion(true);             // TODO configurable?
            opts.setUseThumbprintForSignature(true); // TODO configurable?

            SubjectStatement authenticationStatement = SubjectStatement.createAuthenticationStatement(creds, SubjectStatement.SENDER_VOUCHES, true);
            SamlAssertionGenerator sag = new SamlAssertionGenerator(si);
            CertificateResolver thumbResolver = new SimpleCertificateResolver(new X509Certificate[] { ssg.getClientCertificate(), ssg.getServerCertificate() });
            // TODO cache
            ass = new SamlAssertion(sag.createAssertion(authenticationStatement, opts).getDocumentElement(), thumbResolver);
        } else {
            // Look up or apply for SAML ticket
            ass = context.getOrCreateSamlHolderOfKeyAssertion();
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
