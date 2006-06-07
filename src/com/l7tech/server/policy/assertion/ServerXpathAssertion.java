/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.xml.ElementCursor;
import com.l7tech.common.xml.xpath.CompiledXpath;
import com.l7tech.common.xml.xpath.XpathResult;
import com.l7tech.common.xml.xpath.XpathResultIterator;
import com.l7tech.common.xml.xpath.XpathResultNodeSet;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SimpleXpathAssertion;
import com.l7tech.policy.variable.PolicyVariableUtils;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract superclass for server assertions whose operation centers around running a single xpath against
 * either the request or the response message, possibly with variable capture.
 */
public abstract class ServerXpathAssertion extends ServerXpathBasedAssertion {
    private static final Logger logger = Logger.getLogger(ServerXpathAssertion.class.getName());
    private final boolean req; // true = operate on request; false = operate on response
    private final String vfound;
    private final String vcount;
    private final String vresult;
    private final String velement;

    public ServerXpathAssertion(SimpleXpathAssertion assertion, ApplicationContext springContext, boolean isReq) {
        super(assertion, springContext);
        this.req = isReq;

        Set<String> varsUsed = PolicyVariableUtils.getVariablesUsedBySuccessors(assertion);
        vfound = varsUsed.contains(assertion.foundVariable()) ? assertion.foundVariable() : null;
        vresult = varsUsed.contains(assertion.resultVariable()) ? assertion.resultVariable() : null;
        vcount = varsUsed.contains(assertion.countVariable()) ? assertion.countVariable() : null;
        velement = varsUsed.contains(assertion.elementVariable()) ? assertion.elementVariable() : null;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException
    {
        final Message message = req ? context.getRequest() : context.getResponse();

        context.setVariable(vfound, SimpleXpathAssertion.FALSE);
        context.setVariable(vcount, "0");
        context.setVariable(vresult, null);
        context.setVariable(velement, null);

        CompiledXpath compiledXpath = getCompiledXpath();
        if (compiledXpath == null) {
            auditor.logAndAudit(AssertionMessages.XPATH_PATTERN_INVALID_MORE_INFO, new String[]{getXpath()});
            //the xpath could not be compiled, so the assertion cannot work ... FAILED
            return AssertionStatus.FAILED;
        }

        final ElementCursor cursor;
        try {
            if (!message.isXml()) {
                auditNotXml();

                //the mesage isn't XML, so an XPath can't be applied ... NOT_APPLICABLE
                return AssertionStatus.NOT_APPLICABLE;
            }

            final XmlKnob xmlKnob = message.getXmlKnob();
            cursor = xmlKnob.getElementCursor();

        } catch (SAXException e) {
            auditNotXml();
            //can't proceed cause the XML message probably isn't well formed ... FAILED
            return AssertionStatus.FAILED;
        }

        cursor.moveToRoot();

        XpathResult xpathResult = null;
        try {
            xpathResult = cursor.getXpathResult(compiledXpath, velement != null);
        } catch (XPathExpressionException e) {
            // Log it, but treat it as null
            if (logger.isLoggable(Level.WARNING)) logger.log(Level.WARNING, "XPath failed: " + ExceptionUtils.getMessage(e), e);
        }
        if (xpathResult == null) {
            auditor.logAndAudit(req ? AssertionMessages.XPATH_PATTERN_NOT_MATCHED_REQUEST_MI
                                    : AssertionMessages.XPATH_PATTERN_NOT_MATCHED_RESPONSE_MI,
                                new String[]{getXpath()});

            //the xpath ran, but nothing was matched ... FALSIFIED
            return AssertionStatus.FALSIFIED;
        }

        final short resultType = xpathResult.getType();
        switch (resultType) {
            case XpathResult.TYPE_BOOLEAN:
                if (xpathResult.getBoolean()) {
                    auditor.logAndAudit(AssertionMessages.XPATH_RESULT_TRUE);
                    context.setVariable(vresult, SimpleXpathAssertion.TRUE);
                    context.setVariable(velement, SimpleXpathAssertion.TRUE);
                    context.setVariable(vcount, "1");
                    context.setVariable(vfound, SimpleXpathAssertion.TRUE);
                    return AssertionStatus.NONE;
                }
                auditor.logAndAudit(AssertionMessages.XPATH_RESULT_FALSE);
                context.setVariable(vresult, SimpleXpathAssertion.FALSE);
                context.setVariable(velement, SimpleXpathAssertion.FALSE);
                context.setVariable(vcount, "1");
                context.setVariable(vfound, SimpleXpathAssertion.FALSE);
                return AssertionStatus.FALSIFIED;

            case XpathResult.TYPE_NUMBER:
                if (vresult != null || velement != null) {
                    String val = Double.toString(xpathResult.getNumber());
                    context.setVariable(vresult, val);
                    context.setVariable(velement, val);
                }

                context.setVariable(vcount, "1");
                context.setVariable(vfound, SimpleXpathAssertion.TRUE);
                // TODO what to log for this?
                // auditor.logAndAudit(AssertionMessages.XPATH_TEXT_NODE_FOUND);
                return AssertionStatus.NONE;

            case XpathResult.TYPE_STRING:
                if (vresult != null || velement != null) {
                    String strVal = xpathResult.getString();
                    context.setVariable(vresult, strVal);
                    context.setVariable(velement, strVal);
                }
                context.setVariable(vcount, "1");
                context.setVariable(vfound, SimpleXpathAssertion.TRUE);
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

        context.setVariable(vfound, SimpleXpathAssertion.TRUE);
        if (vcount != null) context.setVariable(vcount, Integer.toString(size));

        int nodeType = ns.getType(0);
        switch (nodeType) {
            case Node.ELEMENT_NODE:
                auditor.logAndAudit(AssertionMessages.XPATH_ELEMENT_FOUND);
                if (vresult != null) context.setVariable(vresult, ns.getNodeValue(0));
                if (velement != null) {
                    XpathResultIterator it = ns.getIterator();
                    if (it.hasNext()) context.setVariable(velement, it.nextElementAsCursor().asString());
                }
                return AssertionStatus.NONE;

            case Node.TEXT_NODE:
                auditor.logAndAudit(AssertionMessages.XPATH_TEXT_NODE_FOUND);
                if (vresult != null || velement != null) {
                    String val = ns.getNodeValue(0);
                    context.setVariable(vresult, val);
                    context.setVariable(velement, val);
                }
                return AssertionStatus.NONE;

            default:
                /* FALLTHROUGH and handle other type of node */
                break;
        }

        auditor.logAndAudit(AssertionMessages.XPATH_OTHER_NODE_FOUND);
        if (vresult != null || velement != null) {
            String val = ns.getNodeValue(0);
            context.setVariable(vresult, val);
            context.setVariable(velement, val);
        }
        return AssertionStatus.NONE;
    }

    private void auditNotXml() {
        auditor.logAndAudit(req ? AssertionMessages.XPATH_REQUEST_NOT_XML: AssertionMessages.XPATH_RESPONSE_NOT_XML);
    }
}