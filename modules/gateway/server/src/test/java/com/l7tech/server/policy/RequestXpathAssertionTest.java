package com.l7tech.server.policy;

import com.l7tech.common.TestDocuments;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.ServerRequestXpathAssertion;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathVersion;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import java.util.HashMap;
import java.util.Map;

import static com.l7tech.xml.xpath.XpathVersion.UNSPECIFIED;
import static com.l7tech.xml.xpath.XpathVersion.XPATH_2_0;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the ServerRequestXpathAssertion class.
 * @author franco
 */
public class RequestXpathAssertionTest {

    private XpathVersion xpathVersion;

    @Before
    public void setUp() {
        xpathVersion = XpathVersion.UNSPECIFIED;
    }

    @Test
    public void testMatchesValueUsingNamespaces() throws Exception {
        String xp = "/soap:Envelope/soap:Body/ware:placeOrder/productid";
        assertXpathPasses(xp);
    }

    @Test
    public void testOKExpression() throws Exception {
        for (final String xp : passingXpaths) {
            assertXpathPasses(xp);
        }
    }

    private void assertXpathPasses(String xp) throws Exception {
        System.out.println("xpath should succeed: " + xp);
        if (!xp.startsWith("/")) {
            System.out.println("** note: xpath relative to root");
        }
        final AssertionStatus ret = getResultForXPath(xp);
        assertTrue(ret == AssertionStatus.NONE);
    }

    @Test
    public void testBadExpression() throws Exception {
        for (int i = 0 ; i < failingXpaths.length; i++) {
            AssertionStatus ret = null;
            final String xp = failingXpaths[i];
            System.out.println("xpath should fail: " + xp);
            ret = getResultForXPath(xp);
            assertTrue((ret == AssertionStatus.FALSIFIED || ret == AssertionStatus.FAILED));
        }
    }

    @Test
    public void testXpath20() throws Exception {
        xpathVersion = XPATH_2_0;
        assertXpathPasses("(/soap:Envelope/soap:Body/ware:placeOrder/productid, /soap:Envelope[3=3])");
    }

    @Test
    public void testXpath20_withWrongVersion() throws Exception {
        xpathVersion = UNSPECIFIED;
        final AssertionStatus ret = getResultForXPath("(/soap:Envelope/soap:Body/ware:placeOrder/productid, /soap:Envelope[3=3])");
        assertEquals("XPath using XPath-2.0-only syntax currently expected to fail if XPath version is not specified explicitly as 2.0", AssertionStatus.FAILED, ret);
    }

    @Test
    public void testXpathVariables() throws Exception {
        AssertionStatus ret = getResultForXPath("$myvar");
        assertEquals(AssertionStatus.NONE, ret);
    }

    private AssertionStatus getResultForXPath(String expression) throws Exception {
        ServerRequestXpathAssertion serverAssertion = getAssertion(new XpathExpression(expression, xpathVersion, namespaces));
        Message m = new Message();
        Document testDoc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        m.initialize(testDoc);

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(m, new Message(), false);
        pec.setVariable("myvar", "test123");
        return serverAssertion.checkRequest(pec);
    }

    private ServerRequestXpathAssertion getAssertion(XpathExpression expression) {
        RequestXpathAssertion assertion = new RequestXpathAssertion(expression);
        return new ServerRequestXpathAssertion(assertion);
    }

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
