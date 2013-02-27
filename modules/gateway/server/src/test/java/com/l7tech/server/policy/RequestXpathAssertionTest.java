package com.l7tech.server.policy;

import com.l7tech.common.TestDocuments;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.HasOutputVariables;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.ServerRequestXpathAssertion;
import com.l7tech.test.BugId;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathVersion;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static com.l7tech.xml.xpath.XpathVersion.UNSPECIFIED;
import static com.l7tech.xml.xpath.XpathVersion.XPATH_2_0;
import static org.junit.Assert.*;

/**
 * Tests the ServerRequestXpathAssertion class.
 * @author franco
 */
public class RequestXpathAssertionTest {

    private static final String NO_SUCH_VAR = "!!<{NO SUCH VARIABLE EXCEPTION}>!!"; // semaphore object used for comparison if NoSuchVariableException thrown when testing variable set in pec
    private XpathVersion xpathVersion;
    private RequestXpathAssertion customAssertionBean;

    @Before
    public void setUp() {
        xpathVersion = XpathVersion.UNSPECIFIED;
        customAssertionBean = null;
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
        ContextPreparer prep = new ContextPreparer() {
            @Override
            public PolicyEnforcementContext prepareContext(PolicyEnforcementContext context) {
                context.setVariable("myvar", "test123");
                return context;
            }
        };
        AssertionStatus ret = getResultForXPath("$myvar", prep, null);
        assertEquals(AssertionStatus.NONE, ret);
    }

    @Test
    public void testVariablesNotSetWhenNotUsed() throws Exception {
        VariableChecker checker = new VariableChecker(CollectionUtils.MapBuilder.<String, Object>builder()
            .put("requestXpath.found", NO_SUCH_VAR)
            .put("requestXpath.result", NO_SUCH_VAR)
            .put("requestXpath.results", NO_SUCH_VAR)
            .put("requestXpath.element", NO_SUCH_VAR)
            .put("requestXpath.elements", NO_SUCH_VAR)
            .put("requestXpath.count", NO_SUCH_VAR)
            .unmodifiableMap());

        AssertionStatus ret = getResultForXPath("/soap:Envelope/soap:Body/ware:placeOrder/productid", null, checker);
        assertEquals(AssertionStatus.NONE, ret);
    }

    @Test
    public void testVariablesSetWhenVisiblyUsedBySuccessorAssertionsAtCompileTime() throws Exception {
        final String expression = "/soap:Envelope/soap:Body/ware:placeOrder/productid";

        // Place the assertion into context within a policy tree that contains a successor assertion that uses some (but not all) its output variables
        new AllAssertion(Arrays.asList(
            customAssertionBean = new RequestXpathAssertion(new XpathExpression(expression, xpathVersion, namespaces)),
            new SetVariableAssertion("dummy", "${requestXpath.found}${requestXpath.count}${requestXpath.elements}${requestXpath.results}")
        ));

        VariableChecker checker = new VariableChecker(CollectionUtils.MapBuilder.<String, Object>builder()
            .put("requestXpath.found", "true")
            .put("requestXpath.result", NO_SUCH_VAR)
            .put("requestXpath.results", mustBeArray(String.class, 1))
            .put("requestXpath.element", NO_SUCH_VAR)
            .put("requestXpath.elements", mustBeArray(Element.class, 1))
            .put("requestXpath.count", "1")
            .unmodifiableMap());

        AssertionStatus ret = getResultForXPath(expression, null, checker);
        assertEquals(AssertionStatus.NONE, ret);
    }

    @Test
    public void testVariablesSetWhenVisiblyUsedBySuccessorAssertionsAtCompileTime2() throws Exception {
        final String expression = "/soap:Envelope/soap:Body/ware:placeOrder/productid";

        // Place the assertion into context within a policy tree that contains a successor assertion that uses some (but not all) its output variables
        new AllAssertion(Arrays.asList(
            customAssertionBean = new RequestXpathAssertion(new XpathExpression(expression, xpathVersion, namespaces)),
            new SetVariableAssertion("dummy", "${requestXpath.result}${requestXpath.element}")
        ));

        VariableChecker checker = new VariableChecker(CollectionUtils.MapBuilder.<String, Object>builder()
            .put("requestXpath.found", NO_SUCH_VAR)
            .put("requestXpath.result", "-9206260647417300294")
            .put("requestXpath.results", NO_SUCH_VAR)
            .put("requestXpath.element", "<productid xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xsd:long\">-9206260647417300294</productid>")
            .put("requestXpath.elements", NO_SUCH_VAR)
            .put("requestXpath.count", NO_SUCH_VAR)
            .unmodifiableMap());

        AssertionStatus ret = getResultForXPath(expression, null, checker);
        assertEquals(AssertionStatus.NONE, ret);
    }

