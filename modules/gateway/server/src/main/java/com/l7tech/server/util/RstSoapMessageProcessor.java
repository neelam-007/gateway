package com.l7tech.server.util;

import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.message.Message;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.*;
import com.l7tech.xml.MessageNotSoapException;
import com.l7tech.xml.SoapFaultLevel;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Process an inbound RST SOAP message and log/report errors.
 *
 * @author ghuang
 */
public class RstSoapMessageProcessor {
    public static final String WST_FAULT_CODE_INVALID_REQUEST = "wst:InvalidRequest";
    public static final String WST_FAULT_CODE_INVALID_SECURITY_TOKEN = "wst:InvalidSecurityToken";
    public static final String WST_FAULT_CODE_EXPIRED_DATA = "wst:ExpiredData";
    public static final String WST_FAULT_CODE_FAILED_AUTHENTICATION = "wst:FailedAuthentication";

    public final static String SOAP_VERSION = "soap_version";
    public final static String SOAP_ENVELOPE_NS = "soap_envelope_namespace";
    public final static String WSSE_NS = "ws-security_namespace";
    public final static String WSC_NS = "ws_secure_conversation_namespace";
    public final static String WSU_NS = "ws_utilities_namespace";
    public final static String WST_NS = "ws_trust_namespace";
    public final static String WSA_NS = "ws_addressing_namespace";
    public final static String WSP_NS = "ws_policy_namespace";

    public final static String HAS_WS_ADDRESSING_ACTION = "has_ws_addressing_action";
    public final static String WS_ADDRESSING_ACTION = "ws_addressing_action";
    public final static String HAS_TOKEN_TYPE = "has_rst_token_type";
    public final static String TOKEN_TYPE = "rst_token_type";
    public final static String REQUEST_TYPE = "rst_request_type";
    public final static String REFERENCE_ATTR_URI = "reference_attribute_uri";
    public final static String REFERENCE_ATTR_VALUE_TYPE = "reference_attribute_value_type";
    public final static String HAS_ENTROPY = "has_entropy_element";
    public final static String HAS_BINARY_SECRET = "has_binary_secret_element";
    public final static String BINARY_SECRET_ATTR_TYPE = "binary_secret_type";
    public final static String BINARY_SECRET = "binary_secret";
    public final static String HAS_KEY_SIZE = "has_key_size";
    public final static String KEY_SIZE = "key_size";
    public final static String ERROR = "parsing_error";

    /**
     * Get all information from the RST SOAP message such as namespaces, element values, etc.
     *
     * @param message: the SOAP message to be processed.
     * @param isForIssuance: A flag indicates if the SOAP message is for issuing a security token.  If it is false, it means it is for Security Token Cancellation.
     * @return a map containing all info.  If an error occurs, then the map is returned immediately with the validation detail.
     */
    public static Map<String, String> getRstParameters(final Message message, final boolean isForIssuance) {
        Map<String, String> parameters = new HashMap<String, String>();

        Document doc = null;
        boolean validSoapXml = false;
        try {
            // First check if the message is a SOAP message.  If it is, get the SOAP Envelope URI (or namespace)
            if (message.isSoap()) {
                parameters.put(SOAP_ENVELOPE_NS, message.getSoapKnob().getSoapEnvelopeUri());
                parameters.put(SOAP_VERSION, message.getSoapKnob().getSoapVersion().getVersionNumber());
            }

            // Get the document associated with the SOAP message
            doc = message.getXmlKnob().getDocumentReadOnly();

            // The message is fine.
            validSoapXml = true;
        } catch (IOException e) {
            parameters.put(ERROR, "Cannot read the XML associated with the SOAP message.");
        } catch (SAXException e) {
            parameters.put(ERROR, "The XML associated with the SOAP message is not well formatted.");
        } catch (MessageNotSoapException e) {
            parameters.put(ERROR, "The XML associated with the SOAP message does not have a valid SOAP envelope.");
        } catch (Throwable t) {
            //noinspection ThrowableResultOfMethodCallIgnored
            parameters.put(ERROR, ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(t)));
        }

