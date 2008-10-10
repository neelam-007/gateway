/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.io.IOUtils;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Test for MimeBody class.
 */
public class MimeBodyTest extends TestCase {
    private static Logger log = Logger.getLogger(MimeBodyTest.class.getName());

    public MimeBodyTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(MimeBodyTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testEmptySinglePartMessage() throws Exception {
        MimeBody mm = new MimeBody(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new EmptyInputStream());
        assertEquals(-1, mm.getFirstPart().getContentLength()); // size of part not yet known
        long len = mm.getEntireMessageBodyLength(); // force entire body to be read
        assertEquals(0, len);
        assertEquals(0, mm.getFirstPart().getContentLength()); // size now known
    }

    public void testSimple() throws Exception {
        MimeBody mm = makeMessage(MESS, MESS_CONTENT_TYPE);

        PartInfo rubyPart = mm.getPart(1);
        InputStream rubyStream = rubyPart.getInputStream(true);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        IOUtils.copyStream(rubyStream, bo);
        assertTrue(Arrays.equals(RUBY.getBytes(), bo.toByteArray()));
        log.info("Ruby part retrieved " + bo.toByteArray().length + " bytes: \n" + new String(bo.toByteArray()));

        PartInfo soapPart = mm.getPart(0);
        InputStream soapStream = soapPart.getInputStream(true);
        bo = new ByteArrayOutputStream();
        IOUtils.copyStream(soapStream, bo);
        assertTrue(Arrays.equals(SOAP.getBytes(), bo.toByteArray()));
        log.info("Soap part retrieved " + bo.toByteArray().length + " bytes: \n" + new String(bo.toByteArray()));

        // We read them out of order, so this part will have been stashed
        soapStream = soapPart.getInputStream(false);

        try {
            rubyStream = rubyPart.getInputStream(false);
            fail("Ruby part was read destructively, and is last part, but was stashed anyway");
        } catch (NoSuchPartException e) {
            log.info("The Ruby part was consumed successfully: " + e.getMessage());
        }

        try {
            mm.getPart(2);
            fail("Failed to get expected exception");
        } catch (NoSuchPartException e) {
            log.info("Got proper exception on trying to get a nonexistent MIME part: " + e.getMessage());
        }
    }

    public void testSimpleWithNoPreamble() throws Exception {
        MimeBody mm = makeMessage(MESS2, MESS2_CONTENT_TYPE);

        PartInfo rubyPart = mm.getPart(1);
        InputStream rubyStream = rubyPart.getInputStream(true);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        IOUtils.copyStream(rubyStream, bo);
        log.info("Ruby part retrieved " + bo.toByteArray().length + " bytes: \n" + new String(bo.toByteArray()));

        PartInfo soapPart = mm.getPart(0);
        InputStream soapStream = soapPart.getInputStream(true);
        bo = new ByteArrayOutputStream();
        IOUtils.copyStream(soapStream, bo);
        log.info("Soap part retrieved " + bo.toByteArray().length + " bytes: \n" + new String(bo.toByteArray()));

        // We read them out of order, so this part will have been stashed
        soapStream = soapPart.getInputStream(false);

        try {
            rubyStream = rubyPart.getInputStream(false);
            fail("Ruby part was read destructively, and is last part, but was stashed anyway");
        } catch (NoSuchPartException e) {
            log.info("The Ruby part was consumed successfully: " + e.getMessage());
        }

        try {
            mm.getPart(2);
            fail("Failed to get expected exception");
        } catch (NoSuchPartException e) {
            log.info("Got proper exception on trying to get a nonexistent MIME part: " + e.getMessage());
        }
    }

    private MimeBody makeMessage(String message, String contentTypeValue) throws IOException, NoSuchPartException {
        InputStream mess = new ByteArrayInputStream(message.getBytes());
        ContentTypeHeader mr = ContentTypeHeader.parseValue(contentTypeValue);
        StashManager sm = new ByteArrayStashManager();
        MimeBody mm = new MimeBody(sm, mr, mess);
        return mm;
    }

    public void testSinglePart() throws Exception {
        final String body = "<foo/>";
        MimeBody mm = makeMessage(body, "text/xml");
        PartInfo p = mm.getPart(0);
        InputStream in = p.getInputStream(true);
        final byte[] bodyStream = IOUtils.slurpStream(in);
        assertTrue(Arrays.equals(bodyStream, body.getBytes()));

    }

    public void testStreamAllWithNoPreamble() throws Exception {
        MimeBody mm = makeMessage(MESS2, MESS2_CONTENT_TYPE);

        InputStream bodyStream = mm.getEntireMessageBodyAsInputStream(true);
        byte[] body = IOUtils.slurpStream(bodyStream);
        final String bodyStr = new String(body);
        int bodyStart = bodyStr.indexOf("<?xml ");

        assertEquals(MESS2.substring(MESS2.indexOf("<?xml ")), bodyStr.substring(bodyStart));
    }

    public void testStreamAllWithPreamble() throws Exception {
        MimeBody mm = makeMessage(MESS, MESS_CONTENT_TYPE);

        InputStream bodyStream = mm.getEntireMessageBodyAsInputStream(true);
        byte[] body = IOUtils.slurpStream(bodyStream);
        final String bodyStr = new String(body);
        int bodyStart = bodyStr.indexOf("<?xml ");

        assertEquals(MESS.substring(MESS.indexOf("<?xml ")), bodyStr.substring(bodyStart));

        try {
            long len = mm.getEntireMessageBodyLength();
            fail("Failed to get expected exception trying to compute body length with part bodies missing (got len=" + len + ")");
        } catch (NoSuchPartException e) {
            log.info("Got expected exception: " + e.getMessage());
        }
    }

    public void testStreamAllConsumedRubyPart() throws Exception {
        MimeBody mm = makeMessage(MESS2, MESS2_CONTENT_TYPE);

        // Destroy body of ruby part
        IOUtils.slurpStream(mm.getPart(1).getInputStream(true));

        try {
            mm.getEntireMessageBodyAsInputStream(true);
            fail("Failed to get expected exception when trying to stream all after destructively reading 1 part");
        } catch (NoSuchPartException e) {
            log.info("Got expected exception: " + e.getMessage());
        }
    }

    public void testStreamAllWithAllStashed() throws Exception {
        MimeBody mm = makeMessage(MESS, MESS_CONTENT_TYPE);

        mm.getPart(1).getInputStream(false);

        InputStream bodyStream = mm.getEntireMessageBodyAsInputStream(true);
        byte[] body = IOUtils.slurpStream(bodyStream);
        assertEquals(body.length, mm.getEntireMessageBodyLength());

        // strip added content-length for the comparison
        String bodyStr = new String(body).replaceAll("Content-Length:\\s*\\d+\r\n", "");

        // Skip over the HTTP headers for the comparison
        int bodyStart = bodyStr.indexOf("<?xml ");

        assertEquals(MESS.substring(MESS.indexOf("<?xml ")), bodyStr.substring(bodyStart));
    }

    public void testStreamAllWithFirstPartStashed() throws Exception {
        MimeBody mm = makeMessage(MESS, MESS_CONTENT_TYPE);

        mm.getPart(0).getInputStream(false);

        InputStream bodyStream = mm.getEntireMessageBodyAsInputStream(true);
        byte[] body = IOUtils.slurpStream(bodyStream);
        final String bodyStr = new String(body);
        int bodyStart = bodyStr.indexOf("<?xml ");

        // TODO less sensitive comparision that will not give false negative here (due to reordered headers)
        assertEquals(MESS.substring(MESS.indexOf("<?xml ")), bodyStr.substring(bodyStart));
    }

    public void testLookupsByCid() throws Exception {
        MimeBody mm = makeMessage(MESS, MESS_CONTENT_TYPE);

        PartInfo rubyPart = mm.getPartByContentId(MESS_RUBYCID);
        InputStream rubyStream = rubyPart.getInputStream(true);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        IOUtils.copyStream(rubyStream, bo);
        assertTrue(Arrays.equals(RUBY.getBytes(), bo.toByteArray()));
        log.info("Ruby part retrieved " + bo.toByteArray().length + " bytes: \n" + new String(bo.toByteArray()));

        PartInfo soapPart = mm.getPartByContentId(MESS_SOAPCID);
        InputStream soapStream = soapPart.getInputStream(true);
        bo = new ByteArrayOutputStream();
        IOUtils.copyStream(soapStream, bo);
        assertTrue(Arrays.equals(SOAP.getBytes(), bo.toByteArray()));
        log.info("Soap part retrieved " + bo.toByteArray().length + " bytes: \n" + new String(bo.toByteArray()));

        // We read them out of order, so this part will have been stashed
        soapStream = soapPart.getInputStream(false);

        try {
            rubyStream = rubyPart.getInputStream(false);
            fail("Ruby part was read destructively, and is last part, but was stashed anyway");
        } catch (NoSuchPartException e) {
            log.info("The Ruby part was consumed successfully: " + e.getMessage());
        }

        try {
            mm.getPart(2);
            fail("Failed to get expected exception");
        } catch (NoSuchPartException e) {
            log.info("Got proper exception on trying to get a nonexistent MIME part: " + e.getMessage());
        }
    }

    public void testByteArrayCtorNullByteArray() {
        try {
            new MimeBody(null, ContentTypeHeader.XML_DEFAULT);
            fail("Did not get a fast-failure exception passing null byte array to MimeBody(byte[], ctype)");
        } catch (NullPointerException e) {
            // This is acceptable
        } catch (IllegalArgumentException e) {
            // So is this
        } catch (NoSuchPartException e) {
            throw new RuntimeException("Wrong exception", e);
        } catch (IOException e) {
            throw new RuntimeException("Wrong exception", e);
        }
    }

    public void testIterator() throws Exception {
        MimeBody mm = makeMessage(MESS, MESS_CONTENT_TYPE);

        List parts = new ArrayList();
        for (PartIterator i = mm.iterator(); i.hasNext(); ) {
            PartInfo partInfo = i.next();
            parts.add(partInfo);
            log.info("Saw part: " + partInfo.getContentId(true));
            // Force body to be stashed
            partInfo.getInputStream(false).close();
        }

        assertEquals(2, parts.size());
        assertEquals(MESS_SOAPCID, ((PartInfo)parts.get(0)).getContentId(true));
        assertEquals(MESS_RUBYCID, ((PartInfo)parts.get(1)).getContentId(true));
    }

    public void testIterator2() throws Exception {
        SwaTestcaseFactory stfu = new SwaTestcaseFactory(23, 888, 29);
        byte[] testMsg = stfu.makeTestMessage();
        InputStream mess = new ByteArrayInputStream(testMsg);
        MimeBody mm = new MimeBody(new ByteArrayStashManager(),
                                                   ContentTypeHeader.parseValue("multipart/mixed; boundary=\"" +
                                                                                new String(stfu.getBoundary()) + "\""),
                                                   mess);

        List parts = new ArrayList();
        for (PartIterator i = mm.iterator(); i.hasNext(); ) {
            PartInfo partInfo = i.next();
            if (partInfo.getPosition() == 0)
                continue;
            parts.add(partInfo);
            log.info("Saw part: " + partInfo.getContentId(true));
            partInfo.getPosition();
            // Force body to be stashed
            //partInfo.getInputStream(false).close();
        }

        assertEquals(22, parts.size());
    }

    /**
     * Test case for bug 3470.
     *
     * There was an off by one error in the part iteration logic that could
     * cause the last part to be missed when iterating.
     */
    public void testIterationWithoutConsumption() throws Exception {
        SwaTestcaseFactory stfu = new SwaTestcaseFactory(4, 1024*50, 33);
        byte[] testMsg = stfu.makeTestMessage();
        InputStream mess = new ByteArrayInputStream(testMsg);

        MimeBody mm = new MimeBody(new ByteArrayStashManager(),
                                                   ContentTypeHeader.parseValue("multipart/mixed; boundary=\"" +
                                                                                new String(stfu.getBoundary()) + "\""),
                                                   mess);

        List parts = new ArrayList();
        for (PartIterator i = mm.iterator(); i.hasNext(); ) {
            PartInfo partInfo = i.next();
            parts.add(partInfo);
        }

        assertEquals(4, parts.size());
    }

    public void testContentLengthThatLies() throws Exception {
        final ContentTypeHeader ct = ContentTypeHeader.parseValue("multipart/related; boundary=blah");
        final String mess = "--blah\r\nContent-Length: 10\r\n\r\n\r\n--blah\r\n\r\n--blah--";
        try {
            // Test fail on getActualContentLength
            MimeBody mm = new MimeBody(mess.getBytes(), ct);
            long len = mm.getPart(0).getActualContentLength();
            fail("Failed to throw expected exception on Content-Length: header that lies (got len=" + len + ")");
        } catch (IOException e) {
            // ok
            log.info("Correct exception thrown on Content-Length: header discovered to be lying: " + e.getMessage());
        }

        try {
            // Test fail during iteration
            int num = 0;
            MimeBody mm = new MimeBody(mess.getBytes(), ct);
            for (PartIterator i = mm.iterator(); i.hasNext(); ) {
                PartInfo partInfo = i.next();
                partInfo.getInputStream(false).close();
                num++;
            }
            fail("Failed to throw expected exception on Content-Length: header that lies (saw " + num + " parts)");
        } catch (IOException e) {
            // ok
            log.info("Correct exception thrown on Content-Length: header discovered to be lying: " + e.getMessage());
        }
    }

    public void testFailureToStartAtPartZero() throws Exception {
        try {
            makeMessage(MESS3, MESS3_CONTENT_TYPE);
            fail("Failed to detect multipart content type start parameter that points somewhere other than the first part");
        } catch (IOException e) {
            // Ok
        }
    }

    public void testBug2180() throws Exception {
        makeMessage(BUG_2180, MESS_BUG_2180_CTYPE);
    }

    public void testStreamValidatedParts() throws Exception {
        MimeBody mm = makeMessage(MESS2, MESS2_CONTENT_TYPE);
        mm.setEntireMessageBodyAsInputStreamIsValidatedOnly();
        mm.readAndStashEntireMessage();
        assertTrue(mm.getNumPartsKnown()!=1); // Not a valid test if the message only has one part to start with

        // Serialize
        byte[] output = IOUtils.slurpStream(mm.getEntireMessageBodyAsInputStream(false));

        // Check only one part
        MimeBody rebuilt = new MimeBody(output, ContentTypeHeader.parseValue(MESS2_CONTENT_TYPE));
        rebuilt.readAndStashEntireMessage();

        assertEquals(1,rebuilt.getNumPartsKnown());
    }
    
    public void testGetBytesIfAlreadyAvailable() throws Exception {
        SwaTestcaseFactory stfu = new SwaTestcaseFactory(1, 50000, 29);
        InputStream in = new ByteArrayInputStream(stfu.makeTestMessage());

        final HybridStashManager stashManager = new HybridStashManager(2038, new File("."), "testGBIAA_1");
        try {
            MimeBody mm = new MimeBody(
                    stashManager,
                    ContentTypeHeader.parseValue("multipart/mixed; boundary=\"" +
                            new String(stfu.getBoundary()) + "\""),
                    in);

            byte[] firstPart = IOUtils.slurpStream(mm.getPart(0).getInputStream(false));
            assertEquals(firstPart.length, 50000);

            // Fetch with high limit should succeed
            byte[] got = mm.getFirstPart().getBytesIfAvailableOrSmallerThan(99999);
            assertTrue(Arrays.equals(got, firstPart));

            // Fetch with low limit should return null
            got = mm.getFirstPart().getBytesIfAvailableOrSmallerThan(1024);
            assertEquals(got, null);
        } finally {
            stashManager.close();
        }
    }

    public final String MESS_SOAPCID = "-76394136.15558";
    public final String MESS_RUBYCID = "-76392836.15558";
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

    // A test message with a start parameter pointing at the second attachment
    public static final String MESS3_BOUNDARY = "----=Part_-763936460.00306951464153826";
    public static final String MESS3_CONTENT_TYPE = "multipart/related; type=\"text/xml\"; boundary=\"" +
            MESS2_BOUNDARY + "\"; start=\"-76392836.13454\"";
    public static final String MESS3 = "------=Part_-763936460.00306951464153826\r\n" +
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


    private static final String MESS_BUG_2180_CTYPE = "multipart/related; type=\"text/xml\"; start=\"<40080056B6C225289B8E845639E05547>\"; \tboundary=\"----=_Part_4_20457766.1136482180671\"";
    public static final String BUG_2180 =
            "------=_Part_4_20457766.1136482180671\r\n" +
            "Content-Type: text/xml; charset=UTF-8\r\n" +
            "Content-Transfer-Encoding: binary\r\n" +
            "Content-Id: <40080056B6C225289B8E845639E05547>\r\n" +
            "\r\n" +
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            " <soapenv:Body>\n" +
            "  <ns1:echoDir soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:ns1=\"urn:EchoAttachmentsService\">\n" +
            "   <source xsi:type=\"soapenc:Array\" soapenc:arrayType=\"ns1:DataHandler[1]\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
            "    <item href=\"cid:5DE0F2F64B5AFDB73D4C8FF242FABEE6\"/>\n" +
            "   </source>\n" +
            "  </ns1:echoDir>\n" +
            " </soapenv:Body>\n" +
            "</soapenv:Envelope>\n" +
            "------=_Part_4_20457766.1136482180671\r\n" +
            "Content-Type: text/plain\r\n" +
            "Content-Transfer-Encoding: binary\r\n" +
            "Content-Id: <5DE0F2F64B5AFDB73D4C8FF242FABEE6>\r\n" +
            "\r\n" +
            "abc123\n" +
            "------=_Part_4_20457766.1136482180671--";

}
