/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import com.l7tech.common.util.HexUtils;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    public void testSimple() throws Exception {
        MultipartMessage mm = makeRubyMessage();

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
        } catch (IOException e) {
            log.info("The Ruby part was consumed successfully: " + e.getMessage());
        }

        try {
            PartInfo bogusPart = mm.getPart(2);
            fail("Failed to get expected exception");
        } catch (NoSuchPartException e) {
            log.info("Got proper exception on trying to get a nonexistent MIME part: " + e.getMessage());
        }
    }

    private MultipartMessage makeRubyMessage() throws IOException, NoSuchPartException {
        InputStream mess = new ByteArrayInputStream(MESS.getBytes());
        ContentTypeHeader mr = ContentTypeHeader.parseValue("multipart/related; type=\"text/xml\"; boundary=\"----=Part_-763936460.407197826076299\"; start=\"-76394136.15558\"");
        StashManager sm = new ByteArrayStashManager();
        MultipartMessage mm = MultipartMessage.createMultipartMessage(sm, mr, mess);
        return mm;
    }

    public void testSinglePart() throws Exception {
        // TODO
    }

    public void testStreamAll() throws Exception {
        MultipartMessage mm = makeRubyMessage();

        // TODO
    }

    public void testLookupsByCid() throws Exception {
        MultipartMessage mm = makeRubyMessage();
        // TODO
    }

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
            "------=Part_-763936460.407197826076299--";
}
