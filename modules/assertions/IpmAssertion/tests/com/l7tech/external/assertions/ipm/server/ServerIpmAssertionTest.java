package com.l7tech.external.assertions.ipm.server;

import com.l7tech.common.message.Message;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.external.assertions.ipm.IpmAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Test the IpmAssertion.
 */
public class ServerIpmAssertionTest extends TestCase {
    private static final Logger log = Logger.getLogger(ServerIpmAssertionTest.class.getName());

    static final String RESOURCE_DIR = "com/l7tech/external/assertions/ipm/resources/";
    static final String TEMPLATE_PAC_REPLY = "template-pac-reply.xml";
    static final String SOAP_PAC_REPLY = "soap-pac-reply.xml";

    public ServerIpmAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ServerIpmAssertionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }


    static String loadFile(String filename) throws IOException {
        InputStream is = ServerIpmAssertionTest.class.getClassLoader().getResourceAsStream(RESOURCE_DIR + filename);
        byte[] got = HexUtils.slurpStream(is);
        return new String(got);
    }

    public void testUnpackIpm() throws Exception {
        String template = loadFile(TEMPLATE_PAC_REPLY);
        String requestStr = loadFile(SOAP_PAC_REPLY);

        IpmAssertion ass = new IpmAssertion();
        ass.template(template);
        ass.setSourceVariableName("databuff");
        ass.setTargetVariableName("ipmresult");

        ServerIpmAssertion sass = new ServerIpmAssertion(ass, null);

        Message request = new Message();
        request.initialize(ContentTypeHeader.XML_DEFAULT, requestStr.getBytes("UTF-8"));
        Message response = new Message();
        PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
        context.setVariable("databuff", extractDataBuff(requestStr));

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        String ipmresult = context.getVariable("ipmresult").toString();
        assertTrue(ipmresult.length() > 0);
        XmlUtil.stringToDocument(ipmresult);
    }

    static String extractDataBuff(String requestSoapStr) throws SAXException {
        Document doc = XmlUtil.stringToDocument(requestSoapStr);
        NodeList found = doc.getElementsByTagNameNS("http://soapam.com/service/Pacquery/", "DATA_BUFF");
        int numFound = found.getLength();
        if (numFound < 1) throw new SAXException("Did not find a DATA_BUFF element");
        if (numFound > 1) throw new SAXException("Found more than one DATA_BUFF element");
        return found.item(0).getTextContent();
    }
}
