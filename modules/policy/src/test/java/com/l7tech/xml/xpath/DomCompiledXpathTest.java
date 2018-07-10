package com.l7tech.xml.xpath;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.rsa.sslj.x.M;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.xpath.XPathExpressionException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static com.l7tech.xml.xpath.XpathVersion.XPATH_1_0;
import static com.l7tech.xml.xpath.XpathVersion.XPATH_2_0;
import static org.junit.Assert.*;

/**
 * Unit tests for DomCompiledXpath.
 */
public class DomCompiledXpathTest {
    ElementCursor cursor = new DomElementCursor(XmlUtil.stringAsDocument("<foo><a/><b><c/><d/><e/></b></foo>"));
    Element bEl, cEl, dEl, eEl;
    XpathVariableFinder varFinder = new XpathVariableFinder() {
        @Override
        public Object getVariableValue(String namespaceUri, String variableName) throws NoSuchXpathVariableException {
            if (namespaceUri == null || namespaceUri.isEmpty()) {
                if ("stringvar".equals(variableName))
                    return "stringvar_value";
                if ("intvar".equals(variableName))
                    return 42;
                if ("doublevar".equals(variableName))
                    return 42d;
                if ("booleanvar".equals(variableName))
                    return true;
                if ("bigintegervar".equals(variableName))
                    return new BigInteger("12348765876587658765");
                if ("bigdecimalvar".equals(variableName))
                    return new BigDecimal("29384723984723984234");
            }
            throw new NoSuchXpathVariableException();
        }
    };

    @Before
    public void setUp() throws Exception {
        bEl = XmlUtil.findOnlyOneChildElementByName(cursor.asDomElement(), (String) null, "b");
        cEl = XmlUtil.findOnlyOneChildElementByName(bEl, (String)null, "c");
        dEl = XmlUtil.findOnlyOneChildElementByName(bEl, (String)null, "d");
        eEl = XmlUtil.findOnlyOneChildElementByName(bEl, (String) null, "e");
    }

    @Test
    public void testSimpleXpathSuccess() throws Exception {
        assertTrue(cursor.matches(new DomCompiledXpath(new XpathExpression("//b"))));
    }

    @Test
    public void testSimpleXpathFailure() throws Exception {
        assertFalse(cursor.matches(new DomCompiledXpath(new XpathExpression("//asdf"))));
    }

    @Test
    public void testSimpleXpathSuccessXP20() throws Exception {
        assertTrue(cursor.matches(new DomCompiledXpath(new XpathExpression("//b intersect //*", XPATH_2_0, null))));
    }

    @Test
    public void testSimpleXpathSuccessUnionXP20() throws Exception {
        assertTrue(cursor.matches(new DomCompiledXpath(new XpathExpression("//b union //nonexistent", XPATH_2_0, null))));
    }

    @Test
    public void testSimpleXpathFailureXP20() throws Exception {
        assertFalse(cursor.matches(new DomCompiledXpath(new XpathExpression("//b intersect //nonexistent", XPATH_2_0, null))));
    }

    @Test
    public void testSimpleXpathFailureIntersectXP20() throws Exception {
        assertFalse(cursor.matches(new DomCompiledXpath(new XpathExpression("//b intersect //a", XPATH_2_0, null))));
    }

    @Test
    public void testSelectNodeSet() throws Exception {
        final DomCompiledXpath dxp = new DomCompiledXpath(new XpathExpression("//b//*"));
        checkBStarResult(dxp);
    }

    @Test
    public void testSelectNodeSetXP20Syntax() throws Exception {
        final DomCompiledXpath dxp = new DomCompiledXpath(new XpathExpression("(//b/*[1], //b/d, //b/*[2]/following-sibling::*)", XPATH_2_0, (Map<String,String>)null));
        checkBStarResult(dxp);
    }

    @Test(expected = InvalidXpathException.class)
    public void testSelectNodeSetXP20Syntax_asXP10() throws Exception {
        final DomCompiledXpath dxp = new DomCompiledXpath(new XpathExpression("(//b/*[1], //b/d, //b/*[2]/following-sibling::*)"));
        checkBStarResult(dxp);
    }

    @Test(expected = XPathExpressionException.class)
    public void testSelectNodeSetXP20Function_asXP10() throws Exception {
        // Should fail, Jaxen XPath 1.0 does not include "matches" function
        final DomCompiledXpath dxp = new DomCompiledXpath(new XpathExpression("//b/*[matches(.,'')]"));
        checkBStarResult(dxp);
    }

    @Test
    public void testSelectNodeSetXP20Function() throws Exception {
        final DomCompiledXpath dxp = new DomCompiledXpath(new XpathExpression("//b/*[matches(.,'')]", XPATH_2_0, (Map<String,String>)null));
        checkBStarResult(dxp);
    }

