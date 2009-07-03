package com.l7tech.server.wsdm.method;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.server.wsdm.faults.FaultMappableException;
import com.l7tech.server.wsdm.faults.InvalidWsAddressingHeaderFault;
import com.l7tech.server.wsdm.subscription.Subscription;
import com.l7tech.server.wsdm.Namespaces;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.message.Message;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
import java.util.List;
import java.io.IOException;

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
    private String messageId;
    private Element soapHeader;

    private static final String WSA_ANONYMOUS_ADDRESS = "http://www.w3.org/2005/08/addressing/anonymous";

    public static ESMMethod resolve(Message request) throws FaultMappableException, SAXException, IOException {

        ESMMethod method = GetMultipleResourceProperties.resolve(request);
        if (method == null) method = Renew.resolve(request);
        if (method == null) method = Subscribe.resolve(request);
        if (method == null) method = Unsubscribe.resolve(request);
        // if (method == null) method = GetnResourceProperty.resolve(request);
        // if (method == null) method = GetManageabilityReferences.resolve(request);
        if (method != null)
            method.validateRequest();
        return method;
    }

    protected ESMMethod(Document reqestDoc, Message request) {
        this.reqestDoc = reqestDoc;
        this.requestMessage = request;
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

        // ReplyTo address checks
        Element replyTo = validateNoMoreThanOne(soapHeader, Namespaces.WSA, "ReplyTo");
        if (replyTo != null) {
            Element address = validateExactlyOneElement(replyTo, Namespaces.WSA, "Address", InvalidWsAddressingHeaderFault.FaultCode.MISSING_ADDRESS_IN_EPR);
            String replyToAddress = DomUtils.getTextValue(address);
            if (!WSA_ANONYMOUS_ADDRESS.equals(replyToAddress))
                throw new InvalidWsAddressingHeaderFault("Unsupported wsa:ReplyTo endpoint reference address " + replyToAddress,
                    InvalidWsAddressingHeaderFault.FaultCode.ONLY_ANNONYMOUS_ADDRESS_SUPPORTED, address);
        }

        // WSA action checks
        Element wsaAction = validateExactlyOneElement(soapHeader, Namespaces.WSA, "Action", null);
        try {
            if (!DomUtils.getTextValue(wsaAction).equals(requestMessage.getHttpRequestKnob().getHeaderSingleValue(SoapConstants.SOAPACTION)))
                throw new InvalidWsAddressingHeaderFault("wsa:Action does not match the HTTP request SOAPAction header",
                    InvalidWsAddressingHeaderFault.FaultCode.ACTION_MISMATCH, wsaAction);
        } catch (IOException e) {
            throw new InvalidWsAddressingHeaderFault("More than one SOAPAction HTTP header found in the request", InvalidWsAddressingHeaderFault.FaultCode.ACTION_MISMATCH, wsaAction);
        }
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

    protected Element getSoapHeader() throws InvalidWsAddressingHeaderFault {
        try {
            if (soapHeader == null)
                soapHeader = DomUtils.findExactlyOneChildElementByName(reqestDoc.getDocumentElement(), SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, SoapConstants.HEADER_EL_NAME);
        } catch (InvalidDocumentFormatException e) {
            throw new InvalidWsAddressingHeaderFault("Soap header not found", reqestDoc.getDocumentElement()); // unlikely
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
}
