package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.xml.XslTransformation;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

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

    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
        System.out.println("Test complete: " + XslTransformationTest.class);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(XslTransformationTest.class);
        return suite;
    }

    public void testMaskWsse() throws Exception {
        String xslStr = getResAsString(XSL_MASK_WSSE);
        XslTransformation assertion = new XslTransformation();
        assertion.setXslSrc(xslStr);
        assertion.setDirection(XslTransformation.APPLY_TO_REQUEST);
        ServerXslTransformation transformer = new ServerXslTransformation(assertion);
        String res = XmlUtil.nodeToString(transformer.transform(getResAsDoc(SOAPMSG_WITH_WSSE)));
        // visual inspection - todo automate the verification of the transformation
        System.out.println(res);
    }

    public void testSsgComment() throws Exception {
        String xslStr = getResAsString(XSL_SSGCOMMENT);
        XslTransformation assertion = new XslTransformation();
        assertion.setXslSrc(xslStr);
        assertion.setDirection(XslTransformation.APPLY_TO_REQUEST);
        ServerXslTransformation transformer = new ServerXslTransformation(assertion);
        String res = XmlUtil.nodeToString(transformer.transform(getResAsDoc(SOAPMSG_WITH_WSSE)));
        // visual inspection - todo automate the verification of the transformation
        System.out.println(res);
    }

    private String getResAsString(String path) throws IOException {
        InputStream is = getClass().getResourceAsStream(path);
        byte[] resbytes = HexUtils.slurpStream(is, 20000);
        return new String(resbytes);
    }

    private InputSource getRes(String path) throws IOException {
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            throw new IOException("\ncannot load resource " + path + ".\ncheck your runtime properties.\n");
        }
        return new InputSource(is);
    }

    private Document getResAsDoc(String path) throws IOException, ParserConfigurationException,
            SAXException, IllegalArgumentException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        return dbf.newDocumentBuilder().parse(getRes(path));
    }

    private static final String RESOURCE_PATH = "/com/l7tech/server/policy/assertion/xml/";
	private static final String XSL_MASK_WSSE = RESOURCE_PATH + "wssemask.xsl";
    private static final String XSL_SSGCOMMENT = RESOURCE_PATH + "ssgwashere.xsl";
    private static final String SOAPMSG_WITH_WSSE = RESOURCE_PATH + "simpleRequestWithWsseHeader.xml";
}
