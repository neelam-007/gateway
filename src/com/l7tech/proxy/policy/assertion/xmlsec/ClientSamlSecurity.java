/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.saml.SamlHolderOfKeyAssertion;
import com.l7tech.common.security.xml.WssDecorator;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.SamlSecurity;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import com.l7tech.common.xml.saml.SamlHolderOfKeyAssertion;
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

    public AssertionStatus decorateRequest(PendingRequest request)
            throws BadCredentialsException, OperationCanceledException, GeneralSecurityException,
                   ClientCertificateException, IOException, SAXException, KeyStoreCorruptException,
                   HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException,
                   InvalidDocumentFormatException
    {
        request.prepareClientCertificate();
        final Ssg ssg = request.getSsg();
        final PrivateKey privateKey = SsgKeyStoreManager.getClientCertPrivateKey(ssg);

        // Look up or apply for SAML ticket
        final SamlHolderOfKeyAssertion ass = request.getOrCreateSamlHolderOfKeyAssertion();

        request.getPendingDecorations().put(this, new ClientDecorator() {
            public AssertionStatus decorateRequest(PendingRequest request) {
                WssDecorator.DecorationRequirements wssReqs = request.getWssRequirements();
                wssReqs.setSignTimestamp(true);
                wssReqs.setSenderSamlToken(ass.asElement());
                wssReqs.setSenderPrivateKey(privateKey);
                return AssertionStatus.NONE;
            }
        });

        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response)
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
