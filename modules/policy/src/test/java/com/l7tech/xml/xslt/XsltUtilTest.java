package com.l7tech.xml.xslt;

import com.l7tech.common.io.XmlUtil;
import org.junit.Test;

import java.text.ParseException;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Unit tests for {@link XsltUtil}.
 */
public class XsltUtilTest {
    @Test
    public void testGetVariablesUsedByStylesheet() throws Exception {
        List<String> got = XsltUtil.getVariablesUsedByStylesheet(TEST_XSLT_10, "1.0");
        assertEquals(2, got.size());
        assertTrue(got.contains("blarg"));
        assertTrue(got.contains("foo"));
    }

    public static final String TEST_XSLT_10 = "<?xml version=\"1.0\"?>\n" +
        "<xsl:stylesheet version=\"1.0\" xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>\n" +
        "  <xsl:param name=\"blarg\"/>\n" +
        "  <xsl:param name=\"foo\"/>\n" +
        "  <xsl:template match=\"@*|*|processing-instruction()|comment()\">\n" +
        "    <xsl:copy>\n" +
        "      <xsl:apply-templates select=\"*|@*|text()|processing-instruction()|comment()\"/>\n" +
        "    </xsl:copy>\n" +
        "  </xsl:template>\n" +
        "</xsl:stylesheet>";

    @Test
    public void testGetVariablesUsedByStylesheetXS20() throws Exception {
        List<String> got = XsltUtil.getVariablesUsedByStylesheet(TEST_XSLT_20, "2.0");
        assertEquals(2, got.size());
        assertTrue(got.contains("blarg"));
        assertTrue(got.contains("foo"));
    }

    public static final String TEST_XSLT_20 = "<?xml version=\"1.0\"?>\n" +
        "<xsl:stylesheet version=\"2.0\" xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>\n" +
        "  <xsl:param name=\"blarg\"/>\n" +
        "  <xsl:param name=\"bazz\" select=\"asdf\"/>\n" +
        "  <xsl:param name=\"foo\"/>\n" +
        "  <xsl:template match=\"@*|*|processing-instruction()|comment()\">\n" +
        "    <xsl:copy>\n" +
        "      <xsl:apply-templates select=\"*|@*|text()|processing-instruction()|comment()\"/>\n" +
        "    </xsl:copy>\n" +
        "  </xsl:template>\n" +
        "</xsl:stylesheet>";

    @Test
    public void testCheckXsltSyntax() throws Exception {
        XsltUtil.checkXsltSyntax(XmlUtil.stringAsDocument(TEST_XSLT_10), "1.0", null);
    }

    @Test(expected = ParseException.class)
    public void testCheckXsltSyntaxBad() throws Exception {
        XsltUtil.checkXsltSyntax(XmlUtil.stringAsDocument(TEST_XSLT_10.replace("xsl:stylesheet", "xsl:stilesheet")), "1.0", null);
    }

    @Test
    public void testCheckXsltSyntaxXS20() throws Exception {
        XsltUtil.checkXsltSyntax(XmlUtil.stringAsDocument(TEST_XSLT_20), "2.0", null);
    }

    @Test(expected = ParseException.class)
    public void testCheckXsltSyntaxBadXS20() throws Exception {
        XsltUtil.checkXsltSyntax(XmlUtil.stringAsDocument(TEST_XSLT_20.replace("xsl:copy", "xsl:floppy")), "2.0", null);
    }

}