        if (! validSoapXml) return parameters;

        // Find other namespaces (Note: only WS-Trust Namespace is mandatory to have.)
        Collection<String> allNamespaces = DomUtils.findAllNamespaces(doc.getDocumentElement()).values();

        // Get the namespace of WS Security
        for (String namespace: SoapConstants.WS_SECURITY_NAMESPACE_LIST) {
            if (allNamespaces.contains(namespace)) {
                parameters.put(WSSE_NS, namespace);
                break;
            }
        }
        // Get the namespace of WS Utilities
        for (String namespace: SoapConstants.WSU_URIS) {
            if (allNamespaces.contains(namespace)) {
                parameters.put(WSU_NS, namespace);
                break;
            }
        }
        // Get the namespace of WS Secure Conversation
        for (String namespace: SoapConstants.WSSC_NAMESPACE_ARRAY) {
            if (allNamespaces.contains(namespace)) {
                parameters.put(WSC_NS, namespace);
                break;
            }
        }
        // Get the namespace of WS-Trust
        String wstNS = null;
        for (String namespace: SoapConstants.WST_NAMESPACE_ARRAY) {
            if (allNamespaces.contains(namespace)) {
                parameters.put(WST_NS, namespace);
                wstNS = namespace;
                break;
            }
        }
        // Since WS-Trust namespace is mandatory, needs to check its existence.
        if (wstNS == null || wstNS.trim().isEmpty()) {
            parameters.put(ERROR, "The namespace of WS-Trust is not specified in the SOAP envelope.");
            return parameters;
        }
        // Get the namespace of WS-Addressing
        for (String namespace: SoapConstants.WSA_NAMESPACE_ARRAY) {
            if (allNamespaces.contains(namespace)) {
                parameters.put(WSA_NS, namespace);
                break;
            }
        }
        // Get the namespace of WS-Policy
        for (String namespace: SoapConstants.WSP_NAMESPACE_ARRAY) {
            if (allNamespaces.contains(namespace)) {
                parameters.put(WSP_NS, namespace);
                break;
            }
        }

        // Find header (Note: it is mandatory to have.)
        Element header;
        try {
            header = SoapUtil.getHeaderElement(doc);
        } catch (InvalidDocumentFormatException e) {
            parameters.put(ERROR, "There is more than one Header element in the SOAP envelope."); // Checking SOAP is done already.
            return parameters;
        }
        if (header == null) {
            parameters.put(ERROR, "There is no Header element in the SOAP envelope.");
            return parameters;
        }

        // Find body (Note: it is mandatory to have.)
        Element body;
        try {
            body = SoapUtil.getBodyElement(doc);
        } catch (InvalidDocumentFormatException e) {
            parameters.put(ERROR, "There is more than one Body element in the SOAP envelope."); // Checking SOAP is done already.
            return parameters;
        }
        if (body == null) {
            parameters.put(ERROR, "There is no Body element in the SOAP envelope.");
            return parameters;
        }

        // Find WS-Addressing Action (Note: it is optional to have.)
        Element actionEl;
        try {
            actionEl = DomUtils.findOnlyOneChildElementByName(header, parameters.get(WSA_NS), SoapConstants.WSA_ACTION);
        } catch (TooManyChildElementsException e) {
            parameters.put(ERROR,  "There is more than one Action element of WS-Addressing in the SOAP Header.");
            return parameters;
        }

