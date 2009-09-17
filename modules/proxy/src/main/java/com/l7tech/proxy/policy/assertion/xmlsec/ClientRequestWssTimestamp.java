/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.xmlsec.RequireWssTimestamp;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import com.l7tech.proxy.policy.assertion.ClientAssertionWithMetaSupport;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class ClientRequestWssTimestamp extends ClientAssertionWithMetaSupport {
    private static final Logger logger = Logger.getLogger(ClientRequestWssTimestamp.class.getName());

    private final RequireWssTimestamp assertion;

    public ClientRequestWssTimestamp(RequireWssTimestamp assertion) {
        super(assertion);
        this.assertion = assertion;
    }

    @Override
    public AssertionStatus decorateRequest(PolicyApplicationContext context) throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, ClientCertificateException, IOException, SAXException, KeyStoreCorruptException, HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException, InvalidDocumentFormatException, ConfigurationException {
        // No useful SSB behaviour when target != request
        if (assertion.getTarget() != TargetMessageType.REQUEST) return AssertionStatus.NONE;

        context.getPendingDecorations().put(this, new ClientDecorator() {
            @Override
            public AssertionStatus decorateRequest(PolicyApplicationContext context) throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, ClientCertificateException, IOException, SAXException, KeyStoreCorruptException, HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException, InvalidDocumentFormatException, ConfigurationException {
                DecorationRequirements wssReqs = context.getWssRequirements(assertion);
                if (assertion.isSignatureRequired())
                    wssReqs.setSignTimestamp();
                return AssertionStatus.NONE;
            }
        });
        return AssertionStatus.NONE;
    }

    @Override
    public AssertionStatus unDecorateReply(PolicyApplicationContext context) {
        // no action on response
        return AssertionStatus.NONE;
    }
}
