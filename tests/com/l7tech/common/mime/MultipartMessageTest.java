/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.util.HexUtils;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class MultipartMessageTest extends TestCase {
    private static Logger log = Logger.getLogger(MultipartMessageTest.class.getName());

    public MultipartMessageTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(MultipartMessageTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testEmptySinglePartMessage() throws Exception {
        MultipartMessage mm = new MultipartMessage(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new EmptyInputStream());
        assertEquals(-1, mm.getFirstPart().getContentLength()); // size of part not yet known
        long len = mm.getEntireMessageBodyLength(); // force entire body to be read
        assertEquals(0, len);
        assertEquals(0, mm.getFirstPart().getContentLength()); // size now known
    }

    public void testSimple() throws Exception {
        MultipartMessage mm = makeMessage(MESS, CT);

        PartInfo rubyPart = mm.getPart(1);
        InputStream rubyStream = rubyPart.getInputStream(true);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        HexUtils.copyStream(rubyStream, bo);
        assertTrue(Arrays.equals(RUBY.getBytes(), bo.toByteArray()));
        log.info("Ruby part retrieved " + bo.toByteArray().length + " bytes: \n" + new String(bo.toByteArray()));

        PartInfo soapPart = mm.getPart(0);
        InputStream soapStream = soapPart.getInputStream(true);
        bo = new ByteArrayOutputStream();
        HexUtils.copyStream(soapStream, bo);
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
        MultipartMessage mm = makeMessage(MESS2, CT2);

        PartInfo rubyPart = mm.getPart(1);
        InputStream rubyStream = rubyPart.getInputStream(true);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        HexUtils.copyStream(rubyStream, bo);
        log.info("Ruby part retrieved " + bo.toByteArray().length + " bytes: \n" + new String(bo.toByteArray()));

        PartInfo soapPart = mm.getPart(0);
        InputStream soapStream = soapPart.getInputStream(true);
        bo = new ByteArrayOutputStream();
        HexUtils.copyStream(soapStream, bo);
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

    private MultipartMessage makeMessage(String message, String contentTypeValue) throws IOException, NoSuchPartException {
        InputStream mess = new ByteArrayInputStream(message.getBytes());
        ContentTypeHeader mr = ContentTypeHeader.parseValue(contentTypeValue);
        StashManager sm = new ByteArrayStashManager();
        MultipartMessage mm = new MultipartMessage(sm, mr, mess);
        return mm;
    }

    public void testSinglePart() throws Exception {
        final String body = "<foo/>";
        MultipartMessage mm = makeMessage(body, "text/xml");
        PartInfo p = mm.getPart(0);
        InputStream in = p.getInputStream(true);
        final byte[] bodyStream = HexUtils.slurpStream(in);
        assertTrue(Arrays.equals(bodyStream, body.getBytes()));

    }

    public void testStreamAllWith() throws Exception {
        MultipartMessage mm = makeMessage(MESS2, CT2);

        InputStream bodyStream = mm.getEntireMessageBodyAsInputStream(true);
        byte[] body = HexUtils.slurpStream(bodyStream);
        final String bodyStr = new String(body);
        int bodyStart = bodyStr.indexOf("<?xml ");

        assertEquals(MESS2.substring(MESS2.indexOf("<?xml ")), bodyStr.substring(bodyStart));
    }

    public void testStreamAllNoPreamble() throws Exception {
        MultipartMessage mm = makeMessage(MESS, CT);

        InputStream bodyStream = mm.getEntireMessageBodyAsInputStream(true);
        byte[] body = HexUtils.slurpStream(bodyStream);
        final String bodyStr = new String(body);
        int bodyStart = bodyStr.indexOf("<?xml ");

        assertEquals(MESS.substring(MESS.indexOf("<?xml ")), bodyStr.substring(bodyStart));

        try {
            mm.getEntireMessageBodyLength();
            fail("Failed to get expected exception trying to compute body length with part bodies missing");
        } catch (NoSuchPartException e) {
            log.info("Got expected exception: " + e.getMessage());
        }
    }

    public void testStreamAllConsumedRubyPart() throws Exception {
        MultipartMessage mm = makeMessage(MESS2, CT2);

        // Destroy body of ruby part
        HexUtils.slurpStream(mm.getPart(1).getInputStream(true));

        try {
            mm.getEntireMessageBodyAsInputStream(true);
            fail("Failed to get expected exception when trying to stream all after destructively reading 1 part");
        } catch (NoSuchPartException e) {
            log.info("Got expected exception: " + e.getMessage());
        }
    }

    public void testStreamAllWithAllStashed() throws Exception {
        MultipartMessage mm = makeMessage(MESS, CT);

        mm.getPart(1).getInputStream(false);

        InputStream bodyStream = mm.getEntireMessageBodyAsInputStream(true);
        byte[] body = HexUtils.slurpStream(bodyStream);
        final String bodyStr = new String(body);
        assertEquals(body.length, mm.getEntireMessageBodyLength());
        int bodyStart = bodyStr.indexOf("<?xml ");

        // TODO less sensitive comparision that will not give false negative here (due to reordered headers)
        assertEquals(MESS.substring(MESS.indexOf("<?xml ")), bodyStr.substring(bodyStart));
    }

    public void testStreamAllWithFirstPartStashed() throws Exception {
        MultipartMessage mm = makeMessage(MESS, CT);

        mm.getPart(0).getInputStream(false);

        InputStream bodyStream = mm.getEntireMessageBodyAsInputStream(true);
        byte[] body = HexUtils.slurpStream(bodyStream);
        final String bodyStr = new String(body);
        int bodyStart = bodyStr.indexOf("<?xml ");

        // TODO less sensitive comparision that will not give false negative here (due to reordered headers)
        assertEquals(MESS.substring(MESS.indexOf("<?xml ")), bodyStr.substring(bodyStart));
    }

    public void testLookupsByCid() throws Exception {
        MultipartMessage mm = makeMessage(MESS, CT);

        PartInfo rubyPart = mm.getPartByContentId(MESS_RUBYCID);
        InputStream rubyStream = rubyPart.getInputStream(true);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        HexUtils.copyStream(rubyStream, bo);
        assertTrue(Arrays.equals(RUBY.getBytes(), bo.toByteArray()));
        log.info("Ruby part retrieved " + bo.toByteArray().length + " bytes: \n" + new String(bo.toByteArray()));

        PartInfo soapPart = mm.getPartByContentId(MESS_SOAPCID);
        InputStream soapStream = soapPart.getInputStream(true);
        bo = new ByteArrayOutputStream();
        HexUtils.copyStream(soapStream, bo);
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
            new MultipartMessage(null, ContentTypeHeader.XML_DEFAULT);
            fail("Did not get a fast-failure exception passing null byte array to MultipartMessage(byte[], ctype)");
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
        MultipartMessage mm = makeMessage(MESS, CT);

        List parts = new ArrayList();
        for (PartIterator i = mm.iterator(); i.hasNext(); ) {
            PartInfo partInfo = i.next();
            parts.add(partInfo);
            log.info("Saw part: " + partInfo.getContentId());
            // Force body to be stashed
            partInfo.getInputStream(false).close();
        }

        assertEquals(2, parts.size());
        assertEquals(MESS_SOAPCID, ((PartInfo)parts.get(0)).getContentId());
        assertEquals(MESS_RUBYCID, ((PartInfo)parts.get(1)).getContentId());
    }

    public void testIterator2() throws Exception {
        SwaTestcaseFactory stfu = new SwaTestcaseFactory(23, 888, 29);
        byte[] testMsg = stfu.makeTestMessage();
        InputStream mess = new ByteArrayInputStream(testMsg);
        MultipartMessage mm = new MultipartMessage(new ByteArrayStashManager(),
                                                   ContentTypeHeader.parseValue("multipart/mixed; boundary=\"" +
                                                                                new String(stfu.getBoundary()) + "\""),
                                                   mess);

        List parts = new ArrayList();
        for (PartIterator i = mm.iterator(); i.hasNext(); ) {
            PartInfo partInfo = i.next();
            if (partInfo.getPosition() == 0)
                continue;
            parts.add(partInfo);
            log.info("Saw part: " + partInfo.getContentId());
            partInfo.getPosition();
            // Force body to be stashed
            //partInfo.getInputStream(false).close();
        }

        assertEquals(22, parts.size());
    }

    public final String MESS_SOAPCID = "-76394136.15558";
    public final String MESS_RUBYCID = "-76392836.15558";
    public static final String CT = "multipart/related; type=\"text/xml\"; boundary=\"----=Part_-763936460.407197826076299\"; start=\"-76394136.15558\"";
    public static final String SOAP = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
            "<env:Envelope xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "    xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "  <env:Body>\n" +
            "    <n1:echoOne xmlns:n1=\"urn:EchoAttachmentsService\"\n" +
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

    public static final String CT2 = "multipart/related; type=\"text/xml\"; boundary=\"----=Part_-763936460.00306951464153826\"; start=\"-76394136.13454\"";
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