    @Test
    public void testExpressionWithVariables() throws Exception {
        final DomCompiledXpath dxp = new DomCompiledXpath(new XpathExpression("$doublevar + 100", XPATH_1_0, (Map<String,String>)null));
        XpathResult xr = cursor.getXpathResult(dxp, varFinder, true);
        assertTrue(xr.matches());
        assertEquals(XpathResult.TYPE_NUMBER, xr.getType());
        assertEquals(142, xr.getNumber(), 1);
    }

    @Test
    public void testExpressionWithVariablesXP20() throws Exception {
        final DomCompiledXpath dxp = new DomCompiledXpath(new XpathExpression("$doublevar + 100", XPATH_2_0, (Map<String,String>)null));
        XpathResult xr = cursor.getXpathResult(dxp, varFinder, true);
        assertTrue(xr.matches());
        assertEquals(XpathResult.TYPE_NUMBER, xr.getType());
        assertEquals(142, xr.getNumber(), 1);
    }

    @Test
    public void testExpressionWithVariablesIntXP20() throws Exception {
        final DomCompiledXpath dxp = new DomCompiledXpath(new XpathExpression("$intvar + 100", XPATH_2_0, (Map<String,String>)null));
        XpathResult xr = cursor.getXpathResult(dxp, varFinder, true);
        assertTrue(xr.matches());
        assertEquals(XpathResult.TYPE_NUMBER, xr.getType());
        assertEquals(142, xr.getNumber(), 1);
    }

    @Test
    public void testExpressionWithVariablesBigIntegerXP20() throws Exception {
        final DomCompiledXpath dxp = new DomCompiledXpath(new XpathExpression("$bigintegervar + 100", XPATH_2_0, (Map<String,String>)null));
        XpathResult xr = cursor.getXpathResult(dxp, varFinder, true);
        assertTrue(xr.matches());
        assertEquals(XpathResult.TYPE_NUMBER, xr.getType());
        assertEquals("12348765876587658865", xr.getString());
    }

    @Test
    public void testExpressionWithVariablesBigDecimalXP20() throws Exception {
        final DomCompiledXpath dxp = new DomCompiledXpath(new XpathExpression("$bigdecimalvar + 100", XPATH_2_0, (Map<String,String>)null));
        XpathResult xr = cursor.getXpathResult(dxp, varFinder, true);
        assertTrue(xr.matches());
        assertEquals(XpathResult.TYPE_NUMBER, xr.getType());
        assertEquals("29384723984723984334", xr.getString());
    }

    private void checkBStarResult(DomCompiledXpath dxp) throws XPathExpressionException {
        XpathResult result = cursor.getXpathResult(dxp, null, true);
        assertTrue(result.matches());
        assertEquals(XpathResult.TYPE_NODESET, result.getType());
        XpathResultNodeSet ns = result.getNodeSet();
        assertEquals(3, ns.size());

        // Check results using XpathResultNode (fast, Tarari-FastXpath-compatible) interface
        XpathResultIterator it = ns.getIterator();
        XpathResultNode n = new XpathResultNode();

        assertTrue(it.hasNext());
        it.next(n);
        assertEquals("c", n.getNodeLocalName());
        assertEquals("c", n.getNodeName());

        assertTrue(it.hasNext());
        it.next(n);
        assertEquals("d", n.getNodeLocalName());
        assertEquals("d", n.getNodeName());

        assertTrue(it.hasNext());
        it.next(n);
        assertEquals("e", n.getNodeLocalName());
        assertEquals("e", n.getNodeName());

        // Check results using DOM cursor (slow when not using DOM, not Tarari FastXpath-compatible)
        it = ns.getIterator();
        ElementCursor nc;
        nc = it.nextElementAsCursor();
        assertNotNull(nc);
        Element de = nc.asDomElement();
        assertTrue("Strong element identity preserved", de == cEl);

        nc = it.nextElementAsCursor();
        assertNotNull(nc);
        de = nc.asDomElement();
        assertTrue("Strong element identity preserved", de == dEl);

        nc = it.nextElementAsCursor();
        assertNotNull(nc);
        de = nc.asDomElement();
        assertTrue("Strong element identity preserved", de == eEl);
    }

    @Test
    public void testRawSelectElements() throws Exception {
        Document doc = XmlUtil.stringAsDocument("<foo><bar/><baz/></foo>");
        Element bar = XmlUtil.findFirstChildElement(doc.getDocumentElement());
        assertEquals("bar", bar.getLocalName());
        Element baz = (Element) bar.getNextSibling();
        assertEquals("baz", baz.getLocalName());

        DomCompiledXpath cx = new DomCompiledXpath(new XpathExpression("//bar", XPATH_1_0, null));
        List<Element> elms = cx.rawSelectElements(doc, null);
        assertNotNull(elms);
        assertEquals(1, elms.size());
        assertEquals(bar, elms.get(0));
    }

