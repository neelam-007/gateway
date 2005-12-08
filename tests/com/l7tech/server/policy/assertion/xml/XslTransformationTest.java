package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.XmlUtil;
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
        return XmlUtil.softXSLTransform(srcdoc, transformer);
    }

    public void testMaskWsse() throws Exception {
        String xslStr = getResAsString(XSL_MASK_WSSE);
        String xmlStr = getResAsString(SOAPMSG_WITH_WSSE);
        String res = XmlUtil.nodeToString(transform(xslStr, xmlStr));
        String expected = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
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
        if (!expected.equals(res)) {
            logger.severe("result of transformation not as expected\nresult:\n" + res + "\nexpected:\n" + expected);
        } else {
            logger.fine("transformation ok");
        }
        assertTrue(expected.equals(res));
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

    private String getResAsString(String path) throws IOException {
        InputStream is = getClass().getResourceAsStream(path);
        byte[] resbytes = HexUtils.slurpStream(is, 20000);
        return new String(resbytes);
    }

    private static final String RESOURCE_PATH = "/com/l7tech/server/policy/assertion/xml/";
	private static final String XSL_MASK_WSSE = RESOURCE_PATH + "wssemask.xsl";
    private static final String XSL_SSGCOMMENT = RESOURCE_PATH + "ssgwashere.xsl";
    private static final String SOAPMSG_WITH_WSSE = RESOURCE_PATH + "simpleRequestWithWsseHeader.xml";
}
