package com.l7tech.xml.xpath;

import com.l7tech.common.io.XmlUtil;
import org.jaxen.*;
import org.jaxen.dom.DOMXPath;
import org.jaxen.saxpath.XPathHandler;
import org.jaxen.saxpath.base.XPathReader;
import static org.junit.Assert.*;
import org.junit.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class XpathVariableTest {
    private Document testdoc;

    @Before
    public void setUp() {
        testdoc = XmlUtil.stringAsDocument("<test><inner/></test>");
    }

    @Test
    public void testWithinExpression() throws Exception {
        doTestExpr("/*[local-name() = $nodename]");
    }

    @Test
    public void testWithinNodeName() throws Exception {
        // Currently this is expected to fail.
        // Should we find a way to make this work safely in the future we will update this test.
        try {
            doTestExpr("/$nodename");
            fail("Expected exception was not thrown");
        } catch (XPathSyntaxException e) {
            // Ok
        }
    }

    @Test
    public void testPathFromNodeVariable() throws Exception {
        DOMXPath dx = newDx("/test");
        final List firstNodelist = (List)dx.evaluate(testdoc);
        checkResult(firstNodelist, "test");
        dx = newDx("$var/inner");
        dx.setVariableContext(new VariableContext() {
            public Object getVariableValue(String ns, String prefix, String localName) throws UnresolvableException {
                assertTrue(ns == null || ns.length() == 0);
                assertTrue(prefix == null || prefix.length() == 0);
                assertEquals("var", localName);
                return firstNodelist.get(0);
            }
        });
        List secondNodelist = (List)dx.evaluate(testdoc);
        checkResult(secondNodelist, "inner");
    }

    @Test
    public void testMatchNodeListVariable() throws Exception {
        DOMXPath dx = newDx("/test");
        final List firstNodelist = (List)dx.evaluate(testdoc);
        checkResult(firstNodelist, "test");
        dx = newDx("$var");
        dx.setVariableContext(new VariableContext() {
            public Object getVariableValue(String ns, String prefix, String localName) throws UnresolvableException {
                assertTrue(ns == null || ns.length() == 0);
                assertTrue(prefix == null || prefix.length() == 0);
                assertEquals("var", localName);
                return firstNodelist;
            }
        });
        List secondNodelist = (List)dx.evaluate(testdoc);
        Node node = checkResult(secondNodelist, "test");
        assertEquals(node, firstNodelist.get(0));
    }
    
    @Test
    public void testBooleanExpression() throws Exception {
        DOMXPath dx = newDx("1=1");
        assertEquals(Boolean.TRUE, dx.evaluate(testdoc));

        dx = newDx("1=0");
        assertEquals(Boolean.FALSE, dx.evaluate(testdoc));
    }

    @Test
    @SuppressWarnings({"UnnecessaryBoxing"})
    public void testDoubleExpression() throws Exception {
        DOMXPath dx = newDx("23984.4353");
        assertEquals(new Double(23984.4353), dx.evaluate(testdoc));
    }

    @Test
    @SuppressWarnings({"UnnecessaryBoxing"})
    public void testIntegerExpression() throws Exception {
        DOMXPath dx = newDx("4953390");
        assertEquals(new Double(4953390), dx.evaluate(testdoc));
    }

    @Test
    @SuppressWarnings({"UnnecessaryBoxing"})
    public void testComplexExpression() throws Exception {
        DOMXPath dx = newDx("$val > 4 and $val < 15");
        setVariableContext(dx, "val", new Double(7));
        assertEquals(Boolean.TRUE, dx.evaluate(testdoc));
    }

    @Test
    @SuppressWarnings({"UnnecessaryBoxing"})
    public void testComparisonExpression() throws Exception {
        DOMXPath dx = newDx("$val > 4 and $val < 15");
        setVariableContext(dx, "val", new Double(7));
        assertEquals(Boolean.TRUE, dx.evaluate(testdoc));
    }

    @Test
    @SuppressWarnings({"UnnecessaryBoxing"})
    public void testArithmeticExpression() throws Exception {
        DOMXPath dx = newDx("(($val + 3) * 32) div 17");
        setVariableContext(dx, "val", new Double(7));
        final Double expected = new Double((7d + 3d) * 32d / 17d);
        assertEquals(expected, dx.evaluate(testdoc));
    }

    @Test
    public void testStringExpression() throws Exception {
        DOMXPath dx = newDx("\"Foo\"");
        assertEquals("Foo", dx.evaluate(testdoc));
    }

    @Test
    public void testCustomResultClass() throws Exception {
        final Object customResult = new Object() {
            final String blah = "";
        };
        DOMXPath dx = newDx("$var");
        dx.setVariableContext(new VariableContext() {
            public Object getVariableValue(String s, String s1, String s2) throws UnresolvableException {
                return customResult;
            }
        });
        List got = (List) dx.evaluate(testdoc);
        assertTrue(got.get(0) == customResult);
    }

    @Test
    public void testNodeVarFromForeignDocument() throws Exception {
        Document foreign = XmlUtil.stringAsDocument("<foreigner/>");
        DOMXPath dx = newDx("$nodename");
        setVariableContext(dx, "nodename", foreign.getDocumentElement());
        List got = (List) dx.evaluate(testdoc);
        assertEquals(1, got.size());
        assertTrue(got.get(0) == foreign.getDocumentElement());
    }

    @Test
    public void testGetVarsUsed() throws Exception {
        String testExpr = "/foo/bar/*[local-name() = $ns1:blat]/*[$hmm]";

        // Try using the SAXPath built into Jaxen to find variable references within an XPath expression
        final List<String> seenvars = new ArrayList<String>();
        final Method varRefMethod = XPathHandler.class.getMethod("variableReference", String.class, String.class);
        XPathHandler handler = (XPathHandler)Proxy.newProxyInstance(XPathHandler.class.getClassLoader(), new Class[] {XPathHandler.class}, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (varRefMethod.equals(method)) {
                    String prefix = (String)args[0];
                    String localname = (String)args[1];
                    seenvars.add(prefix + ":" + localname);
                }
                return null;
            }
        });
        XPathReader reader = new XPathReader();
        reader.setXPathHandler(handler);
        reader.parse(testExpr);
        assertEquals(2, seenvars.size());
        assertEquals("ns1:blat", seenvars.get(0));
        assertEquals(":hmm", seenvars.get(1));
    }

    private Node checkResult(Object result, String expectedLocalName) {
        assertTrue(result instanceof List);
        List nodeList = (List)result;
        assertEquals(1, nodeList.size());
        Node node = (Node)nodeList.get(0);
        assertEquals(expectedLocalName, node.getLocalName());
        return node;
    }

    private void doTestExpr(String expr) throws JaxenException {
        DOMXPath dx = newDx(expr);
        setVariableContext(dx, "nodename", "test");
        checkResult(dx.evaluate(testdoc), "test");
    }

    private static void setVariableContext(DOMXPath dx, final String varname, final Object varvalue) {
        dx.setVariableContext(new VariableContext() {
            public Object getVariableValue(String ns, String prefix, String localName) throws UnresolvableException {
                assertTrue(ns == null || ns.length() == 0);
                assertTrue(prefix == null || prefix.length() == 0);
                assertEquals(varname, localName);
                return varvalue;
            }
        });
    }

    private DOMXPath newDx(String expr) throws JaxenException {
        DOMXPath dx = new DOMXPath(expr);
        dx.setFunctionContext(new XPathFunctionContext(false));
        return dx;
    }
}
