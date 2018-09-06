package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.http.GenericHttpClientFactory;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.Message;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.MockGenericHttpClient;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.ServerConfigParams;
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
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Charsets;
import com.l7tech.util.IOUtils;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.SyspropUtil;
import com.l7tech.xml.xslt.CompiledStylesheet;
import org.junit.*;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

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

    private static final String EXPECTED_NOVAR_RESULT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
            "defaultValueOfParam routingStatus=None" +
            "</soap:Envelope>";

    private Auditor auditor = new Auditor(this, null, logger);
    private TestHelper helper = new TestHelper();

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
    public void testNonXmlInputParsePhoneNumberXS20_noVar() throws Exception {
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

    @Test
    @BugId( "SSG-12209" )
    public void testNonXmlInputParsePhoneNumberXS20_withVar() throws Exception {
        XslTransformation ass = new XslTransformation();
        ass.setTarget(TargetMessageType.REQUEST);
        ass.setWhichMimePart(0);
        ass.setResourceInfo(new StaticResourceInfo(PARSE_PHONE_NUMBER_REGEX_XSLT20));
        ass.setXsltVersion("2.0");
        ServerXslTransformation sass = new TestStaticOnlyServerXslTransformation(ass);

        // An XML target message is currently still required in order to run an XSLT, so we will use a dummy value
        Message req = new Message(XmlUtil.stringAsDocument("<dummy/>"));

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, null);
        context.setVariable( "phone", "542-555-5753" );

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        String after = toString(req);

        // TODO Remove this test after XsltUtil is fixed per SSG-12209
        assertEquals( "value from context variable is currently ignored (for Saxon only) if a select expression is specified",
                "num=555-2938,area=604", after);

        // TODO Uncomment this test after XsltUtil is fixed per SSG-12209
        // assertEquals( "value from context variable shall override value from select expression", "num=555-5753,area=542", after);
    }

    @Test
    public void testNonXmlInputParsePhoneNumberXS20_fromVariable() throws Exception {
        XslTransformation ass = new XslTransformation();
        ass.setTarget(TargetMessageType.REQUEST);
        ass.setWhichMimePart(0);
        ass.setResourceInfo( new StaticResourceInfo( PARSE_PHONE_NUMBER_REGEX_XSLT20_NO_SELECT_EXPR ) );
        ass.setXsltVersion("2.0");
        ServerXslTransformation sass = new TestStaticOnlyServerXslTransformation(ass);

        // An XML target message is currently still required in order to run an XSLT, so we will use a dummy value
        Message req = new Message(XmlUtil.stringAsDocument("<dummy/>"));

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, null);
        context.setVariable( "phone", "761-555-8371" );

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        String after = toString(req);
        assertEquals("num=555-8371,area=761", after);
    }

    @Test
    public void testNonXmlInputParsePhoneNumberXS20_fromVariable_noVar() throws Exception {
        XslTransformation ass = new XslTransformation();
        ass.setTarget(TargetMessageType.REQUEST);
        ass.setWhichMimePart(0);
        ass.setResourceInfo( new StaticResourceInfo( PARSE_PHONE_NUMBER_REGEX_XSLT20_NO_SELECT_EXPR ) );
        ass.setXsltVersion("2.0");
        ServerXslTransformation sass = new TestStaticOnlyServerXslTransformation(ass);

        // An XML target message is currently still required in order to run an XSLT, so we will use a dummy value
        Message req = new Message(XmlUtil.stringAsDocument("<dummy/>"));

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, null);
        // Allow source var to not exist
        //context.setVariable( "phone", "761-555-8371" );

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        String after = toString(req);
        assertEquals( "No output should be produced -- regex should fail to match empty input", "", after);
    }

    private String toString(Message req) throws Exception {
        try (PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream()) {
            IOUtils.copyStream(req.getMimeKnob().getEntireMessageBodyAsInputStream(), baos);
            return baos.toString(Charsets.UTF8);
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
    @BugNumber(13231)
    public void testMessageUrlResourceInfo() throws Exception {
        XslTransformation ass = new XslTransformation();
        ass.setResourceInfo(new MessageUrlResourceInfo(new String[] { ".*" }));

        final byte[] xslBytes = getResAsString(XSL_BODYSUBST).getBytes(Charsets.UTF8);

        ServerXslTransformation sass = new ServerXslTransformation(ass, null) {
            @Override
            protected UrlResolver<CompiledStylesheet> getCache(AbstractUrlObjectCache.UserObjectFactory<CompiledStylesheet> cacheObjectFactory, BeanFactory spring) {
                GenericHttpClientFactory clientFactory = new TestingHttpClientFactory(new MockGenericHttpClient(200, null, ContentTypeHeader.XML_DEFAULT, (long)xslBytes.length, xslBytes));
                return httpObjectCache = new HttpObjectCache<CompiledStylesheet>(
                    "XSL-T",
                    10000,
                    300000,
                    -1,
                    clientFactory, cacheObjectFactory, HttpObjectCache.WAIT_INITIAL, ServerConfigParams.PARAM_XSL_MAX_DOWNLOAD_SIZE);
            }
        };

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.getRequest().initialize(XmlUtil.stringAsDocument(getResAsString(SOAPMSG_WITH_WSSE_AND_PI)));

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        String xmlAfter = XmlUtil.nodeToString(context.getRequest().getXmlKnob().getDocumentReadOnly());
        assertEqualsIgnoringWhitespace(EXPECTED_AFTER_BODY_SUBSTITUTION, xmlAfter.replaceAll("<\\?.*?\\?>", ""));
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
        if (!EXPECTED_AFTER_BODY_SUBSTITUTION.equals(res)) {
            logger.severe("result of transformation not as expected\nresult:\n" + res + "\nexpected:\n" + EXPECTED_AFTER_BODY_SUBSTITUTION);
        } else {
            logger.fine("transformation ok");
        }
        Assert.assertTrue(EXPECTED_AFTER_BODY_SUBSTITUTION.equals(res));
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
    public void testContextVariablesStatic_contextVariableDoesntExist() throws Exception {
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
        // Do NOT set variable, and ensure result takes default value from select expr
        //context.setVariable("ecf-mde-id", "VARIABLE CONTENT");
        sa.checkRequest(context);

        String res = new String( IOUtils.slurpStream(request.getMimeKnob().getFirstPart().getInputStream(false)));

        Assert.assertEquals(res, EXPECTED_NOVAR_RESULT);
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

        // TODO Remove this test after XsltUtil is fixed per SSG-12209
        Assert.assertEquals( "value from context variable is currently ignored (for Saxon) if a select expression is specified",
                res, EXPECTED_NOVAR_RESULT );

        // TODO Uncomment this test after XsltUtil is fixed per SSG-12209
        // Assert.assertEquals( "value from select expression is overridded by context variable", res, EXPECTED_VAR_RESULT);

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
    private static final String SOAPMSG_WITH_WSSE_AND_PI = "simpleRequestWithWsseHeaderAndXmlStylesheetPi.xml";

    private static final String DUMMY_SOAP_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
            "<soap:Body><payload xmlns=\"http://blit.example.com/\">body body</payload></soap:Body></soap:Envelope>";

    private static final String ECF_MDE_ID_XSL =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xsl:transform version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n" +
            "                             xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
            "<xsl:param name=\"ecf-mde-id\" select=\"'defaultValueOfParam'\" />" +
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

    private static final String PARSE_PHONE_NUMBER_REGEX_XSLT20_NO_SELECT_EXPR =
            "<?xml version=\"1.0\"?>\n" +
                    "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xdt=\"http://www.w3.org/2003/05/xpath-datatypes\" version=\"2.0\">\n" +
                    "<xsl:output method=\"text\"/>\n" +
                    "<xsl:param name=\"phOne\" />\n" +
                    "<xsl:template match=\"/\">\n" +
                    "  <xsl:analyze-string select=\"$phOne\" \n" +
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

    // What the SOAPMSG_WITH_WSSE is expected to look like after the body substitution xslt is performed
    public static final String EXPECTED_AFTER_BODY_SUBSTITUTION =
        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
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

    private static void assertEqualsIgnoringWhitespace(String expected, String actual) {
        assertEquals(expected.replaceAll(" |\r|\n|\t", ""), actual.replaceAll(" |\r|\n|\t", ""));
    }

    private static final String XSLT_IDENTITY_WITH_MESSAGE =
        "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" +
            "  <!-- Identity transform -->\n" +
            "  <xsl:template match=\"@*|*|processing-instruction()|comment()\">\n" +
            "    <xsl:message>New msg 1</xsl:message>\n" +
            "    <xsl:message>New msg 2</xsl:message>\n" +
            "    <xsl:copy>\n" +
            "      <xsl:apply-templates\n" +
            "select=\"*|@*|text()|processing-instruction()|comment()\"/>\n" +
            "    </xsl:copy>\n" +
            "  </xsl:template>\n" +
            "</xsl:stylesheet>";

    @BugNumber(13218)
    @Test
    public void testXslMessages() throws Exception {
        doTestXslMessages("1.0");
    }

    @BugNumber(13218)
    @Test
    public void testXslMessagesXslt20() throws Exception {
        doTestXslMessages("2.0");
    }

    private void doTestXslMessages(String xsltVersion) throws SAXException, IOException, PolicyAssertionException, NoSuchVariableException {
        XslTransformation assertion = new XslTransformation();
        assertion.setTarget(TargetMessageType.REQUEST);
        AssertionResourceInfo ri = new StaticResourceInfo(XSLT_IDENTITY_WITH_MESSAGE);
        assertion.setResourceInfo(ri);
        assertion.setMsgVarPrefix("pfx");
        assertion.setXsltVersion(xsltVersion);
        ServerAssertion sa = new TestStaticOnlyServerXslTransformation(assertion);

        Message request = new Message(XmlUtil.stringToDocument(DUMMY_SOAP_XML));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());

        AssertionStatus result = sa.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        String[] messages = (String[])context.getVariable("pfx.messages");
        assertEquals(2, messages.length);

        assertEquals("New msg 1", context.getVariable("pfx.messages.first"));
        assertEquals("New msg 2", context.getVariable("pfx.messages.last"));
    }

    @BugId( "SSG-9081" )
    @Test
    public void testTransformEmitsXhtmlWithAttributes() throws Exception {
        // Stock Xalan 2.7.2 has a problem with XHTML attributes when processing in secure mode.
        // One of our attempts to fix this "worked" but had the side effect of stripping the attributes
        // out of the output completely.  This unit test checks this.

        XslTransformation assertion = new XslTransformation();
        assertion.setTarget(TargetMessageType.REQUEST);
        AssertionResourceInfo ri = new StaticResourceInfo(
                "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" +
                "  <xsl:output method=\"html\"/>\n" +
                "  <xsl:template match=\"/*\">\n" +
                "    <th colspan=\"2\"/>\n" +
                "  </xsl:template>\n" +
                "</xsl:stylesheet>" );
        assertion.setResourceInfo(ri);
        assertion.setXsltVersion( "1.0" );
        ServerAssertion sa = new TestStaticOnlyServerXslTransformation(assertion);

        Message request = new Message(XmlUtil.stringToDocument(DUMMY_SOAP_XML));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());

        AssertionStatus result = sa.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        String output = new String( IOUtils.slurpStream( context.getRequest().getMimeKnob().getEntireMessageBodyAsInputStream() ), Charsets.UTF8 );
        assertEquals( "<th colspan=\"2\"></th>" + System.lineSeparator(), output );
    }

    @BugId("DE364342")
    @Test
    public void testXsltTransformForInvalidSoapXmlInMainPart() throws Exception {
        final String inputXml = DUMMY_SOAP_XML.replace("<soap:Body>", "<Body>");
        final PolicyEnforcementContext context = helper.createContext(helper.createMessage(inputXml));
        final AssertionStatus status = helper.checkRequest(helper.createAssertion(), context);

        assertNotEquals(AssertionStatus.NONE, status);
        assertEquals("Should be able to read the request", inputXml, helper.readMessage(context.getRequest()));
    }

    @BugId("DE364342")
    @Test
    public void testXsltTransformForInvalidSoapXmlInPart1() throws Exception {
        final String contentType = "multipart/form-data; boundary=simple";
        final String multipartMessage = "--simple\r\n" +
                "Content-Type: text/xml\r\n" +
                "Content-Disposition: form-data; name=\"part0\"\r\n" +
                "\r\n" +
                DUMMY_SOAP_XML + "\r\n" +
                "--simple\r\n" +
                "Content-Type: text/xml\r\n" +
                "Content-Disposition: form-data; name=\"part1\"\r\n" +
                "\r\n" +
                DUMMY_SOAP_XML.replace("<soap:Body>", "<Body>") + "\r\n" +
                "--simple--";
        final PolicyEnforcementContext context = helper.createContext(helper.createMessage(contentType, multipartMessage));
        final AssertionStatus status = helper.checkRequest(helper.createAssertion(1), context);

        assertNotEquals(AssertionStatus.NONE, status);
        assertEquals("Should be able to read the request", multipartMessage, helper.readMessage(context.getRequest()).trim());
    }

    @Test (expected = SAXParseException.class)
    public void testXsltTranformParseXmlContainingDocTypeDeclarations() throws Exception {
        final XslTransformation assertion = new XslTransformation();
        assertion.setTarget(TargetMessageType.REQUEST);
        AssertionResourceInfo ri = new StaticResourceInfo(XSLT_IDENTITY_WITH_MESSAGE);
        assertion.setResourceInfo(ri);

        final String inputXml =
                "<!DOCTYPE foo [\n" +
                "  <!ELEMENT foo (#PCDATA)>\n" +
                "  <!ENTITY bar \"Hello World\">\n" +
                "]>\n" +
                "<foo>&bar;</foo>";
        final ServerXslTransformation serverAssertion = new TestStaticOnlyServerXslTransformation(assertion);
        serverAssertion.parseXml(inputXml, new DefaultHandler() {
            public void startElement (String uri, String localName,
                                      String qName, Attributes attributes)
                    throws SAXException {
                fail("DOCTYPE declarations are disallowed and hence XML shouldn't be parsed");
            }
        });
    }

    private class TestHelper {

        public XslTransformation createAssertion(final String xsltVersion) {
            final XslTransformation assertion = new XslTransformation();

            assertion.setTarget(TargetMessageType.REQUEST);
            assertion.setResourceInfo(new StaticResourceInfo(XSLT_IDENTITY_WITH_MESSAGE));
            assertion.setMsgVarPrefix("pfx");
            assertion.setXsltVersion(xsltVersion);

            return assertion;
        }

        public XslTransformation createAssertion() {
            return createAssertion("1.0");
        }

        public XslTransformation createAssertion(final int partIndex) {
            final XslTransformation assertion = createAssertion();
            assertion.setWhichMimePart(partIndex);
            return assertion;
        }

        public PolicyEnforcementContext createContext(final Message request) {
            return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        }

        public Message createMessage(final String xml) throws IOException {
            final Message message = new Message();
            message.initialize(ContentTypeHeader.XML_DEFAULT, xml.getBytes("UTF-8"));
            return message;
        }

        public Message createMessage(final String contentType, final String content) throws IOException {
            final Message message = new Message();
            message.initialize(ContentTypeHeader.create(contentType), content.getBytes("UTF-8"));
            return message;
        }

        public String readMessage(final Message message) throws Exception {
            return XslTransformationTest.this.toString(message);
        }

        public AssertionStatus checkRequest(final XslTransformation assertion, final PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
            return new TestStaticOnlyServerXslTransformation(assertion).checkRequest(context);
        }

    }
}
