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
import com.l7tech.policy.assertion.ResponseXpathAssertion;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.ResponseValidationException;
import com.l7tech.proxy.message.PolicyApplicationContext;
import org.jaxen.JaxenException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Client side support for ResponseXpathAssertion.
 * @author mike
 * @version 1.0
 */
public class ClientResponseXpathAssertion extends ClientXpathAssertion {
    private static final Logger log = Logger.getLogger(ClientResponseXpathAssertion.class.getName());

    public ClientResponseXpathAssertion(ResponseXpathAssertion responseXpathAssertion) {
        super(responseXpathAssertion, false);
    }

    public AssertionStatus decorateRequest(PolicyApplicationContext context) {
        // No action required on request.
        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context)
            throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, IOException,
            SAXException, ResponseValidationException, KeyStoreCorruptException, PolicyAssertionException
    {
        final XpathExpression xpathExpression = getXpathExpression();
        final XpathEvaluator eval = XpathEvaluator.newEvaluator(context.getResponse().getXmlKnob().getDocumentReadOnly(),
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
}
