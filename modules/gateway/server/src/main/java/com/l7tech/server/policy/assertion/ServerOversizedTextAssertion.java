package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.message.Message;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.SoapValidator;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.CompiledXpath;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.OversizedTextAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.policy.ServerPolicyException;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.*;
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
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the OversizedTextAssertion convenience assertion.
 * Internally this is implemented, essentially, as just a nested xpath assertion.
 */
public class ServerOversizedTextAssertion extends AbstractMessageTargetableServerAssertion<OversizedTextAssertion> {
    private static final Logger logger = Logger.getLogger(ServerOversizedTextAssertion.class.getName());
    private final Auditor auditor;
    private final CompiledXpath matchBigText;
    private final long attrLimit; // maximum attribute length to enforce with software; -1 means do not enforce a length with software
    private final int attrNameLimit; // maximum attribute name length to enforce with software; -1 means do not enforce attr name length with software
    private final CompiledXpath matchOverdeepNesting;
    private final CompiledXpath matchExtraPayload;
    private final boolean requireValidSoap;
    private final int nsCountLimit;
    private final int nsPrefixCountLimit;

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
        super(data,data);
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

        this.nsCountLimit = (data.isLimitNamespaceCount() ? data.getMaxNamespaceCount() : -1);
        this.nsPrefixCountLimit = (data.isLimitNamespacePrefixCount() ? data.getMaxNamespacePrefixCount() : -1);
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

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message msg,
                                              final String targetName,
                                              final AuthenticationContext authContext ) throws PolicyAssertionException, IOException {
        if ( isRequest() && context.isPostRouting()) {
            auditor.logAndAudit(AssertionMessages.OVERSIZEDTEXT_ALREADY_ROUTED);
            return AssertionStatus.FAILED;
        }

        if ( isResponse() && !context.isPostRouting() ) {
            auditor.logAndAudit(AssertionMessages.OVERSIZEDTEXT_SKIP_RESPONSE_NOT_ROUTED);
            return AssertionStatus.NONE;
        }

        if (!msg.isXml()) {
            auditor.logAndAudit(AssertionMessages.OVERSIZEDTEXT_NOT_XML, targetName);
            return AssertionStatus.NOT_APPLICABLE;
        }

        try {
            ElementCursor cursor = msg.getXmlKnob().getElementCursor();
            cursor.moveToRoot();

            if (matchBigText != null && cursor.matches(matchBigText)) {
                auditor.logAndAudit(AssertionMessages.OVERSIZEDTEXT_NODE_OR_ATTRIBUTE, targetName);
                return getBadMessageStatus();
            }

            if ((attrLimit >= 0 || attrNameLimit >= 0) && exceedsAttrLimit(cursor, attrLimit, attrNameLimit)) {
                auditor.logAndAudit(AssertionMessages.OVERSIZEDTEXT_NODE_OR_ATTRIBUTE, targetName);
                return getBadMessageStatus();
            }

            if (matchOverdeepNesting != null && cursor.matches(matchOverdeepNesting)) {
                auditor.logAndAudit(AssertionMessages.OVERSIZEDTEXT_XML_NESTING_DEPTH_EXCEEDED, targetName);
                return getBadMessageStatus();
            }

            return checkAllNonTarariSpecific(msg, targetName, cursor, auditor);

        } catch (SAXException e) {
            if ( isRequest() )
                auditor.logAndAudit(AssertionMessages.XPATH_REQUEST_NOT_XML);
            else if ( isResponse() )
                auditor.logAndAudit(AssertionMessages.XPATH_RESPONSE_NOT_XML);
            else 
                auditor.logAndAudit(AssertionMessages.XPATH_MESSAGE_NOT_XML, targetName);
            return getBadMessageStatus();
        } catch (XPathExpressionException e) {
            auditor.logAndAudit(AssertionMessages.XPATH_PATTERN_INVALID);
            return AssertionStatus.FAILED;
        } catch (TransformerConfigurationException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO,
                                new String[] {ExceptionUtils.getMessage(e)},
                                e);
            return getBadMessageStatus();
        }
    }

    private boolean exceedsAttrLimit(ElementCursor cursor, final long attrLimit, final long attrNameLimit) throws TransformerConfigurationException {
        Source source = new DOMSource(cursor.asDomElement().getOwnerDocument());
        SAXResult result = new SAXResult(new DefaultHandler() {
            @Override
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
     * @param targetName  Message target name (request, response, or context var name).
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
    AssertionStatus checkAllNonTarariSpecific(Message request, String targetName, ElementCursor cursor, Auditor auditor)
            throws XPathExpressionException, IOException, SAXException
    {
        if (requireValidSoap) {
            if (!request.isSoap()) {
                auditor.logAndAudit(AssertionMessages.OVERSIZEDTEXT_NOT_SOAP, targetName);
                return getBadMessageStatus();
            }

            String problem = SoapValidator.validateSoapMessage(cursor);
            if (problem != null) {
                if (logger.isLoggable(Level.INFO)) logger.info("Request not valid SOAP: " + problem);
                auditor.logAndAudit(AssertionMessages.OVERSIZEDTEXT_NOT_SOAP, targetName);
                return getBadMessageStatus();
            }
        }

        cursor.moveToRoot();

        if (matchExtraPayload != null && cursor.matches(matchExtraPayload)) {
            auditor.logAndAudit(AssertionMessages.OVERSIZEDTEXT_EXTRA_PAYLOAD_ELEMENTS, targetName);
            return getBadMessageStatus();
        }

        // Everything looks good.
        // new check for namespace declarations - bug 9401
        return checkNamespaceLimits(request, targetName, cursor, auditor);
    }

    /**
     * This method performs the checks on namespace and namespace prefix declaration limits (if enabled in
     * the assertion). This currently uses a DOM walk to achieve this so it is software only -- the use of
     * Tarari raxj tokens should be implemented to maximize performance (ServerAcceleratedOversizedTextAssertion).
     *
     * @param request  the request to examine.  Must not be null.
     * @param targetName  Message target name (request, response, or context var name).
     * @param cursor   an ElementCursor positioned anywhere on the request to examine.  Must not be null.
     * @param auditor  where to save audit records
     * @return AssertionStatus.NONE if all enabled constraints were satisfied; otherwise AssertionStatus.BAD_REQUEST,
     *         and failure has already been logged and audited using the provided auditor.
     * @throws IOException if there is a problem reading XML from the first part's InputStream; or,
     *                     if XML serialization is necessary, and it throws IOException (perhaps due to a lazy DOM)
     * @throws SAXException if the XML in the first part's InputStream is not well formed
     */
    AssertionStatus checkNamespaceLimits(Message request, String targetName, ElementCursor cursor, Auditor auditor)
        throws IOException, SAXException
    {
        final boolean doNsCheck = nsCountLimit > 0;
        final boolean doNsPrefixCheck = nsPrefixCountLimit > 0;
        // this method should only be called after the request.isXml() returns true
        if ((doNsCheck || doNsPrefixCheck) && request.isXml()) {
            // there is no benefit of using the cursor to generate the dom element
//            cursor.moveToRoot();
//            Element docFromCursor = cursor.asDomElement();
//            logger.log(Level.INFO, "doc from cursor: {0}", new String(XmlUtil.toByteArray(docFromCursor)));
//            Pair<Set<String>, Set<String>> cursorCount = findNamespaceDeclarations(docFromCursor, nsCountLimit, nsPrefixCountLimit);
//            logger.log(Level.INFO, "Counts from cursor ns={0}; prefixes={1}", new Object[] { cursorCount.left.size(), cursorCount.right.size() });
            Pair<Set<String>, Set<String>> nsCounts = findNamespaceDeclarations(request.getXmlKnob().getDocumentReadOnly().getDocumentElement(), nsCountLimit, nsPrefixCountLimit);
            logger.log(Level.INFO, "Counts ns={0}; prefixes={1}", new Object[] { nsCounts.left.size(), nsCounts.right.size() });

            if (doNsCheck && nsCounts.left.size() > nsCountLimit) {
                auditor.logAndAudit(AssertionMessages.OVERSIZEDTEXT_NS_DECLARATION_EXCEEDED, targetName);
                return getBadMessageStatus();

            } else if (doNsPrefixCheck && nsCounts.right.size() > nsPrefixCountLimit) {
                auditor.logAndAudit(AssertionMessages.OVERSIZEDTEXT_NS_PREFIX_DECLARATION_EXCEEDED, targetName);
                return getBadMessageStatus();
            }
        }
        return AssertionStatus.NONE;
    }

    /**
     * For bug #9401 - see bugzilla for details
     *
     * This is the software only implementation to find all distinct namespace and ns prefix declarations within an
     * xml document. This logic is copied from the DomUtils.findAllNamespaces helper method modified to include
     * two limit arguments that will short circuit the DOM tree walk.
     *
     * @param element the element to parse all namespace declarations from
     * @param nsLimit the limit on the number of distinct ns declarations
     * @param prefixLimit the limit on the number of distinct ns prefixes
     * @return two Sets of string values, one for namespace Uri's; the other for the namespace prefixes
     * @see com.l7tech.util.DomUtils#findAllNamespaces(org.w3c.dom.Element)
     */
    private Pair<Set<String>,Set<String>> findNamespaceDeclarations(Element element, int nsLimit, int prefixLimit) {
        NamedNodeMap foo = element.getAttributes();

        Set<String> nsUrlSet = new TreeSet<String>();
        Set<String> nsPrefixSet = new TreeSet<String>();

        // Find xmlns:foo, xmlns=
        for (int j = 0; j < foo.getLength(); j++) {
            Attr attrNode = (Attr)foo.item(j);
            String attPrefix = attrNode.getPrefix();
            String attNsUri = attrNode.getNamespaceURI();
            String attLocalName = attrNode.getLocalName();
            String attValue = attrNode.getValue();

            // Bug 2053: Avoid adding xmlns="" to the map
            if (attValue != null && attValue.trim().length() > 0) {

                if (("xmlns".equals(attPrefix) && DomUtils.XMLNS_NS.equals(attNsUri)) || "xmlns".equals(attLocalName)) {
                    nsUrlSet.add(attValue);
                    if (!"xmlns".equals(attLocalName))
                        nsPrefixSet.add(attLocalName);
                }
            }
        }
        NodeList nodes = element.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                NamedNodeMap foo1 = n.getAttributes();
                // Find xmlns:foo, xmlns=
                for (int j = 0; j < foo1.getLength(); j++) {
                    Attr attrNode = (Attr) foo1.item(j);
                    String attPrefix = attrNode.getPrefix();
                    String attNsUri = attrNode.getNamespaceURI();
                    String attLocalName = attrNode.getLocalName();
                    String attValue = attrNode.getValue();

                    // Bug 2053: Avoid adding xmlns="" to the map
                    if (attValue != null && attValue.trim().length() > 0) {
                        if (("xmlns".equals(attPrefix) && DomUtils.XMLNS_NS.equals(attNsUri)) || "xmlns".equals(attLocalName)) {
                            nsUrlSet.add(attValue);
                            if (!"xmlns".equals(attLocalName))
                                nsPrefixSet.add(attLocalName);
                        }
                    }
                }
            }

            // check for limits reached
            if (nsLimit > 0 && nsUrlSet.size() > nsLimit)
                break;
            else if (prefixLimit > 0 && nsPrefixSet.size() > prefixLimit)
                break;
        }

        return new Pair<Set<String>, Set<String>>( nsUrlSet, nsPrefixSet );
    }
}
