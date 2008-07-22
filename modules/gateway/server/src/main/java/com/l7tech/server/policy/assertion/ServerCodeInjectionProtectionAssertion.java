/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.message.XmlKnob;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.io.IOUtils;
import com.l7tech.policy.assertion.*;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private static final int EVIDENCE_MARGIN_BEFORE = 16;

    /** Number of characters behind the suspicious code to log when detected. */
    private static final int EVIDENCE_MARGIN_AFTER = 24;

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
        if (_assertion.isIncludeRequestBody() &&
                ("POST".equalsIgnoreCase(httpServletRequestKnob.getMethod()) ||
                 "PUT".equalsIgnoreCase(httpServletRequestKnob.getMethod()))) {
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
        final StringBuilder evidence = new StringBuilder();
        final Map<String, String[]> urlParams = httpServletRequestKnob.getQueryParameterMap();
        for (String urlParamName : urlParams.keySet()) {
            for (String urlParamValue : urlParams.get(urlParamName)) {
                final CodeInjectionProtectionType protectionViolated = scan(urlParamValue, _assertion.getProtections(), evidence);
                if (protectionViolated != null) {
                    _auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED_PARAM,
                            "request URL", urlParamName, evidence.toString(), protectionViolated.getDisplayName());
                    return AssertionStatus.FALSIFIED;
                }
            }
        }

        return AssertionStatus.NONE;
    }

    private AssertionStatus scanRequestBody(final Message requestMessage) throws IOException {
        AssertionStatus status = AssertionStatus.NONE;

        final ContentTypeHeader contentType = requestMessage.getMimeKnob().getOuterContentType();
        if (contentType.matches("application", "x-www-form-urlencoded")) {
            _logger.finer("Scanning request message body as application/x-www-form-urlencoded.");
            final StringBuilder evidence = new StringBuilder();
            final HttpServletRequestKnob httpServletRequestKnob = (HttpServletRequestKnob) requestMessage.getKnob(HttpServletRequestKnob.class);
            final Map<String, String[]> urlParams = httpServletRequestKnob.getRequestBodyParameterMap();
            for (String urlParamName : urlParams.keySet()) {
                for (String urlParamValue : urlParams.get(urlParamName)) {
                    final CodeInjectionProtectionType protectionViolated = scan(urlParamValue, _assertion.getProtections(), evidence);
                    if (protectionViolated != null) {
                        _auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED_PARAM,
                                "request message body", urlParamName, evidence.toString(), protectionViolated.getDisplayName());
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
        AssertionStatus status = AssertionStatus.NONE;

        final ContentTypeHeader contentType = responseMessage.getMimeKnob().getOuterContentType();
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
     * @throws IOException if error in parsing
     */
    private AssertionStatus scanBodyAsMultipartFormData(final Message message, final Direction direction) throws IOException {
        if (_logger.isLoggable(Level.FINER)) {
            _logger.finer("Scanning " + direction + " message body as multipart/form-data.");
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
                        final byte[] partBytes = IOUtils.slurpStream(partInfo.getInputStream(false));
                        final String partString = new String(partBytes, partContentType.getEncoding());
                        StringBuilder evidence = new StringBuilder();
                        final CodeInjectionProtectionType protectionViolated = scan(partString, _assertion.getProtections(), evidence);
                        if (protectionViolated != null) {
                            _auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED,
                                    where, evidence.toString(), protectionViolated.getDisplayName());
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
            final Document xmlDoc = xmlKnob.getDocumentReadOnly();
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
            final byte[] bodyBytes = IOUtils.slurpStream(mimeKnob.getEntireMessageBodyAsInputStream());
            final String bodyString = new String(bodyBytes, encoding);
            final StringBuilder evidence = new StringBuilder();
            final CodeInjectionProtectionType protectionViolated = scan(bodyString, _assertion.getProtections(), evidence);
            if (protectionViolated != null) {
                _auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED,
                        where, evidence.toString(), protectionViolated.getDisplayName());
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
        final StringBuilder evidence = new StringBuilder(EVIDENCE_MARGIN_BEFORE + 1 + EVIDENCE_MARGIN_AFTER);
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
                final CodeInjectionProtectionType protectionViolated = scan(node.getNodeValue(), _assertion.getProtections(), evidence);
                if (protectionViolated != null) {
                    String nodePath = node.getNodeName();
                    if (node.getParentNode() != null) {
                        nodePath = node.getParentNode().getNodeName() + "/" + node.getNodeName();
                    } else if (node instanceof Attr) {
                        final Element element = ((Attr) node).getOwnerElement();
                        if (element != null) {
                            nodePath = element.getNodeName() + "@" + node.getNodeName();
                        }
                    }
                    _auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED,
                            where + " in XML node " + nodePath, evidence.toString(), protectionViolated.getDisplayName());
                    return true;
                }
                break;
        }

        return false;
    }

    /**
     * Scans for code injection pattern.
     *
     * @param s             string to scan
     * @param protections   protection types to apply
     * @param evidence      for passing back snippet of string surrounding the
     *                      first match (if found), for logging purpose
     * @return the first protection type violated if found (<code>evidence</code> is then populated);
     *         <code>null</code> if none found
     */
    private static CodeInjectionProtectionType scan(final String s, final CodeInjectionProtectionType[] protections, final StringBuilder evidence) {
        CodeInjectionProtectionType protectionViolated = null;
        int minIndex = -1;

        for (CodeInjectionProtectionType protection : protections) {
            final StringBuilder tmpEvidence = new StringBuilder();
            final int index = scan(s, protection.getPattern(), tmpEvidence);
            if (index != -1 && (minIndex == -1 || index < minIndex)) {
                minIndex = index;
                evidence.setLength(0);
                evidence.append(tmpEvidence);
                protectionViolated = protection;
            }
        }

        return protectionViolated;
    }

    /**
     * Scans for a string pattern.
     *
     * @param s         string to scan
     * @param pattern   regular expression pattern to search for
     * @param evidence  for passing back snippet of string surrounding the first
     *                  (if found) match, for logging purpose
     * @return starting character index if found (<code>evidence</code> is then populated); -1 if not found
     */
    private static int scan(final String s, final Pattern pattern, final StringBuilder evidence) {
        final Matcher matcher = pattern.matcher(s);
        if (matcher.find()) {
            evidence.setLength(0);

            int start = matcher.start() - EVIDENCE_MARGIN_BEFORE;
            if (start <= 0) {
                start = 0;
            } else {
                evidence.append("...");
            }

            int end = matcher.end() + EVIDENCE_MARGIN_AFTER;
            if (end >= s.length()) {
                end = s.length();
                evidence.append(s.substring(start, end));
            } else {
                evidence.append(s.substring(start, end));
                evidence.append("...");
            }

            return matcher.start();
        } else {
            return -1;
        }
    }
}
