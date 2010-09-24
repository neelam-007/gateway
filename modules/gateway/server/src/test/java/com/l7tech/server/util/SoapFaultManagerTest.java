package com.l7tech.server.util;

import com.ibm.xml.dsig.IDResolver;
import com.ibm.xml.dsig.SignatureContext;
import com.ibm.xml.dsig.Validity;
import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.xml.SecurityActor;
import com.l7tech.security.xml.processor.MockProcessorResult;
import com.l7tech.server.TestDefaultKey;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.audit.AuditContextStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.message.PolicyEnforcementContextWrapper;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.security.keystore.SsgKeyFinderStub;
import com.l7tech.server.security.keystore.SsgKeyStoreManagerStub;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.MockConfig;
import com.l7tech.xml.SoapFaultLevel;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 *
 */
public class SoapFaultManagerTest {

    @Test
    public void testSoap12ExceptionFault() throws Exception {
        SoapFaultManager sfm = buildSoapFaultManager();
        final SoapFaultManager.FaultResponse faultInfo = sfm.constructExceptionFault( constructException(), null, getSoap12PEC(false) );
        String fault = faultInfo.getContent();
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault );
        assertEquals( "Http Status", 500, faultInfo.getHttpStatus() );
        assertEquals( "SOAP 1.2", SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, doc.getDocumentElement().getNamespaceURI() );
        assertFalse( "Policy version fault", fault.contains("Incorrect policy version"));
        assertTrue("Service URL in fault", fault.contains( SERVICE_URL ));
        assertNull( "No SOAP header", SoapUtil.getHeaderElement(doc) );
        assertEquals( "SOAP 1.2 Content Type", ContentTypeHeader.SOAP_1_2_DEFAULT, faultInfo.getContentType() );
        assertFalse( "Contains medium detail", fault.contains("No credentials found!") );
        assertFalse( "Contains full detail", fault.contains("Authorization header not applicable for this assertion") );
    }

    @Test
    public void testSoap12MediumDetailExceptionFault() throws Exception {
        AuditContextStub stub = new AuditContextStub();
        PolicyEnforcementContext context = getSoap12PEC(false);
        addAuditInfo( stub, context );
        SoapFaultManager sfm = buildSoapFaultManager( stub );
        SoapFaultLevel level = new SoapFaultLevel();
        level.setLevel(SoapFaultLevel.MEDIUM_DETAIL_FAULT);
        final SoapFaultManager.FaultResponse faultInfo = sfm.constructExceptionFault( constructException(), level, context );
        String fault = faultInfo.getContent();
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault );
        assertEquals( "Http Status", 500, faultInfo.getHttpStatus() );
        assertEquals( "SOAP 1.2", SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, doc.getDocumentElement().getNamespaceURI() );
        assertFalse( "Policy version fault", fault.contains("Incorrect policy version"));
        assertTrue("Service URL in fault", fault.contains( SERVICE_URL ));
        assertNull( "No SOAP header", SoapUtil.getHeaderElement(doc) );
        assertEquals( "SOAP 1.2 Content Type", ContentTypeHeader.SOAP_1_2_DEFAULT, faultInfo.getContentType() );
        assertTrue( "Contains medium detail", fault.contains("No credentials found!") );
        assertFalse( "Contains full detail", fault.contains("Authorization header not applicable for this assertion") );
    }

    @Test
    public void testSoap12DetailExceptionFault() throws Exception {
        AuditContextStub stub = new AuditContextStub();
        PolicyEnforcementContext context = getSoap12PEC(false);
        SoapFaultManager sfm = buildSoapFaultManager(stub);
        addAuditInfo( stub, context );
        SoapFaultLevel level = new SoapFaultLevel();
        level.setLevel(SoapFaultLevel.FULL_TRACE_FAULT);
        final SoapFaultManager.FaultResponse faultInfo = sfm.constructExceptionFault( constructException(), level, context );
        String fault = faultInfo.getContent();
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault );
        assertEquals( "Http Status", 500, faultInfo.getHttpStatus() );
        assertEquals( "SOAP 1.2", SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, doc.getDocumentElement().getNamespaceURI() );
        assertFalse( "Policy version fault", fault.contains("Incorrect policy version"));
        assertTrue("Service URL in fault", fault.contains( SERVICE_URL ));
        assertNull( "No SOAP header", SoapUtil.getHeaderElement(doc) );
        assertEquals( "SOAP 1.2 Content Type", ContentTypeHeader.SOAP_1_2_DEFAULT, faultInfo.getContentType() );
        assertTrue( "Contains medium detail", fault.contains("No credentials found!") );
        assertTrue( "Contains full detail", fault.contains("Authorization header not applicable for this assertion") );
    }

    @Test
    public void testSoap11ExceptionFault() throws Exception {
        SoapFaultManager sfm = buildSoapFaultManager();
        final SoapFaultManager.FaultResponse faultInfo = sfm.constructExceptionFault( constructException(), null, getSoap11PEC(false) );
        String fault = faultInfo.getContent();
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault );
        assertEquals( "Http Status", 500, faultInfo.getHttpStatus() );
        assertEquals( "SOAP 1.1", SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, doc.getDocumentElement().getNamespaceURI() );
        assertFalse( "Policy version fault", fault.contains("Incorrect policy version"));
        assertTrue("Service URL in fault", fault.contains( SERVICE_URL ));
        assertNull( "No SOAP header", SoapUtil.getHeaderElement(doc) );
        assertEquals( "SOAP 1.1 Content Type", ContentTypeHeader.XML_DEFAULT, faultInfo.getContentType() );
        assertFalse( "Contains medium detail", fault.contains("No credentials found!") );
        assertFalse( "Contains full detail", fault.contains("Authorization header not applicable for this assertion") );
    }

    @Test
    public void testSoap11MediumDetailExceptionFault() throws Exception { //TODO
        AuditContextStub stub = new AuditContextStub();
        PolicyEnforcementContext context = getSoap11PEC(false);
        SoapFaultManager sfm = buildSoapFaultManager(stub);
        addAuditInfo( stub, context );
        SoapFaultLevel level = new SoapFaultLevel();
        level.setLevel(SoapFaultLevel.MEDIUM_DETAIL_FAULT);
        final SoapFaultManager.FaultResponse faultInfo = sfm.constructExceptionFault( constructException(), level, context );
        String fault = faultInfo.getContent();
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault );
        assertEquals( "Http Status", 500, faultInfo.getHttpStatus() );
        assertEquals( "SOAP 1.1", SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, doc.getDocumentElement().getNamespaceURI() );
        assertFalse( "Policy version fault", fault.contains("Incorrect policy version"));
        assertTrue("Service URL in fault", fault.contains( SERVICE_URL ));
        assertNull( "No SOAP header", SoapUtil.getHeaderElement(doc) );
        assertEquals( "SOAP 1.1 Content Type", ContentTypeHeader.XML_DEFAULT, faultInfo.getContentType() );
        assertTrue( "Contains medium detail", fault.contains("No credentials found!") );
        assertFalse( "Contains full detail", fault.contains("Authorization header not applicable for this assertion") );
    }

    @Test
    public void testSoap11DetailExceptionFault() throws Exception {
        AuditContextStub stub = new AuditContextStub();
        PolicyEnforcementContext context = getSoap11PEC(false);
        SoapFaultManager sfm = buildSoapFaultManager(stub);
        addAuditInfo( stub, context );
        SoapFaultLevel level = new SoapFaultLevel();
        level.setLevel(SoapFaultLevel.FULL_TRACE_FAULT);
        final SoapFaultManager.FaultResponse faultInfo = sfm.constructExceptionFault( constructException(), level, context );
        String fault = faultInfo.getContent();
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault );
        assertEquals( "Http Status", 500, faultInfo.getHttpStatus() );
        assertEquals( "SOAP 1.1", SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, doc.getDocumentElement().getNamespaceURI() );
        assertFalse( "Policy version fault", fault.contains("Incorrect policy version"));
        assertTrue("Service URL in fault", fault.contains( SERVICE_URL ));
        assertNull( "No SOAP header", SoapUtil.getHeaderElement(doc) );
        assertEquals( "SOAP 1.1 Content Type", ContentTypeHeader.XML_DEFAULT, faultInfo.getContentType() );
        assertTrue( "Contains medium detail", fault.contains("No credentials found!") );
        assertTrue( "Contains full detail", fault.contains("Authorization header not applicable for this assertion") );
    }

    @Test
    public void testSoap12PolicyVersionFault() throws Exception {
        SoapFaultManager sfm = buildSoapFaultManager();
        final SoapFaultManager.FaultResponse faultInfo = sfm.constructExceptionFault( constructException(), null, getSoap12PEC(true) );
        String fault = faultInfo.getContent();
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault );
        assertEquals( "Http Status", 500, faultInfo.getHttpStatus() );
        assertEquals( "SOAP 1.2", SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, doc.getDocumentElement().getNamespaceURI() );
        assertTrue( "Policy version fault", fault.contains("Incorrect policy version"));
        assertTrue("Service URL in fault", fault.contains( SERVICE_URL ));
        assertNull( "No SOAP header", SoapUtil.getHeaderElement(doc) );
        assertEquals( "SOAP 1.2 Content Type", ContentTypeHeader.SOAP_1_2_DEFAULT, faultInfo.getContentType() );
    }

    @Test
    public void testSoap11PolicyVersionFault() throws Exception {
        SoapFaultManager sfm = buildSoapFaultManager();
        final SoapFaultManager.FaultResponse faultInfo = sfm.constructExceptionFault( constructException(), null, getSoap11PEC(true) );
        String fault = faultInfo.getContent();
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault );
        assertEquals( "Http Status", 500, faultInfo.getHttpStatus() );
        assertEquals( "SOAP 1.1", SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, doc.getDocumentElement().getNamespaceURI() );
        assertTrue( "Policy version fault", fault.contains("Incorrect policy version"));
        assertTrue("Service URL in fault", fault.contains( SERVICE_URL ));
        assertNull( "No SOAP header", SoapUtil.getHeaderElement(doc) );
        assertEquals( "SOAP 1.1 Content Type", ContentTypeHeader.XML_DEFAULT, faultInfo.getContentType() );
    }

    @Test
    public void testSoap12GenericFault() throws Exception {
        SoapFaultManager sfm = buildSoapFaultManager();
        SoapFaultLevel level = new SoapFaultLevel();
        level.setLevel(SoapFaultLevel.GENERIC_FAULT);
        final SoapFaultManager.FaultResponse fault = sfm.constructReturningFault( level, getSoap12PEC(false) );
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault.getContent() );
        assertEquals( "Http Status", 500, fault.getHttpStatus() );
        assertEquals( "SOAP 1.2", SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, doc.getDocumentElement().getNamespaceURI() );
        assertFalse( "Policy version fault", fault.getContent().contains("Incorrect policy version"));
        assertTrue("Service URL in fault", fault.getContent().contains( SERVICE_URL ));
        assertNull( "No SOAP header", SoapUtil.getHeaderElement(doc) );
        assertEquals( "SOAP 1.2 Content Type", ContentTypeHeader.SOAP_1_2_DEFAULT, fault.getContentType() );
    }

    @Test
    public void testSoap11GenericFault() throws Exception {
        SoapFaultManager sfm = buildSoapFaultManager();
        SoapFaultLevel level = new SoapFaultLevel();
        level.setLevel(SoapFaultLevel.GENERIC_FAULT);
        final SoapFaultManager.FaultResponse fault = sfm.constructReturningFault( level, getSoap11PEC(false) );
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault.getContent() );
        assertEquals( "Http Status", 500, fault.getHttpStatus() );
        assertEquals( "SOAP 1.1", SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, doc.getDocumentElement().getNamespaceURI() );
        assertFalse( "Policy version fault", fault.getContent().contains("Incorrect policy version"));
        assertTrue("Service URL in fault", fault.getContent().contains( SERVICE_URL ));
        assertNull( "No SOAP header", SoapUtil.getHeaderElement(doc) );
        assertEquals( "SOAP 1.1 Content Type", ContentTypeHeader.XML_DEFAULT, fault.getContentType() );
    }

    @Test
    public void testSoap12MediumFault() throws Exception {
        SoapFaultManager sfm = buildSoapFaultManager();
        SoapFaultLevel level = new SoapFaultLevel();
        level.setLevel(SoapFaultLevel.MEDIUM_DETAIL_FAULT);
        final SoapFaultManager.FaultResponse fault = sfm.constructReturningFault( level, getSoap12PEC(false) );
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault.getContent() );
        assertEquals( "Http Status", 500, fault.getHttpStatus() );
        assertEquals( "SOAP 1.2", SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, doc.getDocumentElement().getNamespaceURI() );
        assertFalse( "Policy version fault", fault.getContent().contains("Incorrect policy version"));
        assertTrue("Service URL in fault", fault.getContent().contains( SERVICE_URL ));
        assertNull( "No SOAP header", SoapUtil.getHeaderElement(doc) );
        assertEquals( "SOAP 1.2 Content Type", ContentTypeHeader.SOAP_1_2_DEFAULT, fault.getContentType() );
    }

    @Test
    public void testSoap11MediumFault() throws Exception {
        SoapFaultManager sfm = buildSoapFaultManager();
        SoapFaultLevel level = new SoapFaultLevel();
        level.setLevel(SoapFaultLevel.MEDIUM_DETAIL_FAULT);
        final SoapFaultManager.FaultResponse fault = sfm.constructReturningFault( level, getSoap11PEC(false) );
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault.getContent() );
        assertEquals( "Http Status", 500, fault.getHttpStatus() );
        assertEquals( "SOAP 1.1", SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, doc.getDocumentElement().getNamespaceURI() );
        assertFalse( "Policy version fault", fault.getContent().contains("Incorrect policy version"));
        assertTrue("Service URL in fault", fault.getContent().contains( SERVICE_URL ));
        assertNull( "No SOAP header", SoapUtil.getHeaderElement(doc) );
        assertEquals( "SOAP 1.1 Content Type", ContentTypeHeader.XML_DEFAULT, fault.getContentType() );
    }
    
    @Test
    public void testSoap12DetailFault() throws Exception {
        SoapFaultManager sfm = buildSoapFaultManager();
        SoapFaultLevel level = new SoapFaultLevel();
        level.setLevel(SoapFaultLevel.FULL_TRACE_FAULT);
        final SoapFaultManager.FaultResponse fault = sfm.constructReturningFault( level, getSoap12PEC(false) );
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault.getContent() );
        assertEquals( "Http Status", 500, fault.getHttpStatus() );
        assertEquals( "SOAP 1.2", SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, doc.getDocumentElement().getNamespaceURI() );
        assertFalse( "Policy version fault", fault.getContent().contains("Incorrect policy version"));
        assertTrue("Service URL in fault", fault.getContent().contains( SERVICE_URL ));
        assertNull( "No SOAP header", SoapUtil.getHeaderElement(doc) );
        assertEquals( "SOAP 1.2 Content Type", ContentTypeHeader.SOAP_1_2_DEFAULT, fault.getContentType() );
    }

    @Test
    public void testSoap11DetailFault() throws Exception {
        SoapFaultManager sfm = buildSoapFaultManager();
        SoapFaultLevel level = new SoapFaultLevel();
        level.setLevel(SoapFaultLevel.FULL_TRACE_FAULT);
        final SoapFaultManager.FaultResponse fault = sfm.constructReturningFault( level, getSoap11PEC(false) );
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault.getContent() );
        assertEquals( "Http Status", 500, fault.getHttpStatus() );
        assertEquals( "SOAP 1.1", SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, doc.getDocumentElement().getNamespaceURI() );
        assertFalse( "Policy version fault", fault.getContent().contains("Incorrect policy version"));
        assertTrue("Service URL in fault", fault.getContent().contains( SERVICE_URL ));
        assertNull( "No SOAP header", SoapUtil.getHeaderElement(doc) );
        assertEquals( "SOAP 1.1 Content Type", ContentTypeHeader.XML_DEFAULT, fault.getContentType() );
    }

    @Test
    public void testSoap12TemplateFault() throws Exception {
        SoapFaultManager sfm = buildSoapFaultManager();
        SoapFaultLevel level = new SoapFaultLevel();
        level.setLevel(SoapFaultLevel.TEMPLATE_FAULT);
        level.setFaultTemplate( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<soapenv:Envelope xmlns:xml=\"" + XMLConstants.XML_NS_URI + "\" xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE + "\">\n" +
                                "    <soapenv:Body>\n" +
                                "        <soapenv:Fault>\n" +
                                "            <soapenv:Code>\n" +
                                "                <soapenv:Value>YOUR_FAULT_CODE</soapenv:Value>\n" +
                                "            </soapenv:Code>\n" +
                                "            <soapenv:Reason>\n" +
                                "                <soapenv:Text xml:lang=\"en-US\">YOUR_FAULT_STRING</soapenv:Text>\n" +
                                "            </soapenv:Reason>\n" +
                                "            <soapenv:Role>YOUR_FAULT_ROLE</soapenv:Role>\n" +
                                "            <soapenv:Detail>\n" +
                                "               http://myservicehost/myservice\n" +
                                "            </soapenv:Detail>\n" +
                                "        </soapenv:Fault>\n" +
                                "    </soapenv:Body>\n" +
                                "</soapenv:Envelope>" );
        final SoapFaultManager.FaultResponse fault = sfm.constructReturningFault( level, getSoap12PEC(false) );
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault.getContent() );
        assertEquals( "Http Status", 500, fault.getHttpStatus() );
        assertEquals( "SOAP 1.2", SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, doc.getDocumentElement().getNamespaceURI() );
        assertFalse( "Policy version fault", fault.getContent().contains("Incorrect policy version"));
        assertTrue("Service URL in fault", fault.getContent().contains( SERVICE_URL ));
        assertNull( "No SOAP header", SoapUtil.getHeaderElement(doc) );
        assertEquals( "SOAP 1.2 Content Type", ContentTypeHeader.SOAP_1_2_DEFAULT, fault.getContentType() );
    }

    @Test
    public void testSoap11TemplateFault() throws Exception {
        SoapFaultManager sfm = buildSoapFaultManager();
        SoapFaultLevel level = new SoapFaultLevel();
        level.setLevel(SoapFaultLevel.TEMPLATE_FAULT);
        level.setFaultTemplate( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                                "    <soapenv:Body>\n" +
                                "        <soapenv:Fault>\n" +
                                "            <faultcode>YOUR_FAULT_CODE</faultcode>\n" +
                                "            <faultstring>YOUR_FAULT_STRING</faultstring>\n" +
                                "            <faultactor>http://myservicehost/myservice</faultactor>\n" +
                                "            <detail>YOUR_FAULT_DETAIL</detail>\n" +
                                "        </soapenv:Fault>\n" +
                                "    </soapenv:Body>\n" +
                                "</soapenv:Envelope>" );
        final SoapFaultManager.FaultResponse fault = sfm.constructReturningFault( level, getSoap11PEC(false) );
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault.getContent() );
        assertEquals( "Http Status", 500, fault.getHttpStatus() );
        assertEquals( "SOAP 1.1", SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, doc.getDocumentElement().getNamespaceURI() );
        assertFalse( "Policy version fault", fault.getContent().contains("Incorrect policy version"));
        assertTrue("Service URL in fault", fault.getContent().contains( SERVICE_URL ));
        assertNull( "No SOAP header", SoapUtil.getHeaderElement(doc) );
        assertEquals( "SOAP 1.1 Content Type", ContentTypeHeader.XML_DEFAULT, fault.getContentType() );
    }

    @Test
    public void testNonSoapTemplateFault() throws Exception {
        SoapFaultManager sfm = buildSoapFaultManager();
        SoapFaultLevel level = new SoapFaultLevel();
        level.setLevel(SoapFaultLevel.TEMPLATE_FAULT);
        level.setFaultTemplate( "Server Error" );
        level.setFaultTemplateHttpStatus( "555" );
        level.setFaultTemplateContentType( "text/plain" );       
        final SoapFaultManager.FaultResponse fault = sfm.constructReturningFault( level, getSoap11PEC(false) );
        System.out.println(fault);
        assertEquals( "Http Status", 555, fault.getHttpStatus() );
        assertEquals( "Text/Plain Content Type", ContentTypeHeader.create( "text/plain" ), fault.getContentType() );
        assertEquals( "Fault contents", "Server Error", fault.getContent() );
    }


    @Test
    public void testDefaultSignedSoapFault() throws Exception {
        SoapFaultManager sfm = buildSoapFaultManager(true, null);
        final SoapFaultManager.FaultResponse faultInfo = sfm.constructExceptionFault( constructException(), null, getSoap11PEC(true) );
        String fault = faultInfo.getContent();
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault );
        assertEquals( "Http Status", 500, faultInfo.getHttpStatus() );
        assertEquals( "SOAP 1.1", SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, doc.getDocumentElement().getNamespaceURI() );
        assertTrue("Service URL in fault", fault.contains( SERVICE_URL ));
        assertNotNull( "SOAP header", SoapUtil.getHeaderElement(doc) );
        assertNotNull( "Security header", SoapUtil.getSecurityElementForL7(doc) );
        assertTrue("Valid signature", isValidSignature(doc, getBobKey()));
        assertEquals( "SOAP 1.1 Content Type", ContentTypeHeader.XML_DEFAULT, faultInfo.getContentType() );
    }

    @Test
    public void testDefaultSignedSoap12Fault() throws Exception {
        SoapFaultManager sfm = buildSoapFaultManager(true, null);
        final SoapFaultManager.FaultResponse faultInfo = sfm.constructExceptionFault( constructException(), null, getSoap12PEC(true) );
        String fault = faultInfo.getContent();
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault );
        assertEquals( "Http Status", 500, faultInfo.getHttpStatus() );
        assertEquals( "SOAP 1.2", SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, doc.getDocumentElement().getNamespaceURI() );
        assertTrue("Service URL in fault", fault.contains( SERVICE_URL ));
        assertNotNull( "SOAP header", SoapUtil.getHeaderElement(doc) );
        assertNotNull( "Security header", SoapUtil.getSecurityElementForL7(doc) );
        assertTrue("Valid signature", isValidSignature(doc, getBobKey()));
        assertEquals( "SOAP 1.2 Content Type", ContentTypeHeader.SOAP_1_2_DEFAULT, faultInfo.getContentType() );
    }

    @Test
    public void testDefaultSignedSoapFaultWithPrivateKey() throws Exception {
        SoapFaultManager sfm = buildSoapFaultManager(true, "alice");
        final SoapFaultManager.FaultResponse faultInfo = sfm.constructExceptionFault( constructException(), null, getSoap11PEC(true) );
        String fault = faultInfo.getContent();
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault );
        assertEquals( "Http Status", 500, faultInfo.getHttpStatus() );
        assertTrue("Service URL in fault", fault.contains( SERVICE_URL ));
        assertNotNull( "SOAP header", SoapUtil.getHeaderElement(doc) );
        assertNotNull( "Security header", SoapUtil.getSecurityElementForL7(doc) );
        assertTrue("Valid signature", isValidSignature(doc, getAliceKey()));
        assertEquals( "SOAP 1.1 Content Type", ContentTypeHeader.XML_DEFAULT, faultInfo.getContentType() );
    }

    @Test
    public void testDefaultSignedSoapFaultWithInvalidPrivateKey() throws Exception {
        SoapFaultManager sfm = buildSoapFaultManager(true, "123111alice");
        final SoapFaultManager.FaultResponse faultInfo = sfm.constructExceptionFault( constructException(), null, getSoap11PEC(true) );
        String fault = faultInfo.getContent();
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault );
        assertEquals( "Http Status", 500, faultInfo.getHttpStatus() );
        assertTrue("Service URL in fault", fault.contains( SERVICE_URL ));
        assertNull( "SOAP header", SoapUtil.getHeaderElement(doc) );
        assertEquals( "SOAP 1.1 Content Type", ContentTypeHeader.XML_DEFAULT, faultInfo.getContentType() );
    }

    @Test
    public void testSignedSoapFault() throws Exception {
        SoapFaultManager sfm = buildSoapFaultManager();
        SoapFaultLevel level = new SoapFaultLevel();
        level.setLevel(SoapFaultLevel.GENERIC_FAULT);
        level.setSignSoapFault( true );
        final SoapFaultManager.FaultResponse fault = sfm.constructReturningFault( level, getSoap11PEC(false) );
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault.getContent() );
        assertEquals( "Http Status", 500, fault.getHttpStatus() );
        assertTrue("Service URL in fault", fault.getContent().contains( SERVICE_URL ));
        assertNotNull( "SOAP header", SoapUtil.getHeaderElement(doc) );
        assertNotNull( "Security header", SoapUtil.getSecurityElementForL7(doc) );
        assertTrue("Valid signature", isValidSignature(doc, getBobKey()));
        assertEquals( "SOAP 1.1 Content Type", ContentTypeHeader.XML_DEFAULT, fault.getContentType() );
   }

    @Test
    public void testSignedSoapFaultWithPrivateKey() throws Exception {
        SoapFaultManager sfm = buildSoapFaultManager();
        SoapFaultLevel level = new SoapFaultLevel();
        level.setLevel(SoapFaultLevel.GENERIC_FAULT);
        level.setSignSoapFault( true );
        level.setUsesDefaultKeyStore( false );
        level.setNonDefaultKeystoreId( -1 );
        level.setKeyAlias( "alice" );
        final SoapFaultManager.FaultResponse fault = sfm.constructReturningFault( level, getSoap11PEC(false) );
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault.getContent() );
        assertEquals( "Http Status", 500, fault.getHttpStatus() );
        assertTrue("Service URL in fault", fault.getContent().contains( SERVICE_URL ));
        assertNotNull( "SOAP header", SoapUtil.getHeaderElement(doc) );
        assertNotNull( "Security header", SoapUtil.getSecurityElementForL7(doc) );
        assertTrue("Valid signature", isValidSignature(doc, getAliceKey()));
        assertEquals( "SOAP 1.1 Content Type", ContentTypeHeader.XML_DEFAULT, fault.getContentType() );
    }

    @Test
    public void testSignedSoapFaultActor() throws Exception {
        SoapFaultManager sfm = buildSoapFaultManager();
        SoapFaultLevel level = new SoapFaultLevel();
        level.setLevel(SoapFaultLevel.GENERIC_FAULT);
        level.setSignSoapFault( true );

        PolicyEnforcementContext context = getSoap11PEC(false);
        context.getRequest().getSecurityKnob().setProcessorResult( new MockProcessorResult(){
            @Override
            public SecurityActor getProcessedActor() {
                return SecurityActor.L7ACTOR;
            }

            @Override
            public String getProcessedActorUri() {
                return TEST_ACTOR;
            }
        } );

        final SoapFaultManager.FaultResponse fault = sfm.constructReturningFault( level, context );
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault.getContent() );
        assertEquals( "Http Status", 500, fault.getHttpStatus() );
        assertTrue("Service URL in fault", fault.getContent().contains( SERVICE_URL ));
        assertNotNull( "SOAP header", SoapUtil.getHeaderElement(doc) );
        assertNotNull( "Security header", SoapUtil.getSecurityElement(doc, TEST_ACTOR) );
        assertTrue("Valid signature", isValidSignature(doc, getBobKey()));
        assertEquals( "SOAP 1.1 Content Type", ContentTypeHeader.XML_DEFAULT, fault.getContentType() );
    }

    @Test
    public void testSignedSoapFaultNoActor() throws Exception {
        SoapFaultManager sfm = buildSoapFaultManager();
        SoapFaultLevel level = new SoapFaultLevel();
        level.setLevel(SoapFaultLevel.GENERIC_FAULT);
        level.setSignSoapFault( true );

        PolicyEnforcementContext context = getSoap11PEC(false);
        context.getRequest().getSecurityKnob().setProcessorResult( new MockProcessorResult(){
            @Override
            public SecurityActor getProcessedActor() {
                return SecurityActor.NOACTOR;
            }

            @Override
            public String getProcessedActorUri() {
                return null;
            }
        } );

        final SoapFaultManager.FaultResponse fault = sfm.constructReturningFault( level, context );
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault.getContent() );
        assertEquals( "Http Status", 500, fault.getHttpStatus() );
        assertTrue("Service URL in fault", fault.getContent().contains( SERVICE_URL ));
        assertNotNull( "SOAP header", SoapUtil.getHeaderElement(doc) );
        assertNotNull( "Security header", SoapUtil.getSecurityElement(doc) );
        assertTrue("Valid signature", isValidSignature(doc, getBobKey()));
        assertEquals( "SOAP 1.1 Content Type", ContentTypeHeader.XML_DEFAULT, fault.getContentType() );
    }

    private SoapFaultManager buildSoapFaultManager() throws Exception {
        return buildSoapFaultManager(false, null, null);
    }

    private SoapFaultManager buildSoapFaultManager( final AuditContext auditContext ) throws Exception {
        return buildSoapFaultManager(false, null, auditContext);
    }

    private SoapFaultManager buildSoapFaultManager( final boolean sign, final String keyAlias ) throws Exception {
        return buildSoapFaultManager(sign, keyAlias, null);
    }

    private SoapFaultManager buildSoapFaultManager( final boolean sign, final String keyAlias, final AuditContext auditContext ) throws Exception {
        Properties props = new Properties();
        props.setProperty( "defaultfaultlevel", "2" );
        props.setProperty( "defaultfaultsign", Boolean.toString(sign) );
        if ( keyAlias != null ) {
            props.setProperty( "defaultfaultkeyalias", keyAlias );
        }
        SoapFaultManager sfm = new SoapFaultManager(new MockConfig(props), auditContext==null ? new AuditContextStub() : auditContext);
        sfm.setBeanFactory( new SimpleSingletonBeanFactory( new HashMap<String,Object>(){{
            put( "ssgKeyStoreManager", new SsgKeyStoreManagerStub(new SsgKeyFinderStub( Arrays.asList(getAliceKey()))) );
            put( "defaultKey", new TestDefaultKey( getBobKey() ) );
        }} ) );

        return sfm;
    }

    private SsgKeyEntry getAliceKey() throws Exception {
        return new SsgKeyEntry( -1, "alice", TestDocuments.getWssInteropAliceChain(), TestDocuments.getWssInteropAliceKey());
    }

    private SsgKeyEntry getBobKey() throws Exception {
        return new SsgKeyEntry( -1, "bob", TestDocuments.getWssInteropBobChain(), TestDocuments.getWssInteropBobKey());
    }

    private Exception constructException() {
        return new RuntimeException("Something went wrong");
    }

    private PolicyEnforcementContext getSoap12PEC( final boolean wrongPolicyVersion ) {
        PolicyEnforcementContext pec = new PolicyEnforcementContextWrapper( PolicyEnforcementContextFactory.createPolicyEnforcementContext( null, null ) ){
            @Override
            public Object getVariable(String name) throws NoSuchVariableException {
                return SERVICE_URL;
            }

            @Override
            public boolean isRequestClaimingWrongPolicyVersion() {
                return wrongPolicyVersion;
            }
        };

        PublishedService service = new PublishedService();
        service.parseWsdlStrategy( new ServiceDocumentWsdlStrategy(null) );
        service.setWsdlXml(WSDL_WAREHOUSE_SOAP12);
        pec.setService(service);

        assertEquals( "SOAP 1.2 service", SoapVersion.SOAP_1_2, service.getSoapVersion() );

        return pec;
    }

    private PolicyEnforcementContext getSoap11PEC( final boolean wrongPolicyVersion ) {
        return new PolicyEnforcementContextWrapper( PolicyEnforcementContextFactory.createPolicyEnforcementContext( null, null ) ){
            @Override
            public Object getVariable(String name) throws NoSuchVariableException {
                return SERVICE_URL;
            }

            @Override
            public boolean isRequestClaimingWrongPolicyVersion() {
                return wrongPolicyVersion;
            }
        };
    }

    private boolean isValidSignature( final Document document, final SsgKeyEntry key ) throws Exception {
        boolean valid = false;

        NodeList list = document.getElementsByTagNameNS( "http://www.w3.org/2000/09/xmldsig#", "Signature" );
        if ( list.getLength()==0 )
            System.out.println( "No signature!" );

        for ( int j=0; j<list.getLength(); j++ ) {
            Element signature = (Element) list.item( j );
            SignatureContext sigContext = new SignatureContext();
            sigContext.setIDResolver(new IDResolver() {
                @Override
                public Element resolveID(Document doc, String s) {
                    try {
                        return SoapUtil.getElementByWsuId( doc, s );
                    } catch (InvalidDocumentFormatException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            Validity validity = sigContext.verify( signature, key.getCertificate().getPublicKey() );
            if (!validity.getCoreValidity()) {
                StringBuilder msg = new StringBuilder("Signature not valid. " + validity.getSignedInfoMessage());
                for (int i = 0; i < validity.getNumberOfReferences(); i++) {
                    msg.append("\n\tElement ").append(validity.getReferenceURI(i)).append(": ").append(validity.getReferenceMessage(i));
                }
                System.out.println( msg.toString() );
                valid = false;
            } else if ( j==0 ) {
                valid = true;
            }
        }

        return valid;
    }

    private void addAuditInfo( final AuditContextStub stub, final PolicyEnforcementContext context ) {
        TestServerAssertion ass1 = new TestServerAssertion(new HttpBasic());
        TestServerAssertion ass2 = new TestServerAssertion(new HttpBasic());
        TestServerAssertion ass3 = new TestServerAssertion(new SpecificUser());

        context.assertionFinished( ass1, AssertionStatus.NONE );
        context.assertionFinished( ass2, AssertionStatus.NONE );
        context.assertionFinished( ass3, AssertionStatus.FALSIFIED );

        stub.addDetail( new AuditDetail( AssertionMessages.HTTPCREDS_NA_AUTHN_HEADER ), ass1 );
        stub.addDetail( new AuditDetail( AssertionMessages.HTTPCREDS_FOUND_USER ), ass2 );
        stub.addDetail( new AuditDetail( AssertionMessages.IDENTITY_NO_CREDS ), ass3 );
    }

    private static final class TestServerAssertion implements ServerAssertion {
        private final Assertion assertion;

        TestServerAssertion( final Assertion assertion ) {
            this.assertion = assertion;
        }

        @Override
        public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
            return null;
        }

        @Override
        public Assertion getAssertion() {
            return assertion;
        }

        @Override
        public void close() throws IOException {
        }
    }

    private static final String SERVICE_URL = "http://myservicehost/myservice";
    private static final String TEST_ACTOR = "http://tempuri.org/someactor";
    private static final String WSDL_WAREHOUSE_SOAP12 =
            "<wsdl:definitions name=\"Warehouse\" targetNamespace=\"http://warehouse.acme.com/ws\" xmlns:http=\"http://schemas.xmlsoap.org/wsdl/http/\" xmlns:mime=\"http://schemas.xmlsoap.org/wsdl/mime/\" xmlns:s=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://schemas.xmlsoap.org/wsdl/soap12/\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:tm=\"http://microsoft.com/wsdl/mime/textMatching/\" xmlns:tns=\"http://warehouse.acme.com/ws\" xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\">\n" +
            "    <wsdl:types>\n" +
            "        <s:schema elementFormDefault=\"qualified\" targetNamespace=\"http://warehouse.acme.com/ws\">\n" +
            "            <s:element name=\"listProducts\">\n" +
            "                <s:complexType>\n" +
            "                    <s:sequence>\n" +
            "                        <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"delay\" type=\"s:int\"/>\n" +
            "                    </s:sequence>\n" +
            "                </s:complexType>\n" +
            "            </s:element>\n" +
            "            <s:element name=\"listProductsResponse\">\n" +
            "                <s:complexType>\n" +
            "                    <s:sequence>\n" +
            "                        <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"listProductsResult\" type=\"tns:ArrayOfProductListHeader\"/>\n" +
            "                    </s:sequence>\n" +
            "                </s:complexType>\n" +
            "            </s:element>\n" +
            "            <s:complexType name=\"ArrayOfProductListHeader\">\n" +
            "                <s:sequence>\n" +
            "                    <s:element maxOccurs=\"unbounded\" minOccurs=\"0\" name=\"ProductListHeader\" nillable=\"true\" type=\"tns:ProductListHeader\"/>\n" +
            "                </s:sequence>\n" +
            "            </s:complexType>\n" +
            "            <s:complexType name=\"ProductListHeader\">\n" +
            "                <s:sequence>\n" +
            "                    <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"productName\" type=\"s:string\"/>\n" +
            "                    <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"productId\" type=\"s:long\"/>\n" +
            "                </s:sequence>\n" +
            "            </s:complexType>\n" +
            "        </s:schema>\n" +
            "    </wsdl:types>\n" +
            "    <wsdl:message name=\"listProductsSoapIn\">\n" +
            "        <wsdl:part element=\"tns:listProducts\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"listProductsSoapOut\">\n" +
            "        <wsdl:part element=\"tns:listProductsResponse\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:portType name=\"WarehouseSoap\">\n" +
            "        <wsdl:operation name=\"listProducts\">\n" +
            "            <wsdl:input message=\"tns:listProductsSoapIn\"/>\n" +
            "            <wsdl:output message=\"tns:listProductsSoapOut\"/>\n" +
            "        </wsdl:operation>\n" +
            "    </wsdl:portType>\n" +
            "    <wsdl:binding name=\"WarehouseSoap12\" type=\"tns:WarehouseSoap\">\n" +
            "        <soap12:binding transport=\"http://schemas.xmlsoap.org/soap/http\"/>\n" +
            "        <wsdl:operation name=\"listProducts\">\n" +
            "            <soap12:operation soapAction=\"http://warehouse.acme.com/ws/listProducts\" style=\"document\"/>\n" +
            "            <wsdl:input>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:input>\n" +
            "            <wsdl:output>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:output>\n" +
            "        </wsdl:operation>\n" +
            "    </wsdl:binding>\n" +
            "    <wsdl:service name=\"Warehouse\">\n" +
            "        <wsdl:port binding=\"tns:WarehouseSoap12\" name=\"WarehouseSoap12\">\n" +
            "            <soap12:address location=\"http://www.layer7tech.com:8888/WarehouseService.asmx\"/>\n" +
            "        </wsdl:port>\n" +
            "    </wsdl:service>\n" +
            "</wsdl:definitions>";
}
