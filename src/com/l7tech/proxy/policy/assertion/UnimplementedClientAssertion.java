/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.ClientCertificateException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.ResponseValidationException;
import com.l7tech.proxy.message.PolicyApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Base class for assertions which are not implemented inside the Client Proxy.
 * @author mike
 * @version 1.0
 */
public class UnimplementedClientAssertion extends ClientAssertion {
    public AssertionStatus decorateRequest(PolicyApplicationContext context)
            throws BadCredentialsException, OperationCanceledException, GeneralSecurityException,
            ClientCertificateException, IOException, SAXException
    {
        return AssertionStatus.NOT_APPLICABLE;
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context)
            throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, IOException,
            SAXException, ResponseValidationException
    {
        return AssertionStatus.NONE;
    }

    public String getName() {
        return "Unsupported assertion " + getClass().getName();
    }

    public String iconResource(boolean open) {
        return null;
    }
}
