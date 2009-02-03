package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.IOUtils;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.Message;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.TestingHttpClientFactory;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.TestStashManagerFactory;
import com.l7tech.server.url.HttpObjectCache;
import com.l7tech.server.url.UrlResolver;
import com.l7tech.server.url.AbstractUrlObjectCache;
import com.l7tech.spring.util.SimpleSingletonBeanFactory;
import com.l7tech.xml.xslt.CompiledStylesheet;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Document;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Ignore;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;
import java.text.ParseException;

/**
 * Tests ServerXslTransformation and XslTransformation classes.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 11, 2004<br/>
 * $Id$<br/>
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
            "        <listProducts xmlns=\"http://warehouse.acme.com/ws\"></listProducts>\n" +
            "    </soap:Body>\n" +
            "    \n" +
            "</soap:Envelope>";
    private static final String EXPECTED_VAR_RESULT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
    "VARIABLE CONTENT routingStatus=None" +
    "</soap:Envelope>";

    private Document transform(String xslt, String src) throws Exception {
        TransformerFactory transfoctory = TransformerFactory.newInstance();
        StreamSource xsltsource = new StreamSource(new StringReader(xslt));
        Transformer transformer = transfoctory.newTemplates(xsltsource).newTransformer();
        Document srcdoc = XmlUtil.stringToDocument(src);
        DOMResult result = new DOMResult();
        XmlUtil.softXSLTransform(srcdoc, result, transformer, Collections.EMPTY_MAP);
        return (Document) result.getNode();
    }

    @Test
    public void testServerAssertion() throws Exception {
        XslTransformation ass = new XslTransformation();
        ass.setDirection(XslTransformation.APPLY_TO_REQUEST);
        ass.setWhichMimePart(0);
        ass.setResourceInfo(new StaticResourceInfo(getResAsString(XSL_MASK_WSSE)));

        ServerXslTransformation serverAss = new ServerXslTransformation(ass, null){
            @Override
            protected UrlResolver<CompiledStylesheet> getCache(HttpObjectCache.UserObjectFactory<CompiledStylesheet> cacheObjectFactory, BeanFactory spring) {
                return null;
            }
        };

        Message req = new Message(TestStashManagerFactory.getInstance().createStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(getResAsString(SOAPMSG_WITH_WSSE).getBytes("UTF-8")));
        Message res = new Message();
        PolicyEnforcementContext context = new PolicyEnforcementContext(req, res);

        serverAss.checkRequest(context);
        String after = XmlUtil.nodeToString(req.getXmlKnob().getDocumentReadOnly());
        Assert.assertEquals(after, EXPECTED);
    }

    @Test
    @Ignore("Developer benchmark")
    public void testBenchmark() throws Exception {
        XslTransformation ass = new XslTransformation();
        ass.setResourceInfo(new StaticResourceInfo(getResAsString(XSL_MASK_WSSE)));
        ass.setDirection(XslTransformation.APPLY_TO_REQUEST);
        ass.setWhichMimePart(0);

        ServerXslTransformation serverAss = new ServerXslTransformation(ass, ApplicationContexts.getTestApplicationContext());

        long before = System.currentTimeMillis();
        int num = 5000;
        for (int i = 0; i < num; i++) {
            Message req = new Message(TestStashManagerFactory.getInstance().createStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(getResAsString(SOAPMSG_WITH_WSSE).getBytes("UTF-8")));
            Message res = new Message();
            PolicyEnforcementContext context = new PolicyEnforcementContext(req, res);

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
                "        <listProducts xmlns=\"http://warehouse.acme.com/ws\"></listProducts>\n" +
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
                "        <listProducts xmlns=\"http://warehouse.acme.com/ws\"></listProducts>\n" +
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
        assertion.setDirection(XslTransformation.APPLY_TO_REQUEST);
        assertion.setResourceInfo(ri);
        BeanFactory beanFactory = new SimpleSingletonBeanFactory(new HashMap<String,Object>() {{
            put("httpClientFactory", new TestingHttpClientFactory());
        }});
        ServerAssertion sa = new ServerXslTransformation(assertion, beanFactory);

        Message request = new Message(XmlUtil.stringToDocument(DUMMY_SOAP_XML));
        Message response = new Message();
        PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
        context.setVariable("ecf-mde-id", "VARIABLE CONTENT");
        sa.checkRequest(context);

        String res = new String( IOUtils.slurpStream(request.getMimeKnob().getFirstPart().getInputStream(false)));

        Assert.assertEquals(res, EXPECTED_VAR_RESULT);
    }

    @Test
    public void testContextVariablesRemote() throws Exception {
        XslTransformation assertion = new XslTransformation();
        assertion.setDirection(XslTransformation.APPLY_TO_REQUEST);
        AssertionResourceInfo ri = new SingleUrlResourceInfo("http://bogus.example.com/blah.xsd");
        assertion.setResourceInfo(ri);
        ServerAssertion sa = new ServerXslTransformation(assertion, null){
            @Override
            protected UrlResolver<CompiledStylesheet> getCache( final HttpObjectCache.UserObjectFactory<CompiledStylesheet> cacheObjectFactory,
                                                                final BeanFactory spring) {
                return new UrlResolver<CompiledStylesheet>(){
                    public CompiledStylesheet resolveUrl(String url) throws IOException, ParseException {
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
        PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
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
        xsl.setDirection(XslTransformation.APPLY_TO_REQUEST);

        Message request = new Message(TestStashManagerFactory.getInstance().createStashManager(), ContentTypeHeader.XML_DEFAULT, new URL(responseUrl).openStream());
        PolicyEnforcementContext pec = new PolicyEnforcementContext(request, new Message());

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

}
