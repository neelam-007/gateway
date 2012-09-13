package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.TestStashManagerFactory;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.url.AbstractUrlObjectCache;
import com.l7tech.server.url.HttpObjectCache;
import com.l7tech.server.url.UrlResolver;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.server.util.TestingHttpClientFactory;
import com.l7tech.util.Charsets;
import com.l7tech.util.IOUtils;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.SyspropUtil;
import com.l7tech.xml.xslt.CompiledStylesheet;
import org.junit.*;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * Tests ServerXslTransformation and XslTransformation classes.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 11, 2004<br/>
 *
 */
public class XslTransformationTest {
    private static Logger logger = Logger.getLogger(XslTransformationTest.class.getName());
    private static final String EXPECTED =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "\n" +
            "    <soap:Header xmlns:wsse=\"http://schemas.xmlsoap.org/ws/2002/04/secext\">\n" +
            "        <!--a wsse:Security element was stripped out-->\n" +
            "    </soap:Header>\n" +
            "\n" +
            "    <soap:Body>\n" +
            "        <listProducts xmlns=\"http://warehouse.acme.com/ws\"/>\n" +
            "    </soap:Body>\n" +
            "    \n" +
            "</soap:Envelope>";
    private static final String EXPECTED_VAR_RESULT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
    "VARIABLE CONTENT routingStatus=None" +
    "</soap:Envelope>";
    private Auditor auditor = new Auditor(this, null, logger);

    private Document transform(String xslt, String src) throws Exception {
        TransformerFactory transfoctory = TransformerFactory.newInstance();
        StreamSource xsltsource = new StreamSource(new StringReader(xslt));
        Transformer transformer = transfoctory.newTemplates(xsltsource).newTransformer();
        Document srcdoc = XmlUtil.stringToDocument(src);
        DOMResult result = new DOMResult();
        XmlUtil.softXSLTransform(srcdoc, result, transformer, Collections.EMPTY_MAP);
        return (Document) result.getNode();
    }

