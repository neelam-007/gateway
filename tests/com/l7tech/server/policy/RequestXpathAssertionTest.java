package com.l7tech.server.policy;

import com.l7tech.common.ApplicationContexts;
import com.l7tech.common.message.Message;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.server.audit.AuditContextStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerRequestXpathAssertion;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;

import java.util.HashMap;
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
        namespaces.put("sesyn", namespaces.get("soapenv"));
        ServerRequestXpathAssertion serverAssertion = getAssertion(new XpathExpression(expression, namespaces));
        Message m = new Message();
        m.initialize(testDoc);

        PolicyEnforcementContext pec = new PolicyEnforcementContext(m, new Message());
        pec.setAuditContext(new AuditContextStub());
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
        "//", // sanity
        "/soapenv:Envelope/soapenv:Body/ns1:placeOrder/productid", // contains a value
        "/soapenv:Envelope/soapenv:Body/ns1:placeOrder/productid='-9206260647417300294'", // works with proper namespaces
        "/*[local-name(.)='Envelope']/*[local-name(.)='Body']/*[local-name(.)='placeOrder']/productid='-9206260647417300294'", // works with no-namespace hack
        "/soapenv:Envelope[@*[local-name()=\"soapenv\"]]",
        "//*[@sesyn:encodingStyle]",
        "/soapenv:Envelope[3=3]",
        "soapenv:Envelope/soapenv:Body", // relative match should match against document element
    };

    private String[] failingXpaths =
    {
        "[", // invalid expression
        "/Envelope/Body/placeOrder/productid='-9206260647417300294'", // fails without namespaces
        "/foo:Envelope/bar:Body/baz:placeOrder/productid='-9206260647417300294'", // fails with bogus namespaces
        "/soapenv:Envelope/soapenv:Body/ns1:placeOrder/productid='blah'", // wrong value with correct namespaces
        "/Envelope/Body/placeOrder/productid='blah'", // wrong value without namespaces
        "soapenv:Envelopee/soapenv:Body", // misspelled Envelopee
    };
}
