package com.l7tech.server.policy.assertion;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.ContentTypeAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Charsets;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Tests for ServerContentTypeAssertion.
 */
public class ServerContentTypeAssertionTest {
    ContentTypeAssertion ass = new ContentTypeAssertion();
    Message request;
    PolicyEnforcementContext context;

    @Test
    public void testValidateSuccess() throws Exception {
        context(req(ContentTypeHeader.XML_DEFAULT.getFullValue()));
        AssertionStatus result = sass().checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
    }

    @Test
    public void testValidateSuccessPartNum() throws Exception {
        context(req(ContentTypeHeader.XML_DEFAULT.getFullValue()));
        ass.setMessagePart(true);
        ass.setMessagePartNum("1");
        AssertionStatus result = sass().checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
    }

    @Test
    @BugNumber(10733)
    public void testValidateSuccessPartNumBackwardCompat() throws Exception {
        context(req(ContentTypeHeader.XML_DEFAULT.getFullValue()));
        ass.setMessagePart(true);
        ass.setMessagePartNum("0");
        AssertionStatus result = sass().checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
    }

    @Test
    public void testValidateFailure() throws Exception {
        context(req("foo /bar"));
        assertEquals(AssertionStatus.FAILED, sass().checkRequest(context));
    }

    @Test
    public void testChangeTypeFixed() throws Exception {
        ass.setChangeContentType(true);
        ass.setNewContentTypeValue("application/x-changed");
        context(req("old/crap"));
        assertEquals(AssertionStatus.NONE, sass().checkRequest(context));
        assertEquals("application/x-changed", request.getMimeKnob().getOuterContentType().getFullValue());
    }

    @Test
    public void testChangeTypeVariable() throws Exception {
        ass.setChangeContentType(true);
        ass.setNewContentTypeValue("${ctype}");
        context(req("old/crap"));
        context.setVariable("ctype", "application/x-changed");
        assertEquals(AssertionStatus.NONE, sass().checkRequest(context));
        assertEquals("application/x-changed", request.getMimeKnob().getOuterContentType().getFullValue());
    }

    @Test
    public void testMultipartValidateSuccess() throws Exception {
        context(multireq());
        ass.setMessagePart(true);
        ass.setMessagePartNum("2");
        AssertionStatus result = sass().checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
    }

    @Test
    public void testMultipartValidateFailure() throws Exception {
        context(multireq());
        ass.setMessagePart(true);
        ass.setMessagePartNum("2");
        request.getMimeKnob().getPart(1).setContentType(ContentTypeHeader.create("blah /wrong"));
        assertEquals(AssertionStatus.FAILED, sass().checkRequest(context));
    }

    @Test
    public void testMultipartValidateFailureVar() throws Exception {
        context(multireq());
        context.setVariable("partNum", "2");
        ass.setMessagePart(true);
        ass.setMessagePartNum("${partNum}");
        request.getMimeKnob().getPart(1).setContentType(ContentTypeHeader.create("blah /wrong"));
        assertEquals(AssertionStatus.FAILED, sass().checkRequest(context));
    }

    @Test(expected = PolicyAssertionException.class)
    public void testMultipartValidateBadPartNum() throws Exception {
        context(multireq());
        ass.setMessagePart(true);
        ass.setMessagePartNum("asdf");
        sass().checkRequest(context);
    }

    @Test
    public void testMultipartValidateBadPartNumVariable() throws Exception {
        context(multireq());
        context.setVariable("partNum", "wharrgarbl");
        ass.setMessagePart(true);
        ass.setMessagePartNum("${partNum}");
        request.getMimeKnob().getPart(1).setContentType(ContentTypeHeader.create("blah /wrong"));
        assertEquals(AssertionStatus.FAILED, sass().checkRequest(context));
    }

