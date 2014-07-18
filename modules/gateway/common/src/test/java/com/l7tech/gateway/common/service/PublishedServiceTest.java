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
        zone.setName("TestZone");
        s1 = new PublishedService();
        s1.parseWsdlStrategy( wsdlStrategy );
        s2 = new PublishedService();
    }

    @Test
    public void equalsDifferentSecurityZone() {
        s1.setSecurityZone(zone);
        s2.setSecurityZone(null);
        assertFalse(s1.equals(s2));
        assertFalse(s2.equals(s1));
    }

    @Test
    public void equalsSameSecurityZone() {
        s1.setSecurityZone(zone);
        s2.setSecurityZone(zone);
        assertTrue(s1.equals(s2));
        assertTrue(s2.equals(s1));
    }

    @Test
    public void testHashCodeDifferentSecurityZone() {
        s1.setSecurityZone(zone);
        s2.setSecurityZone(null);
        assertFalse(s1.hashCode() == s2.hashCode());
    }

    @Test
    public void testHashCodeSameSecurityZone() {
        s1.setSecurityZone(zone);
        s2.setSecurityZone(zone);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    public void copyConstructorSetsSecurityZone() {
        s1.setSecurityZone(zone);
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

}
