/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.xmlsec.RequestWssTimestamp;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class ClientRequestWssTimestamp extends ClientAssertion {
    private static final Logger logger = Logger.getLogger(ClientRequestWssTimestamp.class.getName());

    private final RequestWssTimestamp assertion;

    public ClientRequestWssTimestamp(RequestWssTimestamp assertion) {
        this.assertion = assertion;
    }

    public AssertionStatus decorateRequest(PolicyApplicationContext context) throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, ClientCertificateException, IOException, SAXException, KeyStoreCorruptException, HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException, InvalidDocumentFormatException, ConfigurationException {
        // No useful SSB behaviour when target != request
        if (assertion.getTarget() != TargetMessageType.REQUEST) return AssertionStatus.NONE;

        context.getPendingDecorations().put(this, new ClientDecorator() {
            public AssertionStatus decorateRequest(PolicyApplicationContext context) throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, ClientCertificateException, IOException, SAXException, KeyStoreCorruptException, HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException, InvalidDocumentFormatException, ConfigurationException {
                DecorationRequirements wssReqs;
                if (assertion.getRecipientContext().localRecipient()) {
                    wssReqs = context.getDefaultWssRequirements();
                } else {
                    wssReqs = context.getAlternateWssRequirements(assertion.getRecipientContext());
                }
                if (assertion.isSignatureRequired())
                    wssReqs.setSignTimestamp();
                return AssertionStatus.NONE;
            }
        });
        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context) {
        // no action on response
        return AssertionStatus.NONE;
    }

    public String getName() {
        return "Request Timestamp";
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }

}
