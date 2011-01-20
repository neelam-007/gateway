package com.l7tech.wsdl;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.test.BugNumber;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class WsdlUtilTest {
    @Test
    public void testWsdlRewriting() throws Exception {
        doTestWsdlRewriting(SIMPLE_WSDL);
    }

    @Test
    @BugNumber(9108)
    public void testWsdlRewritingHttpBindings() throws Exception {
        doTestWsdlRewriting(REST_WSDL);
    }

    private void doTestWsdlRewriting(String wsdlString) throws Exception {
        Document wsdlDoc = XmlUtil.stringAsDocument(wsdlString);

        WsdlUtil.rewriteAddressLocations(wsdlDoc, new WsdlUtil.LocationBuilder() {
            @Override
            public String buildLocation(Element address) throws MalformedURLException {
                return "urn:rewritten:1234";
            }
        });


        NodeList nl = wsdlDoc.getElementsByTagName("*");
        int nll = nl.getLength();
        for (int i = 0; i < nll; i++) {
            Element element = (Element) nl.item(i);
            if ("address".equals(element.getLocalName())) {
                assertEquals("urn:rewritten:1234", element.getAttribute("location"));
            }
        }
    }

    private static final String SIMPLE_WSDL =
            "<definitions xmlns=\"http://schemas.xmlsoap.org/wsdl/\" xmlns:http=\"http://schemas.xmlsoap.org/wsdl/http/\" xmlns:mime=\"http://schemas.xmlsoap.org/wsdl/mime/\" xmlns:s=\"http://www.w3.org/2001/XMLSchema\" xmlns:s0=\"http://webservices.geomonster.com/\" xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:sourcens0=\"http://webservices.geomonster.com/\" xmlns:tm=\"http://microsoft.com/wsdl/mime/textMatching/\" xmlns:tns=\"http://tempuri.org/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" name=\"NewService\" targetNamespace=\"http://tempuri.org/\">\n" +
            "  <types>\n" +
            "  </types>\n" +
            "  <portType name=\"NewPortType\">\n" +
            "  </portType>\n" +
            "  <binding name=\"NewPortTypeBinding\" type=\"tns:NewPortType\">\n" +
            "    <soap:binding style=\"rpc\" transport=\"http://schemas.xmlsoap.org/soap/http\"></soap:binding>\n" +
            "  </binding>\n" +
            "  <service name=\"Service\">\n" +
            "\n" +
            "    <port binding=\"tns:NewPortTypeBinding\" name=\"ServicePort\">\n" +
            "      <soap:address location=\"http://localhost:8080/service/22609923\"></soap:address>\n" +
            "    </port>\n" +
            "  </service>\n" +
            "</definitions>";

    private static final String REST_WSDL =
            "<wsdl:definitions targetNamespace=\"http://tempuri.org/\" xmlns:http=\"http://schemas.xmlsoap.org/wsdl/http/\" xmlns:mime=\"http://schemas.xmlsoap.org/wsdl/mime/\" xmlns:s=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\" xmlns:soap12=\"http://schemas.xmlsoap.org/wsdl/soap12/\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:tm=\"http://microsoft.com/wsdl/mime/textMatching/\" xmlns:tns=\"http://tempuri.org/\" xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\">\n" +
            "    <wsdl:types>\n" +
            "        <s:schema elementFormDefault=\"qualified\" targetNamespace=\"http://tempuri.org/\">\n" +
            "            <s:element name=\"GetNumWithinDist\">\n" +
            "                <s:complexType>\n" +
            "                    <s:sequence>\n" +
            "                        <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"Lon\" type=\"s:string\"/>\n" +
            "                        <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"Lat\" type=\"s:string\"/>\n" +
            "                        <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"DistNum\" type=\"s:string\"/>\n" +
            "                        <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"Unit\" type=\"s:string\"/>\n" +
            "                    </s:sequence>\n" +
            "                </s:complexType>\n" +
            "            </s:element>\n" +
            "            <s:element name=\"GetNumWithinDistResponse\">\n" +
            "                <s:complexType>\n" +
            "                    <s:sequence>\n" +
            "                        <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"GetNumWithinDistResult\" type=\"s:string\"/>\n" +
            "                    </s:sequence>\n" +
            "                </s:complexType>\n" +
            "            </s:element>\n" +
            "            <s:element name=\"GetSDO_NN\">\n" +
            "                <s:complexType>\n" +
            "                    <s:sequence>\n" +
            "                        <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"Lon\" type=\"s:string\"/>\n" +
            "                        <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"Lat\" type=\"s:string\"/>\n" +
            "                    </s:sequence>\n" +
            "                </s:complexType>\n" +
            "            </s:element>\n" +
            "            <s:element name=\"GetSDO_NNResponse\">\n" +
            "                <s:complexType>\n" +
            "                    <s:sequence>\n" +
            "                        <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"GetSDO_NNResult\" type=\"tns:Milepost\"/>\n" +
            "                    </s:sequence>\n" +
            "                </s:complexType>\n" +
            "            </s:element>\n" +
            "            <s:complexType name=\"Milepost\">\n" +
            "                <s:sequence>\n" +
            "                    <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"ObjectID\" type=\"s:string\"/>\n" +
            "                    <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"Div\" type=\"s:string\"/>\n" +
            "                    <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"Pre\" type=\"s:string\"/>\n" +
            "                    <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"Milepost\" type=\"s:string\"/>\n" +
            "                    <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"Suf\" type=\"s:string\"/>\n" +
            "                    <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"Latitude\" type=\"s:string\"/>\n" +
            "                    <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"Longitude\" type=\"s:string\"/>\n" +
            "                    <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"Elevation\" type=\"s:string\"/>\n" +
            "                    <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"Distance\" type=\"s:string\"/>\n" +
            "                </s:sequence>\n" +
            "            </s:complexType>\n" +
            "            <s:element name=\"string\" nillable=\"true\" type=\"s:string\"/>\n" +
            "            <s:element name=\"Milepost\" nillable=\"true\" type=\"tns:Milepost\"/>\n" +
            "        </s:schema>\n" +
            "    </wsdl:types>\n" +
            "    <wsdl:message name=\"GetNumWithinDistSoapIn\">\n" +
            "        <wsdl:part element=\"tns:GetNumWithinDist\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"GetNumWithinDistSoapOut\">\n" +
            "        <wsdl:part element=\"tns:GetNumWithinDistResponse\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"GetSDO_NNSoapIn\">\n" +
            "        <wsdl:part element=\"tns:GetSDO_NN\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"GetSDO_NNSoapOut\">\n" +
            "        <wsdl:part element=\"tns:GetSDO_NNResponse\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"GetNumWithinDistHttpGetIn\">\n" +
            "        <wsdl:part name=\"Lon\" type=\"s:string\"/>\n" +
            "        <wsdl:part name=\"Lat\" type=\"s:string\"/>\n" +
            "        <wsdl:part name=\"DistNum\" type=\"s:string\"/>\n" +
            "        <wsdl:part name=\"Unit\" type=\"s:string\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"GetNumWithinDistHttpGetOut\">\n" +
            "        <wsdl:part element=\"tns:string\" name=\"Body\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"GetSDO_NNHttpGetIn\">\n" +
            "        <wsdl:part name=\"Lon\" type=\"s:string\"/>\n" +
            "        <wsdl:part name=\"Lat\" type=\"s:string\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"GetSDO_NNHttpGetOut\">\n" +
            "        <wsdl:part element=\"tns:Milepost\" name=\"Body\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"GetNumWithinDistHttpPostIn\">\n" +
            "        <wsdl:part name=\"Lon\" type=\"s:string\"/>\n" +
            "        <wsdl:part name=\"Lat\" type=\"s:string\"/>\n" +
            "        <wsdl:part name=\"DistNum\" type=\"s:string\"/>\n" +
            "        <wsdl:part name=\"Unit\" type=\"s:string\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"GetNumWithinDistHttpPostOut\">\n" +
            "        <wsdl:part element=\"tns:string\" name=\"Body\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"GetSDO_NNHttpPostIn\">\n" +
            "        <wsdl:part name=\"Lon\" type=\"s:string\"/>\n" +
            "        <wsdl:part name=\"Lat\" type=\"s:string\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"GetSDO_NNHttpPostOut\">\n" +
            "        <wsdl:part element=\"tns:Milepost\" name=\"Body\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:portType name=\"ConvertSoap\">\n" +
            "        <wsdl:operation name=\"GetNumWithinDist\">\n" +
            "            <wsdl:input message=\"tns:GetNumWithinDistSoapIn\"/>\n" +
            "            <wsdl:output message=\"tns:GetNumWithinDistSoapOut\"/>\n" +
            "        </wsdl:operation>\n" +
            "        <wsdl:operation name=\"GetSDO_NN\">\n" +
            "            <wsdl:input message=\"tns:GetSDO_NNSoapIn\"/>\n" +
            "            <wsdl:output message=\"tns:GetSDO_NNSoapOut\"/>\n" +
            "        </wsdl:operation>\n" +
            "    </wsdl:portType>\n" +
            "    <wsdl:portType name=\"ConvertHttpGet\">\n" +
            "        <wsdl:operation name=\"GetNumWithinDist\">\n" +
            "            <wsdl:input message=\"tns:GetNumWithinDistHttpGetIn\"/>\n" +
            "            <wsdl:output message=\"tns:GetNumWithinDistHttpGetOut\"/>\n" +
            "        </wsdl:operation>\n" +
            "        <wsdl:operation name=\"GetSDO_NN\">\n" +
            "            <wsdl:input message=\"tns:GetSDO_NNHttpGetIn\"/>\n" +
            "            <wsdl:output message=\"tns:GetSDO_NNHttpGetOut\"/>\n" +
            "        </wsdl:operation>\n" +
            "    </wsdl:portType>\n" +
            "    <wsdl:portType name=\"ConvertHttpPost\">\n" +
            "        <wsdl:operation name=\"GetNumWithinDist\">\n" +
            "            <wsdl:input message=\"tns:GetNumWithinDistHttpPostIn\"/>\n" +
            "            <wsdl:output message=\"tns:GetNumWithinDistHttpPostOut\"/>\n" +
            "        </wsdl:operation>\n" +
            "        <wsdl:operation name=\"GetSDO_NN\">\n" +
            "            <wsdl:input message=\"tns:GetSDO_NNHttpPostIn\"/>\n" +
            "            <wsdl:output message=\"tns:GetSDO_NNHttpPostOut\"/>\n" +
            "        </wsdl:operation>\n" +
            "    </wsdl:portType>\n" +
            "    <wsdl:binding name=\"ConvertSoap\" type=\"tns:ConvertSoap\">\n" +
            "        <soap:binding transport=\"http://schemas.xmlsoap.org/soap/http\"/>\n" +
            "        <wsdl:operation name=\"GetNumWithinDist\">\n" +
            "            <soap:operation soapAction=\"http://tempuri.org/GetNumWithinDist\" style=\"document\"/>\n" +
            "            <wsdl:input>\n" +
            "                <soap:body use=\"literal\"/>\n" +
            "            </wsdl:input>\n" +
            "            <wsdl:output>\n" +
            "                <soap:body use=\"literal\"/>\n" +
            "            </wsdl:output>\n" +
            "        </wsdl:operation>\n" +
            "        <wsdl:operation name=\"GetSDO_NN\">\n" +
            "            <soap:operation soapAction=\"http://tempuri.org/GetSDO_NN\" style=\"document\"/>\n" +
            "            <wsdl:input>\n" +
            "                <soap:body use=\"literal\"/>\n" +
            "            </wsdl:input>\n" +
            "            <wsdl:output>\n" +
            "                <soap:body use=\"literal\"/>\n" +
            "            </wsdl:output>\n" +
            "        </wsdl:operation>\n" +
            "    </wsdl:binding>\n" +
            "    <wsdl:binding name=\"ConvertSoap12\" type=\"tns:ConvertSoap\">\n" +
            "        <soap12:binding transport=\"http://schemas.xmlsoap.org/soap/http\"/>\n" +
            "        <wsdl:operation name=\"GetNumWithinDist\">\n" +
            "            <soap12:operation soapAction=\"http://tempuri.org/GetNumWithinDist\" style=\"document\"/>\n" +
            "            <wsdl:input>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:input>\n" +
            "            <wsdl:output>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:output>\n" +
            "        </wsdl:operation>\n" +
            "        <wsdl:operation name=\"GetSDO_NN\">\n" +
            "            <soap12:operation soapAction=\"http://tempuri.org/GetSDO_NN\" style=\"document\"/>\n" +
            "            <wsdl:input>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:input>\n" +
            "            <wsdl:output>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:output>\n" +
            "        </wsdl:operation>\n" +
            "    </wsdl:binding>\n" +
            "    <wsdl:binding name=\"ConvertHttpGet\" type=\"tns:ConvertHttpGet\">\n" +
            "        <http:binding verb=\"GET\"/>\n" +
            "        <wsdl:operation name=\"GetNumWithinDist\">\n" +
            "            <http:operation location=\"/GetNumWithinDist\"/>\n" +
            "            <wsdl:input>\n" +
            "                <http:urlEncoded/>\n" +
            "            </wsdl:input>\n" +
            "            <wsdl:output>\n" +
            "                <mime:mimeXml part=\"Body\"/>\n" +
            "            </wsdl:output>\n" +
            "        </wsdl:operation>\n" +
            "        <wsdl:operation name=\"GetSDO_NN\">\n" +
            "            <http:operation location=\"/GetSDO_NN\"/>\n" +
            "            <wsdl:input>\n" +
            "                <http:urlEncoded/>\n" +
            "            </wsdl:input>\n" +
            "            <wsdl:output>\n" +
            "                <mime:mimeXml part=\"Body\"/>\n" +
            "            </wsdl:output>\n" +
            "        </wsdl:operation>\n" +
            "    </wsdl:binding>\n" +
            "    <wsdl:binding name=\"ConvertHttpPost\" type=\"tns:ConvertHttpPost\">\n" +
            "        <http:binding verb=\"POST\"/>\n" +
            "        <wsdl:operation name=\"GetNumWithinDist\">\n" +
            "            <http:operation location=\"/GetNumWithinDist\"/>\n" +
            "            <wsdl:input>\n" +
            "                <mime:content type=\"application/x-www-form-urlencoded\"/>\n" +
            "            </wsdl:input>\n" +
            "            <wsdl:output>\n" +
            "                <mime:mimeXml part=\"Body\"/>\n" +
            "            </wsdl:output>\n" +
            "        </wsdl:operation>\n" +
            "        <wsdl:operation name=\"GetSDO_NN\">\n" +
            "            <http:operation location=\"/GetSDO_NN\"/>\n" +
            "            <wsdl:input>\n" +
            "                <mime:content type=\"application/x-www-form-urlencoded\"/>\n" +
            "            </wsdl:input>\n" +
            "            <wsdl:output>\n" +
            "                <mime:mimeXml part=\"Body\"/>\n" +
            "            </wsdl:output>\n" +
            "        </wsdl:operation>\n" +
            "    </wsdl:binding>\n" +
            "    <wsdl:service name=\"Convert\">\n" +
            "        <wsdl:port binding=\"tns:ConvertSoap\" name=\"ConvertSoap\">\n" +
            "            <soap:address location=\"http://ogisd.nscorp.com/egis/rail.asmx\"/>\n" +
            "        </wsdl:port>\n" +
            "        <wsdl:port binding=\"tns:ConvertSoap12\" name=\"ConvertSoap12\">\n" +
            "            <soap12:address location=\"http://ogisd.nscorp.com/egis/rail.asmx\"/>\n" +
            "        </wsdl:port>\n" +
            "        <wsdl:port binding=\"tns:ConvertHttpGet\" name=\"ConvertHttpGet\">\n" +
            "            <http:address location=\"http://ogisd.nscorp.com/egis/rail.asmx\"/>\n" +
            "        </wsdl:port>\n" +
            "        <wsdl:port binding=\"tns:ConvertHttpPost\" name=\"ConvertHttpPost\">\n" +
            "            <http:address location=\"http://ogisd.nscorp.com/egis/rail.asmx\"/>\n" +
            "        </wsdl:port>\n" +
            "    </wsdl:service>\n" +
            "</wsdl:definitions>";
}
