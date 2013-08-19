package com.l7tech.server.util;

import com.ibm.xml.dsig.IDResolver;
import com.ibm.xml.dsig.SignatureContext;
import com.ibm.xml.dsig.Validity;
import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.objectmodel.Goid;
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
import com.l7tech.server.audit.AuditContextFactoryStub;
import com.l7tech.server.audit.AuditContextStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.message.PolicyEnforcementContextWrapper;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.security.keystore.SsgKeyFinderStub;
import com.l7tech.server.security.keystore.SsgKeyStoreManagerStub;
import com.l7tech.test.BugNumber;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.MockConfig;
import com.l7tech.util.NameValuePair;
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
    public void testSoap12MediumDetailExceptionFaultWithExtraHeaders() throws Exception {
        AuditContextStub stub = new AuditContextStub();
        PolicyEnforcementContext context = getSoap12PEC(false);
        addAuditInfo( stub, context );
        SoapFaultManager sfm = buildSoapFaultManager( stub );
        SoapFaultLevel level = new SoapFaultLevel();
        level.setLevel(SoapFaultLevel.MEDIUM_DETAIL_FAULT);
        level.setExtraHeaders(new NameValuePair[] { new NameValuePair("Custom-Header-${hname}", "val-${hvalue}") } );
        final SoapFaultManager.FaultResponse faultInfo = sfm.constructExceptionFault( constructException(), level, context );
        String fault = faultInfo.getContent();
        System.out.println(fault);
        Document doc = XmlUtil.parse( fault );
        assertEquals( "Http Status", 500, faultInfo.getHttpStatus() );
        assertEquals( "SOAP 1.2", SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, doc.getDocumentElement().getNamespaceURI() );
        assertFalse("Policy version fault", fault.contains("Incorrect policy version"));
        assertTrue("Service URL in fault", fault.contains(SERVICE_URL));
        assertNull("No SOAP header", SoapUtil.getHeaderElement(doc));
        assertEquals("SOAP 1.2 Content Type", ContentTypeHeader.SOAP_1_2_DEFAULT, faultInfo.getContentType());
        assertTrue("Contains medium detail", fault.contains("No credentials found!"));
        assertFalse("Contains full detail", fault.contains("Authorization header not applicable for this assertion"));
        assertEquals("Contains extra header", 1, faultInfo.getExtraHeaders().size());
        assertEquals("Contains correct header name", "Custom-Header-HeadName", faultInfo.getExtraHeaders().get(0).getKey());
        assertEquals("Contains correct header value", "val-HeadValue", faultInfo.getExtraHeaders().get(0).getValue());
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
    public void testSoap11DetailExceptionFaultWithExtraHeaders() throws Exception {
        AuditContextStub stub = new AuditContextStub();
        PolicyEnforcementContext context = getSoap11PEC(false);
        SoapFaultManager sfm = buildSoapFaultManager(stub);
        addAuditInfo( stub, context );
        SoapFaultLevel level = new SoapFaultLevel();
        level.setLevel(SoapFaultLevel.FULL_TRACE_FAULT);
        level.setExtraHeaders(new NameValuePair[] {
                new NameValuePair("Custom-Header-${hname}", "val-${hvalue}"),
                new NameValuePair("Extra-Two", "blah"),
                new NameValuePair("Extra-Two", "floo-${hvalue}")
        });
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
        assertEquals("Contains extra headers", 3, faultInfo.getExtraHeaders().size());
        assertEquals("Contains correct header name 0", "Custom-Header-HeadName", faultInfo.getExtraHeaders().get(0).getKey());
        assertEquals("Contains correct header value 0", "val-HeadValue", faultInfo.getExtraHeaders().get(0).getValue());
        assertEquals("Contains correct header name 1", "Extra-Two", faultInfo.getExtraHeaders().get(1).getKey());
        assertEquals("Contains correct header value 1", "blah", faultInfo.getExtraHeaders().get(1).getValue());
        assertEquals("Contains correct header name 2", "Extra-Two", faultInfo.getExtraHeaders().get(2).getKey());
        assertEquals("Contains correct header value 2", "floo-HeadValue", faultInfo.getExtraHeaders().get(2).getValue());
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
        assertTrue( "Text/Plain Content Type", ContentTypeHeader.create( "text/plain" ).matches(fault.getContentType()) );
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
        level.setNonDefaultKeystoreId( new Goid(0,-1) );
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

    /* Bug 9402 additional error detail for SOAP 1.1
     * Malformed XML and Bad WS-S Dig Signature exceptions should
     * return for FULL Detail and Medium Detail
     */
    @Test
    public void testSoap11FullDetailExceptionExtraInfo() throws Exception {
           AuditContextStub stub = new AuditContextStub();
           PolicyEnforcementContext context = getSoap11PEC(false);
           SoapFaultManager sfm = buildSoapFaultManager(stub);
           addAuditInfoExtraDetail( stub );
           SoapFaultLevel level = new SoapFaultLevel();
           level.setLevel(SoapFaultLevel.FULL_TRACE_FAULT);
           // ** new method for bug 9402
           level.setAlwaysReturnSoapFault(true);
           context.setFaultlevel(level);

           final SoapFaultManager.FaultResponse faultInfo = sfm.constructExceptionFault( constructException(), level, context );
           String fault = faultInfo.getContent();
           NodeList elements = getL7AddInfoNodeElements(fault);

           assertTrue("contains l7:additionalInfo element", elements.getLength() > 0);
           for(int i = 0;i < elements.getLength();i++) {
                Element element = (Element)elements.item(i);
                NodeList detailElements = element.getElementsByTagName("l7:detailMessage");
                assertNotNull(detailElements);
                if (detailElements != null){
                    for(int j = 0;j < detailElements.getLength();j++) {
                        Element detailElement = (Element)detailElements.item(j);
                        assertNotNull(detailElement);
                        assertTrue(detailElement.getAttribute("id").contains( "3017" ) || detailElement.getAttribute("id").contains( "3022" ) || detailElement.getAttribute("id").contains( "3025" ));
                    }

                }
            }


           assertEquals( "Http Status", 500, faultInfo.getHttpStatus() );
           assertEquals( "SOAP 1.1 Content Type", ContentTypeHeader.XML_DEFAULT, faultInfo.getContentType() );
    }

    /* Bug 9402 additional error detail for SOAP 1.2
     * Malformed XML and Bad WS-S Dig Signature exceptions should
     * return for FULL Detail and Medium Detail
     */
    @Test
    public void testSoap12FullDetailExceptionExtraInfo() throws Exception {
           AuditContextStub stub = new AuditContextStub();
           PolicyEnforcementContext context = getSoap12PEC(false);
           SoapFaultManager sfm = buildSoapFaultManager(stub);
           addAuditInfoExtraDetail( stub );
           SoapFaultLevel level = new SoapFaultLevel();
           level.setLevel(SoapFaultLevel.FULL_TRACE_FAULT);
           // ** new method for bug 9402
           level.setAlwaysReturnSoapFault(true);
           context.setFaultlevel(level);

           final SoapFaultManager.FaultResponse faultInfo = sfm.constructExceptionFault( constructException(), level, context );
           String fault = faultInfo.getContent();
           NodeList elements = getL7AddInfoNodeElements(fault);
           assertTrue("contains l7:additionalInfo element", elements.getLength() > 0);
           for(int i = 0;i < elements.getLength();i++) {
                Element element = (Element)elements.item(i);
                NodeList detailElements = element.getElementsByTagName("l7:detailMessage");
                assertNotNull(detailElements);
                if (detailElements != null){
                    for(int j = 0;j < detailElements.getLength();j++) {
                        Element detailElement = (Element)detailElements.item(j);
                        assertNotNull(detailElement);
                        assertTrue(detailElement.getAttribute("id").contains( "3017" ) || detailElement.getAttribute("id").contains( "3022" ) || detailElement.getAttribute("id").contains( "3025" ));
                    }

                }
            }
           assertEquals( "Http Status", 500, faultInfo.getHttpStatus() );
           assertEquals( "SOAP 1.2 Content Type", ContentTypeHeader.SOAP_1_2_DEFAULT, faultInfo.getContentType() );
    }


    @Test
    @BugNumber(12217)
    public void testFullDetailFlushedAuditContext() throws Exception {
        AuditContextStub stub = new AuditContextStub();
        PolicyEnforcementContext context = getSoap11PEC(false);
        SoapFaultManager sfm = buildSoapFaultManager(stub);
        addAuditInfoExtraDetail(stub);
        SoapFaultLevel level = new SoapFaultLevel();
        level.setLevel(SoapFaultLevel.FULL_TRACE_FAULT);
        level.setAlwaysReturnSoapFault(true);
        context.setFaultlevel(level);

        // Simulate audit context flush and pop from current context stack
        context.setAuditContext(stub);
        AuditContextFactoryStub.setCurrent(new AuditContextStub());

        final SoapFaultManager.FaultResponse faultInfo = sfm.constructExceptionFault(constructException(), level, context);
        String fault = faultInfo.getContent();
        NodeList elements = getL7AddInfoNodeElements(fault);
        assertTrue("contains l7:additionalInfo element", elements.getLength() > 0);
        for(int i = 0;i < elements.getLength();i++) {
            Element element = (Element)elements.item(i);
            NodeList detailElements = element.getElementsByTagName("l7:detailMessage");
            assertNotNull(detailElements);
            assertTrue(detailElements.getLength() > 0);
            for(int j = 0;j < detailElements.getLength();j++) {
                Element detailElement = (Element)detailElements.item(j);
                assertNotNull(detailElement);
                assertTrue(detailElement.getAttribute("id").contains( "3017" ) || detailElement.getAttribute("id").contains( "3022" ) || detailElement.getAttribute("id").contains( "3025" ));
            }

        }
        assertEquals( "Http Status", 500, faultInfo.getHttpStatus() );
    }

    /* Bug 9402 additional error detail for SOAP 1.1
     * Malformed XML and Bad WS-S Dig Signature exceptions should
     * NOT return for Generic Fault Detail
     */
    @Test
    public void testSoap11GenericDetailExceptionExtraInfo() throws Exception {
           AuditContextStub stub = new AuditContextStub();
           PolicyEnforcementContext context = getSoap11PEC(false);
           SoapFaultManager sfm = buildSoapFaultManager(stub);
           addAuditInfoExtraDetail( stub );
           SoapFaultLevel level = new SoapFaultLevel();
           level.setLevel(SoapFaultLevel.GENERIC_FAULT);
           // ** new method for bug 9402
           level.setAlwaysReturnSoapFault(true);
           context.setFaultlevel(level);

           final SoapFaultManager.FaultResponse faultInfo = sfm.constructExceptionFault( constructException(), level, context );
           String fault = faultInfo.getContent();
           NodeList elements = getL7AddInfoNodeElements(fault);
           assertFalse("doesn't contain l7:additionalInfo element", elements.getLength() > 0);
           assertEquals( "Http Status", 500, faultInfo.getHttpStatus() );
           assertEquals( "SOAP 1.1 Content Type", ContentTypeHeader.XML_DEFAULT, faultInfo.getContentType() );
    }

    /* Bug 9402 additional error detail for SOAP 1.2
     * Malformed XML and Bad WS-S Dig Signature exceptions should
     * NOT return for Generic Fault Detail
     */
    @Test
    public void testSoap12GenericDetailExceptionExtraInfo() throws Exception {
           AuditContextStub stub = new AuditContextStub();
           PolicyEnforcementContext context = getSoap12PEC(false);
           SoapFaultManager sfm = buildSoapFaultManager(stub);
           addAuditInfoExtraDetail( stub );
           SoapFaultLevel level = new SoapFaultLevel();
           level.setLevel(SoapFaultLevel.GENERIC_FAULT);
           // ** new method for bug 9402
           level.setAlwaysReturnSoapFault(true);
           context.setFaultlevel(level);

           final SoapFaultManager.FaultResponse faultInfo = sfm.constructExceptionFault( constructException(), level, context );
           String fault = faultInfo.getContent();
           System.out.println(fault);
            NodeList elements = getL7AddInfoNodeElements(fault);
           assertFalse("doesn't contain l7:additionalInfo element", elements.getLength() > 0);
           assertEquals( "Http Status", 500, faultInfo.getHttpStatus() );
           assertEquals( "SOAP 1.2 Content Type", ContentTypeHeader.SOAP_1_2_DEFAULT, faultInfo.getContentType() );
    }

    /* Bug 9402 additional error detail for SOAP 1.2
     * Malformed XML and Bad WS-S Dig Signature exceptions should
     * NOT return for Generic Fault Detail
     */
    @Test
    public void testSoap12MediumDetailNoExtraInfo() throws Exception {
           AuditContextStub stub = new AuditContextStub();
           PolicyEnforcementContext context = getSoap12PEC(false);
           SoapFaultManager sfm = buildSoapFaultManager(stub);
           addAuditInfoExtraDetail( stub );
           SoapFaultLevel level = new SoapFaultLevel();
           level.setLevel(SoapFaultLevel.MEDIUM_DETAIL_FAULT);
           // ** new method for bug 9402
           level.setAlwaysReturnSoapFault(false);
           context.setFaultlevel(level);

           final SoapFaultManager.FaultResponse faultInfo = sfm.constructExceptionFault( constructException(), level, context );
           String fault = faultInfo.getContent();
           NodeList elements = getL7AddInfoNodeElements(fault);
           assertFalse("doesn't contain l7:additionalInfo element", elements.getLength() > 0);
           assertEquals( "Http Status", 500, faultInfo.getHttpStatus() );
           assertEquals( "SOAP 1.2 Content Type", ContentTypeHeader.SOAP_1_2_DEFAULT, faultInfo.getContentType() );
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
        System.out.println(fault.getContent());
        Document doc = XmlUtil.parse( fault.getContent() );
        assertEquals( "Http Status", 500, fault.getHttpStatus() );
        assertTrue("Service URL in fault", fault.getContent().contains( SERVICE_URL ));
        assertNotNull( "SOAP header", SoapUtil.getHeaderElement(doc) );
        assertNotNull( "Security header", SoapUtil.getSecurityElement(doc) );
        assertTrue("Valid signature", isValidSignature(doc, getBobKey()));
        assertEquals( "SOAP 1.1 Content Type", ContentTypeHeader.XML_DEFAULT, fault.getContentType() );
    }

    @Test
    public void testGeneralFault() throws Exception {
        final SoapFaultManager sfm = buildSoapFaultManager();

        final SoapFaultManager.FaultResponse fault = sfm.constructFault( false, "http://actor", true, "faultstringtext" );
        System.out.println(fault.getContent());
        assertEquals( "Http Status", 500, fault.getHttpStatus() );
        assertEquals( "SOAP 1.1 Content Type", ContentTypeHeader.XML_DEFAULT, fault.getContentType() );
        assertTrue("Actor in fault", fault.getContent().contains( "http://actor"));
        assertTrue("Is client fault", fault.getContent().contains( "soapenv:Client"));
        assertTrue("Faultstring in falut", fault.getContent().contains( "faultstringtext"));

        final SoapFaultManager.FaultResponse fault12 = sfm.constructFault( true, "http://actor", true, "faultstringtext" );
        System.out.println(fault12.getContent());
        assertEquals( "Http Status", 500, fault12.getHttpStatus() );
        assertEquals( "SOAP 1.2 Content Type", ContentTypeHeader.SOAP_1_2_DEFAULT, fault12.getContentType() );
        assertTrue("Actor in fault", fault12.getContent().contains( "http://actor"));
        assertTrue("Is client fault", fault12.getContent().contains( "soapenv:Sender"));
        assertTrue("Faultstring in falut", fault12.getContent().contains( "faultstringtext"));
    }

    private NodeList getL7AddInfoNodeElements(String fault) throws Exception {
           Document doc = XmlUtil.parse( fault );
           return doc.getElementsByTagName("l7:additionalInfo");
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
        AuditContextFactoryStub.setCurrent((AuditContextStub) auditContext);
        Properties props = new Properties();
        props.setProperty( "defaultfaultlevel", "2" );
        props.setProperty( "defaultfaultsign", Boolean.toString(sign) );
        if ( keyAlias != null ) {
            props.setProperty( "defaultfaultkeyalias", keyAlias );
        }
        SoapFaultManager sfm = new SoapFaultManager(new MockConfig(props));
        sfm.setBeanFactory( new SimpleSingletonBeanFactory( new HashMap<String,Object>(){{
            put( "ssgKeyStoreManager", new SsgKeyStoreManagerStub(new SsgKeyFinderStub( Arrays.asList(getAliceKey()))) );
            put( "defaultKey", new TestDefaultKey( getBobKey() ) );
            put( "auditFactory", LoggingAudit.factory() );
        }} ) );

        return sfm;
    }

    private SsgKeyEntry getAliceKey() throws Exception {
        return new SsgKeyEntry( new Goid(0,-1), "alice", TestDocuments.getWssInteropAliceChain(), TestDocuments.getWssInteropAliceKey());
    }

    private SsgKeyEntry getBobKey() throws Exception {
        return new SsgKeyEntry( new Goid(0,-1), "bob", TestDocuments.getWssInteropBobChain(), TestDocuments.getWssInteropBobKey());
    }

    private Exception constructException() {
        return new RuntimeException("Something went wrong");
    }

    private PolicyEnforcementContext getSoap12PEC( final boolean wrongPolicyVersion ) {
        PolicyEnforcementContext pec = getSoap11PEC(wrongPolicyVersion);

        PublishedService service = new PublishedService();
        service.parseWsdlStrategy( new ServiceDocumentWsdlStrategy(null) );
        service.setWsdlXml(WSDL_WAREHOUSE_SOAP12);
        pec.setService(service);

        assertEquals( "SOAP 1.2 service", SoapVersion.SOAP_1_2, service.getSoapVersion() );

        return pec;
    }

    private PolicyEnforcementContext getSoap11PEC( final boolean wrongPolicyVersion ) {
        PolicyEnforcementContext pec = new PolicyEnforcementContextWrapper( PolicyEnforcementContextFactory.createPolicyEnforcementContext( null, null ) ){
            @Override
            public Object getVariable(String name) throws NoSuchVariableException {
                if ("request.url".equalsIgnoreCase(name))
                    return SERVICE_URL;
                else
                    return super.getVariable(name);
            }

            @Override
            public boolean isRequestClaimingWrongPolicyVersion() {
                return wrongPolicyVersion;
            }
        };
        pec.setVariable("hname", "HeadName");
        pec.setVariable("hvalue", "HeadValue");
        return pec;
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

    // *** Bug 9402 additional error detail for SOAP
    private void addAuditInfoExtraDetail( final AuditContextStub stub ) {
        // second AuditDetail param as null so that the message is not associated with an assertion result but instead an l7 information detail result
        stub.addDetail( new AuditDetail( MessageProcessingMessages.INVALID_REQUEST_WITH_DETAIL, "XML document structures must start and end within the same entity."), this );
        stub.addDetail( new AuditDetail( MessageProcessingMessages.ERROR_WSS_SIGNATURE, "Signature not valid."), this );
        stub.addDetail( new AuditDetail( MessageProcessingMessages.POLICY_EVALUATION_RESULT, "Warehouse [1769472]", "400", "Bad Request" ), this );
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
