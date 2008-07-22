package com.l7tech.external.assertions.multipartassembly.server;

import com.l7tech.message.Message;
import com.l7tech.common.mime.*;
import com.l7tech.util.HexUtils;
import com.l7tech.external.assertions.multipartassembly.MultipartAssemblyAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Test the MultipartAssemblyAssertion.
 */
public class ServerMultipartAssemblyAssertionTest extends TestCase {
    private ApplicationContext applicationContext;
    private ContentTypeHeader ctypeSoap;
    private ContentTypeHeader ctypeRuby;
    private byte[] bodySoap;
    private byte[] bodyRuby;
    private String partIdSoap;
    private String partIdRuby;

    public ServerMultipartAssemblyAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ServerMultipartAssemblyAssertionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void setUp() throws Exception {
        super.setUp();
        applicationContext = new ClassPathXmlApplicationContext(new String[]{
                "com/l7tech/external/assertions/multipartassembly/server/multipartAssemblyApplicationContext.xml"
        });
        MimeBody mimeBody = new MimeBody(new ByteArrayStashManager(),
                                         ContentTypeHeader.parseValue(MESS2_CONTENT_TYPE),
                                         new ByteArrayInputStream(MESS2.getBytes()));

        List<PartInfo> infos = collectParts(mimeBody.iterator());
        PartInfo soapPart = infos.get(0);
        PartInfo rubyPart = infos.get(1);
        ctypeSoap = soapPart.getContentType();
        bodySoap = soapPart.getBytesIfAlreadyAvailable();
        partIdSoap = soapPart.getContentId(true);
        ctypeRuby = rubyPart.getContentType();
        bodyRuby = rubyPart.getBytesIfAlreadyAvailable();
        partIdRuby = rubyPart.getContentId(true);
    }

    public void testMakeBodyInputStream() throws Exception {
        ContentTypeHeader multipartRelated = ServerMultipartAssemblyAssertion.makeMultipartRelated(partIdSoap);
        final ByteArrayStashManager sm = new ByteArrayStashManager();
        List<ContentTypeHeader> ctypes = Arrays.asList(ctypeSoap, ctypeRuby);
        List<String> partIds = Arrays.asList(partIdSoap, partIdRuby);

        sm.stash(0, bodySoap);
        sm.stash(1, bodyRuby);

        InputStream messStream = ServerMultipartAssemblyAssertion.makeBodyInputStream(
                multipartRelated,
                sm,
                ctypes,
                partIds);

        // Make sure the resulting stream matches its ingredients
        MimeBody mb = new MimeBody(new ByteArrayStashManager(), multipartRelated, messStream);
        List<PartInfo> parts = collectParts(mb.iterator());
        final PartInfo part0 = parts.get(0);
        final PartInfo part1 = parts.get(1);
        assertTrue(HexUtils.compareInputStreams(part0.getInputStream(false), true, new ByteArrayInputStream(bodySoap), true));
        assertTrue(HexUtils.compareInputStreams(part1.getInputStream(false), true, new ByteArrayInputStream(bodyRuby), true));
        assertEquals(part0.getContentType().getFullValue(), ctypeSoap.getFullValue());
        assertEquals(part1.getContentType().getFullValue(), ctypeRuby.getFullValue());
        assertEquals(part0.getContentId(true), partIdSoap);
        assertEquals(part1.getContentId(true), partIdRuby);
    }

    public void testMultipartAssembly() throws Exception {

        MultipartAssemblyAssertion ass = new MultipartAssemblyAssertion();
        ass.setActOnRequest(true);
        ass.setVariablePrefix("blprefix");
        ServerMultipartAssemblyAssertion sass = new ServerMultipartAssemblyAssertion(ass, applicationContext);

        Message request = new Message(new ByteArrayStashManager(),
                                      ContentTypeHeader.parseValue(MESS2_CONTENT_TYPE),
                                      new ByteArrayInputStream(MESS2.getBytes()));
        Message response = new Message();
        PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);

        context.setVariable("blprefix.payloads", new Object[] { bodySoap, bodyRuby, bodySoap } );
        context.setVariable("blprefix.contentTypes", new Object[] { ctypeSoap, ctypeRuby, ctypeSoap });
        context.setVariable("blprefix.partIds", new Object[] { "123", "234", "543" });

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(result, AssertionStatus.NONE);

