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
import com.l7tech.policy.assertion.xmlsec.SamlSecurity;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;

/**
 * Unimplemented on the client-side.
 * @author mike
 * @version 1.0
 */
public class ClientSamlSecurity extends ClientAssertion {
    private SamlSecurity data;

    public ClientSamlSecurity(SamlSecurity data) {
        this.data = data;
    }

    public AssertionStatus decorateRequest(PolicyApplicationContext context)
            throws BadCredentialsException, OperationCanceledException, GeneralSecurityException,
                   ClientCertificateException, IOException, SAXException, KeyStoreCorruptException,
                   HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException,
                   InvalidDocumentFormatException
    {
        context.prepareClientCertificate();
        final Ssg ssg = context.getSsg();
        final PrivateKey privateKey = SsgKeyStoreManager.getClientCertPrivateKey(ssg);

        // Look up or apply for SAML ticket
        final SamlAssertion ass = context.getOrCreateSamlAssertion();

        context.getPendingDecorations().put(this, new ClientDecorator() {
            public AssertionStatus decorateRequest(PolicyApplicationContext context) {
                // todo fla, look at the recipient information of the assertion before assuming it's for default
                // recipient
                DecorationRequirements wssReqs = context.getDefaultWssRequirements();
                wssReqs.setSignTimestamp(true);
                wssReqs.setSenderSamlToken(ass.asElement());
                wssReqs.setSenderPrivateKey(privateKey);
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
        return "SAML Holder-of-key token";
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }
}
