package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.test.BugId;
import com.l7tech.xml.soap.SoapVersion;
import org.junit.Before;
import org.junit.Test;

import java.text.MessageFormat;
import java.util.Collections;

import static org.junit.Assert.*;

public class PublishedServiceTest {
    private PublishedService s1;
    private PublishedService s2;
    private SecurityZone zone;
    private PublishedService.WsdlStrategy wsdlStrategy = new ServiceDocumentWsdlStrategy( Collections.<ServiceDocument>emptyList() );

    @Before
    public void setup() {
        zone = new SecurityZone();
        zone.setName( "TestZone" );
        s1 = new PublishedService();
        s1.parseWsdlStrategy( wsdlStrategy );
        s2 = new PublishedService();
    }

    @Test
    public void equalsDifferentSecurityZone() {
        s1.setSecurityZone(zone);
        s2.setSecurityZone(null);
        assertFalse( s1.equals( s2 ) );
        assertFalse( s2.equals( s1 ) );
    }

    @Test
    public void equalsSameSecurityZone() {
        s1.setSecurityZone(zone);
        s2.setSecurityZone(zone);
        assertTrue( s1.equals( s2 ) );
        assertTrue( s2.equals( s1 ) );
    }

    @Test
    public void testHashCodeDifferentSecurityZone() {
        s1.setSecurityZone(zone);
        s2.setSecurityZone(null);
        assertFalse( s1.hashCode() == s2.hashCode() );
    }