        if (actionEl != null) {
            String actionValue = DomUtils.getTextValue(actionEl);

            // Check if the action value is empty or not.
            if (actionValue != null && !actionValue.trim().isEmpty()) {
                // Check if the action value is known.
                if (!SoapConstants.WSC_RST_SCT_ACTION_LIST.contains(actionValue) &&
                    !SoapConstants.WST_RST_ISSUE_ACTION_LIST.contains(actionValue) &&
                    !SoapConstants.WSC_RST_CANCEL_ACTION_LIST.contains(actionValue)) {
                    
                    parameters.put(ERROR, "The Action element of WS-Addressing has an unknown value in the SOAP Header.");
                    return parameters;
                }

                parameters.put(HAS_WS_ADDRESSING_ACTION, "true"); // "true" means there exists a ws-addressing action element.
                parameters.put(WS_ADDRESSING_ACTION, actionValue);
            } else {
                // If the Action value is empty, it is ok and we treat it as not specified.
                parameters.put(HAS_WS_ADDRESSING_ACTION, "false");
            }
        } else {
            parameters.put(HAS_WS_ADDRESSING_ACTION, "false");
        }

        // Find RequestSecurityToken (Note: it is mandatory to have.)
        Element rstEl;
        try {
            rstEl = DomUtils.findExactlyOneChildElementByName(body, parameters.get(WST_NS), SoapConstants.WST_REQUESTSECURITYTOKEN);
        } catch (TooManyChildElementsException e) {
            parameters.put(ERROR,  "There is more than one RequestSecurityToken element of WS-Trust in the SOAP Body.");
            return parameters;
        } catch (MissingRequiredElementException e) {
            parameters.put(ERROR,  "There is no RequestSecurityToken element of WS-Trust in the SOAP Body.");
            return parameters;
        }

