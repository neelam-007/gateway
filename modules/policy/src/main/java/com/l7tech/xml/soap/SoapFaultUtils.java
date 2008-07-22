package com.l7tech.xml.soap;

import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.MissingRequiredElementException;
import com.l7tech.xml.SoapFaultDetail;
import com.l7tech.xml.SoapFaultDetailImpl;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.DomUtils;
import com.l7tech.util.SoapConstants;
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
            tmp.getDocumentElement().appendChild(DomUtils.createTextNode(tmp, subTextNode));
        }
        return tmp.getDocumentElement();
    }

    public static String badContextTokenFault(String actor) {
        String output = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                            "  <soapenv:Body>\n" +
                            "    <soapenv:Fault xmlns:wsc=\"http://schemas.xmlsoap.org/ws/2005/02/sc\">\n" +
                            "      <faultcode>wsc:BadContextToken</faultcode>\n" +
                            "      <faultactor>@@actor@@</faultactor>\n" +
                            "    </soapenv:Fault>\n" +
                            "  </soapenv:Body>\n" +
                            "</soapenv:Envelope>";
        output = output.replace("@@actor@@", actor);
        return output;
    }

    public static String badKeyInfoFault(String actor) {
        String output = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                            "  <soapenv:Body>\n" +
                            "    <soapenv:Fault xmlns:wsse=\"" + SoapConstants.SECURITY_NAMESPACE + "\">\n" +
                            "      <faultcode>wsse:SecurityTokenUnavailable</faultcode>\n" +
                            "      <faultactor>@@actor@@</faultactor>\n" +
                            "    </soapenv:Fault>\n" +
                            "  </soapenv:Body>\n" +
                            "</soapenv:Envelope>";
        output = output.replace("@@actor@@", actor);
        return output;
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
                                              " xmlns:wsse=\"" + SoapConstants.SECURITY_NAMESPACE + "\"" +
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
            faultcodeEl.appendChild(DomUtils.createTextNode(tmpDoc, faultCode));
            if (faultString != null) {
                faultstringEl.appendChild(DomUtils.createTextNode(tmpDoc, faultString));
            }
            faultactorEl.appendChild(DomUtils.createTextNode(tmpDoc, faultActor));
            if (faultDetails != null) {
                Node fdc = tmpDoc.importNode(faultDetails, true);
                faultdetailEl.appendChild(fdc);
            }
        } catch ( InvalidDocumentFormatException e) {
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
            Element faultcodeEl = DomUtils.findFirstChildElementByName(payload, (String)null, FAULTCODE);
            if (faultcodeEl == null)
                throw new MissingRequiredElementException("SOAP fault did not have a faultcode element");
            faultcode = DomUtils.getTextValue(faultcodeEl);
            if (faultcode == null || faultcode.length() < 1)
                throw new MissingRequiredElementException("SOAP fault had an empty faultcode element");
            Element faultstringEl = DomUtils.findFirstChildElementByName(payload, (String)null, FAULTSTRING);
            if (faultstringEl != null)
                faultstring = DomUtils.getTextValue(faultstringEl);
            Element faultactorEl = DomUtils.findFirstChildElementByName(payload, (String)null, FAULTACTOR);
            if (faultactorEl != null)
                faultactor = DomUtils.getTextValue(faultactorEl);
            faultdetail = DomUtils.findFirstChildElementByName(payload, (String)null, FAULTDETAIL);
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
}
