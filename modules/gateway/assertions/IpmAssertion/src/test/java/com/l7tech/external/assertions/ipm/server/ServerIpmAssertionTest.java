package com.l7tech.external.assertions.ipm.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.ipm.IpmAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.IOUtils;
import org.junit.*;
import static org.junit.Assert.*;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Test the IpmAssertion.
 */
public class ServerIpmAssertionTest {
    static final String RESOURCE_DIR = "com/l7tech/external/assertions/ipm/resources/";
    static final String TEMPLATE_PAC_REPLY = "template-pac-reply.xml";
    static final String SOAP_PAC_REPLY = "soap-pac-reply.xml";

    static String loadFile(String filename) throws IOException {
        InputStream is = ServerIpmAssertionTest.class.getClassLoader().getResourceAsStream(RESOURCE_DIR + filename);
        byte[] got = IOUtils.slurpStream(is);
        return new String(got);
    }

    @Test
    public void testUnpackToVariable() throws Exception {
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
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        context.setVariable("databuff", extractDataBuff(requestStr));

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        String ipmresult = context.getVariable("ipmresult").toString();
        assertTrue(ipmresult.length() > 0);
        XmlUtil.stringToDocument(ipmresult);
    }
    
    @Test
    public void testUnpackToMessage() throws Exception {
        doUnpackToMessage();
    }

    private static void doUnpackToMessage() throws Exception {
        String template = loadFile(TEMPLATE_PAC_REPLY);
        String requestStr = loadFile(SOAP_PAC_REPLY);

        IpmAssertion ass = new IpmAssertion();
        ass.template(template);
        ass.setSourceVariableName("databuff");
        ass.setTargetVariableName(null);
        ass.setUseResponse(true);

        ServerIpmAssertion sass = new ServerIpmAssertion(ass, null);

        Message request = new Message();
        request.initialize(ContentTypeHeader.XML_DEFAULT, requestStr.getBytes("UTF-8"));
        Message response = new Message();
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        context.setVariable("databuff", extractDataBuff(requestStr));

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        String ipmresult = new String(IOUtils.slurpStream(response.getMimeKnob().getFirstPart().getInputStream(false)));
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
