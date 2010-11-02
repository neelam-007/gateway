package com.l7tech.server.util;

import com.l7tech.message.Message;
import com.l7tech.message.XmlKnob;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Process an inbound RST SOAP message and log/report errors.
 *
 * @author ghuang
 */
public class RstSoapMessageProcessor {
    private static final Logger logger = Logger.getLogger(RstSoapMessageProcessor.class.getName());

    public final static String SOAP_ENVELOPE_NS = "soap_envelope_namespace";
    public final static String WSSE_NS = "ws-security_namespace";
    public final static String WSC_NS = "ws_secure_conversation_namespace";
    public final static String WSU_NS = "ws_utilities_namespace";
    public final static String WST_NS = "ws_trust_namespace";
    public final static String WSA_NS = "ws_addressing_namespace";
    public final static String WSP_NS = "ws_policy_namespace";

    public final static String HAS_WS_ADDRESSING_ACTION = "has_ws_addressing_action";
    public final static String WS_ADDRESSING_ACTION = "ws_addressing_action";
    public final static String HAS_REQUEST_SECURITY_TOKEN = "has_request_security_token_element";
    public final static String TOKEN_TYPE = "rst_token_type";
    public final static String HAS_REQUEST_TYPE = "has_rst_request_type";
    public final static String REQUEST_TYPE = "rst_request_type";
    public final static String HAS_CANCEL_TARGET = "has_cancel_target";
    public final static String HAS_SECURITY_TOKEN_REFERENCE = "has_security_token_reference";
    public final static String HAS_REFERENCE = "has_reference";
    public final static String REFERENCE_ATTR_URI = "reference_attribute_uri";
    public final static String REFERENCE_ATTR_VALUE_TYPE = "reference_attribute_value_type";
    public final static String HAS_ENTROPY = "has_entropy_element";
    public final static String HAS_BINARY_SECRET = "has_binary_secret_element";
    public final static String BINARY_SECRET_ATTR_TYPE = "binary_secret_type";
    public final static String BINARY_SECRET = "binary_secret";
    public final static String KEY_SIZE = "key_size";
    public final static String ERROR = "parsing_error";

