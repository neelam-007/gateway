package com.l7tech.external.assertions.icapantivirusscanner.server;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.icapantivirusscanner.IcapAntivirusScannerAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Collections;

/**
 * <p>The test coverage for the {@link ServerIcapAntivirusScannerAssertion} class.</p>
 *
 * @author Ken Diep
 */
public class ServerIcapAntivirusScannerAssertionTest {

    private static final byte[] EICAR_PAYLOAD = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*".getBytes();

    private static final byte[] CLEAN_PAYLOAD = "ServerIcapAntivirusScannerAssertionTest".getBytes();

    private static final String ICAP_URI = "icap://nowhere:1344/avscan";

    private static final String FAKE_HOST = "nowhere";

    private static final String MESS2_BOUNDARY = "----=Part_-763936460.00306951464153826";

    private static final String MESS2_CONTENT_TYPE = "multipart/related; type=\"text/xml\"; boundary=\"" +
            MESS2_BOUNDARY + "\"; start=\"-76394136.13454\"";

    private static final String MESS2 = "------=Part_-763936460.00306951464153826\r\n" +
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
            "\n" + "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*" +
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

    private IcapAntivirusScannerAssertion assertion;

    private ServerIcapAntivirusScannerAssertion serverAssertion;

    private AbstractIcapResponseHandler handler = new MockIcapResponseHandler();

    @Before
    public void setUp() {
        assertion = new IcapAntivirusScannerAssertion();
        assertion.setIcapServers(Collections.singletonList(ICAP_URI));
        assertion.setFailoverStrategy("ordered");

        try {
            serverAssertion = new ServerIcapAntivirusScannerAssertion(assertion);
        } catch (PolicyAssertionException e) {
            Assert.fail("Error creating server assertion: " + e.getMessage());
        }
    }

    @Test
    public void testScanMessageSingleCleanPayload() {
        assertion.setContinueOnVirusFound(false);
        try {
            ByteArrayStashManager basm = new ByteArrayStashManager();
            basm.stash(0, CLEAN_PAYLOAD);
            Message message = new Message(basm, ContentTypeHeader.TEXT_DEFAULT, new ByteArrayInputStream(CLEAN_PAYLOAD));
            AssertionStatus status = serverAssertion.scanMessage(handler, ICAP_URI, FAKE_HOST, message);
            Assert.assertEquals("testScanMessageSingleCleanPayload()", AssertionStatus.NONE, status);

        } catch (Exception e) {
            Assert.fail("testScanMessageSingleCleanPayload failed: " + e.getMessage());
        }
    }



    @Test
    public void testScanMessageSingleInfectedPayloadWithContinue() {
        assertion.setContinueOnVirusFound(false);
        try {
            ByteArrayStashManager basm = new ByteArrayStashManager();
            basm.stash(0, EICAR_PAYLOAD);
            Message message = new Message(basm, ContentTypeHeader.TEXT_DEFAULT, new ByteArrayInputStream(EICAR_PAYLOAD));
            AssertionStatus status = serverAssertion.scanMessage(handler, ICAP_URI, FAKE_HOST, message);
            Assert.assertEquals("testInfectedMessageWithoutContinue()", AssertionStatus.FAILED, status);
        } catch (Exception e) {
            Assert.fail("testInfectedMessageWithoutContinue failed: " + e.getMessage());
        }
    }

    @Test
    public void testMimePartsInfected() {
        try {
            assertion.setContinueOnVirusFound(false);
            Message request = new Message(new ByteArrayStashManager(),
                    ContentTypeHeader.parseValue(MESS2_CONTENT_TYPE),
                    new ByteArrayInputStream(MESS2.getBytes()));
            AssertionStatus status = serverAssertion.scanMessage(handler, ICAP_URI, FAKE_HOST, request);
            Assert.assertEquals("testMimePartsInfected()", AssertionStatus.FAILED, status);
        } catch (Exception e) {
            Assert.fail("testMimePartsInfected failed: " + e.getMessage());
        }
    }

    @Test
    public void testMimePartsInfectedWithContinue() {
        try {
            assertion.setContinueOnVirusFound(true);
            Message request = new Message(new ByteArrayStashManager(),
                    ContentTypeHeader.parseValue(MESS2_CONTENT_TYPE),
                    new ByteArrayInputStream(MESS2.getBytes()));
            AssertionStatus status = serverAssertion.scanMessage(handler, ICAP_URI, FAKE_HOST, request);
            Assert.assertEquals("testMimePartsInfected()", AssertionStatus.NONE, status);
        } catch (Exception e) {
            Assert.fail("testMimePartsInfected failed: " + e.getMessage());
        }
    }
}
