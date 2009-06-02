package com.l7tech.xml.soap;

import org.junit.Test;
import org.junit.Assert;

/**
 * 
 */
public class SoapVersionTest {

    @Test
    public void testVersionRecognition() {
        Assert.assertEquals("Soap 1.1", SoapVersion.SOAP_1_1, SoapVersion.namespaceToSoapVersion("http://schemas.xmlsoap.org/soap/envelope/"));
        Assert.assertEquals("Soap 1.2", SoapVersion.SOAP_1_2, SoapVersion.namespaceToSoapVersion("http://www.w3.org/2003/05/soap-envelope"));
        Assert.assertEquals("Soap Unknown", SoapVersion.UNKNOWN, SoapVersion.namespaceToSoapVersion("http://tempuri.org/unknown"));
        Assert.assertEquals("Soap Unknown (case)", SoapVersion.UNKNOWN, SoapVersion.namespaceToSoapVersion("http://schemas.xmlsoap.org/soap/Envelope/"));
        Assert.assertEquals("Soap Unknown (slash)", SoapVersion.UNKNOWN, SoapVersion.namespaceToSoapVersion("http://schemas.xmlsoap.org/soap/envelope"));
    }

    @Test
    public void testVersionOrdering() {
        Assert.assertTrue("SOAP 1.1 before SOAP 1.2", SoapVersion.SOAP_1_2.isPriorVersion(SoapVersion.SOAP_1_1));
        Assert.assertTrue("SOAP UNKNOWN before SOAP 1.2", SoapVersion.SOAP_1_2.isPriorVersion(SoapVersion.UNKNOWN));
        Assert.assertTrue("SOAP UNKNOWN before SOAP 1.1", SoapVersion.SOAP_1_1.isPriorVersion(SoapVersion.UNKNOWN));

        Assert.assertFalse("SOAP 1.2 before SOAP 1.2", SoapVersion.SOAP_1_2.isPriorVersion(SoapVersion.SOAP_1_2));
        Assert.assertFalse("SOAP 1.1 before SOAP 1.1", SoapVersion.SOAP_1_1.isPriorVersion(SoapVersion.SOAP_1_1));
        Assert.assertFalse("SOAP UNKNOWN before SOAP UNKNOWN", SoapVersion.UNKNOWN.isPriorVersion(SoapVersion.UNKNOWN));
        Assert.assertFalse("SOAP 1.2 before SOAP 1.1", SoapVersion.SOAP_1_1.isPriorVersion(SoapVersion.SOAP_1_2));
        Assert.assertFalse("SOAP 1.2 before SOAP UNKNOWN", SoapVersion.UNKNOWN.isPriorVersion(SoapVersion.SOAP_1_2));
        Assert.assertFalse("SOAP 1.1 before SOAP UNKNOWN", SoapVersion.UNKNOWN.isPriorVersion(SoapVersion.SOAP_1_2));
    }
}
