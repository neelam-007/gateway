package com.l7tech.xml.xpath;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

import javax.xml.xpath.XPathExpressionException;
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
}
