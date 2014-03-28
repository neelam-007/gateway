package com.l7tech.server.policy.assertion;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.json.InvalidJsonException;
import com.l7tech.json.JSONData;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.CodeInjectionProtectionAssertion;
import com.l7tech.policy.assertion.CodeInjectionProtectionType;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.TextUtils;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * Enforces the Code Injection Protection Assertion.
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class ServerCodeInjectionProtectionAssertion extends AbstractMessageTargetableServerAssertion<CodeInjectionProtectionAssertion> {
    /** Number of characters in front of the suspicious code to log when detected. */
    private static final int EVIDENCE_MARGIN_BEFORE = 16;

    /** Number of characters behind the suspicious code to log when detected. */
    private static final int EVIDENCE_MARGIN_AFTER = 24;

    public ServerCodeInjectionProtectionAssertion(final CodeInjectionProtectionAssertion assertion) {
        super(assertion);
    }

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message msg,
                                              final String targetName,
                                              final AuthenticationContext authContext)
            throws IOException, PolicyAssertionException {
        boolean routed = context.isPostRouting();
        HttpServletRequestKnob httpServletRequestKnob = null;

        if (isRequest()) {
            if (routed) {
                logAndAudit(AssertionMessages.CODEINJECTIONPROJECTION_ALREADY_ROUTED);
                return AssertionStatus.FAILED;
            }

            httpServletRequestKnob = msg.getKnob(HttpServletRequestKnob.class);
        } else if (isResponse() && !routed) {
            logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_SKIP_RESPONSE_NOT_ROUTED);
            return AssertionStatus.NONE;
        }

        AssertionStatus result = AssertionStatus.NONE;

        if (null != httpServletRequestKnob) { // if the message is HTTP and thereby has a request URL
            if (assertion.isIncludeUrlPath()) {
                result = scanHttpRequestPath(httpServletRequestKnob);

                if (result != AssertionStatus.NONE) {
                    return result;
                }
            }

            if (assertion.isIncludeUrlQueryString()) {
                result = scanHttpRequestUrlQueryString(httpServletRequestKnob);

                if (result != AssertionStatus.NONE) {
                    return result;
                }
            }
        } else if (assertion.isIncludeUrlPath() || assertion.isIncludeUrlQueryString()) {
            // bug 5290: URL scan configured but applicable only to HTTP requests
            logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_NOT_HTTP);
        }

        if (assertion.isIncludeBody()) {
            result = scanBody(msg, targetName);
        }

        return result;
    }

    private AssertionStatus scanBody(final Message message, final String messageDesc) throws IOException {
        final ContentTypeHeader contentType = message.getMimeKnob().getOuterContentType();

        if (isRequest() && contentType.matches("application", "x-www-form-urlencoded")) {
            return scanRequestBodyAsWwwForm(message);
        } else if (contentType.matches("multipart", "form-data")) {
            return scanBodyAsMultipartFormData(message, messageDesc);
        } else if (message.isXml()) {
            return scanBodyAsXml(message, messageDesc);
        } else if (message.isJson()) {
            return scanBodyAsJson(message, messageDesc);
        } else {
            return scanBodyAsText(message, messageDesc, contentType.getEncoding());
        }
    }

    private AssertionStatus scanRequestBodyAsWwwForm(Message message) throws IOException {
        final HttpServletRequestKnob httpServletRequestKnob = message.getKnob(HttpServletRequestKnob.class);
        //this can only work with http
        if (httpServletRequestKnob == null) {
            logAndAudit(AssertionMessages.CODEINJECTIONPROJECTION_CANNOT_PARSE_CONTENT_TYPE, "application/x-www-form-urlencoded");
            return AssertionStatus.FALSIFIED;
        }

        logAndAudit(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_URLENCODED);

        final StringBuilder evidence = new StringBuilder();
        final Map<String, String[]> urlParams = httpServletRequestKnob.getRequestBodyParameterMap();

        for (Map.Entry<String, String[]> entry : urlParams.entrySet()) {
            final String urlParamName = entry.getKey();

            for (String urlParamValue : entry.getValue()) {
                final CodeInjectionProtectionType protectionViolated =
                        scan(urlParamValue, assertion.getProtections(), evidence);

                if (protectionViolated != null) {
                    logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED, "Request message body",
                            urlParamName, evidence.toString(), protectionViolated.getDisplayName());
                    return AssertionStatus.FALSIFIED;
                }
            }
        }

        return AssertionStatus.NONE;
    }

    private AssertionStatus scanHttpRequestPath(final HttpServletRequestKnob httpServletRequestKnob) throws IOException {
        logAndAudit(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_URL_PATH);

        final StringBuilder evidence = new StringBuilder();
        final String urlPath = httpServletRequestKnob.getRequestUri();

        final CodeInjectionProtectionType protectionViolated = scan(urlPath, assertion.getProtections(), evidence);

        if (protectionViolated != null) {
            logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED_PATH,
                    "Request URL", urlPath, evidence.toString(), protectionViolated.getDisplayName());
            return getBadMessageStatus();
        }

        return AssertionStatus.NONE;
    }

    private AssertionStatus scanHttpRequestUrlQueryString(final HttpServletRequestKnob httpServletRequestKnob)
            throws IOException {
        logAndAudit(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_URL_QUERY_STRING);

        final StringBuilder evidence = new StringBuilder();
        final Map<String, String[]> urlParams = httpServletRequestKnob.getQueryParameterMap();

        for (Map.Entry<String, String[]> entry : urlParams.entrySet()) {
            final String urlParamName = entry.getKey();

            for (String urlParamValue : entry.getValue()) {
                final CodeInjectionProtectionType protectionViolated =
                        scan(urlParamValue, assertion.getProtections(), evidence);

                if (protectionViolated != null) {
                    logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED_PARAM,
                            "Request URL", evidence.toString(), urlParamName, protectionViolated.getDisplayName());
                    return getBadMessageStatus();
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
    private AssertionStatus scanBodyAsMultipartFormData(final Message message, final String messageDesc) throws IOException {
        logAndAudit(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_FORMDATA, messageDesc);
        final MimeKnob mimeKnob = message.getMimeKnob();
        try {
            final PartIterator itor = mimeKnob.getParts();
            for (int partPosition = 0; itor.hasNext(); ++ partPosition) {
                final String where = messageDesc + " message MIME part " + Integer.toString(partPosition);
                final PartInfo partInfo = itor.next();
                final ContentTypeHeader partContentType = partInfo.getContentType();
                if (partContentType.isXml()) {
                    logAndAudit(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_ATTACHMENT_XML, where);

                    try {
                        final Document partXmlDoc = XmlUtil.parse(partInfo.getInputStream(false));
                        if (scanXml(partXmlDoc, where)) {
                            return AssertionStatus.FALSIFIED;
                        }
                    } catch (SAXException e) {
                        logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_CANNOT_PARSE,
                                new String[] {where, "text/xml"}, ExceptionUtils.getDebugException(e));
                        return getBadMessageStatus();
                    }
                } else {
                    logAndAudit(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_ATTACHMENT_TEXT, where);

                    try {
                        final byte[] partBytes = IOUtils.slurpStream(partInfo.getInputStream(false));
                        final String partString = new String(partBytes, partContentType.getEncoding());
                        StringBuilder evidence = new StringBuilder();
                        final CodeInjectionProtectionType protectionViolated =
                                scan(partString, assertion.getProtections(), evidence);

                        if (protectionViolated != null) {
                            logAndAuditBodyDetection(where, evidence, protectionViolated);
                            return AssertionStatus.FALSIFIED;
                        }
                    } catch (NoSuchPartException e) {
                        logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_CANNOT_PARSE,
                                new String[] {where, "text"}, ExceptionUtils.getDebugException(e));
                        return getBadMessageStatus();
                    }
                }
            }
        } catch (NoSuchPartException e) {
            logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_CANNOT_PARSE,
                    new String[]{messageDesc + " message body", "multipart/form-data"}, ExceptionUtils.getDebugException(e));
            return getBadMessageStatus();
        }

        return AssertionStatus.NONE;
    }

    private AssertionStatus scanBodyAsJson(final Message message, final String messageDesc) throws IOException {
        logAndAudit(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_JSON, messageDesc);
        final String where = messageDesc + " message body";
        final JsonKnob knob = message.getJsonKnob();
        try {
            final JSONData data = knob.getJsonData();
            if (scanJson(data, where)) {
                return getBadMessageStatus();
            }
        } catch (InvalidJsonException e) {
            logAndAudit(AssertionMessages.JSON_INVALID_JSON,
                    new String[]{messageDesc}, ExceptionUtils.getDebugException(e));
            return getBadMessageStatus();
        }

        return AssertionStatus.NONE;
    }

    private AssertionStatus scanBodyAsXml(final Message message, final String messageDesc) throws IOException {
        logAndAudit(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_XML, messageDesc);
        final String where = messageDesc + " message body";
        try {
            final XmlKnob xmlKnob = message.getXmlKnob();
            final Document xmlDoc = xmlKnob.getDocumentReadOnly();
            if (scanXml(xmlDoc, where))
                return AssertionStatus.FALSIFIED;
        } catch (SAXException e) {
            logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_CANNOT_PARSE,
                    new String[]{where, "XML"}, ExceptionUtils.getDebugException(e));
            return getBadMessageStatus();
        }

        return AssertionStatus.NONE;
    }

    private AssertionStatus scanBodyAsText(final Message message, final String messageDesc, final Charset encoding)
            throws IOException {
        logAndAudit(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_TEXT, messageDesc);
        final String where = messageDesc + " message body";
        final MimeKnob mimeKnob = message.getMimeKnob();
        try {
            final byte[] bodyBytes = IOUtils.slurpStream(mimeKnob.getEntireMessageBodyAsInputStream());
            final String bodyString = new String(bodyBytes, encoding);
            final StringBuilder evidence = new StringBuilder();
            final CodeInjectionProtectionType protectionViolated =
                    scan(bodyString, assertion.getProtections(), evidence);

            if (protectionViolated != null) {
                logAndAuditBodyDetection(where, evidence, protectionViolated);
                return AssertionStatus.FALSIFIED;
            }
        } catch (NoSuchPartException e) {
            logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_CANNOT_PARSE,
                    new String[] {where, "text"}, ExceptionUtils.getDebugException(e));
            return getBadMessageStatus();
        }

        return AssertionStatus.NONE;
    }

    private boolean scanJson(final JSONData jsonData, final String where) throws InvalidJsonException {
        final Object o = jsonData.getJsonObject();
        try {
            process(o);
        } catch (CodeInjectionDetectedException e) {
            logAndAuditBodyDetection(where + " in JSON value",
                    new StringBuilder(e.getEvidence()), e.getProtectionViolated());
            return true;
        }

        return false;
    }

    private class CodeInjectionDetectedException extends Exception{
        private CodeInjectionDetectedException(final String evidence,
                                               final CodeInjectionProtectionType protectionViolated) {
            this.evidence = evidence;
            this.protectionViolated = protectionViolated;
        }

        public String getEvidence() {
            return evidence;
        }

        public CodeInjectionProtectionType getProtectionViolated() {
            return protectionViolated;
        }

        private final String evidence;
        private final CodeInjectionProtectionType protectionViolated;
    }

    private void processString(String value) throws CodeInjectionDetectedException{
        final StringBuilder evidence = new StringBuilder(EVIDENCE_MARGIN_BEFORE + 1 + EVIDENCE_MARGIN_AFTER);
        final CodeInjectionProtectionType protectionViolated = scan(value, assertion.getProtections(), evidence);
        if(protectionViolated != null) {
            throw new CodeInjectionDetectedException(evidence.toString(), protectionViolated);
        }
    }

    private void processList(List<Object> list) throws CodeInjectionDetectedException {
        for (Object o : list) {
            process(o);
        }
    }

    @SuppressWarnings("unchecked")
    private void process(Object value) throws CodeInjectionDetectedException {
        if (value instanceof Map) {
            processMap((Map<Object, Object>) value);
        } else if (value instanceof List) {
            processList((List<Object>) value);
        } else if (value instanceof String) {
            processString((String) value);
        }
    }

    private void processMap(Map<Object, Object> map) throws CodeInjectionDetectedException {
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            process(entry.getKey());
            process(entry.getValue());
        }
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
                final CodeInjectionProtectionType protectionViolated =
                        scan(node.getNodeValue(), assertion.getProtections(), evidence);

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

                    logAndAuditBodyDetection(where + " in XML node " + nodePath, evidence, protectionViolated);
                    return true;
                }

                break;
        }

        return false;
    }

    private void convertAnyNonIdentifiableCharacters(StringBuilder evidence, CodeInjectionProtectionType protection) {
        if (protection.containsNonIdentifiableCharacters()) {
            TextUtils.makeIgnorableCharactersViewableAsUnicode(evidence);
        }
    }

    private void logAndAuditBodyDetection(String where, StringBuilder evidence,
                                          CodeInjectionProtectionType protectionViolated) {
        convertAnyNonIdentifiableCharacters(evidence, protectionViolated);

        logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED,
                where, evidence.toString(), protectionViolated.getDisplayName());
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
            final int index = TextUtils.scanAndRecordMatch(s, protection.getPattern(), tmpEvidence);
            if (index != -1 && (minIndex == -1 || index < minIndex)) {
                minIndex = index;
                evidence.setLength(0);
                evidence.append(tmpEvidence);
                protectionViolated = protection;
            }
        }

        return protectionViolated;
    }
}
