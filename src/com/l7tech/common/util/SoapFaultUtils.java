package com.l7tech.common.util;

import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.MissingRequiredElementException;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.common.xml.SoapFaultDetailImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Util stuff to generate soap faults.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 16, 2004<br/>
 * $Id$
 */
public class SoapFaultUtils {
    private static final String FAULTCODE = "faultcode";
    private static final String FAULTSTRING = "faultstring";
    private static final String FAULTACTOR = "faultactor";
    private static final String FAULTDETAIL = "faultdetail";

    public static Document generateSoapFaultDocument(SoapFaultDetail soapFaultDetail, String faultActor) throws IOException, SAXException {
        return generateSoapFaultDocument(soapFaultDetail.getFaultCode(),
                                 soapFaultDetail.getFaultString(),
                                 soapFaultDetail.getFaultDetail(),
                                 faultActor);
    }

    public static Element makeFaultDetailsSubElement(String elName, String subTextNode) throws IOException, SAXException {
        Document tmp = XmlUtil.stringToDocument("<" + elName + "/>");
        if (subTextNode != null) {
            tmp.getDocumentElement().appendChild(XmlUtil.createTextNode(tmp, subTextNode));
        }
        return tmp.getDocumentElement();
    }

    public static Document generateSoapFaultDocument(String faultCode,
                                             String faultString,
                                             Element faultDetails,
                                             String faultActor) throws IOException, SAXException {
        Document tmpDoc = null;
        try {
            tmpDoc = XmlUtil.stringToDocument("<soapenv:Envelope" +
                                              " xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"" +
                                              " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"" +
                                              " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                                              " <soapenv:Body>\n" +
                                              "  <soapenv:Fault>\n" +
                                              "   <faultcode />" +
                                              "   <faultstring />" +
                                              "   <faultactor />" +
                                              "   <detail />" +
                                              "  </soapenv:Fault>\n" +
                                              " </soapenv:Body>\n" +
                                              "</soapenv:Envelope>");
            Element bodyEl = SoapUtil.getBodyElement(tmpDoc);

            Element faultcodeEl = getChild(bodyEl, "faultcode");
            Element faultstringEl = getChild(bodyEl, "faultstring");
            Element faultactorEl = getChild(bodyEl, "faultactor");
            Element faultdetailEl = getChild(bodyEl, "detail");
            faultcodeEl.appendChild(XmlUtil.createTextNode(tmpDoc, faultCode));
            if (faultString != null) {
                faultstringEl.appendChild(XmlUtil.createTextNode(tmpDoc, faultString));
            }
            faultactorEl.appendChild(XmlUtil.createTextNode(tmpDoc, faultActor));
            if (faultDetails != null) {
                Node fdc = tmpDoc.importNode(faultDetails, true);
                faultdetailEl.appendChild(fdc);
            }
        } catch (InvalidDocumentFormatException e) {
            throw new RuntimeException(e); // this should never happen
        }

        return tmpDoc;
    }

    /**
     * Check if the specified SOAP envelope is a SOAP fault and, if it is, return the SoapFaultDetail information.
     *
     * @param soapFault  the Document to examine, which is assumed to be a SOAP envelope.  Must not be null.
     * @return the SoapFaultDetail if this was a fault, or null if it wasn't.
     * @throws InvalidDocumentFormatException if the message is not SOAP or there is more than one Body
     * @throws MissingRequiredElementException if the message looks like a SOAP fault but has a missing or empty faultcode
     */
    public static SoapFaultDetail gatherSoapFaultDetail(Document soapFault) throws InvalidDocumentFormatException {
        final String faultcode;
        String faultstring = "";
        String faultactor = null;
        Element faultdetail = null;
        Element payload = SoapUtil.getPayloadElement(soapFault);
        if (payload != null && "Fault".equals(payload.getLocalName()) &&
            payload.getNamespaceURI().equals(soapFault.getDocumentElement().getNamespaceURI()))
        {
            Element faultcodeEl = XmlUtil.findFirstChildElementByName(payload, (String)null, FAULTCODE);
            if (faultcodeEl == null)
                throw new MissingRequiredElementException("SOAP fault did not have a faultcode element");
            faultcode = XmlUtil.getTextValue(faultcodeEl);
            if (faultcode == null || faultcode.length() < 1)
                throw new MissingRequiredElementException("SOAP fault had an empty faultcode element");
            Element faultstringEl = XmlUtil.findFirstChildElementByName(payload, (String)null, FAULTSTRING);
            if (faultstringEl != null)
                faultstring = XmlUtil.getTextValue(faultstringEl);
            Element faultactorEl = XmlUtil.findFirstChildElementByName(payload, (String)null, FAULTACTOR);
            if (faultactorEl != null)
                faultactor = XmlUtil.getTextValue(faultactorEl);
            faultdetail = XmlUtil.findFirstChildElementByName(payload, (String)null, FAULTDETAIL);
            return new SoapFaultDetailImpl(faultcode, faultstring, faultdetail, faultactor);
        }

        return null;
    }

    public static String generateSoapFaultXml(String faultCode,
                                              String faultString,
                                              Element faultDetails,
                                              String faultActor) throws IOException, SAXException {

        Document doc = generateSoapFaultDocument(faultCode, faultString, faultDetails, faultActor);
        return XmlUtil.nodeToFormattedString(doc);
    }

    public static Element getChild(Element parent, String childName) {
        NodeList children = parent.getElementsByTagName(childName);
        return (Element) children.item(0);
    }

    public static final String FC_CLIENT = "Client";
    public static final String FC_SERVER = "Server";
}
