package com.l7tech.server.policy;

import com.l7tech.common.RequestId;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.identity.User;
import com.l7tech.message.TransportMetadata;
import com.l7tech.message.XmlRequest;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.ServerRequestXpathAssertion;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Reader;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

/**
 * Tests the ServerRequestXpathAssertion class.
 * @author franco
 * @version $Revision$
 */
public class RequestXpathAssertionTest extends TestCase {

    public RequestXpathAssertionTest(String name) throws Exception {
        super(name);
    }

    public void testOKExpression() throws Exception {
        for (int i = 0 ; i < passingXpaths.length; i++) {
            AssertionStatus ret = null;
            ret = getResultForXPath(passingXpaths[i]);
            assertTrue(ret == AssertionStatus.NONE);
        }
    }

    public void testBadExpression() throws Exception {
        for (int i = 0 ; i < failingXpaths.length; i++) {
            AssertionStatus ret = null;
            ret = getResultForXPath(failingXpaths[i]);
            assertTrue((ret == AssertionStatus.FALSIFIED || ret == AssertionStatus.SERVER_ERROR));
        }
    }

    private AssertionStatus getResultForXPath(String expression) throws Exception {
        Map namespaces = new HashMap();
        namespaces.putAll(XpathEvaluator.getNamespaces(SoapUtil.asSOAPMessage(testDoc)));
        ServerRequestXpathAssertion serverAssertion = getAssertion(new XpathExpression(expression, namespaces));
        XmlRequest req = getTestRequest(testDoc);
        return serverAssertion.checkRequest(req, null);
    }

    private ServerRequestXpathAssertion getAssertion(XpathExpression expression) {
        RequestXpathAssertion assertion = new RequestXpathAssertion(expression);
        return new ServerRequestXpathAssertion(assertion);
    }

    private XmlRequest getTestRequest(final Document doc) {
        return new XmlRequest() {
            public Document getDocument() {
                return doc;
            }
            public Level getAuditLevel() {
                return Level.INFO;
            }

            public void setAuditLevel( Level auditLevel ) {
            }

            public boolean isAuditSaveRequest() {
                return false;
            }

            public void setAuditSaveRequest(boolean saveRequest) {
            }

            public boolean isAuditSaveResponse() {
                return false;
            }

            public void setAuditSaveResponse(boolean saveResponse) {
            }

            public LoginCredentials getPrincipalCredentials() {
                return null;
            }
            public User getUser() {
                return null;
            }
            public void setUser(User user) {}
            public void setPrincipalCredentials(LoginCredentials pc) {}
            public boolean isAuthenticated() {
                return false;
            }
            public boolean isReplyExpected() {
                return false;
            }
            public void setAuthenticated(boolean authenticated) {}
            public RoutingStatus getRoutingStatus() {
                return null;
            }
            public void setRoutingStatus(RoutingStatus status) {}

            public RequestId getId() {
                return null;
            }
            public void setDocument(Document doc) {}
            public String getXml() throws IOException {
                return null;
            }
            public void setXml(String xml) {}

            public TransportMetadata getTransportMetadata() {
                return null;
            }
            public Iterator getParameterNames() {
                return null;
            }
            public void setParameter(String name, Object value) {}
            public void setParameterIfEmpty(String name, Object value) {}
            public Object getParameter(String name) {
                return null;
            }
            public Object[] getParameterValues(String name) {
                return new Object[0];
            }
            public Collection getDeferredAssertions() {
                return null;
            }
            public void addDeferredAssertion(ServerAssertion owner, ServerAssertion decoration) {}
            public void removeDeferredAssertion(ServerAssertion owner) {}

            public ContentTypeHeader getOuterContentType() throws IOException {
                return null;
            }

            public void setInputStream(InputStream stream) {
            }

            public void runOnClose(Runnable runMe) {
            }

            public boolean isMultipart() throws IOException {
                return false;
            }

            public PartIterator getParts() throws IOException {
                return null;
            }

            public PartInfo getPartByContentId(String contentId) throws IOException, NoSuchPartException {
                return null;
            }

            public boolean isAdditionalUnreadPartsPossible() throws IOException {
                return false;
            }

            public long getContentLength() throws IOException, SAXException {
                return 0;
            }

            public InputStream getEntireMessageBody() throws IOException, SAXException {
                return null;
            }

            public void close() {
            }
        };
    }

    /**
     * create the <code>TestSuite</code> for the
     * RequestXpathAssertionTest <code>TestCase</code>
     */
    public static Test suite() {

        TestSuite suite = new TestSuite(RequestXpathAssertionTest.class);
        return suite;
    }

    public static void main(String[] args) throws
      Throwable {
        junit.textui.TestRunner.run(suite());
    }

    private Document testDoc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);

    private String[] passingXpaths =
    {
        "//", // sanity
        "/soapenv:Envelope/soapenv:Body/ns1:placeOrder/productid", // contains a value
        "/soapenv:Envelope/soapenv:Body/ns1:placeOrder/productid='-9206260647417300294'", // works with proper namespaces
        "/*[local-name(.)='Envelope']/*[local-name(.)='Body']/*[local-name(.)='placeOrder']/productid='-9206260647417300294'", // works with no-namespace hack
    };

    private String[] failingXpaths =
    {
        "[", // invalid expression
        "/Envelope/Body/placeOrder/productid='-9206260647417300294'", // fails without namespaces
        "/foo:Envelope/bar:Body/baz:placeOrder/productid='-9206260647417300294'", // fails with bogus namespaces
        "/soapenv:Envelope/soapenv:Body/ns1:placeOrder/productid='blah'", // wrong value with correct namespaces
        "/Envelope/Body/placeOrder/productid='blah'", // wrong value without namespaces
    };
}