    @Test
    public void testHashCodeSameSecurityZone() {
        s1.setSecurityZone(zone);
        s2.setSecurityZone(zone);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    public void copyConstructorSetsSecurityZone() {
        s1.setSecurityZone( zone );
        final PublishedService copy = new PublishedService(s1);
        assertEquals(zone, copy.getSecurityZone());
    }

    @Test
    @BugId( "SSM-4081" )
    public void testGuessSoapVersionBothBindings() throws Exception {
        s1.setSoapVersion( null );
        s1.setWsdlXml( WSDL_PREFIX +
            makeBindingXml( "binding1", "soap" ) +
            makeBindingXml( "binding2", "soap12" ) +
            WSDL_SUFFIX );
        assertNotNull( s1.parsedWsdl() );
        assertEquals( SoapVersion.UNKNOWN, s1.getSoapVersion() );
    }

    @Test
    @BugId( "SSM-4081" )
    public void testGuessSoapVersionNoBindings() throws Exception {
        s1.setSoapVersion( null );
        s1.setWsdlXml( WSDL_PREFIX +
                WSDL_SUFFIX );
        assertNotNull( s1.parsedWsdl() );
        assertEquals( SoapVersion.UNKNOWN, s1.getSoapVersion() );
    }

    @Test
    @BugId( "SSM-4081" )
    public void testGuessSoapVersionOnlySoap11Bindings() throws Exception {
        s1.setSoapVersion( null );
        s1.setWsdlXml( WSDL_PREFIX +
                makeBindingXml( "binding1", "soap" ) +
                WSDL_SUFFIX );
        assertNotNull( s1.parsedWsdl() );
        assertEquals( SoapVersion.SOAP_1_1, s1.getSoapVersion() );
    }

    @Test
    @BugId( "SSM-4081" )
    public void testGuessSoapVersionOnlySoap12Bindings() throws Exception {
        s1.setSoapVersion( null );
        s1.setWsdlXml( WSDL_PREFIX +
                makeBindingXml( "binding1", "soap12" ) +
                WSDL_SUFFIX );
        assertNotNull( s1.parsedWsdl() );
        assertEquals( SoapVersion.SOAP_1_2, s1.getSoapVersion() );
    }

    @Test
    @BugId( "SSM-4081" )
    public void testGuessSoapVersionMultiSoap11Bindings() throws Exception {
        s1.setSoapVersion( null );
        s1.setWsdlXml( WSDL_PREFIX +
                makeBindingXml( "binding1", "soap" ) +
                makeBindingXml( "binding2", "soap" ) +
                WSDL_SUFFIX );
        assertNotNull( s1.parsedWsdl() );
        assertEquals( SoapVersion.SOAP_1_1, s1.getSoapVersion() );
    }

    @Test
    @BugId( "SSM-4081" )
    public void testGuessSoapVersionMultiSoap12Bindings() throws Exception {
        s1.setSoapVersion( null );
        s1.setWsdlXml( WSDL_PREFIX +
                makeBindingXml( "binding1", "soap12" ) +
                makeBindingXml( "binding2", "soap12" ) +
                WSDL_SUFFIX );
        assertNotNull( s1.parsedWsdl() );
        assertEquals( SoapVersion.SOAP_1_2, s1.getSoapVersion() );
    }

    @Test
    @BugId( "SSM-4081" )
    public void testGuessSoapVersionWsdl1() throws Exception {
        s1.setSoapVersion( null );
        s1.setWsdlXml( WSDL1 );
        assertNotNull( s1.parsedWsdl() );
        assertEquals( SoapVersion.UNKNOWN, s1.getSoapVersion() );
    }

    @Test
    @BugId( "SSM-4081" )
    public void testGuessSoapVersionWsdl2() throws Exception {
        s1.setSoapVersion( null );
        s1.setWsdlXml( WSDL2 );
        assertNotNull( s1.parsedWsdl() );
        assertEquals( SoapVersion.UNKNOWN, s1.getSoapVersion() );
    }

    private static final String WSDL_PREFIX =
        "<definitions name=\"test\" targetNamespace=\"urn:test\"\n" +
        "    xmlns=\"http://schemas.xmlsoap.org/wsdl/\"\n" +
        "    xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\"\n" +
        "    xmlns:soap12=\"http://schemas.xmlsoap.org/wsdl/soap12/\"\n" +
        "    xmlns:tns=\"urn:test\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n" +
        "    <message name=\"mess1\"/>\n" +
        "    <portType name=\"port1\">\n" +
        "        <operation name=\"op1\">\n" +
        "            <input message=\"tns:mess1\"/>\n" +
        "            <output message=\"tns:mess1\"/>\n" +
        "        </operation>\n" +
        "    </portType>\n";

    private static final String WSDL_SUFFIX =
        "    <service name=\"svc\">\n" +
        "        <port binding=\"tns:binding1\" name=\"port1\">\n" +
        "            <soap:address location=\"http://localhost:8080/ws/Service\"/>\n" +
        "        </port>\n" +
        "    </service>\n" +
        "</definitions>";

    private static String makeBindingXml( String bindingName, String soapVer ) {
        return ( MessageFormat.format(
            "    <binding name=\"{0}\" type=\"tns:port1\">\n" +
            "        <{1}:binding style=\"rpc\" transport=\"http://schemas.xmlsoap.org/soap/http\"/>\n" +
            "        <operation name=\"op1\">\n" +
            "            <{1}:operation soapAction=\"port1#op1\"/>\n" +
            "            <input>\n" +
            "                <{1}:body\n" +
            "                    encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"\n" +
            "                    namespace=\"urn:test\" use=\"encoded\"/>\n" +
            "            </input>\n" +
            "            <output>\n" +
            "                <{1}:body\n" +
            "                    encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"\n" +
            "                    namespace=\"urn:test\" use=\"encoded\"/>\n" +
            "            </output>\n" +
            "        </operation>\n" +
            "    </binding>\n",
                bindingName, soapVer ) );
    }

    public static final String WSDL1 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<wsdl:definitions xmlns:s=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://schemas.xmlsoap.org/wsdl/soap12/\" xmlns:mime=\"http://schemas.xmlsoap.org/wsdl/mime/\" xmlns:tns=\"http://www.gwsx.gov.sg/demoL1\" xmlns:s1=\"http://www.gwsx.gov.sg/gwsx/request/audit\" xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\" xmlns:tm=\"http://microsoft.com/wsdl/mime/textMatching/\" xmlns:http=\"http://schemas.xmlsoap.org/wsdl/http/\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" targetNamespace=\"http://www.gwsx.gov.sg/demoL1\" xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\">\n" +
                    "  <wsdl:types>\n" +
                    "    <s:schema elementFormDefault=\"qualified\" targetNamespace=\"http://www.gwsx.gov.sg/demoL1\">\n" +
                    "      <s:element name=\"HelloWorld\">\n" +
                    "        <s:complexType/>\n" +
                    "      </s:element>\n" +
                    "      <s:element name=\"HelloWorldResponse\">\n" +
                    "        <s:complexType>\n" +
                    "          <s:sequence>\n" +
                    "            <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"HelloWorldResult\" type=\"s:string\"/>\n" +
                    "          </s:sequence>\n" +
                    "        </s:complexType>\n" +
                    "      </s:element>\n" +
                    "    </s:schema>\n" +
                    "    <s:schema elementFormDefault=\"qualified\" targetNamespace=\"http://www.gwsx.gov.sg/gwsx/request/audit\">\n" +
                    "      <s:element name=\"GWSXHeader\" type=\"s1:GWSXClientRequestHeader\"/>\n" +
                    "      <s:complexType name=\"GWSXClientRequestHeader\">\n" +
                    "        <s:annotation>\n" +
                    "          <s:appinfo>\n" +
                    "            <keepNamespaceDeclarations>xmlns</keepNamespaceDeclarations>\n" +
                    "          </s:appinfo>\n" +
                    "        </s:annotation>\n" +
                    "        <s:sequence>\n" +
                    "          <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"ConsumerTxnID\" type=\"s:string\"/>\n" +
                    "          <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"ConsumerApplicationID\" type=\"s:string\"/>\n" +
                    "        </s:sequence>\n" +
                    "        <s:anyAttribute/>\n" +
                    "      </s:complexType>\n" +
                    "    </s:schema>\n" +
                    "  </wsdl:types>\n" +
                    "  <wsdl:message name=\"HelloWorldSoapIn\">\n" +
                    "    <wsdl:part name=\"parameters\" element=\"tns:HelloWorld\"/>\n" +
                    "  </wsdl:message>\n" +
                    "  <wsdl:message name=\"HelloWorldSoapOut\">\n" +
                    "    <wsdl:part name=\"parameters\" element=\"tns:HelloWorldResponse\"/>\n" +
                    "  </wsdl:message>\n" +
                    "  <wsdl:message name=\"HelloWorldGWSXHeader\">\n" +
                    "    <wsdl:part name=\"GWSXHeader\" element=\"s1:GWSXHeader\"/>\n" +
                    "  </wsdl:message>\n" +
                    "  <wsdl:portType name=\"GWSXDemoL1ServiceSoap\">\n" +
                    "    <wsdl:operation name=\"HelloWorld\">\n" +
                    "      <wsdl:input message=\"tns:HelloWorldSoapIn\"/>\n" +
                    "      <wsdl:output message=\"tns:HelloWorldSoapOut\"/>\n" +
                    "    </wsdl:operation>\n" +
                    "  </wsdl:portType>\n" +
                    "  <wsdl:binding name=\"GWSXDemoL1ServiceSoap\" type=\"tns:GWSXDemoL1ServiceSoap\">\n" +
                    "    <soap:binding transport=\"http://schemas.xmlsoap.org/soap/http\"/>\n" +
                    "    <wsdl:operation name=\"HelloWorld\">\n" +
                    "      <soap:operation soapAction=\"http://www.gwsx.gov.sg/demoL1/HelloWorld\" style=\"document\"/>\n" +
                    "      <wsdl:input>\n" +
                    "        <soap:body use=\"literal\"/>\n" +
                    "        <soap:header message=\"tns:HelloWorldGWSXHeader\" part=\"GWSXHeader\" use=\"literal\"/>\n" +
                    "      </wsdl:input>\n" +
                    "      <wsdl:output>\n" +
                    "        <soap:body use=\"literal\"/>\n" +
                    "      </wsdl:output>\n" +
                    "    </wsdl:operation>\n" +
                    "  </wsdl:binding>\n" +
                    "  <wsdl:binding name=\"GWSXDemoL1ServiceSoap12\" type=\"tns:GWSXDemoL1ServiceSoap\">\n" +
                    "    <soap12:binding transport=\"http://schemas.xmlsoap.org/soap/http\"/>\n" +
                    "    <wsdl:operation name=\"HelloWorld\">\n" +
                    "      <soap12:operation soapAction=\"http://www.gwsx.gov.sg/demoL1/HelloWorld\" style=\"document\"/>\n" +
                    "      <wsdl:input>\n" +
                    "        <soap12:body use=\"literal\"/>\n" +
                    "        <soap12:header message=\"tns:HelloWorldGWSXHeader\" part=\"GWSXHeader\" use=\"literal\"/>\n" +
                    "      </wsdl:input>\n" +
                    "      <wsdl:output>\n" +
                    "        <soap12:body use=\"literal\"/>\n" +
                    "      </wsdl:output>\n" +
                    "    </wsdl:operation>\n" +
                    "  </wsdl:binding>\n" +
                    "  <wsdl:service name=\"GWSXDemoL1Service\">\n" +
                    "    <wsdl:port name=\"GWSXDemoL1ServiceSoap\" binding=\"tns:GWSXDemoL1ServiceSoap\">\n" +
                    "      <soap:address location=\"https://192.168.180.27/GwsxDemoProvider/GWSXDemoL1Service.asmx\"/>\n" +
                    "    </wsdl:port>\n" +
                    "    <wsdl:port name=\"GWSXDemoL1ServiceSoap12\" binding=\"tns:GWSXDemoL1ServiceSoap12\">\n" +
                    "      <soap12:address location=\"https://192.168.180.27/GwsxDemoProvider/GWSXDemoL1Service.asmx\"/>\n" +
                    "    </wsdl:port>\n" +
                    "  </wsdl:service>\n" +
                    "</wsdl:definitions>";


    public static final String WSDL2 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<wsdl:definitions xmlns:s=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://schemas.xmlsoap.org/wsdl/soap12/\" xmlns:mime=\"http://schemas.xmlsoap.org/wsdl/mime/\" xmlns:tns=\"http://www.gwsx.gov.sg/demoL2\" xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\" xmlns:tm=\"http://microsoft.com/wsdl/mime/textMatching/\" xmlns:http=\"http://schemas.xmlsoap.org/wsdl/http/\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" targetNamespace=\"http://www.gwsx.gov.sg/demoL2\" xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\">\n" +
                    "  <wsdl:types>\n" +
                    "    <s:schema elementFormDefault=\"qualified\" targetNamespace=\"http://www.gwsx.gov.sg/demoL2\">\n" +
                    "      <s:element name=\"EchoString\">\n" +
                    "        <s:complexType>\n" +
                    "          <s:sequence>\n" +
                    "            <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"original\" type=\"s:string\"/>\n" +
                    "          </s:sequence>\n" +
                    "        </s:complexType>\n" +
                    "      </s:element>\n" +
                    "      <s:element name=\"EchoStringResponse\">\n" +
                    "        <s:complexType>\n" +
                    "          <s:sequence>\n" +
                    "            <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"EchoStringResult\" type=\"s:string\"/>\n" +
                    "          </s:sequence>\n" +
                    "        </s:complexType>\n" +
                    "      </s:element>\n" +
                    "    </s:schema>\n" +
                    "  </wsdl:types>\n" +
                    "  <wsdl:message name=\"EchoStringSoapIn\">\n" +
                    "    <wsdl:part name=\"parameters\" element=\"tns:EchoString\"/>\n" +
                    "  </wsdl:message>\n" +
                    "  <wsdl:message name=\"EchoStringSoapOut\">\n" +
                    "    <wsdl:part name=\"parameters\" element=\"tns:EchoStringResponse\"/>\n" +
                    "  </wsdl:message>\n" +
                    "  <wsdl:portType name=\"GWSXDemoL2ServiceSoap\">\n" +
                    "    <wsdl:operation name=\"EchoString\">\n" +
                    "      <wsdl:input message=\"tns:EchoStringSoapIn\"/>\n" +
                    "      <wsdl:output message=\"tns:EchoStringSoapOut\"/>\n" +
                    "    </wsdl:operation>\n" +
                    "  </wsdl:portType>\n" +
                    "  <wsdl:binding name=\"GWSXDemoL2ServiceSoap\" type=\"tns:GWSXDemoL2ServiceSoap\">\n" +
                    "    <soap:binding transport=\"http://schemas.xmlsoap.org/soap/http\"/>\n" +
                    "    <wsdl:operation name=\"EchoString\">\n" +
                    "      <soap:operation soapAction=\"http://www.gwsx.gov.sg/demoL2/EchoString\" style=\"document\"/>\n" +
                    "      <wsdl:input>\n" +
                    "        <soap:body use=\"literal\"/>\n" +
                    "      </wsdl:input>\n" +
                    "      <wsdl:output>\n" +
                    "        <soap:body use=\"literal\"/>\n" +
                    "      </wsdl:output>\n" +
                    "    </wsdl:operation>\n" +
                    "  </wsdl:binding>\n" +
                    "  <wsdl:binding name=\"GWSXDemoL2ServiceSoap12\" type=\"tns:GWSXDemoL2ServiceSoap\">\n" +
                    "    <soap12:binding transport=\"http://schemas.xmlsoap.org/soap/http\"/>\n" +
                    "    <wsdl:operation name=\"EchoString\">\n" +
                    "      <soap12:operation soapAction=\"http://www.gwsx.gov.sg/demoL2/EchoString\" style=\"document\"/>\n" +
                    "      <wsdl:input>\n" +
                    "        <soap12:body use=\"literal\"/>\n" +
                    "      </wsdl:input>\n" +
                    "      <wsdl:output>\n" +
                    "        <soap12:body use=\"literal\"/>\n" +
                    "      </wsdl:output>\n" +
                    "    </wsdl:operation>\n" +
                    "  </wsdl:binding>\n" +
                    "  <wsdl:service name=\"GWSXDemoL2Service\">\n" +
                    "    <wsdl:port name=\"GWSXDemoL2ServiceSoap\" binding=\"tns:GWSXDemoL2ServiceSoap\">\n" +
                    "      <soap:address location=\"https://192.168.180.27/GwsxDemoProvider/GWSXDemoL2Service.asmx\"/>\n" +
                    "    </wsdl:port>\n" +
                    "    <wsdl:port name=\"GWSXDemoL2ServiceSoap12\" binding=\"tns:GWSXDemoL2ServiceSoap12\">\n" +
                    "      <soap12:address location=\"https://192.168.180.27/GwsxDemoProvider/GWSXDemoL2Service.asmx\"/>\n" +
                    "    </wsdl:port>\n" +
                    "  </wsdl:service>\n" +
                    "</wsdl:definitions>";
}
