package com.l7tech.xml.xslt;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Functions;
import com.l7tech.xml.DomElementCursor;

import org.junit.Assert;
import org.junit.Test;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

/**
 * Tests the StylesheetCompiler through teh ServerXslTransformation assertion.
 *
 * @user: vchan
 */
public class StylesheetCompilerTest {

    /*
     * Test for bug 4789 - xsl stylesheets with output operations using xalan-extensions should be omitted.
     */
    @BugNumber(4789)
    @Test
    public void testTransformWithXalanExtension() throws Exception {
        try {
            Assert.assertTrue("Xalan XSLT present", isXalan());
            String after = doTransform(XALAN_TEST_XSL, XALAN_TEST_MSG);
            Assert.assertEquals(XALAN_TEST_RESULT, after);
        } catch (Exception ex) {
            Assert.fail("Unexpected exception encountered. " + ex);
        }
    }

    /*
     * Test for bug 4789 - xsl stylesheets with output operations using xalan-extensions should be omitted.
     */
    @BugNumber(4789)
    @Test
    public void testTransformWithXalanExtensionOutputNS() {
        try {
            Assert.assertTrue("Xalan XSLT present", isXalan());
            String after = doTransform(XALAN_TEST_XSL2, XALAN_TEST_MSG);
            Assert.assertEquals(XALAN_TEST_RESULT, after);
        } catch (Exception ex) {
            Assert.fail("Unexpected exception encountered. " + ex);
        }
    }

    /*
     * Test for bug 4789 - xsl stylesheets with output operations using xalan-extensions should be omitted.
     */
    @BugNumber(4789)
    @Test
    public void testCompileWithXalanExtension() {
        try {
            Assert.assertTrue("Xalan XSLT present", isXalan());
            CompiledStylesheet xsl = StylesheetCompiler.compileStylesheet(XALAN_TEST_XSL);
            Assert.assertNotNull(xsl);
        } catch (Exception ex) {
            Assert.fail("Unexpected exception encountered. " + ex);
        }
    }

    /*
     * Test for bug 4789 - xsl stylesheets with output operations using xalan-extensions should be omitted.
     */
    @BugNumber(4789)
    @Test
    public void testCompileWithXalanExtensionOutputNS() {
        try {
            Assert.assertTrue("Xalan XSLT present", isXalan());
            CompiledStylesheet xsl = StylesheetCompiler.compileStylesheet(XALAN_TEST_XSL2);
            Assert.assertNotNull(xsl);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected exception encountered. " + ex);
        }
    }

    @SuppressWarnings({"ConstantIfStatement"})
    public static final class Failure {
        static {
            System.out.println("Failure class created.");
            if (true) throw new RuntimeException("Failure class created.");
        }
    }

    private boolean isXalan() {
        return TransformerFactory.newInstance().getClass().getName().equals("org.apache.xalan.processor.TransformerFactoryImpl");
    }

    private String doTransform(String xslt, String xml) throws Exception {
        CompiledStylesheet xsl = StylesheetCompiler.compileStylesheet(xslt);
        TransformOutput to = new TransformOutput();
        xsl.transform(
                new TransformInput(new DomElementCursor(XmlUtil.stringAsDocument(xml)), new Functions.Unary<Object, String>() {
                    public Object call(String s) {
                        return null;
                    }
                }),
                to,
                new ErrorListener() {
                    public void warning(TransformerException exception) throws TransformerException {
                        throw exception;
                    }

                    public void error(TransformerException exception) throws TransformerException {
                        throw exception;
                    }

                    public void fatalError(TransformerException exception) throws TransformerException {
                        throw exception;
                    }
                }
        );

        // parser round-trip removes the xml declaration
        return XmlUtil.nodeToString(XmlUtil.stringAsDocument(new String(to.getBytes())));        
    }


    private static final String XALAN_TEST_MSG =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<soapenv:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:urn=\"urn:xmltoday-delayed-quotes\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <urn:getQuote soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                    "         <symbol xsi:type=\"xsd:string\">SUNW</symbol>\n" +
                    "      </urn:getQuote>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
    
    private static final String XALAN_TEST_XSL =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<xsl:stylesheet\n" +
                    "  xmlns:tns=\"http://test.tns\"\n" +
                    "  xmlns:xalan=\"http://xml.apache.org/xalan\"\n" +
                    "  xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" exclude-result-prefixes=\"xalan\" version=\"1.0\">\n" +
                    "    <xsl:output xalan:content-handler=\"com.l7tech.common.security.prov.pkcs11.Pkcs11JceProviderEngine\"></xsl:output>\n" +
                    "    <!-- causes enginge to load the contents into memory -->\n" +
                    "    <xsl:output xalan:entities=\"/home/vchan/Dev/test\"/>\n" +
                    "    <xsl:output xalan:entities=\"/home/vchan/Apps\"/>\n" +
                    "    <!-- causes http get resulting in a transformation error -->\n" +
                    "    <xsl:output xalan:entities=\"http://www.whatever.com/nothingness\"></xsl:output>\n" +
                    "  <!-- static output template -->\n" +
                    "  <xsl:template match=\"*\">\n" +
                    "      <tns:a>Xalan-Test-Transformation result</tns:a>\n" +
                    "  </xsl:template>\n" +
                    "</xsl:stylesheet>";

    private static final String XALAN_TEST_XSL2 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<xsl:stylesheet\n" +
                    "  xmlns:tns=\"http://test.tns\"\n" +
                    "  xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">\n" +
                    "    <xsl:output xalan:content-handler=\"com.l7tech.xml.xslt.StylesheetCompilerTest$Failure\" xmlns:xalan=\"http://xml.apache.org/xalan\"></xsl:output>\n" +
                    "    <xsl:output xalan:entities=\"http://127.0.0.1:66574/nothingness\" xmlns:xalan=\"http://xml.apache.org/xalan\"></xsl:output>\n" +
                    "    <xsl:output xalan:content-handler=\"com.l7tech.xml.xslt.StylesheetCompilerTest$Failure\" xmlns:xalan=\"http://xml.apache.org/xslt\"></xsl:output>\n" +
                    "    <xsl:output xalan:entities=\"http://127.0.0.1:66574/nothingness\" xmlns:xalan=\"http://xml.apache.org/xslt\"></xsl:output>\n" +
                    "  <!-- static output template -->\n" +
                    "  <xsl:template match=\"*\">\n" +
                    "      <tns:a>Xalan-Test-Transformation result</tns:a>\n" +
                    "  </xsl:template>\n" +
                    "</xsl:stylesheet>";

    private static final String XALAN_TEST_RESULT = "<tns:a xmlns:tns=\"http://test.tns\">Xalan-Test-Transformation result</tns:a>";
}