        // Find all elements in the RequestSecurityToken element
        String elementName = null; // Just keep tracking the name of the element being processed.
        try {
            // Find TokenType (Note: it is optional to have.)
            elementName = SoapConstants.WST_TOKENTYPE;
            Element tokenTypeEl = DomUtils.findOnlyOneChildElementByName(rstEl, parameters.get(WST_NS), SoapConstants.WST_TOKENTYPE);

            if (tokenTypeEl != null) {
                String tokenTypeValue = DomUtils.getTextValue(tokenTypeEl);

                // Check if the TokenType value is empty or not.
                if (tokenTypeValue != null && !tokenTypeValue.trim().isEmpty()) {
                    if (!SoapConstants.WSC_RST_SCT_TOKEN_TYPE_LIST.contains(tokenTypeValue) &&
                        !ArrayUtils.contains(SoapConstants.VALUETYPE_SAML_ARRAY, tokenTypeValue)) {
                        
                        parameters.put(ERROR,  "The TokenType element in the RequestSecurityToken element has an unknown value in the SOAP Body.");
                        return parameters;
                    }

                    parameters.put(HAS_TOKEN_TYPE, "true");
                    parameters.put(TOKEN_TYPE, tokenTypeValue);
                } else {
                    // If the TokenType value is empty, it is ok and we treat it as not specified.
                    parameters.put(HAS_TOKEN_TYPE, "false");
                }
            } else {
                parameters.put(HAS_TOKEN_TYPE, "false");
            }

            // Find RequestType (Note: it is mandatory to have.)
            elementName = SoapConstants.WST_REQUESTTYPE;
            Element requestTypeEl;
            try {
                requestTypeEl = DomUtils.findExactlyOneChildElementByName(rstEl, parameters.get(WST_NS), SoapConstants.WST_REQUESTTYPE);
                String requestTypeValue = DomUtils.getTextValue(requestTypeEl);

                // Check if it is empty.
                if (requestTypeValue == null || requestTypeValue.trim().isEmpty()) { // Actually requestType is never be null, since DomUtils.getTextValue(...) will not return null.
                    parameters.put(ERROR,  "The value of the RequestType element is empty in the RequestSecurityToken element in the SOAP Body.");
                    return parameters;
                }

                // Check if it is a known RequestType.
                if ((isForIssuance && !SoapConstants.WST_RST_ISSUE_REQUEST_TYPE_LIST.contains(requestTypeValue)) ||
                    (!isForIssuance && !SoapConstants.WST_RST_CANCEL_REQUEST_TYPE_LIST.contains(requestTypeValue))) {

                    parameters.put(ERROR,  "The RequestType element in the RequestSecurityToken element has an unknown value in the SOAP Body.");
                    return parameters;
                }

                parameters.put(REQUEST_TYPE, requestTypeValue);
            } catch (MissingRequiredElementException e) {
                parameters.put(ERROR,  "There is no RequestType element in the RequestSecurityToken element in the SOAP Body.");
                return parameters;
            }

            // Find Entropy (Note: it is optional to have.)
            elementName = SoapConstants.ENTROPY;
            Element entropyEl = DomUtils.findOnlyOneChildElementByName(rstEl, parameters.get(WST_NS), SoapConstants.ENTROPY);
            if (entropyEl != null) {
                parameters.put(HAS_ENTROPY, "true"); // "true" means there exists an Entropy element.

                // Find BinarySecret
                elementName = SoapConstants.BINARY_SECRET;
                Element binarySecretEl = DomUtils.findOnlyOneChildElementByName(entropyEl, parameters.get(WST_NS), SoapConstants.BINARY_SECRET);
                if (binarySecretEl != null) {
                    parameters.put(HAS_BINARY_SECRET, "true"); // "true" means there exists an BinarySecret element.
                    parameters.put(BINARY_SECRET, DomUtils.getTextValue(binarySecretEl));
                    parameters.put(BINARY_SECRET_ATTR_TYPE, binarySecretEl.getAttribute(SoapUtil.BINARY_SECRET_ATTR_TYPE));
                }
            } else {
                parameters.put(HAS_ENTROPY, "false");
            }

            // Find KeySize (Note: it is optional to have.)
            elementName = SoapConstants.KEY_SIZE;
            Element keySizeEl = DomUtils.findOnlyOneChildElementByName(rstEl, parameters.get(WST_NS), SoapConstants.KEY_SIZE);
            if (keySizeEl != null) {
                String keySize = DomUtils.getTextValue(keySizeEl);
                if (keySize == null || keySize.trim().isEmpty()) {
                    parameters.put(HAS_KEY_SIZE, "false");
                } else {
                    try {
                        // Check if it is a valid integer before saving it.
                        int size = Integer.parseInt(keySize); // Unit: bits
                        if (size < 0) {
                            parameters.put(ERROR,  "The key size (" + size + ") in the RequestSecurityToken element is not a positive integer.");
                            return parameters;
                        }

                        parameters.put(HAS_KEY_SIZE, "true");
                        parameters.put(KEY_SIZE, keySize);
                    } catch (NumberFormatException e) {
                        parameters.put(ERROR,  "The key size (" + keySize + ") in the RequestSecurityToken element is not an integer.");
                        return parameters;
                    }
                }
            } else {
                parameters.put(HAS_KEY_SIZE, "false");
            }

            // If the RST SOAP message is for security token issuance, then return the parameters and end up here
            if (isForIssuance) return parameters;

            // Find CancelTarget (Note: it is mandatory to have.)
            elementName = SoapConstants.WST_CANCELTARGET;
            Element cancelTargetEl;
            try {
                cancelTargetEl = DomUtils.findExactlyOneChildElementByName(rstEl, parameters.get(WST_NS), SoapConstants.WST_CANCELTARGET);

                // Find SecurityTokenReference (Note: it is mandatory to have.)
                elementName = SoapConstants.WSSE_SECURITY_TOKEN_REFERENCE;
                Element strEl;
                try {
                    strEl = DomUtils.findExactlyOneChildElementByName(cancelTargetEl, parameters.get(WSSE_NS), SoapConstants.WSSE_SECURITY_TOKEN_REFERENCE);
                } catch (MissingRequiredElementException e1) {
                    parameters.put(ERROR,  "There is no SecurityTokenReference element in the CancelTarget element in the SOAP Body.");
                    return parameters;
                }

                // Find Reference (Note: it is mandatory to have.)
                elementName = SoapConstants.WSSE_REFERENCE;
                Element refEl;
                try {
                    refEl = DomUtils.findExactlyOneChildElementByName(strEl, parameters.get(WSSE_NS), SoapConstants.WSSE_REFERENCE);
                } catch (MissingRequiredElementException e1) {
                    parameters.put(ERROR,  "There is no Reference element in the SecurityTokenReference element in the SOAP Body.");
                    return parameters;
                }

                // Find URI (Note: it is mandatory to have.)
                String targetUri = refEl.getAttribute(SoapUtil.WSSE_REFERENCE_ATTR_URI);
                if (targetUri == null || targetUri.trim().isEmpty()) {
                    parameters.put(ERROR,  "The URI of the cancelled target is not specified in the SecurityTokenReference element in the SOAP Body.");
                    return parameters;
                }
                parameters.put(REFERENCE_ATTR_URI, targetUri);

                // Find ValueType (Note: it is optional to have.)
                String valueTypeValue = refEl.getAttribute(SoapUtil.WSSE_REFERENCE_ATTR_VALUE_TYPE);
                if (valueTypeValue != null && !valueTypeValue.trim().isEmpty()) {
                    if (! SoapConstants.WSC_RST_SCT_TOKEN_TYPE_LIST.contains(valueTypeValue)) {
                        parameters.put(ERROR,  "The ValueType attribute in the Reference element has an unknown value in the SOAP Body.");
                        return parameters;
                    }

                    parameters.put(REFERENCE_ATTR_VALUE_TYPE, valueTypeValue);
                }
            } catch (MissingRequiredElementException e) {
                parameters.put(ERROR,  "There is no CancelTarget element in the RequestSecurityToken element in the SOAP Body.");
                return parameters;
            }
        } catch (TooManyChildElementsException e) {
            parameters.put(ERROR, "There is more than one " + elementName + " element in the RequestSecurityToken element in the SOAP Body.");
            return parameters;
        }