    @Before
    public void init() {
        SyspropUtil.setProperty("com.l7tech.xml.xslt.useSaxon", "false");
    }

    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
            "com.l7tech.xml.xslt.useSaxon"
        );
    }

    @Test(expected = ServerPolicyException.class)
    public void testSimpleXS20withNoVersionSet() throws Exception {
        XslTransformation ass = new XslTransformation();
        ass.setTarget(TargetMessageType.REQUEST);
        ass.setWhichMimePart(0);
        ass.setResourceInfo(new StaticResourceInfo(PARSE_PHONE_NUMBER_REGEX_XSLT20));

        // Tries to parse XSLT 2.0 stylesheet using Jaxon, which will fail (complaining about use of regex).
        new TestStaticOnlyServerXslTransformation(ass);
    }

    @Test
    public void testNonXmlInputParsePhoneNumberXS20() throws Exception {
        XslTransformation ass = new XslTransformation();
        ass.setTarget(TargetMessageType.REQUEST);
        ass.setWhichMimePart(0);
        ass.setResourceInfo(new StaticResourceInfo(PARSE_PHONE_NUMBER_REGEX_XSLT20));
        ass.setXsltVersion("2.0");
        ServerXslTransformation sass = new TestStaticOnlyServerXslTransformation(ass);

        // An XML target message is currently still required in order to run an XSLT, so we will use a dummy value
        Message req = new Message(XmlUtil.stringAsDocument("<dummy/>"));

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, null);

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        String after = toString(req);
        assertEquals("num=555-2938,area=604", after);
    }

    private String toString(Message req) throws Exception {
        PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream();
        try {
            IOUtils.copyStream(req.getMimeKnob().getEntireMessageBodyAsInputStream(), baos);
            return baos.toString(Charsets.UTF8);
        } finally {
            baos.close();
        }
    }

    @Test
    public void testServerAssertion() throws Exception {
        XslTransformation ass = new XslTransformation();
        ass.setTarget(TargetMessageType.REQUEST);
        ass.setWhichMimePart(0);
        ass.setResourceInfo(new StaticResourceInfo(getResAsString(XSL_MASK_WSSE)));

        ServerXslTransformation serverAss = new TestStaticOnlyServerXslTransformation(ass);

        Message req = new Message(TestStashManagerFactory.getInstance().createStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(getResAsString(SOAPMSG_WITH_WSSE).getBytes("UTF-8")));
        Message res = new Message();
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, res);

        serverAss.checkRequest(context);
        String after = XmlUtil.nodeToString(req.getXmlKnob().getDocumentReadOnly());
        Assert.assertEquals(after, EXPECTED);
    }

    @Test
    @Ignore("Developer benchmark")
    public void testBenchmark() throws Exception {
        XslTransformation ass = new XslTransformation();
        ass.setResourceInfo(new StaticResourceInfo(getResAsString(XSL_MASK_WSSE)));
        ass.setTarget(TargetMessageType.REQUEST);
        ass.setWhichMimePart(0);

        ServerXslTransformation serverAss = new ServerXslTransformation(ass, ApplicationContexts.getTestApplicationContext());

        long before = System.currentTimeMillis();
        int num = 5000;
        for (int i = 0; i < num; i++) {
            Message req = new Message(TestStashManagerFactory.getInstance().createStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(getResAsString(SOAPMSG_WITH_WSSE).getBytes("UTF-8")));
            Message res = new Message();
            PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, res);

            serverAss.checkRequest(context);
            String after = XmlUtil.nodeToString(req.getXmlKnob().getDocumentReadOnly());
            Assert.assertEquals(after, EXPECTED);
        }
        long after = System.currentTimeMillis();
        System.out.println(num + " messages in " + (after - before) + "ms (" + num / ((after - before)/1000d) + "/s)" );
    }

    @Test
    public void testMaskWsse() throws Exception {
        String xslStr = getResAsString(XSL_MASK_WSSE);
        String xmlStr = getResAsString(SOAPMSG_WITH_WSSE);
        String res = XmlUtil.nodeToString(transform(xslStr, xmlStr));
        if (!EXPECTED.equals(res)) {
            logger.severe("result of transformation not as expected\nresult:\n" + res + "\nexpected:\n" + EXPECTED);
        } else {
            logger.fine("transformation ok");
        }
        Assert.assertTrue(EXPECTED.equals(res));
    }

    @Test
    public void testSsgComment() throws Exception {
        String xslStr = getResAsString(XSL_SSGCOMMENT);
        String xmlStr = getResAsString(SOAPMSG_WITH_WSSE);
        String res = XmlUtil.nodeToString(transform(xslStr, xmlStr));
        String expected = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "\n" +
                "    <soap:Header xmlns:wsse=\"http://schemas.xmlsoap.org/ws/2002/04/secext\">\n" +
                "        <wsse:Security>\n" +
                "            <wsse:UsernameToken Id=\"MyID\">\n" +
                "                    <wsse:Username>Zoe</wsse:Username>\n" +
                "                    <Password Type=\"wsse:PasswordText\">ILoveLlamas</Password>\n" +
                "            </wsse:UsernameToken>\n" +
                "        </wsse:Security>\n" +
                "    </soap:Header>\n" +
                "\n" +
                "    <soap:Body>\n" +
                "        <listProducts xmlns=\"http://warehouse.acme.com/ws\"/>\n" +
                "    </soap:Body><!--SSG WAS HERE-->\n" +
                "    \n" +
                "</soap:Envelope>";
        if (!expected.equals(res)) {
            logger.severe("result of transformation not as expected\nresult:\n" + res + "\nexpected:\n" + expected);
        } else {
            logger.fine("transformation ok");
        }
        Assert.assertTrue(expected.equals(res));
    }

    @Test
    public void testSubstitution() throws Exception {
        String xslStr = getResAsString(XSL_BODYSUBST);
        String xmlStr = getResAsString(SOAPMSG_WITH_WSSE);
        String res = XmlUtil.nodeToString(transform(xslStr, xmlStr));
        String expected = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "\n" +
                "    <soap:Header xmlns:wsse=\"http://schemas.xmlsoap.org/ws/2002/04/secext\">\n" +
                "        <wsse:Security>\n" +
                "            <wsse:UsernameToken Id=\"MyID\">\n" +
                "                    <wsse:Username>Zoe</wsse:Username>\n" +
                "                    <Password Type=\"wsse:PasswordText\">ILoveLlamas</Password>\n" +
                "            </wsse:UsernameToken>\n" +
                "        </wsse:Security>\n" +
                "    </soap:Header>\n" +
                "\n" +
                "    <Body xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "            <accountid xmlns=\"http://warehouse.acme.com/accountns\">5643249816516813216</accountid>\n" +
                "        <listProducts xmlns=\"http://warehouse.acme.com/ws\"/>\n" +
                "    </Body>\n" +
                "    \n" +
                "</soap:Envelope>";
        if (!expected.equals(res)) {
            logger.severe("result of transformation not as expected\nresult:\n" + res + "\nexpected:\n" + expected);
        } else {
            logger.fine("transformation ok");
        }
        Assert.assertTrue(expected.equals(res));
    }

    @Test
    public void testContextVariablesStatic() throws Exception {
        StaticResourceInfo ri = new StaticResourceInfo();
        ri.setDocument(ECF_MDE_ID_XSL);

        XslTransformation assertion = new XslTransformation();
        assertion.setTarget(TargetMessageType.REQUEST);
        assertion.setResourceInfo(ri);
        BeanFactory beanFactory = new SimpleSingletonBeanFactory(new HashMap<String,Object>() {{
            put("httpClientFactory", new TestingHttpClientFactory());
        }});
        ServerAssertion sa = new ServerXslTransformation(assertion, beanFactory);

        Message request = new Message(XmlUtil.stringToDocument(DUMMY_SOAP_XML));
        Message response = new Message();
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        context.setVariable("ecf-mde-id", "VARIABLE CONTENT");
        sa.checkRequest(context);

        String res = new String( IOUtils.slurpStream(request.getMimeKnob().getFirstPart().getInputStream(false)));

        Assert.assertEquals(res, EXPECTED_VAR_RESULT);
    }

    @Test
    public void testContextVariablesStaticWithSaxon() throws Exception {
        SyspropUtil.setProperty("com.l7tech.xml.xslt.useSaxon", "true");

        StaticResourceInfo ri = new StaticResourceInfo();
        ri.setDocument(ECF_MDE_ID_XSL);

        XslTransformation assertion = new XslTransformation();
        assertion.setTarget(TargetMessageType.REQUEST);
        assertion.setResourceInfo(ri);
        BeanFactory beanFactory = new SimpleSingletonBeanFactory(new HashMap<String,Object>() {{
            put("httpClientFactory", new TestingHttpClientFactory());
        }});
        ServerXslTransformation sa = new ServerXslTransformation(assertion, beanFactory);

        Message request = new Message(XmlUtil.stringToDocument(DUMMY_SOAP_XML));
        Message response = new Message();
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        context.setVariable("ecf-mde-id", "VARIABLE CONTENT");
        sa.checkRequest(context);

        String res = new String( IOUtils.slurpStream(request.getMimeKnob().getFirstPart().getInputStream(false)));

        Assert.assertEquals(res, EXPECTED_VAR_RESULT);

        // Ensure Saxon was actually used
        CompiledStylesheet cs = sa.resourceGetter.getResource(request.getXmlKnob(), context.getVariableMap(assertion.getVariablesUsed(), auditor));
        assertEquals("Saxon should have been used to compile the stylesheet", "net.sf.saxon.PreparedStylesheet", getSoftwareStylesheetClassname(cs));
    }

    private String getSoftwareStylesheetClassname(CompiledStylesheet cs) {
        return getField(cs, "softwareStylesheet").getClass().getName();
    }

    // Gets a value of a private field
    private static Object getField(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testContextVariablesRemote() throws Exception {
        XslTransformation assertion = new XslTransformation();
        assertion.setTarget(TargetMessageType.REQUEST);
        AssertionResourceInfo ri = new SingleUrlResourceInfo("http://bogus.example.com/blah.xsd");
        assertion.setResourceInfo(ri);
        ServerAssertion sa = new ServerXslTransformation(assertion, null){
            @Override
            protected UrlResolver<CompiledStylesheet> getCache( final HttpObjectCache.UserObjectFactory<CompiledStylesheet> cacheObjectFactory,
                                                                final BeanFactory spring) {
                return new UrlResolver<CompiledStylesheet>(){
                    public CompiledStylesheet resolveUrl(Audit audit,String url) throws IOException, ParseException {
                        return cacheObjectFactory.createUserObject(url, new AbstractUrlObjectCache.UserObjectSource(){
                            public ContentTypeHeader getContentType() {return ContentTypeHeader.TEXT_DEFAULT;}
                            public String getString(boolean isXml) throws IOException {return ECF_MDE_ID_XSL;}
                            public byte[] getBytes() throws IOException {return ECF_MDE_ID_XSL.getBytes();}
                        });
                    }
                };
            }
        };

        Message request = new Message(XmlUtil.stringToDocument(DUMMY_SOAP_XML));
        Message response = new Message();
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        context.setVariable("ecf-mde-id", "VARIABLE CONTENT");
        sa.checkRequest(context);

        String res = new String(IOUtils.slurpStream(request.getMimeKnob().getFirstPart().getInputStream(false)));

        Assert.assertEquals(res, EXPECTED_VAR_RESULT);
    }

    @Test
    @Ignore("Developer only test")
    public void testReutersUseCase2() throws Exception {
        String responseUrl = "http://locutus/reuters/response2.xml";
        String xslUrl = "http://locutus/reuters/stylesheet2.xsl";

        XslTransformation xsl = new XslTransformation();
        xsl.setResourceInfo(new SingleUrlResourceInfo(xslUrl));
        xsl.setTarget(TargetMessageType.REQUEST);

        Message request = new Message(TestStashManagerFactory.getInstance().createStashManager(), ContentTypeHeader.XML_DEFAULT, new URL(responseUrl).openStream());
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());

        ServerXslTransformation sxsl = new ServerXslTransformation(xsl, ApplicationContexts.getTestApplicationContext());
        AssertionStatus status = sxsl.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    private String getResAsString(String path) throws IOException {
        InputStream is = getClass().getResourceAsStream(path);
        byte[] resbytes = IOUtils.slurpStream(is, 20000);
        return new String(resbytes);
    }

    private static final String XSL_MASK_WSSE = "wssemask.xsl";
    private static final String XSL_SSGCOMMENT = "ssgwashere.xsl";
    private static final String XSL_BODYSUBST = "bodySubstitution.xsl";
    private static final String SOAPMSG_WITH_WSSE = "simpleRequestWithWsseHeader.xml";

    private static final String DUMMY_SOAP_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
            "<soap:Body><payload xmlns=\"http://blit.example.com/\">body body</payload></soap:Body></soap:Envelope>";

    private static final String ECF_MDE_ID_XSL =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xsl:transform version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n" +
            "                             xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
            "<xsl:param name=\"ecf-mde-id\"/>" +
            "<xsl:param name=\"routingStatus\"/>" +
            "<xsl:template match=\"soapenv:Body\">" +
            "<xsl:value-of select=\"$ecf-mde-id\"/>" +
            " routingStatus=<xsl:value-of select=\"$routingStatus\"/>" +
            "</xsl:template>\n" +
            "    <xsl:template match=\"node()|@*\">\n" +
            "        <xsl:copy>\n" +
            "            <xsl:apply-templates select=\"node()|@*\" />\n" +
            "        </xsl:copy>\n" +
            "    </xsl:template>\n" +
            "</xsl:transform>";

    private static final String PARSE_PHONE_NUMBER_REGEX_XSLT20 =
        "<?xml version=\"1.0\"?>\n" +
            "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xdt=\"http://www.w3.org/2003/05/xpath-datatypes\" version=\"2.0\">\n" +
            "<xsl:output method=\"text\"/>\n" +
            "<xsl:param name=\"phone\" select=\"'604-555-2938'\"/>\n" +
            "<xsl:template match=\"/\">\n" +
            "  <xsl:analyze-string select=\"$phone\" \n" +
            "       regex=\"([0-9]+)-([0-9]+)-([0-9]+)\">\n" +
            "    <xsl:matching-substring>\n" +
            "      <xsl:text>num=</xsl:text>\n" +
            "      <xsl:number value=\"regex-group(2)\" format=\"01\"/>\n" +
            "      <xsl:text>-</xsl:text>\n" +
            "      <xsl:number value=\"regex-group(3)\" format=\"0001\"/>\n" +
            "      <xsl:text>,area=</xsl:text>\n" +
            "      <xsl:number value=\"regex-group(1)\" format=\"01\"/>\n" +
            "    </xsl:matching-substring>\n" +
            "  </xsl:analyze-string>\n" +
            "</xsl:template>\n" +
            "\n" +
            "</xsl:stylesheet>\n";

    private static class TestStaticOnlyServerXslTransformation extends ServerXslTransformation {
        public TestStaticOnlyServerXslTransformation(XslTransformation ass) throws ServerPolicyException {
            //noinspection NullableProblems
            super(ass, null);
        }

        @Override
        protected UrlResolver<CompiledStylesheet> getCache(HttpObjectCache.UserObjectFactory<CompiledStylesheet> cacheObjectFactory, BeanFactory spring) {
            return null;
        }
    }
}
