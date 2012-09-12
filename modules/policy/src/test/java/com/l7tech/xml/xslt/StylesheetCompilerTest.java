package com.l7tech.xml.xslt;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.Message;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Charsets;
import com.l7tech.util.Functions;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXParseException;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import java.io.ByteArrayInputStream;

/**
 * Tests the StylesheetCompiler through teh ServerXslTransformation assertion.
 *
 * @user: vchan
 */
public class StylesheetCompilerTest {

    /*
     * Test for bug 12777 - throw xml parse exception when message is empty
     */
    @BugNumber(12777)
    @Test
    public void testEmptyInputFailsParseException() {
        try {
            doTransform( XALAN_TEST_XSL, "", true );
            Assert.fail("Expected compilation or transformation failure.");
        }catch (SAXParseException ex) {
            ex.printStackTrace();
        }catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Expected SAXParseException.");
        }
    }

    /*
     * Test for bug 4789 - xsl stylesheets with output operations using xalan-extensions should be omitted.
     */
    @BugNumber(4789)
    @Test
    public void testTransformWithXalanExtension() throws Exception {
        try {
            Assert.assertTrue("Xalan XSLT present", isXalan());
            String after = doTransform(XALAN_TEST_XSL, XALAN_TEST_MSG, false);
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
            String after = doTransform(XALAN_TEST_XSL2, XALAN_TEST_MSG, false);
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
            CompiledStylesheet xsl = StylesheetCompiler.compileStylesheet(XALAN_TEST_XSL, "1.0");
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
            CompiledStylesheet xsl = StylesheetCompiler.compileStylesheet(XALAN_TEST_XSL2, "1.0");
            Assert.assertNotNull(xsl);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected exception encountered. " + ex);
        }
    }

    @Test
    public void testXalanExsltEvaluateExtensionFails() {
        try {
            Assert.assertTrue("Xalan XSLT present", isXalan());
            doTransform( XALAN_EXSLT_EXTENSION_EVAL_XSL, XALAN_TEST_MSG, false );
            Assert.fail("Expected compilation or transformation failure.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void testXalanRedirectExtensionFails() {
        try {
            Assert.assertTrue("Xalan XSLT present", isXalan());
            doTransform( XALAN_EXTENSION_ERROR_XSL, XALAN_REDIRECT_MSG, false );
            Assert.fail("Expected compilation or transformation failure.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void testXalanJavaExtensionFails() {
        try {
            Assert.assertTrue("Xalan XSLT present", isXalan());
            doTransform( XALAN_SECURE_PROCESSING, "<test/>", false );
            Assert.fail("Expected compilation or transformation failure.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void testTransformWithXalanUsingSax() throws Exception {
        try {
            Assert.assertTrue("Xalan XSLT present", isXalan());
            String after = doTransform(XALAN_TEST_XSL, XALAN_TEST_MSG, true);
            Assert.assertEquals(XALAN_TEST_RESULT, after);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
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

    private String doTransform(String xslt, String xml, boolean useSax) throws Exception {
        CompiledStylesheet xsl = StylesheetCompiler.compileStylesheet(xslt, "1.0");
        TransformOutput to = new TransformOutput();
        final Message mess = useSax
                ? new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(xml.getBytes(Charsets.UTF8)))
                : new Message(XmlUtil.stringAsDocument(xml));
        xsl.transform(
                new TransformInput(mess.getXmlKnob(), null, new Functions.Unary<Object, String>() {
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

    private static final String XALAN_REDIRECT_MSG = 
            "<doc>\n" +
                    "  <foo file=\"foo.out\">\n" +
                    "    Testing Redirect extension:\n" +
                    "      <bar>A foo subelement text node</bar>\n" +
                    "  </foo>\n" +
                    "  <main>\n" +
                    "    Everything else\n" +
                    "  </main>  \n" +
                    "</doc>";

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

    private static final String XALAN_EXTENSION_ERROR_XSL = 
            "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n" +
                    "    version=\"1.0\"\n" +
                    "    xmlns:redirect=\"http://xml.apache.org/xalan/redirect\"\n" +
                    "    extension-element-prefixes=\"redirect\">\n" +
                    "\n" +
                    "  <xsl:template match=\"/\">\n" +
                    "    <standard-out>\n" +
                    "      Standard output:\n" +
                    "      <xsl:apply-templates/>\n" +
                    "    </standard-out>\n" +
                    "  </xsl:template>\n" +
                    "  \n" +
                    "  <xsl:template match=\"main\">\n" +
                    "    <main>\n" +
                    "      <xsl:apply-templates/>\n" +
                    "    </main>\n" +
                    "  </xsl:template>\n" +
                    "  \n" +
                    "  <xsl:template match=\"/doc/foo\">\n" +
                    "    <redirect:write select=\"@file\">\n" +
                    "      <foo-out>\n" +
                    "        <xsl:apply-templates/>\n" +
                    "      </foo-out>\n" +
                    "    </redirect:write>\n" +
                    "  </xsl:template>\n" +
                    "  \n" +
                    "  <xsl:template match=\"bar\">\n" +
                    "    <foobar-out>\n" +
                    "      <xsl:apply-templates/>\n" +
                    "    </foobar-out>\n" +
                    "  </xsl:template>\n" +
                    "  \n" +
                    "</xsl:stylesheet>";

    private static final String XALAN_EXSLT_EXTENSION_EVAL_XSL =
            "<xsl:stylesheet \n" +
                    "    version=\"1.0\"    \n" +
                    "    xmlns:dyn=\"http://exslt.org/dynamic\"" +
                    "    xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" +
                    "    <xsl:template match=\"/\">\n" +
                    "        evaluate works?: <xsl:value-of select=\"dyn:evaluate(true)\"/>\n" +
                    "    </xsl:template>\n" +
                    "</xsl:stylesheet>";

    private static final String XALAN_SECURE_PROCESSING = 
            "<xsl:stylesheet \n" +
                    "    exclude-result-prefixes=\"java_lang\" \n" +
                    "    version=\"1.0\"    \n" +
                    "    xmlns:java_lang=\"http://xml.apache.org/xalan/java/java.lang\"\n" +
                    "    xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" +
                    "    <xsl:template match=\"/\">\n" +
                    "        shutdown: <xsl:value-of select=\"java_lang:System.exit(0)\"/>\n" +
                    "    </xsl:template>\n" +
                    "</xsl:stylesheet>";

    private static final String XALAN_TEST_RESULT = "<tns:a xmlns:tns=\"http://test.tns\">Xalan-Test-Transformation result</tns:a>";
}