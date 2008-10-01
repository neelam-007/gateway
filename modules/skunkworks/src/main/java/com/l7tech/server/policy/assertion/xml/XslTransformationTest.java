package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.io.IOUtils;
import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.message.Message;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.HttpClientFactory;
import com.l7tech.spring.util.SimpleSingletonBeanFactory;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;

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
public class XslTransformationTest extends TestCase {
    private static Logger logger = Logger.getLogger(XslTransformationTest.class.getName());
    private static final String EXPECTED = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
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

    private HttpClientFactory httpClientFactory;
    private GenericHttpClient mockClient;

    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
        System.out.println("Test complete: " + XslTransformationTest.class);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(XslTransformationTest.class);
        return suite;
    }

    protected void setUp() throws Exception {
        // Make sure the static HTTP object cache inside the ServerXslTransformation class gets initialized
        // with our TestingHttpClientFactory instead of finding a real one
        XslTransformation assertion = new XslTransformation();
        assertion.setDirection(XslTransformation.APPLY_TO_REQUEST);
        AssertionResourceInfo ri = new SingleUrlResourceInfo("http://bogus.example.com/blah.xsd");
        assertion.setResourceInfo(ri);
        httpClientFactory = null;//new TestingHttpClientFactory();
        byte[] xslBytes = null;//ECF_MDE_ID_XSL.getBytes();
        mockClient = null;//new MockGenericHttpClient(200,
//                                               new GenericHttpHeaders(new HttpHeader[0]),
//                                               ContentTypeHeader.XML_DEFAULT,
//                                               (long)xslBytes.length,
//                                               xslBytes);
        //httpClientFactory.setMockHttpClient(mockClient);
        BeanFactory beanFactory = new SimpleSingletonBeanFactory(new HashMap<String,Object>() {{
            put("httpClientFactory", httpClientFactory);
        }});
        new ServerXslTransformation(assertion, beanFactory);
    }

    private Document transform(String xslt, String src) throws Exception {
        TransformerFactory transfoctory = TransformerFactory.newInstance();
        StreamSource xsltsource = new StreamSource(new StringReader(xslt));
        Transformer transformer = transfoctory.newTemplates(xsltsource).newTransformer();
        Document srcdoc = XmlUtil.stringToDocument(src);
        DOMResult result = new DOMResult();
        XmlUtil.softXSLTransform(srcdoc, result, transformer, Collections.EMPTY_MAP);
        return (Document) result.getNode();
    }

    public void testServerAssertion() throws Exception {
        XslTransformation ass = new XslTransformation();
        ass.setDirection(XslTransformation.APPLY_TO_REQUEST);
        ass.setWhichMimePart(0);
        ass.setResourceInfo(new StaticResourceInfo(getResAsString(XSL_MASK_WSSE)));

        ServerXslTransformation serverAss = null;//new ServerXslTransformation(ass, ApplicationContexts.getTestApplicationContext());

        Message req =null;// new Message(TestStashManagerFactory.getInstance().createStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(getResAsString(SOAPMSG_WITH_WSSE).getBytes("UTF-8")));
        Message res = new Message();
        PolicyEnforcementContext context = new PolicyEnforcementContext(req, res);

        serverAss.checkRequest(context);
        String after = XmlUtil.nodeToString(req.getXmlKnob().getDocumentReadOnly());
        assertEquals(after, EXPECTED);
    }

    public void testBenchmark() throws Exception {
        XslTransformation ass = new XslTransformation();
        ass.setResourceInfo(new StaticResourceInfo(getResAsString(XSL_MASK_WSSE)));
        ass.setDirection(XslTransformation.APPLY_TO_REQUEST);
        ass.setWhichMimePart(0);

        ServerXslTransformation serverAss =null;// new ServerXslTransformation(ass, ApplicationContexts.getTestApplicationContext());

        long before = System.currentTimeMillis();
        int num = 5000;
        for (int i = 0; i < num; i++) {
            Message req =null;// new Message(TestStashManagerFactory.getInstance().createStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(getResAsString(SOAPMSG_WITH_WSSE).getBytes("UTF-8")));
            Message res = new Message();
            PolicyEnforcementContext context = new PolicyEnforcementContext(req, res);

            serverAss.checkRequest(context);
            String after = XmlUtil.nodeToString(req.getXmlKnob().getDocumentReadOnly());
            assertEquals(after, EXPECTED);
        }
        long after = System.currentTimeMillis();
        System.out.println(num + " messages in " + (after - before) + "ms (" + num / ((after - before)/1000d) + "/s)" );
    }


    public void testMaskWsse() throws Exception {
        String xslStr = getResAsString(XSL_MASK_WSSE);
        String xmlStr = getResAsString(SOAPMSG_WITH_WSSE);
        String res = XmlUtil.nodeToString(transform(xslStr, xmlStr));
        if (!EXPECTED.equals(res)) {
            logger.severe("result of transformation not as expected\nresult:\n" + res + "\nexpected:\n" + EXPECTED);
        } else {
            logger.fine("transformation ok");
        }
        assertTrue(EXPECTED.equals(res));
    }

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
        assertTrue(expected.equals(res));
    }

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
        assertTrue(expected.equals(res));
    }

    public void testContextVariablesStatic() throws Exception {
        StaticResourceInfo ri = new StaticResourceInfo();
        //ri.setDocument(ECF_MDE_ID_XSL);

        XslTransformation assertion = new XslTransformation();
        assertion.setDirection(XslTransformation.APPLY_TO_REQUEST);
        assertion.setResourceInfo(ri);
        BeanFactory beanFactory =null;// new SimpleSingletonBeanFactory(new HashMap<String,Object>() {{
            //put("httpClientFactory", new TestingHttpClientFactory());
        //}});
        ServerAssertion sa = new ServerXslTransformation(assertion, beanFactory);

        Message request = new Message(XmlUtil.stringToDocument(DUMMY_SOAP_XML));
        Message response = new Message();
        PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
        context.setVariable("ecf-mde-id", "VARIABLE CONTENT");
        sa.checkRequest(context);

        String res = new String( IOUtils.slurpStream(request.getMimeKnob().getFirstPart().getInputStream(false)));

        assertEquals(res, EXPECTED_VAR_RESULT);
    }

    public void testContextVariablesRemote() throws Exception {

        byte[] xslBytes = ECF_MDE_ID_XSL.getBytes();

        XslTransformation assertion = new XslTransformation();
        assertion.setDirection(XslTransformation.APPLY_TO_REQUEST);
        AssertionResourceInfo ri = new SingleUrlResourceInfo("http://bogus.example.com/blah.xsd");
        assertion.setResourceInfo(ri);
        BeanFactory beanFactory = new SimpleSingletonBeanFactory(new HashMap<String,Object>() {{
            put("httpClientFactory", httpClientFactory);
        }});
        ServerAssertion sa = new ServerXslTransformation(assertion, beanFactory);

        Message request = new Message(XmlUtil.stringToDocument(DUMMY_SOAP_XML));
        Message response = new Message();
        PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
        context.setVariable("ecf-mde-id", "VARIABLE CONTENT");
        sa.checkRequest(context);

        String res = new String(IOUtils.slurpStream(request.getMimeKnob().getFirstPart().getInputStream(false)));

        assertEquals(res, EXPECTED_VAR_RESULT);
    }

    public void DISABLED_testReutersUseCase2() throws Exception {
        String responseUrl = "http://locutus/reuters/response2.xml";
        String xslUrl = "http://locutus/reuters/stylesheet2.xsl";

        XslTransformation xsl = new XslTransformation();
        xsl.setResourceInfo(new SingleUrlResourceInfo(xslUrl));
        xsl.setDirection(XslTransformation.APPLY_TO_REQUEST);

        Message request = null;//new Message(TestStashManagerFactory.getInstance().createStashManager(), ContentTypeHeader.XML_DEFAULT, new URL(responseUrl).openStream());
        PolicyEnforcementContext pec = new PolicyEnforcementContext(request, new Message());

        ServerXslTransformation sxsl = null;//new ServerXslTransformation(xsl, ApplicationContexts.getTestApplicationContext());
        AssertionStatus status = sxsl.checkRequest(pec);
        assertEquals(AssertionStatus.NONE, status);
    }

    public void testStuff() throws Exception {
        byte[] bytes = IOUtils.slurpStream(new URL("http://locutus/reuters/response5.xml").openStream());
        char[] chars = new String(bytes, "UTF-8").toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c > 127) System.out.print(c);
        }
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
