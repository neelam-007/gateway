package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.message.Message;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.ApplicationContexts;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.ByteArrayInputStream;
import java.util.logging.Logger;
import java.util.Collections;

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

    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
        System.out.println("Test complete: " + XslTransformationTest.class);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(XslTransformationTest.class);
        return suite;
    }

    private Document transform(String xslt, String src) throws Exception {
        TransformerFactory transfoctory = TransformerFactory.newInstance();
        StreamSource xsltsource = new StreamSource(new StringReader(xslt));
        Transformer transformer = transfoctory.newTemplates(xsltsource).newTransformer();
        Document srcdoc = XmlUtil.stringToDocument(src);
        return XmlUtil.softXSLTransform(srcdoc, transformer, Collections.EMPTY_MAP);
    }

    public void testServerAssertion() throws Exception {
        XslTransformation ass = new XslTransformation();
        ass.setXslSrc(getResAsString(XSL_MASK_WSSE));
        ass.setDirection(XslTransformation.APPLY_TO_REQUEST);
        ass.setWhichMimePart(0);

        ServerXslTransformation serverAss = new ServerXslTransformation(ass, ApplicationContexts.getTestApplicationContext());

        Message req = new Message(StashManagerFactory.createStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(getResAsString(SOAPMSG_WITH_WSSE).getBytes("UTF-8")));
        Message res = new Message();
        PolicyEnforcementContext context = new PolicyEnforcementContext(req, res);

        serverAss.checkRequest(context);
        String after = XmlUtil.nodeToString(req.getXmlKnob().getDocumentReadOnly());
        assertEquals(after, EXPECTED);
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

    private String getResAsString(String path) throws IOException {
        InputStream is = getClass().getResourceAsStream(path);
        byte[] resbytes = HexUtils.slurpStream(is, 20000);
        return new String(resbytes);
    }

    private static final String RESOURCE_PATH = "/com/l7tech/server/policy/assertion/xml/";
	private static final String XSL_MASK_WSSE = RESOURCE_PATH + "wssemask.xsl";
    private static final String XSL_SSGCOMMENT = RESOURCE_PATH + "ssgwashere.xsl";
    private static final String XSL_BODYSUBST = RESOURCE_PATH + "bodySubstitution.xsl";
    private static final String SOAPMSG_WITH_WSSE = RESOURCE_PATH + "simpleRequestWithWsseHeader.xml";
}
