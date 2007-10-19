package com.l7tech.common.uddi;

import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.MessageContext;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPException;

import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.ArrayUtils;
import com.l7tech.common.util.ExceptionUtils;

/**
 * SOAPHandler to repair in SOAP Fault messages with invalid faultcodes.
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
    
    public Set getHeaders() {
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
    public boolean handleMessage(final SOAPMessageContext context) {
        Boolean outboundProperty = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if ( outboundProperty!=null && !outboundProperty.booleanValue() ) {
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
    public boolean handleFault(final SOAPMessageContext context) {
        Boolean outboundProperty = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if ( outboundProperty!=null && !outboundProperty.booleanValue() ) {
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
    public void close(final MessageContext context) {
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(FaultRepairSOAPHandler.class.getName());
    private static final String NAMESPACE_SOAP_1_1 = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String[] FAULTCODES = {"server", "client", "versionmismatch", "mustunderstand"}; // lowercased

    /**
     * Check if the given soap body looks like a SOAP Fault
     */
    private boolean isFault(final Element body) {
        boolean fault = false;

        NodeList list = body.getElementsByTagNameNS(NAMESPACE_SOAP_1_1, "Fault");
        if ( list.getLength() == 1 ) {
            fault = true;
        }

        return fault;
    }

    /**
     * Check fault for invalid data and repair if possible
     */
    private void repairIfRequired(final Element body) {
        Element faultCodeElement = findFaultCode(body);
        if ( !isValidFaultCodeValue( faultCodeElement ) ) {
            repairFaultCodeValue(faultCodeElement);    
        }
    }

    private Element findFaultCode(final Element body) {
        Element faultCodeElement = null;

        // find faultcode
        NodeList list = body.getElementsByTagName("faultcode");
        if ( list.getLength() == 1 ) {
            faultCodeElement = (Element) list.item(0);    
        }

        return faultCodeElement;
    }

    private boolean isSpecDefinedFaultCode(final String value) {
        return ArrayUtils.contains(FAULTCODES, value.toLowerCase());
    }

    private boolean isValidFaultCodeValue(final Element faultCodeElement) {
        boolean valid = true;

        if (faultCodeElement != null) {
            String value = XmlUtil.getTextValue(faultCodeElement);

            // Note that since faultcode has no prefix (since it is in the default namespace)
            // the faultcode QName value must have a prefix if it is one of the predefined values
            if (value == null || value.trim().length()==0 || (value.indexOf(':') < 0 && isSpecDefinedFaultCode(value))) {
                valid = false;
            }
        }

        return valid;
    }

    private void repairFaultCodeValue(final Element faultCodeElement) {
        // Always add the namespce decl to the faultcode node
        // in JDK 1.6 SAAJ there seems to be a problem with the
        // StAX parsers namespace handling
        faultCodeElement.setAttributeNS(XmlUtil.XMLNS_NS, "xmlns:soap", NAMESPACE_SOAP_1_1);

        String value = XmlUtil.getTextValue(faultCodeElement);

        if ( value == null || value.trim().length()==0 ) {
            // Then we'll assume it is a server error
            XmlUtil.setTextContent(faultCodeElement, "soap:Server");
        } else {
            XmlUtil.setTextContent(faultCodeElement, "soap:" + value);
        }
    }
}