/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import org.jaxen.JaxenException;

import java.util.List;
import java.util.logging.Logger;

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
        // Match the Original _undecorated_ document always, so operation-specific paths are deterministic
        final XpathEvaluator eval = XpathEvaluator.newEvaluator(request.getOriginalDocument(),
                                                                xpathExpression.getNamespaces());
        try {
            List nodes = eval.select(xpathExpression.getExpression());
            if (nodes == null || nodes.size() < 1) {
                log.info("XPath expression did not match any nodes in request; assertion fails.");
                return AssertionStatus.FALSIFIED;
            }

            log.info("XPath expression matched " + nodes.size() + " nodes in request; assertion succeeds");
            return AssertionStatus.NONE;
        } catch (JaxenException e) {
            log.warning("Invalid expath expression: " + e.getMessage());
            throw new PolicyAssertionException("Unable to execute xpath expression \"" +
                                               xpathExpression.getExpression() + "\"", e);
        }
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) throws PolicyAssertionException {
        return decorateRequest(request); // make sure we follow the same policy path when processing the response
    }

    public String getName() {
        String str = "";
        if (requestXpathAssertion != null && requestXpathAssertion.pattern() != null)
            str = " \"" + requestXpathAssertion.pattern() + '"';
        return "Request must match XPath expression" + str;
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }
}
