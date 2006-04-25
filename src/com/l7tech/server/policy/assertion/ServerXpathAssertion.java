/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.SimpleXpathAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.xml.ElementCursor;
import com.l7tech.common.xml.xpath.XpathResult;
import com.l7tech.common.xml.xpath.XpathResultNodeSet;
import com.l7tech.common.xml.xpath.CompiledXpath;
import com.l7tech.common.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Abstract superclass for server assertions whose operation centers around running a single xpath against
 * either the request or the response message, possibly with variable capture.
 */
public abstract class ServerXpathAssertion extends ServerXpathBasedAssertion {
    private static final Logger logger = Logger.getLogger(ServerXpathAssertion.class.getName());
    private final boolean req; // true = operate on request; false = operate on response
    private final String foundVariable;
    private final String countVariable;
    private final String resultVariable;

    public ServerXpathAssertion(SimpleXpathAssertion assertion, ApplicationContext springContext, boolean isReq) {
        super(assertion, springContext);
        this.req = isReq;
        this.foundVariable = assertion.foundVariable();
        this.countVariable = assertion.countVariable();
        this.resultVariable = assertion.resultVariable();
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException
    {
        final Message message = req ? context.getRequest() : context.getResponse();

        context.setVariable(foundVariable, SimpleXpathAssertion.FALSE);
        context.setVariable(countVariable, "0");
        context.setVariable(resultVariable, null);

        CompiledXpath compiledXpath = getCompiledXpath();
        if (compiledXpath == null) {
            auditor.logAndAudit(AssertionMessages.XPATH_PATTERN_INVALID_MORE_INFO, new String[]{getXpath()});
            return AssertionStatus.FALSIFIED;
        }

        final ElementCursor cursor;
        try {
            final XmlKnob xmlKnob = message.getXmlKnob();
            cursor = xmlKnob.getElementCursor();
        } catch (SAXException e) {
            auditor.logAndAudit(req ? AssertionMessages.XPATH_REQUEST_NOT_XML
                                    : AssertionMessages.XPATH_RESPONSE_NOT_XML);
            return AssertionStatus.FAILED;
        }

        cursor.moveToRoot();

        XpathResult xpathResult = null;
        try {
            xpathResult = cursor.getXpathResult(compiledXpath);
        } catch (XPathExpressionException e) {
            // Log it, but treat it as null
            if (logger.isLoggable(Level.WARNING)) logger.log(Level.WARNING, "XPath failed: " + ExceptionUtils.getMessage(e), e);
        }
        if (xpathResult == null) {
            auditor.logAndAudit(req ? AssertionMessages.XPATH_PATTERN_NOT_MATCHED_REQUEST_MI
                                    : AssertionMessages.XPATH_PATTERN_NOT_MATCHED_RESPONSE_MI,
                                new String[]{getXpath()});
            return AssertionStatus.FALSIFIED;
        }

        final short resultType = xpathResult.getType();
        switch (resultType) {
            case XpathResult.TYPE_BOOLEAN:
                if (xpathResult.getBoolean()) {
                    auditor.logAndAudit(AssertionMessages.XPATH_RESULT_TRUE);
                    context.setVariable(resultVariable, SimpleXpathAssertion.TRUE);
                    context.setVariable(countVariable, "1");
                    context.setVariable(foundVariable, SimpleXpathAssertion.TRUE);
                    return AssertionStatus.NONE;
                }
                auditor.logAndAudit(AssertionMessages.XPATH_RESULT_FALSE);
                context.setVariable(resultVariable, SimpleXpathAssertion.FALSE);
                context.setVariable(countVariable, "1");
                context.setVariable(foundVariable, SimpleXpathAssertion.FALSE);
                return AssertionStatus.FALSIFIED;

            case XpathResult.TYPE_NUMBER:
                double numVal = xpathResult.getNumber();
                context.setVariable(resultVariable, Double.toString(numVal));
                context.setVariable(countVariable, "1");
                context.setVariable(foundVariable, SimpleXpathAssertion.TRUE);
                // TODO what to log for this?
                // auditor.logAndAudit(AssertionMessages.XPATH_TEXT_NODE_FOUND);
                return AssertionStatus.NONE;

            case XpathResult.TYPE_STRING:
                String strVal = xpathResult.getString();
                context.setVariable(resultVariable, strVal);
                context.setVariable(countVariable, "1");
                context.setVariable(foundVariable, SimpleXpathAssertion.TRUE);
                // TODO what to log for this?
                // auditor.logAndAudit(AssertionMessages.XPATH_TEXT_NODE_FOUND);
                return AssertionStatus.NONE;

            case XpathResult.TYPE_NODESET:
                /* FALLTHROUGH and handle nodeset */
                break;

            default:
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                                    new String[] {" XPath evaluation produced unknown result type " + resultType});
                return AssertionStatus.FAILED;
        }

        // Ok, it's a nodeset.
        XpathResultNodeSet ns = xpathResult.getNodeSet();

        final int size = ns.size();
        if (size > 1)
            auditor.logAndAudit(AssertionMessages.XPATH_MULTIPLE_RESULTS, new String[] { Integer.toString(size) });

        if (size < 1) {
            auditor.logAndAudit(req ? AssertionMessages.XPATH_PATTERN_NOT_MATCHED_REQUEST_MI
                                    : AssertionMessages.XPATH_PATTERN_NOT_MATCHED_RESPONSE_MI,
                                new String[]{getXpath()});
            return AssertionStatus.FALSIFIED;
        }

        context.setVariable(foundVariable, SimpleXpathAssertion.TRUE);
        context.setVariable(countVariable, Integer.toString(size));

        int nodeType = ns.getType(0);
        switch (nodeType) {
            case Node.ELEMENT_NODE:
                auditor.logAndAudit(AssertionMessages.XPATH_ELEMENT_FOUND);
                context.setVariable(resultVariable, ns.getNodeValue(0));
                return AssertionStatus.NONE;

            case Node.TEXT_NODE:
                auditor.logAndAudit(AssertionMessages.XPATH_TEXT_NODE_FOUND);
                context.setVariable(resultVariable, ns.getNodeValue(0));
                return AssertionStatus.NONE;

            default:
                /* FALLTHROUGH and handle other type of node */
                break;
        }

        auditor.logAndAudit(AssertionMessages.XPATH_OTHER_NODE_FOUND);
        context.setVariable(resultVariable, ns.getNodeValue(0));
        return AssertionStatus.NONE;
    }
}
