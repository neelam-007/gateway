/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.common.xml.XpathEvaluator;

import java.security.GeneralSecurityException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.xml.sax.SAXException;
import org.jaxen.JaxenException;

/**
 * Client side support for RequestXpathAssertion.
 * @author mike
 * @version 1.0
 */
public class ClientRequestXpathAssertion extends ClientAssertion {
    private static final Logger log = Logger.getLogger(ClientRequestXpathAssertion.class.getName());
    private RequestXpathAssertion requestXpathAssertion;

    public ClientRequestXpathAssertion(RequestXpathAssertion requestXpathAssertion) {
        this.requestXpathAssertion = requestXpathAssertion;
    }

    public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
        final XpathExpression xpathExpression = requestXpathAssertion.getXpathExpression();
        final XpathEvaluator eval = XpathEvaluator.newEvaluator(request.getDecoratedSoapEnvelope(),
                                                                xpathExpression.getNamespaces());
        try {
            List nodes = eval.select(xpathExpression.getExpression());
            if (nodes == null || nodes.size() < 1) {
                log.info("XPath expression did not match any nodes in response; assertion fails.");
                return AssertionStatus.FALSIFIED;
            }

            log.info("XPath expression matched " + nodes.size() + " nodes in response; assertion succeeds");
            return AssertionStatus.NONE;
        } catch (JaxenException e) {
            log.warning("Invalid expath expression: " + e.getMessage());
            throw new PolicyAssertionException("Unable to execute xpath expression \"" +
                                               xpathExpression.getExpression() + "\"", e);
        }
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) {
        // No action required on response.
        return AssertionStatus.NONE;
    }

    public String getName() {
        String str = "";
        if (requestXpathAssertion != null && requestXpathAssertion.pattern() != null)
            str = " \"" + requestXpathAssertion.pattern() + '"';
        return "Request must match XPath expression";
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlsignature.gif";
    }
}
