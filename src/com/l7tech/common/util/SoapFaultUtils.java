package com.l7tech.common.util;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import com.l7tech.common.xml.SoapFaultDetail;

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
        String rawXml = generateRawSoapFault(faultCode, faultString, faultDetails, faultActor);
        return XmlUtil.stringToDocument(rawXml);
    }

    public static String generateRawSoapFault(SoapFaultDetail soapFaultDetail, String faultActor) {
        return generateRawSoapFault(soapFaultDetail.getFaultCode(),
                                    soapFaultDetail.getFaultString(),
                                    soapFaultDetail.getFaultDetails(),
                                    faultActor);
    }

    public static String generateRawSoapFault(String faultCode,
                                              String faultString,
                                              String faultDetails,
                                              String faultActor) {
        if (faultActor == null) {
            faultActor = "";
        }
        return  "<soapenv:Envelope" +
                " xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"" +
                " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                " <soapenv:Body>\n" +
                "  <soapenv:Fault>\n" +
                "   <faultcode>" + faultCode + "</faultcode>\n" +
                "   <faultstring>" + faultString + "</faultstring>\n" +
                "   <faultactor>" + faultActor + "</faultactor>\n" +
                "   <detail>" + faultDetails + "</detail>\n" +
                "  </soapenv:Fault>\n" +
                " </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    public static final String FC_CLIENT = "Client";
    public static final String FC_SERVER = "Server";
}
