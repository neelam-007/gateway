/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.ClientCertificateException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.ResponseValidationException;
import com.l7tech.proxy.message.PolicyApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Logger;

/**
 * Base class for assertions which are not implemented inside the Client Proxy.
 * @author mike
 * @version 1.0
 */
public class UnimplementedClientAssertion extends ClientAssertion {
    protected static final Logger logger = Logger.getLogger(UnimplementedClientAssertion.class.getName());
    protected Assertion source;

    public UnimplementedClientAssertion() {
    }

    public UnimplementedClientAssertion(Assertion source) {
        this.source = source;
    }

    public AssertionStatus decorateRequest(PolicyApplicationContext context)
            throws BadCredentialsException, OperationCanceledException, GeneralSecurityException,
            ClientCertificateException, IOException, SAXException
    {
        logger.info("Unknown assertion: " + source.getClass().getName() + "; ignoring");
        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context)
            throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, IOException,
            SAXException, ResponseValidationException
    {
        logger.info("Unknown assertion: " + source.getClass().getName() + "; ignoring");
        return AssertionStatus.NONE;
    }

    protected String getShortName() {
        if (source != null) {
            return source.getClass().getName();
        } else {
            return getClass().getName();
        }
    }

    public String getName() {
        return "Unsupported assertion (will ignore): " + getShortName();
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/unknown.gif";
    }
}
