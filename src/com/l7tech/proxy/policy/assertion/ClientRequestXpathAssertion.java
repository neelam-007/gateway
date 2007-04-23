/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.xpath.XpathExpression;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.proxy.message.PolicyApplicationContext;
import org.jaxen.JaxenException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Client side support for RequestXpathAssertion.
 * @author mike
 * @version 1.0
 */
public class ClientRequestXpathAssertion extends ClientXpathAssertion {
    private static final Logger log = Logger.getLogger(ClientRequestXpathAssertion.class.getName());

    public ClientRequestXpathAssertion(RequestXpathAssertion requestXpathAssertion) {
        super(requestXpathAssertion, true);
    }

    public AssertionStatus decorateRequest(PolicyApplicationContext context) throws PolicyAssertionException, SAXException, IOException {
        final XpathExpression xpathExpression = getXpathExpression();
        // Match the Original _undecorated_ document always, so operation-specific paths are deterministic
        final XpathEvaluator eval = XpathEvaluator.newEvaluator(context.getRequest().getXmlKnob().getOriginalDocument(),
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
            throw new PolicyAssertionException(xpathBasedAssertion, "Unable to execute xpath expression \"" +
                                               xpathExpression.getExpression() + "\"", e);
        }
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context) throws PolicyAssertionException, IOException, SAXException {
        return decorateRequest(context); // make sure we follow the same policy path when processing the response
    }
}
