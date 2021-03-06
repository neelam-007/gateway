package com.l7tech.uddi;

import com.l7tech.util.ArrayUtils;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import org.w3c.dom.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SOAPHandler to repair in SOAP Fault messages with invalid faults.
 *
 * <p>This will fix issues with unprefixed faultcodes and faults with details
 * that are not in the default namespace (faultcode, faultstring, faultactor,
 * detail)</p>
 *
 * <p>Many UDDI implementations send soap faultcodes with QNames that SAAJ does
 * not process correctly (e.g. "Server" instead of "soap:Server")</p>
 *
 * <p>This class will repair these fault codes to prevent errors during error
 * handling.</p>
 *
 * <p>This handler is not UDDI specific at all, if generally useful it should be
 * moved elsewhere and made public.</p>
 *
 * @author Steve Jones
 */
class FaultRepairSOAPHandler implements SOAPHandler<SOAPMessageContext>
{
    //- PUBLIC
    
    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    /**
     * If a message looks like a fault "soapenv:Fault" this calls handleFault
     *
     * <p>This behaviour allows for SOAP Faults in responses with a 200 status
     * code.</p>
     *
     * @param context the SOAP message context
     * @return true
     */
    @Override
    public boolean handleMessage(final SOAPMessageContext context) {
        Boolean outboundProperty = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if ( outboundProperty!=null && !outboundProperty ) {
            SOAPMessage soapMessage = context.getMessage();

            if ( soapMessage != null ) {
                try {
                    SOAPBody body = soapMessage.getSOAPBody();
                    if ( isFault(body) ) {
                        handleFault(context);
                    }
                } catch (SOAPException se) {
                    logger.log(Level.INFO,
                            "Error processing SOAP message when checking Fault: " + ExceptionUtils.getMessage(se),
                            ExceptionUtils.getDebugException(se));
                }
            }
        }

        return true;
    }    

    /**
     * If an (inbound) fault has an incorrect faultcode then the message is repaired.
     *
     * @param context The soap message context to process
     * @return true
     */
    @Override
    public boolean handleFault(final SOAPMessageContext context) {
        Boolean outboundProperty = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if ( outboundProperty!=null && !outboundProperty ) {
            SOAPMessage soapMessage = context.getMessage();

            if ( soapMessage != null ) {
                try {
                    SOAPBody body = soapMessage.getSOAPBody();
                    repairIfRequired(body);
                } catch (SOAPException se) {
                    logger.log(Level.INFO,
                            "Error processing SOAP message when checking Fault: " + ExceptionUtils.getMessage(se),
                            ExceptionUtils.getDebugException(se));
                }
            }
        }

        return true;
    }

