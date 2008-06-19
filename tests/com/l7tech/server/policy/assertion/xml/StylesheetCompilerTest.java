package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.ApplicationContexts;
import com.l7tech.common.http.GenericHttpHeaders;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.http.MockGenericHttpClient;
import com.l7tech.common.message.Message;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.xslt.CompiledStylesheet;
import com.l7tech.common.xml.xslt.StylesheetCompiler;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.server.TestStashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.util.TestingHttpClientFactory;
import com.l7tech.spring.util.SimpleSingletonBeanFactory;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Tests the StylesheetCompiler through teh ServerXslTransformation assertion.
 *
 * @user: vchan
 */
public class StylesheetCompilerTest extends TestCase {

    private static Logger logger = Logger.getLogger(StylesheetCompilerTest.class.getName());

    private TestingHttpClientFactory httpClientFactory;
    private ApplicationContext appCtx;

    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
        System.out.println("Test complete: " + StylesheetCompilerTest.class);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(StylesheetCompilerTest.class);
        return suite;
    }

    protected void setUp() throws Exception {

        // initialize only once for this testcase
        if (appCtx == null) {
            appCtx = ApplicationContexts.getTestApplicationContext();
        }

        // Make sure the static HTTP object cache inside the ServerXslTransformation class gets initialized
        // with our TestingHttpClientFactory instead of finding a real one
        httpClientFactory = new TestingHttpClientFactory();
    }

    /*
     * Test for bug 4789 - xsl stylesheets with output operations using xalan-extensions should be omitted.
     */
    public void testTransformWithStaticResource() throws Exception {

        try {
            setupMockHttpClient(XALAN_TEST_XSL);

            XslTransformation ass = new XslTransformation();
            ass.setDirection(XslTransformation.APPLY_TO_REQUEST);
            ass.setWhichMimePart(0);
            ass.setResourceInfo(new StaticResourceInfo(XALAN_TEST_XSL));

            // create the server assertion
            ServerXslTransformation serverAss = new ServerXslTransformation(ass, appCtx);

            Message req = new Message(TestStashManagerFactory.getInstance().createStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(XALAN_TEST_MSG.getBytes("UTF-8")));
            Message res = new Message();
            PolicyEnforcementContext context = new PolicyEnforcementContext(req, res);

            // run the checkRequest
            serverAss.checkRequest(context);
            String after = XmlUtil.nodeToString(req.getXmlKnob().getDocumentReadOnly());
//            System.out.println("xslt:" + after);
            assertEquals(XALAN_TEST_RESULT, after);

        } catch (Exception ex) {
            fail("Unexpected exception encountered. " + ex);
        }
    }


    /*
     * Test for bug 4789 - xsl stylesheets with output operations using xalan-extensions should be omitted.
     */
    public void testTransformWithSingleUrlResource() throws Exception {

        try {
            setupMockHttpClient(XALAN_TEST_XSL);

            XslTransformation ass = new XslTransformation();
            ass.setDirection(XslTransformation.APPLY_TO_REQUEST);
            ass.setWhichMimePart(0);
            // the actual value is initialized in the mockHttpClient
            ass.setResourceInfo(new SingleUrlResourceInfo("http://some.test.url/xalantest.xsl"));

            ServerXslTransformation serverAss = new ServerXslTransformation(ass, appCtx);

            Message req = new Message(TestStashManagerFactory.getInstance().createStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(XALAN_TEST_MSG.getBytes("UTF-8")));
            Message res = new Message();
            PolicyEnforcementContext context = new PolicyEnforcementContext(req, res);

            serverAss.checkRequest(context);
            String after = XmlUtil.nodeToString(req.getXmlKnob().getDocumentReadOnly());
            assertEquals(XALAN_TEST_RESULT, after);

        } catch (Exception ex) {
            fail("Unexpected exception encountered. " + ex);
        }
    }


    public void testCompileWithXalanExtension() {

        try {

            CompiledStylesheet xsl = StylesheetCompiler.compileStylesheet(XALAN_TEST_XSL);
            assertNotNull(xsl);

        } catch (Exception ex) {
            fail("Unexpected exception encountered. " + ex);
        }

    }


    private void setupMockHttpClient(String xslContent) throws ServerPolicyException
    {

        // create dummy assertion
        XslTransformation assertion = new XslTransformation();
        assertion.setDirection(XslTransformation.APPLY_TO_REQUEST);
        AssertionResourceInfo ri = new SingleUrlResourceInfo("http://some.test.url/test.xsd");
        assertion.setResourceInfo(ri);

        // create mock http client
        byte[] xslBytes = xslContent.getBytes();
        MockGenericHttpClient mockClient = new MockGenericHttpClient(200,
                                               new GenericHttpHeaders(new HttpHeader[0]),
                                               ContentTypeHeader.XML_DEFAULT,
                                               (long)xslBytes.length,
                                               xslBytes);
        httpClientFactory.setMockHttpClient(mockClient);
        BeanFactory beanFactory = new SimpleSingletonBeanFactory(new HashMap<String,Object>() {{
            put("httpClientFactory", httpClientFactory);
        }});

        new ServerXslTransformation(assertion, beanFactory);
    }

//    private String getResAsString(String path) throws IOException {
//        InputStream is = getClass().getResourceAsStream(path);
//        byte[] resbytes = HexUtils.slurpStream(is, 20000);
//        return new String(resbytes);
//    }

    /*
     * Test for bug 4789 - xsl stylesheets with output operations using xalan-extensions should be omitted.
     */
//    private static final String XALAN_TEST_FILE = "xalantest.xsl";
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
                    "  xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">\n" +
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
    private static final String XALAN_TEST_RESULT = "<tns:a xmlns:tns=\"http://test.tns\" xmlns:xalan=\"http://xml.apache.org/xalan\">Xalan-Test-Transformation result</tns:a>";
}