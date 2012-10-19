package com.l7tech.server.util;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.*;
import com.l7tech.xml.MessageNotSoapException;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
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
    public static final String WST_FAULT_CODE_FAILED_AUTHENTICATION = "wst:FailedAuthentication"; // Leave it here now for future uses.

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
    public final static String HAS_KEY_ENCRYPTION_ALGORITHM = "has_key_encryption_algorithm";
    public final static String KEY_ENCRYPTION_ALGORITHM = "key_encryption_algorithm";
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

    private static final Map<String,String> trustToPolicyNsMap;
    private static final  Map<String,String> trustToAddressingNsMap;
    private static final  Map<String,String> trustToWsscNsMap;

    static {
        final HashMap<String,String> trustMap = new HashMap<String,String>();
        trustMap.put( SoapConstants.WST_NAMESPACE, SoapConstants.WSP_NAMESPACE );
        trustMap.put( SoapConstants.WST_NAMESPACE2, SoapConstants.WSP_NAMESPACE2 );
        trustMap.put( SoapConstants.WST_NAMESPACE3, SoapConstants.WSP_NAMESPACE2 );
        trustMap.put( SoapConstants.WST_NAMESPACE4, SoapConstants.WSP_NAMESPACE2 );
        trustToPolicyNsMap = Collections.unmodifiableMap( trustMap );

        final HashMap<String,String> addressingMap = new HashMap<String,String>();
        addressingMap.put( SoapConstants.WST_NAMESPACE, SoapConstants.WSA_NAMESPACE );
        addressingMap.put( SoapConstants.WST_NAMESPACE2, SoapConstants.WSA_NAMESPACE2 );
        addressingMap.put( SoapConstants.WST_NAMESPACE3, SoapConstants.WSA_NAMESPACE_10 );
        addressingMap.put( SoapConstants.WST_NAMESPACE4, SoapConstants.WSA_NAMESPACE_10 );
        trustToAddressingNsMap = Collections.unmodifiableMap( addressingMap );

        final HashMap<String,String> wsscMap = new HashMap<String,String>();
        wsscMap.put( SoapConstants.WST_NAMESPACE, SoapConstants.WSSC_NAMESPACE );
        wsscMap.put( SoapConstants.WST_NAMESPACE2, SoapConstants.WSSC_NAMESPACE2 );
        wsscMap.put( SoapConstants.WST_NAMESPACE3, SoapConstants.WSSC_NAMESPACE3 );
        wsscMap.put( SoapConstants.WST_NAMESPACE4, SoapConstants.WSSC_NAMESPACE3 );
        trustToWsscNsMap = Collections.unmodifiableMap( wsscMap );
    }

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
        for (String namespace: SoapConstants.SECURITY_URIS) {
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

        // Find header (Note: it is optional to have.)
        Element header;
        try {
            header = SoapUtil.getHeaderElement(doc);
        } catch (InvalidDocumentFormatException e) {
            parameters.put(ERROR, "There is more than one Header element in the SOAP envelope."); // Checking SOAP is done already.
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
            actionEl = header==null ? null : DomUtils.findOnlyOneChildElementByName(header, parameters.get(WSA_NS), SoapConstants.WSA_ACTION);
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
                    
                    parameters.put(ERROR, "The Action element of WS-Addressing is an unknown value in the SOAP Header.");
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

        // Find EncryptionMethod (Note: it is optional to have.)
        Element encryptionMethodEl = header==null ? null : DomUtils.findFirstDescendantElement(header, SoapConstants.XMLENC_NS, SoapConstants.ENCRYPTION_METHOD);
        if (encryptionMethodEl != null) {
            String alg = encryptionMethodEl.getAttribute(SoapUtil.ATTRIBUTE_ALGORITHM);
            if (alg != null && !alg.trim().isEmpty()) {
                parameters.put(HAS_KEY_ENCRYPTION_ALGORITHM, "true");
                parameters.put(KEY_ENCRYPTION_ALGORITHM, alg);
            } else {
                parameters.put(HAS_KEY_ENCRYPTION_ALGORITHM, "false");
            }
        } else {
            parameters.put(HAS_KEY_ENCRYPTION_ALGORITHM, "false");
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
                        !ArrayUtils.contains(SoapConstants.VALUETYPE_SAML_ARRAY, tokenTypeValue) &&
                        !SoapConstants.SAML_NAMESPACE_LIST.contains(tokenTypeValue)) {
                        
                        parameters.put(ERROR,  "The TokenType element in the RequestSecurityToken element is an unknown value in the SOAP Body.");
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

                    parameters.put(ERROR,  "The RequestType element in the RequestSecurityToken element is an unknown value in the SOAP Body.");
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

                    String valueOfTypeAttr = binarySecretEl.getAttribute(SoapUtil.BINARY_SECRET_ATTR_TYPE);
                    if (valueOfTypeAttr != null) {
                        if (valueOfTypeAttr.trim().isEmpty()) {
                            parameters.put(ERROR,  "The URI of the attribute 'Type' is empty in the BinarySecret element.");
                            return parameters;
                        } else if (valueOfTypeAttr.contains(SoapConstants.WST_BINARY_SECRET_NONCE_TYPE)) {
                            if (! SoapConstants.WST_BINARY_SECRET_NONCE_TYPE_URI_LIST.contains(valueOfTypeAttr)) {
                                parameters.put(ERROR,  "The URI of the attribute 'Type' for Nonce is an unknown value in the BinarySecret element.");
                                return parameters;
                            }
                        } else if (valueOfTypeAttr.endsWith(SoapConstants.WST_BINARY_SECRET_ASYMMETRIC_KEY_TYPE)) {
                            if (! SoapConstants.WST_BINARY_SECRET_ASYMMETRIC_KEY_TYPE_URI_LIST.contains(valueOfTypeAttr)) {
                                parameters.put(ERROR,  "The URI of the attribute 'Type' for AsymmetricKey is an unknown value in the BinarySecret element.");
                                return parameters;
                            }
                        } else if (valueOfTypeAttr.endsWith(SoapConstants.WST_BINARY_SECRET_SYMMETRIC_KEY_TYPE)) {
                            if (! SoapConstants.WST_BINARY_SECRET_SYMMETRIC_KEY_TYPE_URI_LIST.contains(valueOfTypeAttr)) {
                                parameters.put(ERROR,  "The URI of the attribute 'Type' for SymmetricKey is an unknown value in the BinarySecret element.");
                                return parameters;
                            }
                        }  else {
                            parameters.put(ERROR,  "The attribute 'Type' in the BinarySecret element is none of Nonce, AsymmetricKey, and SymmetricKey.");
                            return parameters;
                        }
                    }
                    parameters.put(BINARY_SECRET_ATTR_TYPE, valueOfTypeAttr);
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
                        parameters.put(ERROR,  "The key size (" + keySize + ") in the RequestSecurityToken element is invalid.");
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
                        parameters.put(ERROR,  "The ValueType attribute in the Reference element is an unknown value in the SOAP Body.");
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

    /**
     * Generate a SOAP fault response.
     *
     * @param context The context for the fault
     * @param parameters The parameters for the related RST
     * @param faultCodeOrValue The code (one of WST_FAULT_CODE_*)
     * @param faultStringOrReason The reason detail.
     */
    public static void generateSoapFaultResponse(
        final PolicyEnforcementContext context,
        final Map<String, String> parameters,
        final String rstrResponseVariable,
        final String faultCodeOrValue,
        final String faultStringOrReason) {

        // Set a SOAP fault
        final String wstNs = parameters.get( WST_NS ) == null ? SoapConstants.WST_NAMESPACE3 : parameters.get( WST_NS );
        final String soapVersion = RstSoapMessageProcessor.getSoapVersion(context, parameters);

        String requestUrl;
        try {
            requestUrl = (String) context.getVariable("request.url");
        } catch (NoSuchVariableException e) {
            requestUrl = "";
        }

        String faultResponse = ("1.2".equals(soapVersion))?
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE + "\" " +
                "                  xmlns:wst=\"" + wstNs + "\">\n" +
                "    <soapenv:Body>\n" +
                "        <soapenv:Fault>\n" +
                "            <soapenv:Code>\n" +
                "                <soapenv:Value>soapenv:Sender</soapenv:Value>\n" +
                "                <soapenv:Subcode>\n" +
                "                    <soapenv:Value>" + faultCodeOrValue + "</soapenv:Value>\n" +
                "                </soapenv:Subcode>\n" +
                "            </soapenv:Code>\n" +
                "            <soapenv:Reason>\n" +
                "                <soapenv:Text xml:lang=\"en-US\">" + faultStringOrReason + "</soapenv:Text>\n" +
                "            </soapenv:Reason>\n" +
                "            <soapenv:Role>" + requestUrl + "</soapenv:Role>\n" +
                "        </soapenv:Fault>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>"
            :
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\" " +
                "                  xmlns:wst=\"" + wstNs + "\">\n" +
                "    <soapenv:Body>\n" +
                "        <soapenv:Fault>\n" +
                "            <faultcode>" + faultCodeOrValue + "</faultcode>\n" +
                "            <faultstring>" + faultStringOrReason + "</faultstring>\n" +
                "            <faultactor>" + requestUrl + "</faultactor>\n" +
                "        </soapenv:Fault>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";

        context.setVariable(rstrResponseVariable, new Message(XmlUtil.stringAsDocument(faultResponse)));
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

    /**
     * Get the WS-Addressing namespace to use.
     *
     * <p>The namespace could be detected from the RST message or could be
     * derived from the WS-Trust namespace.</p>
     *
     * <p>If a namespace cannot be determined then the default namespace is
     * returned.</p>
     *
     * @param parameters The parameters to use.
     * @return The WS-Addressing namespace.
     */
    public static String getWsaNamespace( final Map<String, String> parameters ) {
        return getNamespace( parameters, SoapConstants.WSA_NAMESPACE_10, WSA_NS, trustToAddressingNsMap );
    }

     /**
     * Get the WS-Secure Conversation namespace to use.
     *
     * <p>The namespace could be detected from the RST message or could be
     * derived from the WS-Trust namespace.</p>
     *
     * <p>If a namespace cannot be determined then the default namespace is
     * returned.</p>
     *
     * @param parameters The parameters to use.
     * @return The WS-Secure Conversation namespace.
     */
    public static String getWsscNamespace( final Map<String, String> parameters ) {
        return getNamespace( parameters, SoapConstants.WSSC_NAMESPACE2, WSC_NS, trustToWsscNsMap );
    }

    /**
     * Get the WS-Policy namespace to use.
     *
     * <p>The namespace could be detected from the RST message or could be
     * derived from the WS-Trust namespace.</p>
     *
     * <p>If a namespace cannot be determined then the default namespace is
     * returned.</p>
     *
     * @param parameters The parameters to use.
     * @return The WS-Policy namespace.
     */
    public static String getWspNamespace( final Map<String, String> parameters ) {
        return getNamespace( parameters, SoapConstants.WSP_NAMESPACE2, WSP_NS, trustToPolicyNsMap );
    }

    private static String getNamespace( final Map<String, String> parameters,
                                        final String defaultNamespace,
                                        final String namespaceParameter,
                                        final Map<String,String> namespaceMap ) {
        String namespace = parameters.get( namespaceParameter );

        if ( namespace == null ) {
            final String wsTrustNs = parameters.get( WST_NS );
            if ( wsTrustNs != null ) {
                namespace = namespaceMap.get( wsTrustNs );
            }
        }

        if ( namespace == null ) {
            namespace = defaultNamespace;
        }

        return namespace;
    }
}