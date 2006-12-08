package com.l7tech.common.util;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.Wsdl;
import org.w3c.dom.Document;

import javax.wsdl.Operation;
import java.net.URL;
import java.text.ParseException;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Enumeration;

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
