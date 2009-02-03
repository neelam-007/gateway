package com.l7tech.external.assertions.stripparts.server;

import com.l7tech.message.Message;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.util.IOUtils;
import com.l7tech.external.assertions.stripparts.StripPartsAssertion;
import com.l7tech.external.assertions.stripparts.console.StripPartsPropertiesDialog;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Test the StripPartsAssertion.
 * @noinspection SingleCharacterStringConcatenation
 */
public class ServerStripPartsAssertionTest extends TestCase {

    private static final Logger log = Logger.getLogger(ServerStripPartsAssertionTest.class.getName());
    private ApplicationContext applicationContext;

    public ServerStripPartsAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ServerStripPartsAssertionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    protected void setUp() throws Exception {
        super.setUp();
        applicationContext = new ClassPathXmlApplicationContext(new String[]{
                "com/l7tech/external/assertions/stripparts/server/stripPartsApplicationContext.xml"
        });
    }

    public void testMetadata() throws Exception {
        assertEquals(new StripPartsAssertion().meta().get(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME),
                     StripPartsPropertiesDialog.class.getName());
    }

    public void testMultipart() throws Exception {
        Message request = new Message(new ByteArrayStashManager(),
                                      ContentTypeHeader.parseValue(MESS2_CONTENT_TYPE),
                                      new ByteArrayInputStream(MESS2.getBytes()));
        Message response = new Message();
        PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);

        assertIsMultipart(context.getRequest());
    }

    public void testStripRequest() throws Exception {
        StripPartsAssertion ass = new StripPartsAssertion();
        ass.setActOnRequest(true);
        ServerStripPartsAssertion sass = new ServerStripPartsAssertion(ass, applicationContext);

        Message request = new Message(new ByteArrayStashManager(),
                                      ContentTypeHeader.parseValue(MESS2_CONTENT_TYPE),
                                      new ByteArrayInputStream(MESS2.getBytes()));
        Message response = new Message();
        PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(result, AssertionStatus.NONE);

        // Make sure there's only one part
        Message mess = context.getRequest();
        assertNotMultipart(mess);

        log.info("Stripped message: " + new String( IOUtils.slurpStream(mess.getMimeKnob().getEntireMessageBodyAsInputStream())));
    }

    public void testStripResponse() throws Exception {
        StripPartsAssertion ass = new StripPartsAssertion();
        ass.setActOnRequest(false);
        ServerStripPartsAssertion sass = new ServerStripPartsAssertion(ass, applicationContext);

        Message request = new Message(new ByteArrayStashManager(),
                                      ContentTypeHeader.parseValue(MESS2_CONTENT_TYPE),
                                      new ByteArrayInputStream(MESS2.getBytes()));
        Message response = new Message(new ByteArrayStashManager(),
                                      ContentTypeHeader.parseValue(MESS2_CONTENT_TYPE),
                                      new ByteArrayInputStream(MESS2.getBytes()));
        PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(result, AssertionStatus.NONE);

        // Make sure request got left alone
        assertIsMultipart(request);

        // Make sure there's only one part
        assertNotMultipart(context.getResponse());

        log.info("Stripped message: " + new String(IOUtils.slurpStream(context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream())));
    }

    private static void assertIsMultipart(Message mess) throws Exception {
        List<PartInfo> parts = collectParts(mess);
        assertTrue(parts.size() > 1);
        assertTrue(mess.getMimeKnob().getOuterContentType().isMultipart());
        assertFalse(parts.get(0).getContentType().isMultipart());
    }

    private static void assertNotMultipart(Message mess) throws Exception {
        List<PartInfo> parts = collectParts(mess);
        assertEquals(parts.size(), 1);
        assertFalse(parts.get(0).getContentType().isMultipart());
        assertFalse(mess.getMimeKnob().getOuterContentType().isMultipart());
    }

    private static List<PartInfo> collectParts(Message mess) throws Exception {
        List<PartInfo> ret = new ArrayList<PartInfo>();
        PartIterator pi = mess.getMimeKnob().getParts();
        while (pi.hasNext()) {
            PartInfo partInfo = pi.next();
            partInfo.getInputStream(false).close();
            ret.add(partInfo);
        }
        return ret;
    }

    public static final String MESS2_BOUNDARY = "----=Part_-763936460.00306951464153826";
    public static final String MESS2_CONTENT_TYPE = "multipart/related; type=\"text/xml\"; boundary=\"" +
            MESS2_BOUNDARY + "\"; start=\"-76394136.13454\"";
    public static final String MESS2 = "------=Part_-763936460.00306951464153826\r\n" +
            "Content-Transfer-Encoding: 8bit\r\n" +
            "Content-Type: text/xml; charset=utf-8\r\n" +
            "Content-ID: -76394136.13454\r\n" +
            "\r\n" +
            "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
            "<env:Envelope xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "    xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "  <env:Body>\n" +
            "    <n1:echoOne xmlns:n1=\"urn:EchoAttachmentsService\"\n" +
            "        env:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
            "      <file href=\"cid:-76392836.13454\"></file>\n" +
            "    </n1:echoOne>\n" +
            "  </env:Body>\n" +
            "</env:Envelope>\n" +
            "\r\n" +
            "------=Part_-763936460.00306951464153826\r\n" +
            "Content-Transfer-Encoding: 8bit\r\n" +
            "Content-Type: application/octet-stream\r\n" +
            "Content-ID: <-76392836.13454>\r\n" +
            "\r\n" +
            "require 'soap/rpc/driver'\n" +
            "require 'soap/attachment'\n" +
            "\n" +
            "attachment = ARGV.shift || __FILE__\n" +
            "\n" +
            "#server = 'http://localhost:7700/'\n" +
            "server = 'http://data.l7tech.com:80/'\n" +
            "\n" +
            "driver = SOAP::RPC::Driver.new(server, 'urn:EchoAttachmentsService')\n" +
            "driver.wiredump_dev = STDERR\n" +
            "driver.add_method('echoOne', 'file')\n" +
            "\n" +
            "File.open(attachment)  do |fin|\n" +
            "  File.open('attachment.out', 'w') do |fout|\n" +
            ".fout << driver.echoOne(SOAP::Attachment.new(fin))\n" +
            "  end      \n" +
            "end\n" +
            "\n" +
            "\n" +
            "\r\n" +
            "------=Part_-763936460.00306951464153826--\r\n";
}