    public static Map<String, String> getRstParameters(final Message message) {
        Map<String, String> parameters = new HashMap<String, String>();

        Document doc;
        try {
            XmlKnob knob = message.getXmlKnob();
            doc = knob.getDocumentReadOnly();
        } catch (SAXException e) {
            reportAndLogError("The RST message content is not well-formatted.", e, parameters);
            return parameters;
        } catch (IOException e) {
            reportAndLogError("Cannot get the RST message document", e, parameters);
            return parameters;
        }

        // Find all namespaces
        Collection<String> allNamespaces;
        try {
            allNamespaces = DomUtils.findAllNamespaces(SoapUtil.getEnvelopeElement(doc)).values();
        } catch (MessageNotSoapException e) {
            reportAndLogError("The RST message is not a SOAP message", e, parameters);
            return parameters;
        }

        // Get the namespace of SOAP Envelope
        for (String namespace: SoapConstants.ENVELOPE_URIS) {
            if (allNamespaces.contains(namespace)) {
                parameters.put(SOAP_ENVELOPE_NS, namespace);
                break;
            }
        }
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
        for (String namespace: SoapConstants.WST_NAMESPACE_ARRAY) {
            if (allNamespaces.contains(namespace)) {
                parameters.put(WST_NS, namespace);
                break;
            }
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

        // Find header
        Element header;
        try {
            header = SoapUtil.getHeaderElement(doc);
        } catch (InvalidDocumentFormatException e) {
            reportAndLogError("The RST message is not SOAP or has more than one header.", e, parameters);
            return parameters;
        }
        if (header == null) {
            reportAndLogError("The RST SOAP message has no header.", null, parameters);
            return parameters;
        }

        // Find body
        Element body;
        try {
            body = SoapUtil.getBodyElement(doc);
        } catch (InvalidDocumentFormatException e) {
            reportAndLogError("The RST message is not SOAP or has more than one body.", e, parameters);
            return parameters;
        }
        if (body == null) {
            reportAndLogError("The RST SOAP message has no body.", null, parameters);
            return parameters;
        }

        // Find WS-Addressing Action
        Element actionEl;
        try {
            actionEl = DomUtils.findExactlyOneChildElementByName(header, parameters.get(WSA_NS), SoapConstants.WSA_ACTION);
        } catch (InvalidDocumentFormatException e) {
            reportAndLogError("The wsa:Action element is not well-formatted.", e, parameters);
            return parameters;
        }
        if (actionEl != null) {
            parameters.put(HAS_WS_ADDRESSING_ACTION, "true"); // "true" means there exists a ws-addressing action element.
            parameters.put(WS_ADDRESSING_ACTION, DomUtils.getTextValue(actionEl));
        } else {
            parameters.put(HAS_WS_ADDRESSING_ACTION, "false");
        }

        // Find RequestSecurityToken
        Element rstEl;
        try {
            rstEl = DomUtils.findExactlyOneChildElementByName(body, parameters.get(WST_NS), SoapConstants.WST_REQUESTSECURITYTOKEN);
        } catch (InvalidDocumentFormatException e) {
            reportAndLogError("The RequestSecurityToken element is not well-formatted.", e, parameters);
            return parameters;
        }

        if (rstEl != null) {
            parameters.put(HAS_REQUEST_SECURITY_TOKEN, "true");  // "true" means there exits a RequestSecurityToken element.

            String elementName = null; // Just keep tracking the name of the element being processed.
            try {
                // Find TokenType (Optional)
                elementName = SoapConstants.WST_TOKENTYPE;
                Element tokenTypeEl = DomUtils.findOnlyOneChildElementByName(rstEl, parameters.get(WST_NS), SoapConstants.WST_TOKENTYPE);
                if (tokenTypeEl != null) {
                    parameters.put(TOKEN_TYPE, DomUtils.getTextValue(tokenTypeEl));
                }

                // Find RequestType (Required)
                elementName = SoapConstants.WST_REQUESTTYPE;
                Element requestTypeEl = DomUtils.findExactlyOneChildElementByName(rstEl, parameters.get(WST_NS), SoapConstants.WST_REQUESTTYPE);
                if (requestTypeEl != null) {
                    parameters.put(HAS_REQUEST_TYPE, "true"); // "true" means there is a RequestType element.
                    parameters.put(REQUEST_TYPE, DomUtils.getTextValue(requestTypeEl));
                } else {
                    parameters.put(HAS_REQUEST_TYPE, "false");
                }

                // Find CancelTarget (Optional)
                elementName = SoapConstants.WST_CANCELTARGET;
                Element cancelTargetEl = DomUtils.findOnlyOneChildElementByName(rstEl, parameters.get(WST_NS), SoapConstants.WST_CANCELTARGET);
                if (cancelTargetEl != null) {
                    parameters.put(HAS_CANCEL_TARGET, "true"); // "true" means there exists a CancelTarget element.

                    // Find SecurityTokenReference
                    elementName = SoapConstants.WSSE_SECURITY_TOKEN_REFERENCE;
                    Element strEl = DomUtils.findOnlyOneChildElementByName(cancelTargetEl, parameters.get(WSSE_NS), SoapConstants.WSSE_SECURITY_TOKEN_REFERENCE);
                    if (strEl != null) {
                        parameters.put(HAS_SECURITY_TOKEN_REFERENCE, "true"); // "true" means there exists a SecurityTokenReference element.

                        elementName = SoapConstants.WSSE_REFERENCE;
                        Element refEl = DomUtils.findOnlyOneChildElementByName(strEl, parameters.get(WSSE_NS), SoapConstants.WSSE_REFERENCE);
                        if (refEl != null) {
                            parameters.put(HAS_REFERENCE, "true"); // "true" means there exists a Reference element.
                            parameters.put(REFERENCE_ATTR_URI, refEl.getAttribute(SoapUtil.WSSE_REFERENCE_ATTR_URI));
                            parameters.put(REFERENCE_ATTR_VALUE_TYPE, refEl.getAttribute(SoapUtil.WSSE_REFERENCE_ATTR_VALUE_TYPE));
                        } else {
                            parameters.put(HAS_REFERENCE, "false");
                        }
                    } else {
                        parameters.put(HAS_SECURITY_TOKEN_REFERENCE, "false");
                    }
                } else {
                    parameters.put(HAS_CANCEL_TARGET, "false");
                }

                // Find Entropy
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

                // Find KeySize
                elementName = SoapConstants.KEY_SIZE;
                Element keySizeEl = DomUtils.findOnlyOneChildElementByName(rstEl, parameters.get(WST_NS), SoapConstants.KEY_SIZE);
                if (keySizeEl != null) {
                    parameters.put(KEY_SIZE, DomUtils.getTextValue(keySizeEl));
                }
            } catch (InvalidDocumentFormatException e) {
                reportAndLogError("The " + elementName + " element is not well-formatted in the SOAP message.", e, parameters);
                return parameters;
            }
        } else {
            parameters.put(HAS_REQUEST_SECURITY_TOKEN, "false");
        }

        return parameters;
    }

    private static void reportAndLogError(String errorMessage, Throwable throwable, Map<String, String> parameters) {
        logger.log(Level.WARNING, errorMessage, throwable);
        parameters.put(ERROR, errorMessage);
    }

    public static void setAndLogSoapFault(final PolicyEnforcementContext context, String faultCodeOrValue, String faultStringOrReason) {
        SoapFaultLevel fault = new SoapFaultLevel();
        fault.setLevel(SoapFaultLevel.TEMPLATE_FAULT);

        if(context.getService() != null && context.getService().getSoapVersion() == SoapVersion.SOAP_1_2) {
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
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
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
        logger.warning(faultStringOrReason);
    }
}