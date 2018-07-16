package com.l7tech.xml.xslt;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.test.BugId;
import org.junit.Test;

import javax.xml.transform.TransformerException;
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

    private static final String XHTML_XSL =
            "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" +
                    "  <xsl:output method=\"html\"/>\n" +
                    "  <xsl:template match=\"/*\">\n" +
                    "    <th colspan=\"2\"/>\n" +
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

    @Test
    @BugId( "SSG-9081" )
    public void testXhtmlAttributeOutput() throws Exception {
        XsltUtil.getVariablesUsedByStylesheet( XHTML_XSL, "1.0" );
    }

    @Test( expected = ParseException.class )
    @BugId( "SSG-9081" )
    public void testXhtmlAttributeOutput_unsafeGlobalAttrNamespace() throws Exception {
        XsltUtil.getVariablesUsedByStylesheet(
                "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" +
                "  <xsl:output method=\"html\"/>\n" +
                "  <xsl:template match=\"/*\">\n" +
                "    <th foo:colspan=\"2\" xmlns:foo=\"http://xml.apache.org/xalan\"/>\n" +
                "  </xsl:template>\n" +
                "</xsl:stylesheet>", "1.0" );
    }

    @Test
    @BugId( "SSG-9081" )
    public void testXhtmlAttributeOutputXS20() throws Exception {
        XsltUtil.getVariablesUsedByStylesheet( XHTML_XSL, "2.0" );
    }

    // Saxon does not care about global attributes in the Xalan namespace so this is OK
    @Test
    @BugId( "SSG-9081" )
    public void testXhtmlAttributeOutput_unsafeGlobalAttrNamespaceXS20() throws Exception {
        XsltUtil.getVariablesUsedByStylesheet(
                "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" +
                        "  <xsl:output method=\"html\"/>\n" +
                        "  <xsl:template match=\"/*\">\n" +
                        "    <th foo:colspan=\"2\" xmlns:foo=\"http://xml.apache.org/xalan\"/>\n" +
                        "  </xsl:template>\n" +
                        "</xsl:stylesheet>", "2.0" );
    }
}
