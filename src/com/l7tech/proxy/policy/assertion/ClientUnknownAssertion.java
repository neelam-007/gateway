/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.UnknownAssertion;
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
 * @author mike
 */
public class ClientUnknownAssertion extends UnimplementedClientAssertion {
    private static final Logger logger = Logger.getLogger(ClientRequestSwAAssertion.class.getName());
    private UnknownAssertion unknownAssertion;

    public ClientUnknownAssertion(UnknownAssertion data) {
        this.unknownAssertion = data;
    }

    public AssertionStatus decorateRequest(PolicyApplicationContext context) throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, ClientCertificateException, IOException, SAXException {
        String desc = getDesc();
        logger.warning("The unknown assertion invoked. Detail message is '" + desc + "'");
        return AssertionStatus.FALSIFIED;
    }

    private String getDesc() {
        final boolean hasDetailMessage = unknownAssertion != null && unknownAssertion.getDetailMessage() != null;
        String desc = hasDetailMessage ? unknownAssertion.getDetailMessage() : "No more description available";
        return desc;
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context) throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, IOException, SAXException, ResponseValidationException {
        String desc = getDesc();
        logger.warning("The unknown assertion invoked. Detail message is '" + desc + "'");
        return AssertionStatus.FALSIFIED;
    }

    public String getName() {
        return "Unknown assertion: " + getDesc();
    }
}