    @Test
    @BugId("SSM-4246")
    public void testVariablesSetWhenDeclaredAsOutputsInAChildPec() throws Exception {

        ContextPreparer preparer = new ContextPreparer() {
            @Override
            public PolicyEnforcementContext prepareContext(PolicyEnforcementContext context) {
                PolicyEnforcementContext childContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(context);
                ((HasOutputVariables)childContext).addOutputVariableName("requestXpath.found");
                ((HasOutputVariables)childContext).addOutputVariableName("requestXpath.count");
                ((HasOutputVariables)childContext).addOutputVariableName("requestXpath.result");
                return childContext;
            }
        };

        VariableChecker checker = new VariableChecker(CollectionUtils.MapBuilder.<String, Object>builder()
            .put("requestXpath.found", "true")
            .put("requestXpath.result", "-9206260647417300294")
            .put("requestXpath.results", NO_SUCH_VAR)
            .put("requestXpath.element", NO_SUCH_VAR)
            .put("requestXpath.elements", NO_SUCH_VAR)
            .put("requestXpath.count", "1")
            .unmodifiableMap());

        AssertionStatus ret = getResultForXPath("/soap:Envelope/soap:Body/ware:placeOrder/productid", preparer, checker);
        assertEquals(AssertionStatus.NONE, ret);
    }

    private static Functions.UnaryVoid<Object> mustBeArray(final Class clazz, final int size) {
        return new Functions.UnaryVoid<Object>() {
            @Override
            public void call(Object o) {
                assertNotNull(o);
                assertTrue(Object[].class.isAssignableFrom(o.getClass()));
                Object[] objects = (Object[]) o;
                assertEquals(size, objects.length);
                for (Object object : objects) {
                    assertNotNull(object);
                    assertTrue(clazz.isAssignableFrom(object.getClass()));
                }
            }
        };
    }

    private static class VariableChecker implements ContextChecker {
        private final Map<String, Object> expectedValues  = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        private final Map<String, Object> obtainedValues  = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);

        public VariableChecker(Map<String, Object> expectedValues) {
            this.expectedValues.putAll(expectedValues);
        }

        public void checkContext(PolicyEnforcementContext pec) {
            // Gather values
            for (String varName : expectedValues.keySet()) {
                Object value;
                try {
                    value = pec.getVariable(varName);
                } catch (NoSuchVariableException e) {
                    value = NO_SUCH_VAR;
                }
                obtainedValues.put(varName, value);
            }

            // Make sure all were present with correct values
            for (String varName : expectedValues.keySet()) {
                Object actualValue = obtainedValues.get(varName);
                Object expectedValue = expectedValues.get(varName);
                if (expectedValue instanceof Functions.UnaryVoid) {
                    @SuppressWarnings("unchecked") Functions.UnaryVoid<Object> customValueChecker = (Functions.UnaryVoid<Object>) expectedValue;
                    customValueChecker.call(actualValue);
                } else {
                    assertEquals("Wrong value for context variable " + varName, expectedValue, actualValue);
                }
            }
        }
    }

    private AssertionStatus getResultForXPath(String expression) throws Exception {
        return getResultForXPath(expression, null, null);
    }

    private interface ContextPreparer {
        PolicyEnforcementContext prepareContext(PolicyEnforcementContext context);
    }

    private interface ContextChecker {
        void checkContext(PolicyEnforcementContext context);
    }

    /**
     * Create a server request assertion that matches the specified xpath (with the current {@link #xpathVersion} and {@link #namespaces})
     * and invoke it on a new PEC initialized with the PLACEORDER_CLEARTEXT test message as the default request.
     *
     * @param expression an xpath expression
     * @param contextPreparer a callback that can modify the PEC before the server assertion is run
     * @param contextChecker  a callback that can inspect the PEC after the server assertion has completed
     * @return the assertion status returned by the server assertion
     * @throws IOException if thrown by server assertion
     * @throws SAXException if thrown by server assertion
     * @throws PolicyAssertionException if thrown by server assertion
     */
    private AssertionStatus getResultForXPath(String expression,
                                              @Nullable ContextPreparer contextPreparer,
                                              @Nullable ContextChecker contextChecker) throws IOException, SAXException, PolicyAssertionException {
        ServerRequestXpathAssertion serverAssertion = getAssertion(new XpathExpression(expression, xpathVersion, namespaces));
        Message m = new Message();
        Document testDoc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        m.initialize(testDoc);

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(m, new Message(), false);
        if (contextPreparer != null)
            pec = contextPreparer.prepareContext(pec);
        AssertionStatus ret = serverAssertion.checkRequest(pec);
        if (contextChecker != null)
            contextChecker.checkContext(pec);
        return ret;
    }

    private ServerRequestXpathAssertion getAssertion(XpathExpression expression) {
        RequestXpathAssertion assertion = customAssertionBean != null ? customAssertionBean : new RequestXpathAssertion(expression);
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