    /**
     * Does nothing
     *
     * @param context ignored
     */
    @Override
    public void close(final MessageContext context) {
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(FaultRepairSOAPHandler.class.getName());
    private static final String NAMESPACE_SOAP_1_1 = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String[] FAULTCODES = {"server", "client", "versionmismatch", "mustunderstand"}; // lowercased
    private static final String[] FAULT_SUBELEMENTS = {"faultcode", "faultstring", "faultactor", "detail"};

    /**
     * Check if the given soap body looks like a SOAP Fault
     */
    private boolean isFault(final Element body) {
        boolean fault = false;

        List<Element> list = DomUtils.findChildElementsByName(body, NAMESPACE_SOAP_1_1, "Fault");
        if ( list.size() == 1 ) {
            fault = true;
        }

        return fault;
    }

    /**
     * Check fault for invalid data and repair if possible
     */
    private void repairIfRequired(final Element body) {
        Element faultElement = findFault(body);

        if ( !isValidFaultSubelements( faultElement )) {
            repairFaultCodeSubelements(faultElement);
        }

        Element faultCodeElement = findFaultcode(faultElement);
        if ( !isValidFaultCodeValue( faultCodeElement ) ) {
            repairFaultCodeValue(faultCodeElement);    
        }
    }

    private Element findFault(final Element body) {
        Element faultElement = null;

        List<Element> faultElementList = DomUtils.findChildElementsByName(body, NAMESPACE_SOAP_1_1, "Fault");
        if ( faultElementList.size() == 1 ) {
            faultElement = faultElementList.get(0);
        }

        return faultElement;
    }

    private Element findFaultcode(final Element faultElement) {
        Element faultcodeElement = null;

        // find faultcode
        if ( faultElement != null ) {
            List<Element> faultcodeElementList = DomUtils.findChildElementsByName(faultElement, (String)null, "faultcode");
            if ( faultcodeElementList.size() == 1 ) {
                faultcodeElement = faultcodeElementList.get(0);
            }
        }

        return faultcodeElement;
    }

    private boolean isSpecDefinedFaultCode(final String value) {
        return ArrayUtils.contains(FAULTCODES, value.toLowerCase());
    }

    private boolean isValidFaultCodeValue(final Element faultCodeElement) {
        boolean valid = true;

        if (faultCodeElement != null) {
            String value = DomUtils.getTextValue(faultCodeElement);

            // Note that since faultcode has no prefix (since it is in the default namespace)
            // the faultcode QName value must have a prefix if it is one of the predefined values
            if (value == null || value.trim().length()==0 || (value.indexOf(':') < 0 && isSpecDefinedFaultCode(value))) {
                valid = false;
            }
        }

        return valid;
    }

    private void repairFaultCodeValue(final Element faultCodeElement) {
        String prefix = DomUtils.getOrCreatePrefixForNamespace(faultCodeElement, NAMESPACE_SOAP_1_1, "soap");
        String value = DomUtils.getTextValue(faultCodeElement);

        if ( value == null || value.trim().length()==0 ) {
            // Then we'll assume it is a server error
            DomUtils.setTextContent(faultCodeElement, prefix + ":Server");
        } else {
            DomUtils.setTextContent(faultCodeElement, prefix + ":" + value);
        }
    }

    private boolean isValidFaultSubelements(final Element faultElement) {
        boolean valid = true;

        if (faultElement != null) {
            NodeList children = faultElement.getChildNodes();

            for (int n=0; n<children.getLength(); n++) {
                Node node = children.item(n);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element subelement = (Element) node;

                    // check if subelement is not in the default namespace
                    if ( ArrayUtils.contains(FAULT_SUBELEMENTS, subelement.getLocalName()) &&
                         subelement.getNamespaceURI() != null ) {
                        valid = false;
                    }
                }
            }
        }

        return valid;
    }

    private void repairFaultCodeSubelements(final Element faultElement) {
        NodeList children = faultElement.getChildNodes();

        for (int n=0; n<children.getLength(); n++) {
            Node node = children.item(n);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element subelement = (Element) node;

                if ( ArrayUtils.contains(FAULT_SUBELEMENTS, subelement.getLocalName()) &&
                     subelement.getNamespaceURI() != null ) {
                    // replace the element with one in the default namespace
                    Element replacement = subelement.getOwnerDocument().createElementNS(null, subelement.getLocalName());
                    faultElement.replaceChild(replacement, subelement);

                    // ensure empty default NS
                    replacement.removeAttribute(XMLConstants.XMLNS_ATTRIBUTE);
                    replacement.setAttributeNS(DomUtils.XMLNS_NS, XMLConstants.XMLNS_ATTRIBUTE, "");

                    // copy attributes
                    NamedNodeMap attributeMap = subelement.getAttributes();
                    for (int i=0; i<attributeMap.getLength(); i++) {
                        Attr attr = (Attr) attributeMap.item(i);
                        if (!XMLConstants.XMLNS_ATTRIBUTE.equals(attr.getLocalName())) {
                            replacement.setAttributeNode(attr);
                        }
                    }

                    // copy children
                    NodeList subElementChildren = subelement.getChildNodes();
                    for (int i=0; i<subElementChildren.getLength(); i++) {
                        Node subNode = subElementChildren.item(i);
                        replacement.appendChild(subNode);
                    }
                }
            }
        }
    }
}