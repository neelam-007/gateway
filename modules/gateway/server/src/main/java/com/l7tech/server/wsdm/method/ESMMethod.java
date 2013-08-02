package com.l7tech.server.wsdm.method;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.wsdm.faults.FaultMappableException;
import com.l7tech.server.wsdm.faults.InvalidWsAddressingHeaderFault;
import com.l7tech.server.wsdm.subscription.Subscription;
import com.l7tech.server.wsdm.Namespaces;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.message.Message;
import com.l7tech.message.HttpRequestKnob;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Entry point for resolving which method is being invoked
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 2, 2007<br/>
 */
public abstract class ESMMethod {
    private final Document reqestDoc;
    private final Message requestMessage;
    private final URL incomingUrl;
    private final Goid esmServiceGoid;
    private String messageId;
    private Element soapHeader;

    public static ESMMethod resolve(Message request, Goid esmServiceGoid) throws FaultMappableException, SAXException, IOException {

        ESMMethod method = GetMultipleResourceProperties.resolve(request, esmServiceGoid);
        if (method == null) method = Renew.resolve(request, esmServiceGoid);
        if (method == null) method = Subscribe.resolve(request, esmServiceGoid);
        if (method == null) method = Unsubscribe.resolve(request, esmServiceGoid);
        // if (method == null) method = GetResourceProperty.resolve(request);
        // if (method == null) method = GetManageabilityReferences.resolve(request);
        if (method != null)
            method.validateRequest();
        return method;
    }

    protected ESMMethod(Document reqestDoc, Message request, Goid esmServiceGoid) throws MalformedURLException {
        this.reqestDoc = reqestDoc;
        this.requestMessage = request;
        this.esmServiceGoid = esmServiceGoid;
        this.incomingUrl = extractIncomingUrl();
    }

    private URL extractIncomingUrl() throws MalformedURLException {
        HttpRequestKnob reqHttp = requestMessage.getHttpRequestKnob();
        StringBuffer origFullUrl = new StringBuffer(reqHttp.getRequestUrl());
        String qs = reqHttp.getQueryString();
        if (qs != null && qs.length() > 0) {
            origFullUrl.append("?").append(qs);
        }
        return new URL(origFullUrl.toString());
    }

    protected void validateRequest() throws FaultMappableException {
        Element soapHeader = getSoapHeader();
        NodeList wsaElements = soapHeader.getElementsByTagNameNS(Namespaces.WSA, "*");
        if (wsaElements == null || wsaElements.getLength() == 0)
            return; // no WSA in the document

        validateNoMoreThanOne(soapHeader, Namespaces.WSA, "To");
        validateNoMoreThanOne(soapHeader, Namespaces.WSA, "FaultTo");

        messageId = DomUtils.getTextValue(validateExactlyOneElement(soapHeader, Namespaces.WSA, "MessageID", null));
        // todo: should also check that the message id is a valid IRI

        validateReplyTo(soapHeader);
        validateAction(soapHeader);
    }

    /**
     * Validates the cardinality of WSA headers.
     *
     * @return the unique Element that was found
     * @throws InvalidWsAddressingHeaderFault if the cardinality of the specified element is other than 1
     */
    protected final Element validateExactlyOneElement(Element parent, String namespace, String localName, InvalidWsAddressingHeaderFault.FaultCode faultCode) 
        throws InvalidWsAddressingHeaderFault {
        try {
            return DomUtils.findExactlyOneChildElementByName(parent, namespace, localName);
        } catch (MissingRequiredElementException e) {
            throw new InvalidWsAddressingHeaderFault("No " + namespace + " : " + localName + " property found for " + parent.getPrefix() + ":" + parent.getLocalName(),
                faultCode != null ? faultCode : InvalidWsAddressingHeaderFault.FaultCode.INVALID_ADDRESSING_HEADER, parent);
        } catch (TooManyChildElementsException e) {
            throw new InvalidWsAddressingHeaderFault("More than one " + namespace + " : " + localName + " property found for " + parent.getPrefix() + ":" + parent.getLocalName(),
                InvalidWsAddressingHeaderFault.FaultCode.INVALID_CARDINALITY, DomUtils.findFirstChildElementByName(parent, namespace, localName));
        }
    }

    /**
     * Validates the cardinality of WSA headers.
     *
     * @return the first Element found, or null if a matching Element was not found
     * @throws InvalidWsAddressingHeaderFault if the cardinality of the specified element is greater than 1
     */
    protected final Element validateNoMoreThanOne(Element parent, String namespace, String localName) throws InvalidWsAddressingHeaderFault {
        try {
            return DomUtils.findOnlyOneChildElementByName(parent, namespace, localName);
        } catch (TooManyChildElementsException e) {
            throw new InvalidWsAddressingHeaderFault("More than one " + namespace + " : " + localName + " property found for " + parent.getPrefix() + ":" + parent.getLocalName(),
                InvalidWsAddressingHeaderFault.FaultCode.INVALID_CARDINALITY, DomUtils.findFirstChildElementByName(parent, namespace, localName));
        }
    }

    private void validateReplyTo(Element soapHeader) throws InvalidWsAddressingHeaderFault {
        // ReplyTo address checks
        Element replyTo = validateNoMoreThanOne(soapHeader, Namespaces.WSA, "ReplyTo");
        if (replyTo != null) {
            Element address = validateExactlyOneElement(replyTo, Namespaces.WSA, "Address", InvalidWsAddressingHeaderFault.FaultCode.MISSING_ADDRESS_IN_EPR);
            String replyToAddress = DomUtils.getTextValue(address);
            if (!SoapConstants.WSA_ANONYMOUS_ADDRESS.equals(replyToAddress))
                throw new InvalidWsAddressingHeaderFault("Unsupported wsa:ReplyTo endpoint reference address " + replyToAddress,
                    InvalidWsAddressingHeaderFault.FaultCode.ONLY_ANNONYMOUS_ADDRESS_SUPPORTED, address);
        }
    }

