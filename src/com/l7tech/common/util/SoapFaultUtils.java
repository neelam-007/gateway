package com.l7tech.common.util;

import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.SoapFaultDetail;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
    public static Document generateSoapFault(SoapFaultDetail soapFaultDetail, String faultActor) throws IOException, SAXException {
        return generateSoapFault(soapFaultDetail.getFaultCode(),
                                 soapFaultDetail.getFaultString(),
                                 soapFaultDetail.getFaultDetails(),
                                 faultActor);
    }

    public static Document generateSoapFault(String faultCode,
                                             String faultString,
                                             String faultDetails,
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
            faultdetailEl.appendChild(XmlUtil.createTextNode(tmpDoc, faultDetails));
        } catch (InvalidDocumentFormatException e) {
            throw new RuntimeException(e); // this should never happen
        }

        return tmpDoc;
    }

    public static String generateRawSoapFault(SoapFaultDetail soapFaultDetail, String faultActor) throws IOException, SAXException {
        return generateRawSoapFault(soapFaultDetail.getFaultCode(),
                                    soapFaultDetail.getFaultString(),
                                    soapFaultDetail.getFaultDetails(),
                                    faultActor);
    }

    public static String generateRawSoapFault(String faultCode,
                                              String faultString,
                                              String faultDetails,
                                              String faultActor) throws IOException, SAXException {

        Document doc = generateSoapFault(faultCode, faultString, faultDetails, faultActor);
        return XmlUtil.nodeToFormattedString(doc);
    }

    public static Element getChild(Element parent, String childName) {
        NodeList children = parent.getElementsByTagName(childName);
        return (Element) children.item(0);
    }

    public static final String FC_CLIENT = "Client";
    public static final String FC_SERVER = "Server";
}