    @Test
    public void testRawSelectElementsXP20() throws Exception {
        Document doc = XmlUtil.stringAsDocument("<foo><bar/><baz/></foo>");
        Element bar = XmlUtil.findFirstChildElement(doc.getDocumentElement());
        assertEquals("bar", bar.getLocalName());
        Element baz = (Element) bar.getNextSibling();
        assertEquals("baz", baz.getLocalName());

        DomCompiledXpath cx = new DomCompiledXpath(new XpathExpression("(//bar, //baz)", XPATH_2_0, null));
        List<Element> elms = cx.rawSelectElements(doc, null);
        assertNotNull(elms);
        assertEquals(2, elms.size());
        assertEquals(bar, elms.get(0));
        assertEquals(baz, elms.get(1));
    }

    @Test
    public void testXpathReturningMultipleStringValues() throws Exception {
        DomElementCursor cursor = new DomElementCursor(XmlUtil.stringAsDocument("<foo><a>string1</a><a>string2</a><a>string3</a></foo>"));
        DomCompiledXpath cx = new DomCompiledXpath(new XpathExpression("//foo/a/upper-case(text())", XPATH_2_0, null));
        XpathResult.MultiValuedXpathResultAdapter multiValuedXpathResultAdapter = (XpathResult.MultiValuedXpathResultAdapter) cx.getXpathResult(cursor, null);
        int i=1;
        for(String a:multiValuedXpathResultAdapter.getStringArray()) {
            assertEquals("STRING" + i, a);
            i++;
        }
        assertEquals(XpathResult.TYPE_STRING, multiValuedXpathResultAdapter.getType());
        assertEquals(true, multiValuedXpathResultAdapter instanceof XpathResult.MultiValuedXpathResultAdapter);
    }

    @Test
    public void testXpathReturningMultipleNumberValues() throws Exception {
        DomElementCursor cursor = new DomElementCursor(XmlUtil.stringAsDocument("<foo><a>1</a><a>2</a><a>3</a></foo>"));
        DomCompiledXpath cx = new DomCompiledXpath(new XpathExpression("//foo/a/(number(text()))", XPATH_2_0, null));
        XpathResult xpathResult = cx.getXpathResult(cursor, null);
        assertEquals(true, xpathResult instanceof XpathResult.MultiValuedXpathResultAdapter);
        XpathResult.MultiValuedXpathResultAdapter multiValuedXpathResult = (XpathResult.MultiValuedXpathResultAdapter) xpathResult;
        assertEquals(XpathResult.TYPE_NUMBER, multiValuedXpathResult.getType());
        Double[] numbers = {1.0, 2.0, 3.0};
        assertArrayEquals(numbers, multiValuedXpathResult.getNumberArray());
        assertEquals(XpathResult.TYPE_NUMBER, multiValuedXpathResult.getType());

    }

    @Test
    public void testXpathReturningMultipleBooleanValues() throws Exception {
        DomElementCursor cursor = new DomElementCursor(XmlUtil.stringAsDocument("<foo><a>true</a><a>false</a><a>true</a></foo>"));
        DomCompiledXpath cx = new DomCompiledXpath(new XpathExpression("//foo/a/(text()=\"true\")", XPATH_2_0, null));
        XpathResult xpathResult = cx.getXpathResult(cursor, null);
        assertEquals(true, xpathResult instanceof XpathResult.MultiValuedXpathResultAdapter);
        XpathResult.MultiValuedXpathResultAdapter multiValuedXpathResult = (XpathResult.MultiValuedXpathResultAdapter) xpathResult;
        assertEquals(XpathResult.TYPE_BOOLEAN, multiValuedXpathResult.getType());
        Boolean[] numbers = {true, false, true};
        assertArrayEquals(numbers, multiValuedXpathResult.getBooleanArray());
        assertEquals(XpathResult.TYPE_BOOLEAN, multiValuedXpathResult.getType());
    }

    @Test
    public void testXpathWithAtomicValue() throws Exception {
        DomElementCursor cursor = new DomElementCursor(XmlUtil.stringAsDocument("<foo><a>string1</a><b>string2</b><c>string3</c></foo>"));
        DomCompiledXpath cx = new DomCompiledXpath(new XpathExpression("//foo/a", XPATH_2_0, null));
        XpathResult xpathResult = cx.getXpathResult(cursor, null);
        assertEquals(false, xpathResult instanceof XpathResult.MultiValuedXpathResultAdapter);
    }
}
