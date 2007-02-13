/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.*;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.CodeInjectionProtectionAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enforces the Code Injection Protection Assertion.
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class ServerCodeInjectionProtectionAssertion extends AbstractServerAssertion<CodeInjectionProtectionAssertion> {

    private static final Logger _logger = Logger.getLogger(ServerCodeInjectionProtectionAssertion.class.getName());

    private enum Direction {request, response};

    /** Number of characters in front of the suspicious code to log when detected. */
    private static final int SUSPECT_MARGIN_BEFORE = 16;

    /** Number of characters behind the suspicious code to log when detected. */
    private static final int SUSPECT_MARGIN_AFTER = 24;

    private final CodeInjectionProtectionAssertion _assertion;
    private final Auditor _auditor;

    public ServerCodeInjectionProtectionAssertion(final CodeInjectionProtectionAssertion assertion, final ApplicationContext springContext) {
        super(assertion);
        _assertion = assertion;
        _auditor = new Auditor(this, springContext, _logger);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Message requestMessage = context.getRequest();
        final Message responseMessage = context.getResponse();
        AssertionStatus status = AssertionStatus.NONE;

        // Skips if not HTTP.
        final HttpServletRequestKnob httpServletRequestKnob = (HttpServletRequestKnob) requestMessage.getKnob(HttpServletRequestKnob.class);
        if (httpServletRequestKnob == null) {
            _auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_NOT_HTTP);
            return AssertionStatus.NOT_APPLICABLE;
        }

        // Scans request URL.
        if (_assertion.isIncludeRequestUrl()) {
            status = scanRequestUrl(httpServletRequestKnob);
            if (status != AssertionStatus.NONE)
                return status;
        }

        // Scans request message body.
        if (_assertion.isIncludeRequestBody()) {
            status = scanRequestBody(requestMessage);
            if (status != AssertionStatus.NONE)
                return status;
        }

        // Scans response message body.
        if (_assertion.isIncludeResponseBody()) {
            // Skips if no response available because not routed yet.
            if (context.getRoutingStatus() != RoutingStatus.ROUTED) {
                _auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_SKIP_RESPONSE_NOT_ROUTED);
            } else {
                status = scanResponseBody(responseMessage);
                if (status != AssertionStatus.NONE)
                    return status;
            }
        }

        return status;
    }

    private AssertionStatus scanRequestUrl(final HttpServletRequestKnob httpServletRequestKnob) throws IOException {
        _logger.finer("Scanning request URL.");
        final StringBuilder suspect = new StringBuilder();
        final Map<String, String[]> urlParams = httpServletRequestKnob.getQueryParameterMap();
        for (String urlParamName : urlParams.keySet()) {
            for (String urlParamValue : urlParams.get(urlParamName)) {
                if (scan(urlParamValue, _assertion.getProtection().getPattern(), suspect)) {
                    _auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED_PARAM,
                            new String[]{"request URL", urlParamName, suspect.toString()});
                    return AssertionStatus.FALSIFIED;
                }
            }
        }

        return AssertionStatus.NONE;
    }

    private AssertionStatus scanRequestBody(final Message requestMessage) throws IOException {
        final HttpServletRequestKnob httpServletRequestKnob = (HttpServletRequestKnob) requestMessage.getKnob(HttpServletRequestKnob.class);

        ContentTypeHeader contentType = ContentTypeHeader.TEXT_DEFAULT;     // Default.
        final String contentTypeString = httpServletRequestKnob.getHeaderSingleValue("Content-Type");
        if (contentTypeString != null) {
            contentType = ContentTypeHeader.parseValue(contentTypeString);
        }

        AssertionStatus status = AssertionStatus.NONE;
        final StringBuilder suspect = new StringBuilder();

        if (contentType.matches("application", "x-www-form-urlencoded")) {
            _logger.finer("Scanning request message body as application/x-www-form-urlencoded.");
            final Map<String, String[]> urlParams = httpServletRequestKnob.getRequestBodyParameterMap();
            for (String urlParamName : urlParams.keySet()) {
                for (String urlParamValue : urlParams.get(urlParamName)) {
                    if (scan(urlParamValue, _assertion.getProtection().getPattern(), suspect)) {
                        _auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED_PARAM,
                                new String[]{"request message body", urlParamName, suspect.toString()});
                        return AssertionStatus.FALSIFIED;
                    }
                }
            }
        } else if (contentType.matches("multipart", "form-data")) {
            status = scanBodyAsMultipartFormData(requestMessage, Direction.request);
        } else if (contentType.matches("text", "xml")) {
            status = scanBodyAsXml(requestMessage, Direction.request);
        } else {
            status = scanBodyAsText(requestMessage, contentType.getEncoding(), Direction.request);
        }

        return status;
    }

    private AssertionStatus scanResponseBody(final Message responseMessage) throws IOException {
        final HttpServletResponseKnob httpServletResponseKnob = (HttpServletResponseKnob) responseMessage.getKnob(HttpServletResponseKnob.class);

        ContentTypeHeader contentType = ContentTypeHeader.TEXT_DEFAULT;   // Default.
        final String contentTypeString[] = httpServletResponseKnob.getHeaderValues("Content-Type");
        if (contentTypeString.length > 0) {
            contentType = ContentTypeHeader.parseValue(contentTypeString[0]);
        }

        AssertionStatus status = AssertionStatus.NONE;

        if (contentType.matches("multipart", "form-data")) {
            status = scanBodyAsMultipartFormData(responseMessage, Direction.response);
        } else if (contentType.matches("text", "xml")) {
            status = scanBodyAsXml(responseMessage, Direction.response);
        } else {
            status = scanBodyAsText(responseMessage, contentType.getEncoding(), Direction.response);
        }

        return status;
    }

    /**
     * Scans the whole message body as multipart/form-data.
     *
     * @param message       either a request Message or a response Message
     * @param direction     message direction
     * @return an assertion status
     * @throws IOException
     */
    private AssertionStatus scanBodyAsMultipartFormData(final Message message, final Direction direction) throws IOException {
        if (_logger.isLoggable(Level.FINER)) {
            _logger.finer("Scanning " + direction + " message body as mulitpart/form-data.");
        }
        final MimeKnob mimeKnob = message.getMimeKnob();
        try {
            final PartIterator itor = mimeKnob.getParts();
            for (int partPosition = 0; itor.hasNext(); ++ partPosition) {
                final String where = direction + " message MIME part " + Integer.toString(partPosition);
                final PartInfo partInfo = itor.next();
                final ContentTypeHeader partContentType = partInfo.getContentType();
                if (partContentType.matches("text", "xml")) {
                    if (_logger.isLoggable(Level.FINER)) {
                        _logger.finer("Scanning " + where + " as text/xml.");
                    }
                    try {
                        final Document partXmlDoc = XmlUtil.parse(partInfo.getInputStream(false));
                        if (scanXml(partXmlDoc, where)) {
                            return AssertionStatus.FALSIFIED;
                        }
                    } catch (SAXException e) {
                        _auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_CANNOT_PARSE,
                                new String[]{where, "text/xml"}, e);
                        return direction == Direction.request ? AssertionStatus.BAD_REQUEST : AssertionStatus.BAD_RESPONSE;
                    }
                } else {
                    if (_logger.isLoggable(Level.FINER)) {
                        _logger.finer("Scanning " + where + " as text.");
                    }
                    try {
                        final byte[] partBytes = HexUtils.slurpStream(partInfo.getInputStream(false));
                        final String partString = new String(partBytes, partContentType.getEncoding());
                        StringBuilder suspect = new StringBuilder();
                        if (scan(partString, _assertion.getProtection().getPattern(), suspect)) {
                            _auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED,
                                    new String[]{where, suspect.toString()});
                            return AssertionStatus.FALSIFIED;
                        }
                    } catch (NoSuchPartException e) {
                        _auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_CANNOT_PARSE,
                                new String[]{where, "text"}, e);
                        return direction == Direction.request ? AssertionStatus.BAD_REQUEST : AssertionStatus.BAD_RESPONSE;
                    }
                }
            }
        } catch (NoSuchPartException e) {
            _auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_CANNOT_PARSE,
                    new String[]{direction + " message body", "multipart/form-data"}, e);
            return direction == Direction.request ? AssertionStatus.BAD_REQUEST : AssertionStatus.BAD_RESPONSE;
        }

        return AssertionStatus.NONE;
    }

    private AssertionStatus scanBodyAsXml(final Message message, final Direction direction) throws IOException {
        if (_logger.isLoggable(Level.FINER)) {
            _logger.finer("Scanning " + direction + " message body as text/xml.");
        }
        final String where = direction + " message body";
        try {
            final XmlKnob xmlKnob = message.getXmlKnob();
            final Document xmlDoc = xmlKnob.getOriginalDocument();
            if (scanXml(xmlDoc, where))
                return AssertionStatus.FALSIFIED;
        } catch (SAXException e) {
            _auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_CANNOT_PARSE,
                    new String[]{where, "text/xml"}, e);
            return direction == Direction.request ? AssertionStatus.BAD_REQUEST : AssertionStatus.BAD_RESPONSE;
        }

        return AssertionStatus.NONE;
    }

    private AssertionStatus scanBodyAsText(final Message message, final String encoding, final Direction direction) throws IOException {
        if (_logger.isLoggable(Level.FINER)) {
            _logger.finer("Scanning " + direction + " message body as text.");
        }
        final String where = direction + " message body";
        final MimeKnob mimeKnob = message.getMimeKnob();
        try {
            final byte[] bodyBytes = HexUtils.slurpStream(mimeKnob.getEntireMessageBodyAsInputStream());
            final String bodyString = new String(bodyBytes, encoding);
            final StringBuilder suspect = new StringBuilder();
            if (scan(bodyString, _assertion.getProtection().getPattern(), suspect)) {
                _auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED,
                        new String[]{where, suspect.toString()});
                return AssertionStatus.FALSIFIED;
            }
        } catch (NoSuchPartException e) {
            _auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_CANNOT_PARSE,
                    new String[]{where, "text"}, e);
            return direction == Direction.request ? AssertionStatus.BAD_REQUEST : AssertionStatus.BAD_RESPONSE;
        }

        return AssertionStatus.NONE;
    }

    /**
     * Recursively scans an XML node for code injection.
     *
     * @param node      XML node to scan
     * @param where     for identifying message in audit & logging
     * @return <code>true</code> if code injection detected
     */
    private boolean scanXml(final Node node, final String where) {
        final StringBuilder suspect = new StringBuilder(SUSPECT_MARGIN_BEFORE + 1 + SUSPECT_MARGIN_AFTER);
        int type = node.getNodeType();
        switch (type) {
            case Node.DOCUMENT_NODE:
            case Node.ELEMENT_NODE:
                final NamedNodeMap attributes = node.getAttributes();
                if (attributes != null) {
                    for (int i = 0; i < attributes.getLength(); i++) {
                        final Node attribute = attributes.item(i);
                        if (scanXml(attribute, where))
                            return true;
                    }
                }
                for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
                    if (scanXml(child, where))
                        return true;
                }
                break;

            case Node.ATTRIBUTE_NODE:
            case Node.CDATA_SECTION_NODE:
            case Node.TEXT_NODE:
                if (scan(node.getNodeValue(), _assertion.getProtection().getPattern(), suspect)) {
                    _auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED,
                            new String[]{where + " in " + node.getParentNode().getNodeName() + "@" + node.getNodeName(), suspect.toString()});
                    return true;
                }
                break;
        }

        return false;
    }

    /**
     * Scans for a string pattern.
     *
     * @param s         string to scan
     * @param pattern   regular expression pattern to search for
     * @param suspect   for passing back snippet of string surrounding the first match (if found)
     * @return <code>true</code> if found; then <code>suspect</code> is populated
     */
    private static boolean scan(final String s, final Pattern pattern, final StringBuilder suspect) {
        final Matcher matcher = pattern.matcher(s);
        if (matcher.find()) {
            suspect.setLength(0);

            int start = matcher.start() - SUSPECT_MARGIN_BEFORE;
            if (start <= 0) {
                start = 0;
            } else {
                suspect.append("...");
            }

            int end = matcher.end() + SUSPECT_MARGIN_AFTER;
            if (end >= s.length()) {
                end = s.length();
                suspect.append(s.substring(start, end));
            } else {
                suspect.append(s.substring(start, end));
                suspect.append("...");
            }

            return true;
        } else {
            return false;
        }
    }
}
