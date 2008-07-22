/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.message.Message;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.SoapValidator;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.CompiledXpath;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.OversizedTextAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the OversizedTextAssertion convenience assertion.
 * Internally this is implemented, essentially, as just a nested xpath assertion.
 */
public class ServerOversizedTextAssertion extends AbstractServerAssertion<OversizedTextAssertion> implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerOversizedTextAssertion.class.getName());
    private final Auditor auditor;
    private final CompiledXpath matchBigText;
    private final long attrLimit; // maximum attribute length to enforce with software; -1 means do not enforce a length with software
    private final int attrNameLimit; // maximum attribute name length to enforce with software; -1 means do not enforce attr name length with software
    private final CompiledXpath matchOverdeepNesting;
    private final CompiledXpath matchExtraPayload;
    private final boolean requireValidSoap;

    /**
     * Create assertion instance which might be a subassertion handling only the non-Tarari-specific constraints.
     *
     * @param data             assertion bean instance configuring the constraints to check.  Must not be null.
     * @param springContext    spring context for getting access to server beans.  Must not be null.
     * @param omitTarariTests  true if this assertion instance should not bother initializing xpaths for the Tarari-specific tests;
     *                         this will only be the case if a ServerAcceleratedOversizedTextAssertion is invoking this
     *                         constructor and intends to do those tests itself.
     * @throws ServerPolicyException if the provided assertion bean produced an invalid XPath.  Normally not possible.
     */
    ServerOversizedTextAssertion(OversizedTextAssertion data, ApplicationContext springContext, boolean omitTarariTests) throws ServerPolicyException {
        super(data);
        auditor = new Auditor(this, springContext, ServerOversizedTextAssertion.logger);

        // These three tests might be taken over by ServerAcceleratedOversizedTextAssertion
        final String textXpath = omitTarariTests ? null : data.makeTextXpath();
        final long attrLimit = omitTarariTests ? -1 : (data.isLimitAttrChars() ? data.getMaxAttrChars() : -1);
        final int attrNameLimit = omitTarariTests ? -1 : (data.isLimitAttrNameChars() ? data.getMaxAttrNameChars() : -1);
        final String nestingXpath = omitTarariTests ? null : data.makeNestingXpath();

        final String payloadXpath = data.makePayloadLimitXpath();
        try {
            // These three tests might be taken over by ServerAcceleratedOversizedTextAssertion
            matchBigText = textXpath == null ? null : new XpathExpression(textXpath).compile();
            this.attrLimit = attrLimit;
            this.attrNameLimit = attrNameLimit;
            matchOverdeepNesting = nestingXpath == null ? null : new XpathExpression(nestingXpath).compile();

            matchExtraPayload = payloadXpath == null ? null : new XpathExpression(payloadXpath).compile();
            requireValidSoap = data.isRequireValidSoapEnvelope();
        } catch (InvalidXpathException e) {
            // Can't happen
            throw new ServerPolicyException(data, "Invalid protection xpath: " + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * Create normal assertion instance.
     *
     * @param data             assertion bean instance configuring the constraints to check.  Must not be null.
     * @param springContext    spring context for getting access to server beans.  Must not be null.
     * @throws ServerPolicyException if the provided assertion bean produced an invalid XPath.  Normally not possible.
     */
    public ServerOversizedTextAssertion(OversizedTextAssertion data, ApplicationContext springContext) throws ServerPolicyException {
        this(data, springContext, false);
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

            if ((attrLimit >= 0 || attrNameLimit >= 0) && exceedsAttrLimit(cursor, attrLimit, attrNameLimit)) {
                auditor.logAndAudit(AssertionMessages.OVERSIZEDTEXT_OVERSIZED_TEXT);
                return AssertionStatus.BAD_REQUEST;
            }

            if (matchOverdeepNesting != null && cursor.matches(matchOverdeepNesting)) {
                auditor.logAndAudit(AssertionMessages.XML_NESTING_DEPTH_EXCEEDED);
                return AssertionStatus.BAD_REQUEST;
            }

            return checkAllNonTarariSpecific(request, cursor, auditor);

        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.XPATH_REQUEST_NOT_XML);
            return AssertionStatus.BAD_REQUEST;
        } catch (XPathExpressionException e) {
            auditor.logAndAudit(AssertionMessages.XPATH_PATTERN_INVALID);
            return AssertionStatus.FAILED;
        } catch (TransformerConfigurationException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO,
                                new String[] {ExceptionUtils.getMessage(e)},
                                e);
            return AssertionStatus.BAD_REQUEST;
        }
    }

    private boolean exceedsAttrLimit(ElementCursor cursor, final long attrLimit, final long attrNameLimit) throws TransformerConfigurationException {
        Source source = new DOMSource(cursor.asDomElement().getOwnerDocument());
        SAXResult result = new SAXResult(new DefaultHandler() {
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                int num = attributes.getLength();
                for (int i = 0; i < num; i++) {
                    if (attrLimit >= 0 && attributes.getValue(i).length() > attrLimit)
                        throw new SAXException("Atribute value length limit exceeded");
                    if (attrNameLimit >= 0 && attributes.getQName(i).length() > attrNameLimit)
                        throw new SAXException("Atribute name length limit exceeded (attribute QName length)");
                }
            }
        });

        try {
            TransformerFactory.newInstance().newTransformer().transform(source, result);
            return false;
        } catch (TransformerException e) {
            return true;
        }
    }

    /**
     * Check all constraints except matchBigText, matchBigAttr, and matchOverdeepNesting,
     * which can be done more efficiently if Tarari support is known to be available
     * by scanning the token buffer or getting the RaxStatistics.  (See ServerAcceleratedOversizedTextAssertion.)
     *
     * @param request  the request to examine.  Must not be null.
     * @param cursor   an ElementCursor positioned anywhere on the request to examine.  Must not be null.
     * @param auditor  where to save audit records
     * @return AssertionStatus.NONE if all enabled constraints were satisfied; otherwise AssertionStatus.BAD_REQUEST,
     *         and failure has already been logged and audited using the provided auditor.
     * @throws XPathExpressionException if an XPath match failed and no result could be produced.
     * @throws SAXException if the XML in the first part's InputStream is not well formed
     * @throws IOException if there is a problem reading XML from the first part's InputStream; or,
     *                     if XML serialization is necessary, and it throws IOException (perhaps due to a lazy DOM)
     * @throws IllegalStateException if the SOAP MIME part has already been destructively read.
     */
    AssertionStatus checkAllNonTarariSpecific(Message request, ElementCursor cursor, Auditor auditor)
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

        // Everything looks good.
        return AssertionStatus.NONE;
    }
}
