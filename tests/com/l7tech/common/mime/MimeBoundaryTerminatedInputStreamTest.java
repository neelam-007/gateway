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
import java.io.PushbackInputStream;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class MimeBoundaryTerminatedInputStreamTest extends TestCase {
    private static Logger log = Logger.getLogger(MimeBoundaryTerminatedInputStreamTest.class.getName());

    public MimeBoundaryTerminatedInputStreamTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(MimeBoundaryTerminatedInputStreamTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testMultipartWithOnePart() throws Exception {
        int numparts = 1;
        int size = 500;
        SwaTestcaseFactory stfu = new SwaTestcaseFactory(numparts, size);
        byte[] testMsg = stfu.makeTestMessage();
        log.info("Constructed test MIME multipart message " + testMsg.length + " bytes long: \n" + new String(testMsg));
        readParts(testMsg, numparts, 4096);
    }

    public void testManyMultiparts() throws Exception {
        int numparts = 50;
        int size = 2030;
        SwaTestcaseFactory stfu = new SwaTestcaseFactory(numparts, size);
        byte[] testMsg = stfu.makeTestMessage();
        log.info("Constructed test MIME multipart message " + testMsg.length + " bytes long: \n" + new String(testMsg));
        readParts(testMsg, numparts, 4096);
    }

    public void testSmallBlockSize() throws Exception {
        int numparts = 3;
        int size = 1000;
        SwaTestcaseFactory stfu = new SwaTestcaseFactory(numparts, size);
        byte[] testMsg = stfu.makeTestMessage();
        log.info("Constructed test MIME multipart message " + testMsg.length + " bytes long: \n" + new String(testMsg));
        readParts(testMsg, numparts, 512);
    }

    public void testTinyBlockSize() throws Exception {
        int numparts = 4;
        int size = 400;
        SwaTestcaseFactory stfu = new SwaTestcaseFactory(numparts, size);
        byte[] testMsg = stfu.makeTestMessage();
        log.info("Constructed test MIME multipart message " + testMsg.length + " bytes long: \n" + new String(testMsg));
        readParts(testMsg, numparts, 5);
    }

    public void testMicroscopicBlockSize() throws Exception {
        int numparts = 4;
        int size = 400;
        SwaTestcaseFactory stfu = new SwaTestcaseFactory(numparts, size);
        byte[] testMsg = stfu.makeTestMessage();
        log.info("Constructed test MIME multipart message " + testMsg.length + " bytes long: \n" + new String(testMsg));
        readParts(testMsg, numparts, 3);
    }

    public void testHugeBlockSize() throws Exception {
        int numparts = 2;
        int size = 1024 * 200 + 17;
        SwaTestcaseFactory stfu = new SwaTestcaseFactory(numparts, size);
        byte[] testMsg = stfu.makeTestMessage();
        log.info("Constructed test MIME multipart message " + testMsg.length + " bytes long");
        readParts(testMsg, numparts, 60000);
    }

    public void testHugeAttachment() throws Exception {
        int numparts = 2;
        int size = 1024 * 200 + 17;
        SwaTestcaseFactory stfu = new SwaTestcaseFactory(numparts, size);
        byte[] testMsg = stfu.makeTestMessage();
        log.info("Constructed test MIME multipart message " + testMsg.length + " bytes long");
        readParts(testMsg, numparts, 512);
    }

    private void readParts(byte[] testMsg, int numparts, int blockSize) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(testMsg);
        int psize = 4096;
        PushbackInputStream pis = new PushbackInputStream(bais, psize);

        MimeHeaders outerHeaders = MimeUtil.parseHeaders(pis);
        byte[] boundary = ("--" + outerHeaders.getContentType().getMultipartBoundary()).getBytes();
        log.info("Using multipart boundry of <" + new String(boundary) + ">");

        // Eat preamble
        MimeBoundaryTerminatedInputStream mbtis = new MimeBoundaryTerminatedInputStream(boundary, pis, psize);

        ByteArrayOutputStream preamble = new ByteArrayOutputStream();
        HexUtils.copyStream(mbtis, preamble, blockSize);
        log.info("Successfully read " + preamble.toByteArray().length + " bytes of preamble: \n" + new String(preamble.toByteArray()));

        MimeHeaders innerHeaders;
        ByteArrayOutputStream part1;
        int total;

        for (int partNum = 0; partNum < numparts; partNum++) {
            // Read first part headers
            innerHeaders = MimeUtil.parseHeaders(pis);

            // Read first part and count bytes
            mbtis = new MimeBoundaryTerminatedInputStream(boundary, pis, psize);
            part1 = new ByteArrayOutputStream();
            HexUtils.copyStream(mbtis, part1, blockSize);
            total = part1.toByteArray().length;
            log.info("Successfully read " + total + " bytes of part1");

            if (innerHeaders.hasContentLength()) {
                assertEquals(total, innerHeaders.getContentLength());
                log.info("Verified that length of part matched its declared Content-Length.");
            } else
                log.info("(Not verifying length -- part1 had no Content-Length header)");
        }
    }

    public void testRubyMultipartWithTwoParts() throws Exception {
        byte[] testMsg = ("Content-Type: multipart/related; boundary=\"----=Part_-763936460.407197826076299\"\r\n\r\n" +
                MultipartMessageTest.MESS).getBytes();
        log.info("Constructed test MIME multipart message " + testMsg.length + " bytes long: \n" + new String(testMsg));
        readParts(testMsg, 2, 4096);
    }
}
