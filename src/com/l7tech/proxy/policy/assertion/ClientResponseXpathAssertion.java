/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.exceptions.*;

import java.security.GeneralSecurityException;
import java.io.IOException;

import org.xml.sax.SAXException;

/**
 * Client side support for RequestXpathAssertion.
 * @author mike
 * @version 1.0
 */
public class ClientResponseXpathAssertion extends ClientAssertion {
    public ClientResponseXpathAssertion(RequestXpathAssertion requestXpathAssertion) {
    }

    public AssertionStatus decorateRequest(PendingRequest request)
            throws BadCredentialsException, OperationCanceledException, GeneralSecurityException,
            ClientCertificateException, IOException, SAXException, KeyStoreCorruptException, HttpChallengeRequiredException, PolicyRetryableException {
        // No action required.
        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response)
            throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, IOException,
            SAXException, ResponseValidationException, KeyStoreCorruptException {
        // No action required.
        return AssertionStatus.NONE;
    }

    public String getName() {
        return "Response must match XPath expression";
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlsignature.gif";
    }
}
