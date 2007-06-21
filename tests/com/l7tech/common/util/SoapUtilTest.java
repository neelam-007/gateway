package com.l7tech.common.util;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.Wsdl;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;

import javax.wsdl.BindingOperation;
import javax.wsdl.Operation;
import java.io.IOException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.Map;

/**
 * For testing stuff in SoapUtil class
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Mar 23, 2006<br/>
 */
public class SoapUtilTest extends TestCase {
    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(SoapUtilTest.class);
    }

    public void testGetOperationRPC() throws Exception {
        Document soapdoc = TestDocuments.getTestDocument(TestDocuments.DIR + "PlaceOrder_cleartext.xml");
        Document wsdldoc = TestDocuments.getTestDocument(TestDocuments.DIR + "AxisWarehouse.wsdl");
        Message msg = makeMessage(soapdoc, "");
        Wsdl wsdl = Wsdl.newInstance(null, wsdldoc);
        Operation op = SoapUtil.getOperation(wsdl, msg);
        assertFalse(op == null);
        assertTrue(op.getName().equals("placeOrder"));
        // make sure this returns null when it needs to (totally unrelated request)
        soapdoc = TestDocuments.getTestDocument(TestDocuments.DIR + "GetLastTradePriceSoapRequest.xml");
        msg = makeMessage(soapdoc, "");
        op = SoapUtil.getOperation(wsdl, msg);
        assertTrue(op == null);
    }

    public void testGetOperationDOC() throws Exception {
        Document soapdoc = TestDocuments.getTestDocument(TestDocuments.DIR + "dotNetSignedSoapRequest.xml");
        Document wsdldoc = TestDocuments.getTestDocument("com/l7tech/server/policy/assertion/xml/warehouse.wsdl");
        Message msg = makeMessage(soapdoc, "http://warehouse.acme.com/ws/listProducts");
        Wsdl wsdl = Wsdl.newInstance(null, wsdldoc);
        Operation op = SoapUtil.getOperation(wsdl, msg);
        assertFalse(op == null);
        assertTrue(op.getName().equals("listProducts"));
        // in this case, the operation should be identifiable even if the soapaction is incorrect
        msg = makeMessage(soapdoc, ":foo:bar");
        op = SoapUtil.getOperation(wsdl, msg);
        assertFalse(op == null);
        assertTrue(op.getName().equals("listProducts"));
    }

    public void testBugzilla2304() throws Exception {
        Document soapdoc = TestDocuments.getTestDocument(TestDocuments.DIR + "facadeAddService.xml");
        Document wsdldoc = TestDocuments.getTestDocument(TestDocuments.DIR + "bugzilla2304.wsdl");
        Message msg = makeMessage(soapdoc, "http://systinet.com/j2ee/ejb/ServiceFacade#addService?KExjYS9iYy9nb3YvYWcvY3Nvd3Mvc2VydmljZXMvU2VydmljZTspTGNhL2JjL2dvdi9hZy9jc293cy9zZXJ2aWNlcy9TZXJ2aWNlOw==");
        Wsdl wsdl = Wsdl.newInstance(null, wsdldoc);
        Operation op = SoapUtil.getOperation(wsdl, msg);
        assertFalse(op == null);
        assertTrue(op.getName().equals("addService"));
        // same request with different soapaction should yield different operation
        msg = makeMessage(soapdoc, "http://systinet.com/j2ee/ejb/ServiceFacade#updateService?KExjYS9iYy9nb3YvYWcvY3Nvd3Mvc2VydmljZXMvU2VydmljZTspVg==");
        op = SoapUtil.getOperation(wsdl, msg);
        assertFalse(op == null);
        assertTrue(op.getName().equals("updateService"));
        // without the soapaction, this should yield an ambiguity
        msg = makeMessage(soapdoc, "");
        op = SoapUtil.getOperation(wsdl, msg);
        assertTrue(op == null);
    }

    public void testBug3250_QA_Wsdl() throws Exception {
        Wsdl w = Wsdl.newInstance(TestDocuments.DIR, TestDocuments.getTestDocument(TestDocuments.DIR + "bug3250.wsdl"));
        for (Object o : w.getBindingOperations()) {
            BindingOperation bop = (BindingOperation) o;
            System.out.println("Got target namespace for " + bop.getName() + ": " + SoapUtil.findTargetNamespace(w.getDefinition(), bop));
        }
    }

    public void testBug3250_BofA_Wsdl() throws Exception {
        Wsdl w = Wsdl.newInstance(TestDocuments.DIR, TestDocuments.getTestDocument(TestDocuments.DIR + "AuthenticateServiceV001.wsdl"));
        for (Object o : w.getBindingOperations()) {
            BindingOperation bop = (BindingOperation) o;
            System.out.println("Got target namespace for " + bop.getName() + ": " + SoapUtil.findTargetNamespace(w.getDefinition(), bop));
        }
    }

    public void testBug3888_IsSoapRejectsProcessingInstructions() throws Exception {
        Document doc = XmlUtil.stringToDocument(SOAP_MESSAGE_WITH_PROCESSING_INSTRUCTION_BEFORE_CONTENT);
        assertFalse(SoapUtil.isSoapMessage(doc));

        // TODO find a fast way to fix it so it fails on PIs hidden within the body as well
        doc = XmlUtil.stringToDocument(SOAP_MESSAGE_WITH_PROCESSING_INSTRUCTION_IN_BODY);
        assertTrue(SoapUtil.isSoapMessage(doc));
    }

    public static final String SOAP_MESSAGE_WITH_PROCESSING_INSTRUCTION_BEFORE_CONTENT =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<?xml-stylesheet type=\"text/xsl\"\n" +
            "href=\"http://hugh.l7tech.com/xsl/harmless.xsl\"?>\n" +
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "<soap:Body>\n" +
            "  <placeOrder xmlns=\"http://warehouse.acme.com/ws\">\n" +
            "    <productid>111111114</productid>\n" +
            "      <amount>1</amount>\n" +
            "      <price>1230</price>\n" +
            "      <accountid>997</accountid>\n" +
            "  </placeOrder>\n" +
            "</soap:Body>\n" +
            "</soap:Envelope>";

    public static final String SOAP_MESSAGE_WITH_PROCESSING_INSTRUCTION_IN_BODY =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "<soap:Body>\n" +
            "  <placeOrder xmlns=\"http://warehouse.acme.com/ws\">\n" +
            "    <productid>111111114</productid>\n" +
            "<?xml-stylesheet type=\"text/xsl\"\n" +
            "href=\"http://hugh.l7tech.com/xsl/harmless.xsl\"?>\n" +
            "      <amount>1</amount>\n" +
            "      <price>1230</price>\n" +
            "      <accountid>997</accountid>\n" +
            "  </placeOrder>\n" +
            "</soap:Body>\n" +
            "</soap:Envelope>";


    private Message makeMessage(final Document doc, final String saction) {
        // produce fake message with arguments
        Message output = new Message(doc);
        output.attachHttpRequestKnob(new HttpRequestKnob() {
            public String getHeaderSingleValue(String name) throws IOException {
                if (name.equals(SoapUtil.SOAPACTION)) {
                    return saction;
                }
                return null;
            }

            public String getSoapAction() {
                return saction;
            }
            public HttpCookie[] getCookies() {return new HttpCookie[0];}
            public String getMethod() {return "POST";}
            public String getRequestUri() {return null;}
            public String getRequestUrl() {return null;}
            public URL getRequestURL() {return null;}
            public long getDateHeader(String name) throws ParseException {return 0;}
            public int getIntHeader(String name){return -1;}
            public String[] getHeaderNames() {return new String[0];}
            public String[] getHeaderValues(String name) {return new String[0];}
            public X509Certificate[] getClientCertificate() throws IOException {return new X509Certificate[0];}
            public boolean isSecure() {return false;}
            public String getParameter(String name) {return null;}
            public Map getParameterMap() {return null;}
            public String[] getParameterValues(String s) {return new String[0];}
            public Enumeration getParameterNames() {return null;}
            public String getQueryString() {return null;}
            public String getRemoteAddress() {return null;}
            public String getRemoteHost() {return null;}
            public int getLocalPort() {return 0;}
            public Object getConnectionIdentifier() {return new Object();};

        });
        return output;
    }
}