    @Test
    public void testMultipartChangeTypeFixed() throws Exception {
        context(multireq());
        ass.setMessagePart(true);
        ass.setMessagePartNum("2");
        ass.setChangeContentType(true);
        ass.setNewContentTypeValue("application/x-changed");
        assertEquals(AssertionStatus.NONE, sass().checkRequest(context));
        assertEquals("application/x-changed", request.getMimeKnob().getPart(1).getContentType().getFullValue());
    }

    @Test
    public void testMultipartChangeTypeVariable() throws Exception {
        context(multireq());
        ass.setMessagePart(true);
        ass.setMessagePartNum("2");
        ass.setChangeContentType(true);
        ass.setNewContentTypeValue("${ctype}");
        context.setVariable("ctype", "application/x-changed");
        assertEquals(AssertionStatus.NONE, sass().checkRequest(context));
        assertEquals("application/x-changed", request.getMimeKnob().getPart(1).getContentType().getFullValue());
    }

    private PolicyEnforcementContext context(Message request) throws IOException {
        return context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
    }

    private Message req(String requestCtype) throws IOException {
        return request = new Message(new ByteArrayStashManager(), ContentTypeHeader.create(requestCtype), new ByteArrayInputStream("<foobarblah/>".getBytes(Charsets.UTF8)));
    }

    private ServerContentTypeAssertion sass() throws PolicyAssertionException {
        return new ServerContentTypeAssertion(ass);
    }

    private Message multireq() throws IOException {
        return request = new Message(new ByteArrayStashManager(), ContentTypeHeader.create(MESS_CONTENT_TYPE), new ByteArrayInputStream(MESS.getBytes(Charsets.UTF8)));
    }

    public static final String MESS_BOUNDARY = "----=Part_-763936460.407197826076299";
    public static final String MESS_CONTENT_TYPE = "multipart/related; type=\"text/xml\"; boundary=\"" +
            MESS_BOUNDARY+ "\"; start=\"-76394136.15558\"";
    public static final String MESS_PAYLOAD_NS = "urn:EchoAttachmentsService";
    public static final String SOAP = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
            "<env:Envelope xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "    xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "  <env:Body>\n" +
            "    <n1:echoOne xmlns:n1=\"" + MESS_PAYLOAD_NS + "\"\n" +
            "        env:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
            "      <file href=\"cid:-76392836.15558\"></file>\n" +
            "    </n1:echoOne>\n" +
            "  </env:Body>\n" +
            "</env:Envelope>\n";

    public static final String RUBY = "require 'soap/rpc/driver'\n" +
            "require 'soap/attachment'\n" +
            "\n" +
            "attachment = ARGV.shift || __FILE__\n" +
            "\n" +
            "#server = 'http://localhost:7700/'\n" +
            "server = 'http://data.l7tech.com:80/ssg/soap'\n" +
            "\n" +
            "driver = SOAP::RPC::Driver.new(server, 'urn:EchoAttachmentsService')\n" +
            "driver.wiredump_dev = STDERR\n" +
            "driver.add_method('echoOne', 'file')\n" +
            "\n" +
            "File.open(attachment)  do |fin|\n" +
            "  File.open('attachment.out', 'w') do |fout|\n" +
            "\tfout << driver.echoOne(SOAP::Attachment.new(fin))\n" +
            "  end      \n" +
            "end\n" +
            "\n" +
            "\n";

    public static final String MESS = "PREAMBLE GARBAGE\r\nBLAH BLAH BLAH\r\n------=Part_-763936460.407197826076299\r\n" +
            "Content-Transfer-Encoding: 8bit\r\n" +
            "Content-Type: text/xml; charset=utf-8\r\n" +
            "Content-ID: -76394136.15558\r\n" +
            "\r\n" +
            SOAP +
            "\r\n" +
            "------=Part_-763936460.407197826076299\r\n" +
            "Content-Transfer-Encoding: 8bit\r\n" +
            "Content-Type: application/octet-stream\r\n" +
            "Content-ID: <-76392836.15558>\r\n" +
            "\r\n" +
             RUBY +
            "\r\n" +
            "------=Part_-763936460.407197826076299--\r\n";
}
