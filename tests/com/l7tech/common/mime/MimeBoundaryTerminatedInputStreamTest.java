/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import com.l7tech.common.util.HexUtils;
import com.l7tech.common.io.NullOutputStream;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.*;
import java.util.logging.Logger;
import java.util.Random;

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

    /**
     * Create a SwaTestcaseFactory which can be used to generate test messages.  The same stream of test messages
     * will be generated every time this is called with the same numparts, size, and simpleSeed.
     *
     * @param numparts    number of parts to generate, not including the preamble
     * @param size        size of each part in bytes
     * @param simpleSeed  a small number to seed the PRNG so that the test runs the same way every time.
     *                    Will be combined with the size and numparts to make a unique but reproducable test message.
     * @return a new SwaTestCaseFactory configured to produce messages with the given number of parts, each
     *         with the specified size, and with the PRNG seeded based on the numparts, size, and simpleSeed.
     */
    private static SwaTestcaseFactory makeStfu(int numparts, int size, int simpleSeed) {
        return new SwaTestcaseFactory(numparts, size, simpleSeed*28387 + size*149 + numparts*487 + 521 + simpleSeed);
    }

    public void testSinglepart() throws Exception {
        // A singlepart message will not contain the boundary, so should throw.

        // A random boundary
        final SwaTestcaseFactory stfu = makeStfu(1, 100, 10);
        stfu.makeTestMessage();
        byte[] rawBoundary = stfu.getBoundary();
        byte[] boundary = new String("--" + new String(rawBoundary)).getBytes();

        byte[] crap = new byte[11000];
        stfu.getRandom().nextBytes(crap);

        // Make sure we didn't include the boundary in our generated test garbage by accident (unlikely)
        assertEquals(-1, HexUtils.matchSubarrayOrPrefix(crap, 0, crap.length, boundary, 0));

        // Now try to test it
        int pushSize = 4096 - 11;
        PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(crap));
        MimeBoundaryTerminatedInputStream mbti = new MimeBoundaryTerminatedInputStream(boundary, in, pushSize);

        try {
            copyStreamInIrregularChunks(mbti, new NullOutputStream(),
                                        stfu.getRandom(), 1, 128, 0, 10);
            fail("Failed to throw exception when reading a message that contained no MIME boundaries");
        } catch (IOException e) {
            // Ok
            log.info("Caught expected exception reading message that contains no MIME boundaries: " + e.getMessage());
        }
    }

    public void testMultipartWithOnePart() throws Exception {
        int numparts = 1;
        int size = 500;
        SwaTestcaseFactory stfu = makeStfu(numparts, size, 1);
        byte[] testMsg = stfu.makeTestMessage();
        log.info("Constructed test MIME multipart message " + testMsg.length + " bytes long");
        readParts(testMsg, numparts, 4096);
    }

    public void testManyMultiparts() throws Exception {
        int numparts = 50;
        int size = 2030;
        SwaTestcaseFactory stfu = makeStfu(numparts, size, 2);
        byte[] testMsg = stfu.makeTestMessage();
        log.info("Constructed test MIME multipart message " + testMsg.length + " bytes long");
        readParts(testMsg, numparts, 4096);
    }

    public void testSmallBlockSize() throws Exception {
        int numparts = 3;
        int size = 1000;
        SwaTestcaseFactory stfu = makeStfu(numparts, size, 3);
        byte[] testMsg = stfu.makeTestMessage();
        log.info("Constructed test MIME multipart message " + testMsg.length + " bytes long");
        readParts(testMsg, numparts, 512);
    }

    public void testTinyBlockSize() throws Exception {
        int numparts = 4;
        int size = 400;
        SwaTestcaseFactory stfu = makeStfu(numparts, size, 4);
        byte[] testMsg = stfu.makeTestMessage();
        log.info("Constructed test MIME multipart message " + testMsg.length + " bytes long");
        readParts(testMsg, numparts, 5);
    }

    public void testMicroscopicBlockSize() throws Exception {
        int numparts = 4;
        int size = 400;
        SwaTestcaseFactory stfu = makeStfu(numparts, size, 5);
        byte[] testMsg = stfu.makeTestMessage();
        log.info("Constructed test MIME multipart message " + testMsg.length + " bytes long");
        readParts(testMsg, numparts, 3);
    }
    
    public void testRandomBlockSize() throws Exception {
        int numparts = 20;
        int size = 10;
        SwaTestcaseFactory stfu = makeStfu(numparts, size, 5);
        byte[] testMsg = stfu.makeTestMessage();
        log.info("Constructed test MIME multipart message " + testMsg.length + " bytes long");
        //log.info("Message: \n" + new String(testMsg));
        readParts(testMsg, numparts, stfu.getRandom(), 1, 100, 0, 5);
    }

    public void testHugeBlockSize() throws Exception {
        int numparts = 2;
        int size = 1024 * 200 + 17;
        SwaTestcaseFactory stfu = makeStfu(numparts, size, 6);
        byte[] testMsg = stfu.makeTestMessage();
        log.info("Constructed test MIME multipart message " + testMsg.length + " bytes long");
        readParts(testMsg, numparts, 60000);
    }

    public void testHugeAttachment() throws Exception {
        int numparts = 2;
        int size = 1024 * 200 + 17;
        SwaTestcaseFactory stfu = makeStfu(numparts, size, 7);
        byte[] testMsg = stfu.makeTestMessage();
        log.info("Constructed test MIME multipart message " + testMsg.length + " bytes long");
        readParts(testMsg, numparts, 512);
    }

    public void testBadIntermediateBoundaries() throws Exception {
        int numparts = 4;
        int size = 1024 + 17;
        SwaTestcaseFactory stfu = makeStfu(numparts, size, 8);
        byte[] testMsg = stfu.makeTestMessage();
        byte[] crlfBoundary;
        {
            byte[] rawBoundary = stfu.getBoundary();
            byte[] boundary = new String("--" + new String(rawBoundary)).getBytes();
            crlfBoundary = new String("\r\n" + new String(boundary)).getBytes();
        }

        // Find opening boundary of first part
        int first = HexUtils.matchSubarrayOrPrefix(testMsg, 0, testMsg.length, crlfBoundary, 0);
        assertTrue(first > 0);
        assertTrue(HexUtils.compareArrays(testMsg, first, crlfBoundary, 0, crlfBoundary.length));

        // Find closing boundary of first part
        int second = HexUtils.matchSubarrayOrPrefix(testMsg, first + crlfBoundary.length + 2,
                                                    testMsg.length - (first + crlfBoundary.length + 2),
                                                    crlfBoundary, 0);
        assertTrue(second > first);
        assertTrue(HexUtils.compareArrays(testMsg, second, crlfBoundary, 0, crlfBoundary.length));

        class Term {
            String test;
            Class expectedResult; // null = success; otherwise, exception superclass that must be thrown
            Term(String test, Class expectedResult) {
                this.test = test;
                this.expectedResult = expectedResult;
            }
        }

        Term[] terminators = {
            new Term("\r\n", null), // just the line ending (should be ignored)
            new Term("\n\r", null), // a backwards line ending (should be ignored)
            new Term("--\r\n",     NotEnoughPartsException.class),// a normal final terminator (should end the message early)
            new Term(" --\r\n",    NotEnoughPartsException.class),// terminator on same line (should end the message early)
            new Term("ashs--\r\n", NotEnoughPartsException.class),// terminator on same line (should end the message early)
            new Term("---\r",      NotEnoughPartsException.class),// terminator on same line (should end the message early)
            new Term("---\n",      NotEnoughPartsException.class),// terminator on same line (should end the message early)
            new Term("--\r\n",     NotEnoughPartsException.class),// terminator on same line (should end the message early)
            new Term("-\n--\r",    NotEnoughPartsException.class),// terminator on same line (should end the message early)
            new Term("-\n--\r\n",  NotEnoughPartsException.class),// terminator on same line (should end the message early)
            new Term("- - -\r", null), // divided terminator (should be ignored)
            new Term("-\r-\n", null),  // divided terminator (should be ignored)
            new Term("-\n-\r", null),  // divided terminator (should be ignored)
            new Term("\n-\r-", null),  // divided terminator (should be ignored)
            new Term("\r\n--", null),  // terminator on next line (should be ignored as part of next part's headers)
            new Term("\n\r--", null),  // terminator on next line (should be ignored as part of next part's headers)
            new Term("\r\n\r\n", null), // extra line (should be ignored, will terminate next part's headers early)
            new Term("\r\r\n\n", null), // eventual CRLF (should be ignored, will leave bare LF as first byte of header)
            new Term("\n\r\n\r", null), // backwards LFCRs (should be ignored, will leave backwards LFCR as start of next header)
        };

        byte[] saved = new byte[10];
        System.arraycopy(testMsg, second + crlfBoundary.length, saved, 0, saved.length);
        for (int i = 0; i < terminators.length; i++) {
            Term term = terminators[i];
            String t = term.test;
            Class ex = term.expectedResult;
            final String hex = HexUtils.hexDump(t.getBytes());
            System.arraycopy(saved, 0, testMsg, second + crlfBoundary.length, saved.length); // restore saved
            System.arraycopy(t.getBytes(), 0, testMsg, second + crlfBoundary.length, t.length()); // overwrite
            log.info("Testing boundary suffix: " + hex);
            try {
                readParts(testMsg, numparts, 512);
                if (ex != null)
                    fail("Failed to throw expected exception " + ex + " using terminator " + hex);
            } catch (Exception e) {
                if (ex == null)
                    throw new RuntimeException("Unexpected exception was thrown: " + e.getMessage(), e);
                if (!ex.isAssignableFrom(e.getClass()))
                    throw new RuntimeException("Wrong exception was thrown: " + e.getMessage(), e);
                log.info("Expected exception was thrown: " + e.getMessage());
            }
        }
        System.arraycopy(saved, 0, testMsg, second + crlfBoundary.length, saved.length); // restore saved

    }

    public void testBadTerminatingBoundaries() throws Exception {
        int numparts = 4;
        int size = 1024 + 17;
        SwaTestcaseFactory stfu = makeStfu(numparts, size, 9);
        byte[] testMsg = stfu.makeTestMessage();
        byte[] rawBoundary = stfu.getBoundary();
        byte[] boundary = new String("\r\n--" + new String(rawBoundary) + "--\r\n").getBytes();

        int match = HexUtils.matchSubarrayOrPrefix(testMsg, testMsg.length - boundary.length - 6, boundary.length + 6, boundary, 0);
        assertTrue(match > 0);
        assertTrue(HexUtils.compareArrays(testMsg, match, boundary, 0, boundary.length));

        // Verify that acceptable terminator variants work as expected

        String[] goodTerminators = {
            "--\r\n",
            "--\n\r",
            "----",
            "---\r",
            "---\n",
        };

        for (int i = 0; i < goodTerminators.length; i++) {
            String t = goodTerminators[i];
            System.arraycopy(t.getBytes(), 0, testMsg, match + boundary.length - t.length(), t.length());
            readParts(testMsg, numparts, 512);
        }

        // Now try various nasty terminating boundaries

        String[] badTerminators = {
            "-\r-\n",
            "-\n-\r",
            "\r\n--",
            "\n\r--",
            "\n-\r-",
            "\r\n\r\n",
            "\r\r\n\n",
            "\n\r\n\r",
        };

        for (int i = 0; i < badTerminators.length; i++) {
            String t = badTerminators[i];
            System.arraycopy(t.getBytes(), 0, testMsg, match + boundary.length - t.length(), t.length());
            try {
                readParts(testMsg, numparts, 512);
                fail("Failed to complain about extra parts after bogus terminator: " + HexUtils.hexDump(t.getBytes()));
            } catch (TooManyPartsException e) {
                // Ok
            }
        }
    }

    public static long copyStreamInIrregularChunks(InputStream in,
                                  OutputStream out, 
                                  Random random, 
                                  int blockSizeLow, 
                                  int blockSizeHigh,
                                  int blockExtraLow,
                                  int blockExtraHigh)
            throws IOException 
    {
        if (blockSizeHigh < blockSizeLow) throw new IllegalArgumentException();
        if (blockSizeLow < 1) throw new IllegalArgumentException("Minimum blocksize too small");
        if (blockExtraHigh < blockExtraLow) throw new IllegalArgumentException();
        if (blockExtraLow < 0) throw new IllegalArgumentException();
        if (in == null || out == null) throw new NullPointerException();
        int got;
        long total = 0;
        int blocksize;
        int extra;
        for (;;) {
            if (blockSizeLow == blockSizeHigh && blockExtraLow == 0 && blockExtraHigh ==0) {
                extra = 0;
                blocksize = blockSizeLow;
            } else {
                extra = random.nextInt(blockExtraHigh - blockExtraLow + 1) + 1;
                blocksize = random.nextInt(blockSizeHigh - blockSizeLow + 1) + 1;
            }
            byte[] buf = new byte[blocksize + extra];

            // randomize buffer content before read
            random.nextBytes(buf);

            if (extra < 1) {
                // simple full-buffer read
                got = in.read(buf);
            } else {
                // Save a backup of the
                byte[] bufBak = new byte[buf.length];
                System.arraycopy(buf, 0, bufBak, 0, buf.length);
                int start = random.nextInt(extra);

                got = in.read(buf, start, blocksize);

                // Ensure that no portions of the buffer outside the area we asked to write into were overwritten
                if (start > 0)
                    assertTrue(HexUtils.compareArrays(buf, 0, bufBak, 0, start));
                if (start < extra)
                    assertTrue(HexUtils.compareArrays(buf, start + blocksize, bufBak, start + blocksize, extra - start));
            }
            if (got <= 0)
                break;
            out.write(buf, 0, got);
            total += got;
        }
        return total;
    }
    
    private static class TooManyPartsException extends Exception {}
    private static class NotEnoughPartsException extends Exception {}

    private void readParts(byte[] testMsg, int numparts, int blockSize) throws IOException, TooManyPartsException, NotEnoughPartsException {
        readParts(testMsg, numparts, new Random(), blockSize, blockSize, 0, 0);
    }
    
    private void readParts(byte[] testMsg,
                           int numparts,
                           Random random,
                           int blockSizeLow,
                           int blockSizeHigh,
                           int blockExtraLow,
                           int blockExtraHigh)
            throws IOException, TooManyPartsException, NotEnoughPartsException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(testMsg);
        int psize = 4096;
        PushbackInputStream pis = new PushbackInputStream(bais, psize);

        MimeHeaders outerHeaders = MimeUtil.parseHeaders(pis);
        byte[] boundary = ("--" + outerHeaders.getContentType().getMultipartBoundary()).getBytes();
        log.info("Using multipart boundry of <" + new String(boundary) + ">");

        // Eat preamble
        MimeBoundaryTerminatedInputStream mbtis = new MimeBoundaryTerminatedInputStream(boundary, pis, psize);

        ByteArrayOutputStream preamble = new ByteArrayOutputStream();
        copyStreamInIrregularChunks(mbtis, preamble, random, blockSizeLow, blockSizeHigh, blockExtraLow, blockExtraHigh);
        log.info("Successfully read " + preamble.toByteArray().length + " bytes of preamble");

        MimeHeaders innerHeaders;
        ByteArrayOutputStream partOut;
        int total;

        for (int partNum = 0; partNum < numparts; partNum++) {
            // Read first part headers
            innerHeaders = MimeUtil.parseHeaders(pis);

            // Read first part and count bytes
            mbtis = new MimeBoundaryTerminatedInputStream(boundary, pis, psize);
            partOut = new ByteArrayOutputStream();
            copyStreamInIrregularChunks(mbtis, partOut, random, blockSizeLow, blockSizeHigh, blockExtraLow, blockExtraHigh);
            total = partOut.toByteArray().length;
            assertTrue(total > 0);
            log.info("Successfully read " + total + " bytes of part #" + partNum);

            if (innerHeaders.hasContentLength()) {
                assertEquals(total, innerHeaders.getContentLength());
                log.info("Verified that length of part matched its declared Content-Length.");
            } else
                log.info("(Not verifying length -- partOut had no Content-Length header)");

            if (partNum >= numparts - 1) {
                if (!mbtis.isLastPartProcessed()) {
                    throw new TooManyPartsException(); // got at least one extra part
                }
            } else if (mbtis.isLastPartProcessed()) {
                throw new NotEnoughPartsException(); // saw message end before we expected it
            }

        }
    }

    public void testRubyMultipartWithTwoParts() throws Exception {
        byte[] testMsg = ("Content-Type: multipart/related; boundary=\"----=Part_-763936460.407197826076299\"\r\n\r\n" +
                MultipartMessageTest.MESS).getBytes();
        log.info("Constructed test MIME multipart message " + testMsg.length + " bytes long: \n" + new String(testMsg));
        readParts(testMsg, 2, 4096);
    }

    public void testRubyMultipartWithTwoPartsAndNoPreamble() throws Exception {
        // This is actual a legal multipart message -- messages with no preamble will have "--" as their first bytes
        // rather than "\r\n--" -- but rather than special case this, MimeBoundaryTerminatedInputStream requires that
        // the extra "\r\n" be inserted by its client.  This test verifies the expected failure if this is not done.

        byte[] testMsg = ("Content-Type: " + MultipartMessageTest.CT2 + "\r\n\r\n" +
                MultipartMessageTest.MESS2).getBytes();
        log.info("Constructed test MIME multipart message " + testMsg.length + " bytes long: \n" + new String(testMsg));
        try {
            readParts(testMsg, 2, 4096);
            fail("Did not receive expected IOException reading preamble with no CRLF before initial boundary");
        } catch (NotEnoughPartsException e) {
            log.info("Received expected exception when trying to read preamble with no CRLF before initial boundary: " + e.getMessage());
        }
    }

    public void testEmptyStream() throws Exception {
        String boundary = "----=Part_-763936460.407197826076299";
        String stream = boundary + "--\r\n";

        PushbackInputStream is = new PushbackInputStream(new ByteArrayInputStream(stream.getBytes()), 4096);
        MimeBoundaryTerminatedInputStream mbtis =
                new MimeBoundaryTerminatedInputStream(boundary.getBytes(), is, 4096);

        ByteArrayOutputStream got = new ByteArrayOutputStream();
        try {
            HexUtils.copyStream(mbtis, got);
            fail("Did not receive expected IOException reading preamble with no CRLF before initial boundary");
        } catch (IOException e) {
            log.info("Received expected IOException when trying to read preamble with no CRLF before initial boundary: " + e.getMessage());
        }
    }
}