        // Make sure the resulting stream matches its ingredients
        MimeBody mb = new MimeBody(new ByteArrayStashManager(), request.getMimeKnob().getOuterContentType(), request.getMimeKnob().getEntireMessageBodyAsInputStream());
        List<PartInfo> parts = collectParts(mb.iterator());
        final PartInfo multi = parts.get(0);
        final PartInfo soap1 = parts.get(1);
        final PartInfo ruby = parts.get(2);
        final PartInfo soap2 = parts.get(3);
        assertTrue(HexUtils.compareInputStreams(soap1.getInputStream(false), true, new ByteArrayInputStream(bodySoap), true));
        assertTrue(HexUtils.compareInputStreams(ruby.getInputStream(false), true, new ByteArrayInputStream(bodyRuby), true));
        assertTrue(HexUtils.compareInputStreams(soap2.getInputStream(false), true, new ByteArrayInputStream(bodySoap), true));
        assertEquals(soap1.getContentType().getFullValue(), ctypeSoap.getFullValue());
        assertEquals(ruby.getContentType().getFullValue(), ctypeRuby.getFullValue());
        assertEquals(soap2.getContentType().getFullValue(), ctypeSoap.getFullValue());
        assertEquals(soap1.getContentId(true), "123");
        assertEquals(ruby.getContentId(true), "234");
        assertEquals(soap2.getContentId(true), "543");
        assertSimilarToMess2(multi.getContentType(), multi.getInputStream(false));
    }

    static class CloseFlaggingStream extends ByteArrayInputStream {
        boolean closeCalled = false;

        public CloseFlaggingStream(byte[] buf) {
            super(buf);
        }

        public void close() throws IOException {
            closeCalled = true;
            super.close();
        }
    }
    
    public void testAssemblyOfInputStreams() throws Exception {

        MultipartAssemblyAssertion ass = new MultipartAssemblyAssertion();
        ass.setActOnRequest(true);
        ass.setVariablePrefix("blprefix");
        ServerMultipartAssemblyAssertion sass = new ServerMultipartAssemblyAssertion(ass, applicationContext);

        Message request = new Message(new ByteArrayStashManager(),
                                      ContentTypeHeader.parseValue(MESS2_CONTENT_TYPE),
                                      new ByteArrayInputStream(MESS2.getBytes()));
        Message response = new Message();
        PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);

        CloseFlaggingStream bodySoapStream = new CloseFlaggingStream(bodySoap);
        CloseFlaggingStream bodyRubyStream = new CloseFlaggingStream(bodyRuby);
        CloseFlaggingStream bodySoapStream2 = new CloseFlaggingStream(bodySoap);

        context.setVariable("blprefix.payloads", new Object[] { bodySoapStream, bodyRubyStream, bodySoapStream2 } );
        context.setVariable("blprefix.contentTypes", new Object[] { ctypeSoap, ctypeRuby, ctypeSoap });
        context.setVariable("blprefix.partIds", new Object[] { "123", "234", "543" });

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(result, AssertionStatus.NONE);

        // Make sure streams were closed
        assertTrue(bodySoapStream.closeCalled);
        assertTrue(bodyRubyStream.closeCalled);
        assertTrue(bodySoapStream2.closeCalled);

        // Make sure the resulting stream matches its ingredients
        MimeBody mb = new MimeBody(new ByteArrayStashManager(), request.getMimeKnob().getOuterContentType(), request.getMimeKnob().getEntireMessageBodyAsInputStream());
        List<PartInfo> parts = collectParts(mb.iterator());
        final PartInfo multi = parts.get(0);
        final PartInfo soap1 = parts.get(1);
        final PartInfo ruby = parts.get(2);
        final PartInfo soap2 = parts.get(3);
        assertTrue(HexUtils.compareInputStreams(soap1.getInputStream(false), true, new ByteArrayInputStream(bodySoap), true));
        assertTrue(HexUtils.compareInputStreams(ruby.getInputStream(false), true, new ByteArrayInputStream(bodyRuby), true));
        assertTrue(HexUtils.compareInputStreams(soap2.getInputStream(false), true, new ByteArrayInputStream(bodySoap), true));
        assertEquals(soap1.getContentType().getFullValue(), ctypeSoap.getFullValue());
        assertEquals(ruby.getContentType().getFullValue(), ctypeRuby.getFullValue());
        assertEquals(soap2.getContentType().getFullValue(), ctypeSoap.getFullValue());
        assertEquals(soap1.getContentId(true), "123");
        assertEquals(ruby.getContentId(true), "234");
        assertEquals(soap2.getContentId(true), "543");
        assertSimilarToMess2(multi.getContentType(), multi.getInputStream(false));
    }

    private void assertSimilarToMess2(ContentTypeHeader outerctype, InputStream stream) throws Exception {
        MimeBody mb = new MimeBody(new ByteArrayStashManager(), outerctype, stream);
        List<PartInfo> parts = collectParts(mb.iterator());
        final PartInfo part0 = parts.get(0);
        final PartInfo part1 = parts.get(1);
        assertTrue(HexUtils.compareInputStreams(part0.getInputStream(false), true, new ByteArrayInputStream(bodySoap), true));
        assertTrue(HexUtils.compareInputStreams(part1.getInputStream(false), true, new ByteArrayInputStream(bodyRuby), true));
        assertEquals(part0.getContentType().getFullValue(), ctypeSoap.getFullValue());
        assertEquals(part1.getContentType().getFullValue(), ctypeRuby.getFullValue());
        assertEquals(part0.getContentId(true), partIdSoap);
        assertEquals(part1.getContentId(true), partIdRuby);
    }
    
    private static List<PartInfo> collectParts(PartIterator pi) throws IOException, NoSuchPartException {
        List<PartInfo> ret = new ArrayList<PartInfo>();
        while (pi.hasNext()) {
            PartInfo partInfo = pi.next();
            partInfo.getInputStream(false).close(); // work around bug present in 4.2
            ret.add(partInfo);
        }
        return ret;
    }


    public static final String MESS2_BOUNDARY = "----=Part_-763936460.00306951464153826";
    public static final String MESS2_CONTENT_TYPE = "multipart/related; type=\"text/xml\"; boundary=\"" +
            MESS2_BOUNDARY + "\"; start=\"-76394136.13454\"";

    /** @noinspection SingleCharacterStringConcatenation*/
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
