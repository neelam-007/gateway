/**
 * Copyright (C) 2007-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.util.IOUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.message.XmlKnob;
import com.l7tech.policy.assertion.*;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enforces the Code Injection Protection Assertion.
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class ServerCodeInjectionProtectionAssertion extends AbstractMessageTargetableServerAssertion<CodeInjectionProtectionAssertion> {
    private static final Logger logger = Logger.getLogger(ServerCodeInjectionProtectionAssertion.class.getName());
    private static final EnumSet<HttpMethod> putAndPost = EnumSet.of(HttpMethod.POST, HttpMethod.PUT);

    /** Number of characters in front of the suspicious code to log when detected. */
    private static final int EVIDENCE_MARGIN_BEFORE = 16;

    /** Number of characters behind the suspicious code to log when detected. */
    private static final int EVIDENCE_MARGIN_AFTER = 24;

    private final Auditor auditor;

    public ServerCodeInjectionProtectionAssertion(final CodeInjectionProtectionAssertion assertion, final ApplicationContext springContext) {
        super(assertion, assertion);
        auditor = new Auditor(this, springContext, logger);
    }

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message msg,
                                              final String targetName,
                                              final AuthenticationContext authContext )
            throws IOException, PolicyAssertionException {

        boolean routed = context.isPostRouting();
        boolean scanBody = true;

        if (isRequest() && routed) {
            auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROJECTION_ALREADY_ROUTED);
            return AssertionStatus.FAILED;
        }

        if (isResponse() && !routed) {
            auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_SKIP_RESPONSE_NOT_ROUTED);
            return AssertionStatus.NONE;
        }

        if (isRequest()) {
            final HttpServletRequestKnob httpServletRequestKnob = msg.getKnob(HttpServletRequestKnob.class);
            boolean isHttp = httpServletRequestKnob != null;
            scanBody = assertion.isIncludeRequestBody() && (!isHttp || putAndPost.contains(httpServletRequestKnob.getMethod()));

            if (assertion.isIncludeRequestUrl()) {
                if (!isHttp) {
                    //bug 5290: URL scan configured but applicable only to HTTP requests
                    auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_NOT_HTTP);
                } else {
                    AssertionStatus status = scanHttpRequestUrl(httpServletRequestKnob);
                    if (status != AssertionStatus.NONE)
                        return status;
                }
            }
        }

        if (scanBody)
            return scanBody(msg, targetName);
        else
            return AssertionStatus.NONE;
    }

    @Override
    protected Auditor getAuditor() {
        return auditor;
    }

    private AssertionStatus scanBody(final Message message, final String messageDesc) throws IOException {
        final ContentTypeHeader contentType = message.getMimeKnob().getOuterContentType();
        if (isRequest() && contentType.matches("application", "x-www-form-urlencoded")) {
            return scanRequestBodyAsWwwForm(message);
        } else if (contentType.matches("multipart", "form-data")) {
            return scanBodyAsMultipartFormData(message, messageDesc);
        } else if (contentType.matches("text", "xml")) {
            return scanBodyAsXml(message, messageDesc);
        } else {
            return scanBodyAsText(message, messageDesc, contentType.getEncoding());
        }
    }

    private AssertionStatus scanRequestBodyAsWwwForm(Message message) throws IOException {
        final HttpServletRequestKnob httpServletRequestKnob = message.getKnob(HttpServletRequestKnob.class);
        //this can only work with http
        if ( httpServletRequestKnob == null ) {
            auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROJECTION_CANNOT_PARSE_CONTENT_TYPE, "application/x-www-form-urlencoded");
            return AssertionStatus.FALSIFIED;
        }

        auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_URLENCODED);

        final StringBuilder evidence = new StringBuilder();
        final Map<String, String[]> urlParams = httpServletRequestKnob.getRequestBodyParameterMap();
        for (String urlParamName : urlParams.keySet()) {
            for (String urlParamValue : urlParams.get(urlParamName)) {
                final CodeInjectionProtectionType protectionViolated = scan(urlParamValue, assertion.getProtections(), evidence);
                if (protectionViolated != null) {
                    auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED_PARAM,
                            "request message body", urlParamName, evidence.toString(), protectionViolated.getDisplayName());
                    return AssertionStatus.FALSIFIED;
                }
            }
        }
        return AssertionStatus.NONE;
    }

    private AssertionStatus scanHttpRequestUrl(final HttpServletRequestKnob httpServletRequestKnob) throws IOException {
        auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_URL);
        final StringBuilder evidence = new StringBuilder();
        final Map<String, String[]> urlParams = httpServletRequestKnob.getQueryParameterMap();
        for (String urlParamName : urlParams.keySet()) {
            for (String urlParamValue : urlParams.get(urlParamName)) {
                final CodeInjectionProtectionType protectionViolated = scan(urlParamValue, assertion.getProtections(), evidence);
                if (protectionViolated != null) {
                    auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED_PARAM,
                            "request URL", urlParamName, evidence.toString(), protectionViolated.getDisplayName());
                    return AssertionStatus.FALSIFIED;
                }
            }
        }

        return AssertionStatus.NONE;
    }

    /**
     * Scans the whole message body as multipart/form-data.
     *
     * @param message       either a request Message or a response Message
     * @param messageDesc   message description
     * @return an assertion status
     * @throws IOException if error in parsing
     */
    private AssertionStatus scanBodyAsMultipartFormData(final Message message, final String messageDesc ) throws IOException {
        auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_FORMDATA, messageDesc);
        final MimeKnob mimeKnob = message.getMimeKnob();
        try {
            final PartIterator itor = mimeKnob.getParts();
            for (int partPosition = 0; itor.hasNext(); ++ partPosition) {
                final String where = messageDesc + " message MIME part " + Integer.toString(partPosition);
                final PartInfo partInfo = itor.next();
                final ContentTypeHeader partContentType = partInfo.getContentType();
                if (partContentType.matches("text", "xml")) {
                    auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_ATTACHMENT_XML, where);

                    try {
                        final Document partXmlDoc = XmlUtil.parse(partInfo.getInputStream(false));
                        if (scanXml(partXmlDoc, where)) {
                            return AssertionStatus.FALSIFIED;
                        }
                    } catch (SAXException e) {
                        auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_CANNOT_PARSE,
                                new String[]{where, "text/xml"}, e);
                        return getBadMessageStatus();
                    }
                } else {
                    auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_ATTACHMENT_TEXT, where);

                    try {
                        final byte[] partBytes = IOUtils.slurpStream(partInfo.getInputStream(false));
                        final String partString = new String(partBytes, partContentType.getEncoding());
                        StringBuilder evidence = new StringBuilder();
                        final CodeInjectionProtectionType protectionViolated = scan(partString, assertion.getProtections(), evidence);
                        if (protectionViolated != null) {
                            auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED,
                                    where, evidence.toString(), protectionViolated.getDisplayName());
                            return AssertionStatus.FALSIFIED;
                        }
                    } catch (NoSuchPartException e) {
                        auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_CANNOT_PARSE,
                                new String[]{where, "text"}, e);
                        return getBadMessageStatus();
                    }
                }
            }
        } catch (NoSuchPartException e) {
            auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_CANNOT_PARSE,
                    new String[]{messageDesc + " message body", "multipart/form-data"}, e);
            return getBadMessageStatus();
        }

        return AssertionStatus.NONE;
    }

    private AssertionStatus scanBodyAsXml(final Message message, final String messageDesc ) throws IOException {
        auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_XML, messageDesc);
        final String where = messageDesc + " message body";
        try {
            final XmlKnob xmlKnob = message.getXmlKnob();
            final Document xmlDoc = xmlKnob.getDocumentReadOnly();
            if (scanXml(xmlDoc, where))
                return AssertionStatus.FALSIFIED;
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_CANNOT_PARSE,
                    new String[]{where, "text/xml"}, e);
            return getBadMessageStatus();
        }

        return AssertionStatus.NONE;
    }

    private AssertionStatus scanBodyAsText(final Message message, final String messageDesc, final String encoding ) throws IOException {
        auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_TEXT, messageDesc);
        final String where = messageDesc + " message body";
        final MimeKnob mimeKnob = message.getMimeKnob();
        try {
            final byte[] bodyBytes = IOUtils.slurpStream(mimeKnob.getEntireMessageBodyAsInputStream());
            final String bodyString = new String(bodyBytes, encoding);
            final StringBuilder evidence = new StringBuilder();
            final CodeInjectionProtectionType protectionViolated = scan(bodyString, assertion.getProtections(), evidence);
            if (protectionViolated != null) {
                auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED,
                        where, evidence.toString(), protectionViolated.getDisplayName());
                return AssertionStatus.FALSIFIED;
            }
        } catch (NoSuchPartException e) {
            auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_CANNOT_PARSE,
                    new String[]{where, "text"}, e);
            return getBadMessageStatus();
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
                final CodeInjectionProtectionType protectionViolated = scan(node.getNodeValue(), assertion.getProtections(), evidence);
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
                    auditor.logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED,
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
