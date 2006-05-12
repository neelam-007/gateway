/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.common.xml.InvalidXpathException;
import com.l7tech.common.xml.ElementCursor;
import com.l7tech.common.xml.SoapValidator;
import com.l7tech.common.xml.xpath.CompiledXpath;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.message.Message;
import com.l7tech.policy.assertion.*;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Server side implementation of the OversizedTextAssertion convenience assertion.
 * Internally this is implemented, essentially, as just a nested xpath assertion.
 */
public class ServerOversizedTextAssertion extends AbstractServerAssertion implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerOversizedTextAssertion.class.getName());
    private final Auditor auditor;
    private final CompiledXpath matchBigText;
    private final CompiledXpath matchBigAttr;
    private final CompiledXpath matchOverdeepNesting;
    private final CompiledXpath matchExtraPayload;
    private final boolean requireValidSoap;

    public ServerOversizedTextAssertion(OversizedTextAssertion data, ApplicationContext springContext) throws ServerPolicyException {
        super(data);
        auditor = new Auditor(this, springContext, ServerOversizedTextAssertion.logger);

        final String textXpath = data.makeTextXpath();
        final String attrXpath = data.makeAttrXpath();
        final String payloadXpath = data.makePayloadLimitXpath();
        final String nestingXpath = data.makeNestingXpath();
        try {
            matchBigText = textXpath == null ? null : new XpathExpression(textXpath).compile();
            matchBigAttr = attrXpath == null ? null : new XpathExpression(attrXpath).compile();
            matchExtraPayload = payloadXpath == null ? null : new XpathExpression(payloadXpath).compile();
            matchOverdeepNesting = nestingXpath == null ? null : new XpathExpression(nestingXpath).compile();
            requireValidSoap = data.isRequireValidSoapEnvelope();
        } catch (InvalidXpathException e) {
            // Can't happen
            throw new ServerPolicyException(data, "Invalid protection xpath: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context)
            throws PolicyAssertionException, IOException
    {
        if (ServerRegex.isPostRouting(context)) {
            auditor.logAndAudit(AssertionMessages.OVERSIZEDTEXT_ALREADY_ROUTED);
            return AssertionStatus.FAILED;
        }

        try {
            final Message request = context.getRequest();
            ElementCursor cursor = request.getXmlKnob().getElementCursor();
            cursor.moveToRoot();

            if (matchBigText != null && cursor.matches(matchBigText)) {
                auditor.logAndAudit(AssertionMessages.OVERSIZEDTEXT_OVERSIZED_TEXT);
                return AssertionStatus.BAD_REQUEST;
            }

            if (matchBigAttr != null && cursor.matches(matchBigAttr)) {
                auditor.logAndAudit(AssertionMessages.OVERSIZEDTEXT_OVERSIZED_TEXT);
                return AssertionStatus.BAD_REQUEST;
            }

            return checkAllButBigTextAndBigAttr(request, cursor, auditor);

        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.XPATH_REQUEST_NOT_XML);
            return AssertionStatus.BAD_REQUEST;
        } catch (XPathExpressionException e) {
            auditor.logAndAudit(AssertionMessages.XPATH_PATTERN_INVALID);
            return AssertionStatus.FAILED;
        }
    }

    /**
     * Check all constraints except matchBigText and matchBigAttr, which can be done more efficiently if Tarari
     * support is known to be available by scanning the token buffer.  (See ServerAcceleratedOversizedTextAssertion.)
     *
     * @param request  the request to examine.  Must not be null.
     * @param cursor   an ElementCursor positioned anywhere on the request to examine.  Must not be null.
     * @return AssertionStatus.NONE if all enabled constraints were satisfied; otherwise AssertionStatus.BAD_REQUEST,
     *         and failure has already been logged and audited using the provided auditor.
     * @throws XPathExpressionException if an XPath match failed and no result could be produced.
     * @throws SAXException if the XML in the first part's InputStream is not well formed
     * @throws IOException if there is a problem reading XML from the first part's InputStream
     * @throws IOException if XML serialization is necessary, and it throws IOException (perhaps due to a lazy DOM)
     * @throws IllegalStateException if the SOAP MIME part has already been destructively read.
     */
    AssertionStatus checkAllButBigTextAndBigAttr(Message request, ElementCursor cursor, Auditor auditor)
            throws XPathExpressionException, IOException, SAXException
    {
        if (requireValidSoap) {
            if (!request.isSoap()) {
                auditor.logAndAudit(AssertionMessages.REQUEST_NOT_SOAP);
                return AssertionStatus.BAD_REQUEST;
            }

            String problem = SoapValidator.validateSoapMessage(cursor);
            if (problem != null) {
                if (logger.isLoggable(Level.INFO)) logger.info("Request not valid SOAP: " + problem);
                auditor.logAndAudit(AssertionMessages.REQUEST_NOT_SOAP);
                return AssertionStatus.BAD_REQUEST;
            }
        }

        cursor.moveToRoot();

        if (matchExtraPayload != null && cursor.matches(matchExtraPayload)) {
            auditor.logAndAudit(AssertionMessages.OVERSIZEDTEXT_EXTRA_PAYLOAD);
            return AssertionStatus.BAD_REQUEST;
        }

        if (matchOverdeepNesting != null && cursor.matches(matchOverdeepNesting)) {
            auditor.logAndAudit(AssertionMessages.XML_NESTING_DEPTH_EXCEEDED);
            return AssertionStatus.BAD_REQUEST;
        }

        // Everything looks good.
        return AssertionStatus.NONE;
    }
}
