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
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
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
public class ServerCodeInjectionProtectionAssertion extends ServerInjectionThreatProtectionAssertion<CodeInjectionProtectionAssertion> {
    /** Number of characters in front of the suspicious code to log when detected. */
    private static final int EVIDENCE_MARGIN_BEFORE = 16;

    /** Number of characters behind the suspicious code to log when detected. */
    private static final int EVIDENCE_MARGIN_AFTER = 24;

    private static final Logger LOGGER = Logger.getLogger(ServerCodeInjectionProtectionAssertion.class.getName());

    public ServerCodeInjectionProtectionAssertion(final CodeInjectionProtectionAssertion assertion) {
        super(assertion);
    }

    @Override
    protected AssertionStatus doCheckRequest(final PolicyEnforcementContext context, 
                                             final Message msg,
                                             final String targetName, 
                                             final AuthenticationContext authContext)
            throws IOException, PolicyAssertionException {
        return applyThreatProtection(context, msg, targetName);
    }

    @Override
    protected AssertionStatus scanBody(final Message message, final String messageDesc) throws IOException {
        long contentLength;

        try {
            contentLength = message.getMimeKnob().getFirstPart().getActualContentLength();
        } catch (NoSuchPartException ex) {
            logAndAudit(AssertionMessages.NO_SUCH_PART, new String[] {assertion.getTargetName(), "1"},
                    ExceptionUtils.getDebugException(ex));
            return AssertionStatus.FAILED;
        }

        if (0 == contentLength) { // only attempt to scan the body if there's something there
            return AssertionStatus.NONE;
        }

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
            logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_CANNOT_PARSE_CONTENT_TYPE, "application/x-www-form-urlencoded");
            return AssertionStatus.FALSIFIED;
        }

        logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_SCANNING_BODY_URLENCODED);

        final StringBuilder evidence = new StringBuilder();
        final Map<String, String[]> urlParams;
        try {
            urlParams = httpServletRequestKnob.getRawRequestBodyParameterMap();
        } catch (IllegalArgumentException iae) {
            logAndAuditCannotParse("Request message body", iae.getMessage());
            return getBadMessageStatus();
        }

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

    /**
     * Scans the whole message body as multipart/form-data.
     *
     * @param message       either a request Message or a response Message
     * @param messageDesc   message description
     * @return an assertion status
     * @throws IOException if error in parsing
     */
    private AssertionStatus scanBodyAsMultipartFormData(final Message message, final String messageDesc) throws IOException {
        logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_SCANNING_BODY_FORMDATA, messageDesc);
        final MimeKnob mimeKnob = message.getMimeKnob();
        try {
            final PartIterator itor = mimeKnob.getParts();
            for (int partPosition = 0; itor.hasNext(); ++ partPosition) {
                final String where = messageDesc + " message MIME part " + Integer.toString(partPosition);
                final PartInfo partInfo = itor.next();
                final ContentTypeHeader partContentType = partInfo.getContentType();
                if (partContentType.isXml()) {
                    logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_SCANNING_ATTACHMENT_XML, where);

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
                    logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_SCANNING_ATTACHMENT_TEXT, where);

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
        logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_SCANNING_BODY_JSON, messageDesc);
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
        logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_SCANNING_BODY_XML, messageDesc);
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
        logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_SCANNING_BODY_TEXT, messageDesc);
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

    private class CodeInjectionDetectedException extends Exception {
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

    private void processString(String value) throws CodeInjectionDetectedException {
        final StringBuilder evidence = new StringBuilder(EVIDENCE_MARGIN_BEFORE + 1 + EVIDENCE_MARGIN_AFTER);
        final CodeInjectionProtectionType protectionViolated = scan(value, assertion.getProtections(), evidence);
        if (protectionViolated != null) {
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
            String valueToScan = s;
            Pattern compiledPattern = protection.getPattern();
            /*
             * Check if protection is HEX_HTML_JAVASCRIPT. In that case, we need to try converting HEX or OCTAL code
             * (if any) into it's UTF-8 (or default charset) representation and then apply HTML_JAVASCRIPT protection
             * for various HTML tags.
             */
            if (protection == CodeInjectionProtectionType.HEX_HTML_JAVASCRIPT) {
                valueToScan = convertHexOrOctalEncodedToText(s, protection.getPattern());
                compiledPattern = CodeInjectionProtectionType.HTML_JAVASCRIPT.getPattern();
            }
            final StringBuilder tmpEvidence = new StringBuilder();
            final int index = TextUtils.scanAndRecordMatch(valueToScan, compiledPattern, tmpEvidence);
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
     * This method tries converting HEX or OCTAL encoded texts in the string to it's ASCII representation.
     * @param s the string to convert
     * @param pattern Compiled pattern to detect HEX or Octal code
     * @return the converted string
     */
    private static String convertHexOrOctalEncodedToText(final String s, final Pattern pattern) {
        final Matcher matcher = pattern.matcher(s);
        final List<String> searchList = new ArrayList<>();
        final List<String> replacementList = new ArrayList<>();
        while (matcher.find()) {
            final String group = matcher.group();
            searchList.add(group);
            String replacement;
            // Check if group is HEX code
            if (isJavascriptOrPhpHexEncodedCode(group)) {
                replacement = parseHexEncodedCode(group);
            } else {
                // Else, it is Octet code
                replacement = parseOctetCode(group);
            }
            replacementList.add(replacement);
        }
        if (searchList.isEmpty()) {
            // No HEX or OCTAL code found, return the same string.
            return s;
        } else {
            return StringUtils.replaceEach(s, searchList.toArray(new String[searchList.size()]),
                    replacementList.toArray(new String[replacementList.size()]));
        }
    }

    /**
     * Parses the HEX code and decode it to it's UTF-8 (or default charset) representation.
     * @param hexCode HEX encoded code to be parsed
     * @return the UTF-8 representation
     */
    private static String parseHexEncodedCode(final String hexCode) {
        String groupWithoutHexPrefix = hexCode.replace("\\x", "");
        try {
            return new String(Hex.decodeHex(groupWithoutHexPrefix.toCharArray()));
        } catch (DecoderException e) {
            // This should never occur as we have already checked for HEX format in Pattern. Hence, log and ignore.
            LOGGER.log(Level.WARNING, "Hex decoding failed for pattern group {0}", hexCode);
        }
        return hexCode;
    }

    /**
     * Parses the OCTET code and decode it to it's UTF-8 (or default charset) representation.
     * @param octetCode OCTET encoded code to be parsed
     * @return the UTF-8 representation
     */
    private static String parseOctetCode(final String octetCode) {
        String[] split = octetCode.split("\\\\");
        if (split.length > 2) {
            final StringBuilder sb = new StringBuilder();
            // Start with index 1 as 1st string will always be blank because Octal code will start with "\".
            for (int i = 1; i < split.length; i++) {
                sb.append((char) Integer.parseInt(split[i], 8));
            }
            return sb.toString();
        } else {
            return new String(new char[] {(char) Integer.parseInt(split[1], 8)});
        }
    }

    /**
     * Checks if the code is Javascript or PHP HEX encoded code.
     * @param code the code to be checked
     * @return true if it HEX encoded code, else false
     */
    private static boolean isJavascriptOrPhpHexEncodedCode(final String code) {
        return StringUtils.startsWith(code, "\\x");
    }

    @Override
    protected String scan(String s, StringBuilder evidence) {
        CodeInjectionProtectionType protectionViolated =
                scan(s, assertion.getProtections(), evidence);

        return protectionViolated == null ? null : protectionViolated.getDisplayName();
    }

    @Override
    protected void logAndAuditRequestAlreadyRouted() {
        logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_ALREADY_ROUTED);
    }

    @Override
    protected void logAndAuditResponseNotRouted() {
        logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_SKIP_RESPONSE_NOT_ROUTED);
    }

    @Override
    protected void logAndAuditMessageNotHttp() {
        logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_NOT_HTTP);
    }

    @Override
    protected void logAndAuditScanningUrlPath() {
        logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_SCANNING_URL_PATH);
    }

    @Override
    protected void logAndAuditAttackDetectedInUrlPath(StringBuilder evidence, String urlPath, String protectionViolated) {
        logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED_PATH,
                "Request URL", urlPath, evidence.toString(), protectionViolated);
    }

    @Override
    protected void logAndAuditScanningUrlQueryString() {
        logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_SCANNING_URL_QUERY_STRING);
    }

    @Override
    protected void logAndAuditAttackDetectedInQueryParameter(StringBuilder evidence, String urlParamName, String protectionViolated) {
        logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED_PARAM,
                "Request URL", evidence.toString(), urlParamName, protectionViolated);
    }

    @Override
    protected void logAndAuditAttackRejected() {
        // no logging for this case
    }

    @Override
    protected void logAndAuditCannotParse(String location, String errorMessage) {
        logAndAudit(AssertionMessages.CODEINJECTIONPROTECTION_CANNOT_PARSE, location, errorMessage);
    }
}