        return parameters;
    }

    public static void logAuditAndSetSoapFault(
            final Auditor auditor,
            final PolicyEnforcementContext context,
            final AuditDetailMessage assertionMessage,
            String soapVersion,
            String faultCodeOrValue,
            String faultStringOrReason) {

        // Log and audit
        auditor.logAndAudit(assertionMessage, faultStringOrReason);

        // Set a SOAP fault
        SoapFaultLevel fault = new SoapFaultLevel();
        fault.setLevel(SoapFaultLevel.TEMPLATE_FAULT);

        if ("1.2".equals(soapVersion)) {
            fault.setFaultTemplate("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE + "\" " +
                "                  xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\">\n" +
                "    <soapenv:Body>\n" +
                "        <soapenv:Fault>\n" +
                "            <soapenv:Code>\n" +
                "                <soapenv:Value>" + faultCodeOrValue + "</soapenv:Value>\n" +
                "            </soapenv:Code>\n" +
                "            <soapenv:Reason>\n" +
                "                <soapenv:Text xml:lang=\"en-US\">" + faultStringOrReason + "</soapenv:Text>\n" +
                "            </soapenv:Reason>\n" +
                "            <soapenv:Role>${request.url}</soapenv:Role>\n" +
                "        </soapenv:Fault>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>");
        } else {
            fault.setFaultTemplate("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\" " +
                "                  xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\">\n" +
                "    <soapenv:Body>\n" +
                "        <soapenv:Fault>\n" +
                "            <faultcode>" + faultCodeOrValue + "</faultcode>\n" +
                "            <faultstring>" + faultStringOrReason + "</faultstring>\n" +
                "            <faultactor>${request.url}</faultactor>\n" +
                "        </soapenv:Fault>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>");
        }

        context.setFaultlevel(fault);
    }

    public static String getSoapVersion(final PolicyEnforcementContext context, final Map<String, String> parameters) {
        // Get the SOAP version from the target message first.
        String soapVersion = parameters.get(SOAP_VERSION);

        // If the SOAP version is not ready, then use the SOAP version from the published service or use the default one, 1.1.
        if (soapVersion == null) {
            if (context.getService() != null) {
                soapVersion = context.getService().getSoapVersion().getVersionNumber();
            } else {
                soapVersion = SoapVersion.SOAP_1_1.getVersionNumber();
            }
        }
        
        return soapVersion;
    }
}