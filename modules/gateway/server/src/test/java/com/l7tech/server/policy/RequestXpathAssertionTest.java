package com.l7tech.server.policy;

import com.l7tech.server.ApplicationContexts;
import com.l7tech.message.Message;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerRequestXpathAssertion;
import com.l7tech.common.TestDocuments;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests the ServerRequestXpathAssertion class.
 * @author franco
 */
public class RequestXpathAssertionTest extends TestCase {

    public RequestXpathAssertionTest(String name) throws Exception {
        super(name);
    }

    public void testOKExpression() throws Exception {
        for (int i = 0 ; i < passingXpaths.length; i++) {
            AssertionStatus ret = null;
            final String xp = passingXpaths[i];
            System.out.println("xpath should succeed: " + xp);
            if (!xp.startsWith("/")) {
                System.out.println("** note: xpath relative to root");
            }
            ret = getResultForXPath(xp);
            assertTrue(ret == AssertionStatus.NONE);
        }
    }

    public void testBadExpression() throws Exception {
        for (int i = 0 ; i < failingXpaths.length; i++) {
            AssertionStatus ret = null;
            final String xp = failingXpaths[i];
            System.out.println("xpath should fail: " + xp);
            ret = getResultForXPath(xp);
            assertTrue((ret == AssertionStatus.FALSIFIED || ret == AssertionStatus.FAILED));
        }
    }

    private AssertionStatus getResultForXPath(String expression) throws Exception {
        ServerRequestXpathAssertion serverAssertion = getAssertion(new XpathExpression(expression, namespaces));
        Message m = new Message();
        m.initialize(testDoc);

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(m, new Message(), false);
        return serverAssertion.checkRequest(pec);
    }

    private ServerRequestXpathAssertion getAssertion(XpathExpression expression) {
        RequestXpathAssertion assertion = new RequestXpathAssertion(expression);
        return new ServerRequestXpathAssertion(assertion, ApplicationContexts.getTestApplicationContext());
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
        "//*", // sanity
        "/soap:Envelope/soap:Body/ware:placeOrder/productid", // contains a value
        "/soap:Envelope/soap:Body/ware:placeOrder/productid='-9206260647417300294'", // works with proper namespaces
        "/*[local-name(.)='Envelope']/*[local-name(.)='Body']/*[local-name(.)='placeOrder']/productid='-9206260647417300294'", // works with no-namespace hack
        "/soap:Envelope[namespace::*[local-name()=\"soapenv\"]]",
        "//*[@sesyn:encodingStyle]",
        "/soap:Envelope[3=3]",
        "soap:Envelope/soap:Body", // relative match should match against document element
    };

    private String[] failingXpaths =
    {
        "[", // invalid expression
        "/Envelope/Body/placeOrder/productid='-9206260647417300294'", // fails without namespaces
        "/foo:Envelope/bar:Body/baz:placeOrder/productid='-9206260647417300294'", // fails with bogus namespaces
        "/soap:Envelope/soap:Body/ware:placeOrder/productid='blah'", // wrong value with correct namespaces
        "/Envelope/Body/placeOrder/productid='blah'", // wrong value without namespaces
        "soap:Envelopee/soap:Body", // misspelled Envelopee
    };

    private Map<String, String> namespaces = new HashMap<String, String>();
    {
        namespaces.put("soap", "http://schemas.xmlsoap.org/soap/envelope/");
        namespaces.put("ware", "http://warehouse.acme.com/ws");
        namespaces.put("sesyn", "http://schemas.xmlsoap.org/soap/envelope/");
        namespaces.put("foo", "http://schemas.foo.org/");
        namespaces.put("bar", "http://schemas.bar.org/");
        namespaces.put("baz", "http://schemas.baz.org/");
    }
}