package com.l7tech.server.policy;

import com.l7tech.common.RequestId;
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

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
        Map namespaces = new HashMap();
        namespaces.putAll(XpathEvaluator.getNamespaces(SoapUtil.asSOAPMessage(testDoc)));
        XpathExpression expression = new XpathExpression(SoapUtil.SOAP_ENVELOPE_XPATH, namespaces);
        ServerRequestXpathAssertion serverAssertion = getAssertion(expression);
        XmlRequest req = getTestRequest(testDoc);
        AssertionStatus ret = serverAssertion.checkRequest(req, null);
        assertTrue(ret == AssertionStatus.NONE);
    }

    public void testBadExpression() throws Exception {
        Map namespaces = new HashMap();
        namespaces.putAll(XpathEvaluator.getNamespaces(SoapUtil.asSOAPMessage(testDoc)));
        XpathExpression expression = new XpathExpression("/blip:foo", namespaces);
        ServerRequestXpathAssertion serverAssertion = getAssertion(expression);
        XmlRequest req = getTestRequest(testDoc);
        AssertionStatus ret = serverAssertion.checkRequest(req, null);
        assertTrue(ret == AssertionStatus.FALSIFIED);
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
            protected Reader doGetRequestReader() throws IOException {
                return null;
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
            public String getRequestXml() throws IOException {
                return null;
            }
            public void setRequestXml(String xml) {}
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

    public Document testDoc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
}