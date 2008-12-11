package com.l7tech.xml.soap;

import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.MissingRequiredElementException;
import com.l7tech.xml.SoapFaultDetail;
import com.l7tech.xml.SoapFaultDetailImpl;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.DomUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
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

    private static final String SOAP_1_2_CODE = "Code";
    private static final String SOAP_1_2_VALUE = "Value";
    private static final String SOAP_1_2_REASON = "Reason";
    private static final String SOAP_1_2_TEXT = "Text";
    private static final String SOAP_1_2_ROLE = "Role";
    private static final String SOAP_1_2_DETAIL = "Detail";
    
    public static Document generateSoapFaultDocument(SoapVersion soapVersion, SoapFaultDetail soapFaultDetail, String faultActor) throws IOException, SAXException {
        return generateSoapFaultDocument(soapVersion,
                                 soapFaultDetail.getFaultCode(),
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

    public static String badContextTokenFault(SoapVersion soapVersion, String actor) {
        String output;
        if(soapVersion == SoapVersion.SOAP_1_2) {
            output = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE + "\">\n" +
                    "  <soapenv:Body>\n" +
                    "    <soapenv:Fault>\n" +
                    "      <soapenv:Code xmlns:wsc=\"http://schema.xmlsoap.org/ws/2005/02/sc\">\n" +
                    "        <soapenv:Value>wsc:BadContextToken</soapenv:Value>\n" +
                    "      </soapenv:Code>\n" +
                    "      <soapenv:Role>@@actor@@</soapenv:Role>\n" +
                    "    </soapenv:Fault>\n" +
                    "  </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
        } else {
            output = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "  <soapenv:Body>\n" +
                    "    <soapenv:Fault xmlns:wsc=\"http://schemas.xmlsoap.org/ws/2005/02/sc\">\n" +
                    "      <faultcode>wsc:BadContextToken</faultcode>\n" +
                    "      <faultactor>@@actor@@</faultactor>\n" +
                    "    </soapenv:Fault>\n" +
                    "  </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
        }
        output = output.replace("@@actor@@", actor);
        return output;
    }

    public static String badKeyInfoFault(SoapVersion soapVersion, String actor) {
        String output;
        if(soapVersion == SoapVersion.SOAP_1_2) {
            output = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE + "\">\n" +
                    "  <soapenv:Body>\n" +
                    "    <soapenv:Fault xmlns:wsse=\"" + SoapUtil.SECURITY_NAMESPACE + "\">\n" +
                    "      <soapenv:Code>\n" +
                    "        <soapenv:Value>wsse:SecurityTokenUnavailable</soapenv:Value>\n" +
                    "      </soapenv:Code>\n" +
                    "      <soapenv:Role>@@actor@@</soapenv:Role>\n" +
                    "    </soapenv:Fault>\n" +
                    "  </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
        } else {
            output = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "  <soapenv:Body>\n" +
                    "    <soapenv:Fault xmlns:wsse=\"" + SoapUtil.SECURITY_NAMESPACE + "\">\n" +
                    "      <faultcode>wsse:SecurityTokenUnavailable</faultcode>\n" +
                    "      <faultactor>@@actor@@</faultactor>\n" +
                    "    </soapenv:Fault>\n" +
                    "  </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
        }
        output = output.replace("@@actor@@", actor);
        return output;
    }

    public static Document generateSoapFaultDocument(SoapVersion soapVersion,
                                             String faultCode,
                                             String faultString,
                                             Element faultDetails,
                                             String faultActor) throws IOException, SAXException {
        Document tmpDoc;
        if(soapVersion == SoapVersion.SOAP_1_2) {
            try {
                tmpDoc = XmlUtil.stringToDocument("<soapenv:Envelope" +
                                                  " xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE + "\"" +
                                                  " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"" +
                                                  " xmlns:wsse=\"" + SoapUtil.SECURITY_NAMESPACE + "\"" +
                                                  " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                                                  " <soapenv:Body>\n" +
                                                  "   <soapenv:Fault>\n" +
                                                  "     <soapenv:Code>\n" +
                                                  "       <soapenv:Value />\n" +
                                                  "     </soapenv:Code>\n" +
                                                  "     <soapenv:Reason>\n" +
                                                  "       <soapenv:Text xml:lang=\"en-US\" />\n" +
                                                  "     </soapenv:Reason>\n" +
                                                  "     <soapenv:Role />\n" +
                                                  "     <soapenv:Detail />\n" +
                                                  "    </soapenv:Fault>\n" +
                                                  "  </soapenv:Body>\n" +
                                                  "</soapenv:Envelope>");
                Element bodyEl = SoapUtil.getBodyElement(tmpDoc);

                Element faultcodeEl = getChild(getChild(bodyEl, "soapenv:Fault"), "soapenv:Value");
                Element faultstringEl = getChild(getChild(bodyEl, "soapenv:Reason"), "soapenv:Text");
                Element faultroleEl = getChild(bodyEl, "soapenv:Role");
                Element faultdetailEl = getChild(bodyEl, "soapenv:Detail");
                faultcodeEl.appendChild(XmlUtil.createTextNode(tmpDoc, faultCode));
                if (faultString != null) {
                    faultstringEl.appendChild(XmlUtil.createTextNode(tmpDoc, faultString));
                }
                faultroleEl.appendChild(XmlUtil.createTextNode(tmpDoc, faultActor));
                if (faultDetails != null) {
                    Node fdc = tmpDoc.importNode(faultDetails, true);
                    faultdetailEl.appendChild(fdc);
                }
            } catch (InvalidDocumentFormatException e) {
                throw new RuntimeException(e); // this should never happen
            }
        } else {
            try {
                tmpDoc = XmlUtil.stringToDocument("<soapenv:Envelope" +
                                                  " xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"" +
                                                  " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"" +
                                                  " xmlns:wsse=\"" + SoapUtil.SECURITY_NAMESPACE + "\"" +
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
            if(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE.equals(soapFault.getDocumentElement().getNamespaceURI())) {
                Element codeEl = DomUtils.findFirstChildElementByName(payload, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, SOAP_1_2_CODE);
                if (codeEl == null) {
                    throw new MissingRequiredElementException("SOAP fault did not have a Code element");
                }

                Element valueEl = XmlUtil.findFirstChildElementByName(codeEl, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, SOAP_1_2_VALUE);
                if(valueEl == null) {
                    throw new MissingRequiredElementException("SOAP fault Code element did not have a Value element");
                }
                faultcode = DomUtils.getTextValue(valueEl);
                if(faultcode == null || faultcode.length() < 1) {
                    throw new MissingRequiredElementException("SOAP fault Code element had an empty Value element");
                }

                Element reasonEl = DomUtils.findFirstChildElementByName(payload, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, SOAP_1_2_REASON);
                if(reasonEl != null) {
                    NodeList textElements = reasonEl.getElementsByTagNameNS(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, SOAP_1_2_TEXT);
                    for(int i = 0;i < textElements.getLength();i++) {
                        Element textEl = (Element)textElements.item(i);
                        String lang = textEl.getAttributeNS("http://www.w3.org/XML/1998/namespace", "lang");
                        if("en-US".equals(lang)) {
                            faultstring = XmlUtil.getTextValue(textEl);
                            break;
                        }
                    }
                }

                Element roleEl = XmlUtil.findFirstChildElementByName(payload, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, SOAP_1_2_ROLE);
                if(roleEl != null) {
                    faultactor = XmlUtil.getTextValue(roleEl);
                }

                faultdetail = XmlUtil.findFirstChildElementByName(payload, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, SOAP_1_2_DETAIL);

                return new SoapFaultDetailImpl(faultcode, faultstring, faultdetail, faultactor);
            } else {
                Element faultcodeEl = XmlUtil.findFirstChildElementByName(payload, (String)null, FAULTCODE);
                if (faultcodeEl == null)
                    throw new MissingRequiredElementException("SOAP fault did not have a faultcode element");
                faultcode = DomUtils.getTextValue(faultcodeEl);
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
        }

        return null;
    }

    public static String generateSoapFaultXml(SoapVersion soapVersion,
                                              String faultCode,
                                              String faultString,
                                              Element faultDetails,
                                              String faultActor) throws IOException, SAXException {

        Document doc = generateSoapFaultDocument(soapVersion, faultCode, faultString, faultDetails, faultActor);
        return XmlUtil.nodeToFormattedString(doc);
    }

    public static Element getChild(Element parent, String childName) {
        NodeList children = parent.getElementsByTagName(childName);
        return (Element) children.item(0);
    }
}