    private void validateAction(Element soapHeader) throws InvalidWsAddressingHeaderFault {
        // WSA action checks
        String actionErrorMsg = null;

        Element wsaAction = validateExactlyOneElement(soapHeader, Namespaces.WSA, "Action", null);
        try {
            String soapAction = null;
            if (SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE.equals(soapHeader.getNamespaceURI())) {
                String actionHttpHeader = requestMessage.getHttpRequestKnob().getHeaderSingleValue(SoapConstants.SOAPACTION);
                if (actionHttpHeader == null) {
                    actionErrorMsg = "No HTTP SOAPAction header found, required when using WS-Addressing with SOAP 1.1.";
                } else if (actionHttpHeader.length() < 2 || !actionHttpHeader.startsWith("\"") || !actionHttpHeader.endsWith("\"")) {
                    actionErrorMsg = "HTTP SOAPAction header must be enclosed in double quotes when used with WS-Addressing, found: " + actionHttpHeader;
                } else
                if ("\"\"".equals(actionHttpHeader)) {
                    return; // "" is always valid, regardless of the WSA property value
                } else {
                    soapAction = actionHttpHeader.substring(1, actionHttpHeader.length() - 1);
                }
            } else {
                String contentType = requestMessage.getHttpRequestKnob().getHeaderSingleValue(HttpConstants.HEADER_CONTENT_TYPE);
                Pattern SOAP_1_2_ACTION_PATTERN = Pattern.compile(";\\s*action=([^;]+)(?:;|$)");
                if(contentType != null) {
                    Matcher m = SOAP_1_2_ACTION_PATTERN.matcher(contentType);
                    if(m.find()) {
                        soapAction = m.group(1);
                    }
                }
            }

            // if we have a soapAction value from the HTTP headers, validate against the WSA value
            String wsaActionValue = DomUtils.getTextValue(wsaAction);
            if ( soapAction != null && ! soapAction.equals(wsaActionValue)) {
                actionErrorMsg = "HTTP SOAPAction header does not match the WS-Addressing Action property: " + soapAction + " : " + wsaActionValue;
            }

        } catch (IOException e) {
            actionErrorMsg = "More than one SOAPAction HTTP header found in the request";
        }

        if (actionErrorMsg != null)
            throw new InvalidWsAddressingHeaderFault(actionErrorMsg, InvalidWsAddressingHeaderFault.FaultCode.ACTION_MISMATCH, wsaAction);
    }

    protected Element getSoapHeader() throws InvalidWsAddressingHeaderFault {
        try {
            if (soapHeader == null)
                soapHeader = SoapUtil.getHeaderElement(reqestDoc); 
        } catch (InvalidDocumentFormatException e) {
            throw new InvalidWsAddressingHeaderFault("Request is not SOAP or has more than one SOAP Header", reqestDoc.getDocumentElement());
        }
        if (soapHeader == null) {
            throw new InvalidWsAddressingHeaderFault("Soap header not found", reqestDoc.getDocumentElement());
        }
        return soapHeader;
    }

    static Element getFirstBodyChild(Document doc) throws InvalidDocumentFormatException {
        Element bodyel = SoapUtil.getBodyElement(doc);
        if (bodyel == null) return null;
        return XmlUtil.findFirstChildElement(bodyel);
    }

    /**
     * Access the SubscriptionId if present as a WS-Addressing reference parameter.
     *
     * <p>This SubscriptionId must be part of the SOAP Header (when present).</p>
     *
     * <pre>
     *     &lt;l7sub:SubscriptionId wsa:IsReferenceParameter="true" xmlns:l7sub="http://www.layer7tech.com/ns/wsdm/subscription">
     *         54ca0956-2553-41ee-abb1-3215c2ccc30c
     *     &lt;/l7sub:SubscriptionId>
     * </pre>
     *
     * @param doc The SOAP message DOM
     * @return The subscriptionId or null
     * @throws InvalidDocumentFormatException If the message is not SOAP
     */
    static String getSubscriptionIdAddressingParameter(final Document doc) throws InvalidDocumentFormatException {
        String subscriptionIdValue = null;

        Element headerel = SoapUtil.getHeaderElement(doc);
        if ( headerel != null ) {
            // find the subscription id
            List<Element> elements = XmlUtil.findChildElementsByName( headerel, Subscription.NS, "SubscriptionId" );
            for ( Element subidel : elements ) {
                if ( "true".equals(subidel.getAttributeNS(Namespaces.WSA, "IsReferenceParameter")) ) {
                    String maybetheid = XmlUtil.getTextValue(subidel);
                    if ( maybetheid != null && maybetheid.length() > 0 ) {
                        subscriptionIdValue = maybetheid;
                        break;
                    }
                }
            }
        }

        return subscriptionIdValue;
    }

    static boolean testElementLocalName(Element el, String localname) {
        return el.getLocalName() != null && el.getLocalName().equals(localname);
    }

    public Document getReqestDoc() {
        return reqestDoc;
    }

    public String getMessageId() {
        return messageId;
    }

    public Goid getEsmServiceGoid() {
        return esmServiceGoid;
    }

    public URL getIncomingUrl() {
        return incomingUrl;
    }
}
