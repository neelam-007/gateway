/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import javax.mail.internet.HeaderTokenizer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Spews streams of random bytes that look like Soap-with-Attachments (except without all the SOAP)
 *
 * @author alex
 * @version $Revision$
 */
class SwaTestcaseFactory {
    private static final Logger logger = Logger.getLogger(SwaTestcaseFactory.class.getName());

    /**
     * Create a factory for producing random SOAP-with-attachments test messages.
     *
     * @param num number of parts to produce in each multipart/related message
     * @param partSize the size of each part
     */
    SwaTestcaseFactory(int num, int partSize, long seed) {
        this.numParts = num;
        this.partSize = partSize;
        this.random = new Random(seed);
        this.boundary = randomBoundary();
    }

    /**
     * Generates a random boundary consisting of between 1 and 40 legal characters,
     * plus a prefix of 0-5 hyphens and the magic quoted-printable-proof "=_"
     * @return
     */
    private byte[] randomBoundary() {
        StringBuffer bb = new StringBuffer();

        for (int i = 0; i < random.nextInt(5); i++) {
            bb.append('-');
        }

        bb.append("=_");

        for (int i = 0; i < random.nextInt(40)+1; i++) {
            byte printable = (byte)(random.nextInt(127-32)+32);
            if (HeaderTokenizer.MIME.indexOf(printable) < 0)
                bb.append(new String(new byte[] { printable }));
        }

        return bb.toString().getBytes();
    }

    /**
     * Returns an array of max bytes in a small number of bytes have been replaced with a fragment of the {@link #boundary}.
     */
    private byte[] randomBinary(int max) {

        // Fill the part with random bytes
        byte[] bytes = new byte[max];
        random.nextBytes(bytes);

        // Generate a random number of "red herring" fake almost-boundaries
        int numOopses = random.nextInt((max / (random.nextInt(150) + 100)) + random.nextInt(4));
        for (int oopsNum = 0; oopsNum < numOopses; oopsNum++) {
            // Position it right to the end; we'll truncate it if necessary
            int oopsPos = random.nextInt(max);

            // never emit last 2 bytes of boundary -- it starts getting too likely for next random
            // character to happen to match it
            int oopsLen = random.nextInt(boundary.length + 3);

            if (oopsPos < max && oopsLen-- > 0) bytes[oopsPos++] = '\r';
            if (oopsPos < max && oopsLen-- > 0) bytes[oopsPos++] = '\n';
            if (oopsPos < max && oopsLen-- > 0) bytes[oopsPos++] = '-';
            if (oopsPos < max && oopsLen-- > 0) bytes[oopsPos++] = '-';
            for (int i = 0; oopsLen > 0 && i < oopsLen && i + oopsPos < max; i++) {
                bytes[oopsPos + i] = boundary[i];
            }
        }
        return bytes;
    }

    public static void main(String[] args) throws Exception {
        //if (args.length < 2) throw new Exception("Usage: " + SwaTestcaseFactory.class.getName() + " numParts partSize");
        int numParts;
        int maxSize;
        if (args.length < 2) {
            numParts = 8;
            maxSize = 2047;
            logger.info("Using default arguments: " + numParts + " " + maxSize);
        } else {
            numParts = Integer.parseInt(args[0]);
            maxSize = Integer.parseInt(args[1]);
        }
        SwaTestcaseFactory me = new SwaTestcaseFactory(numParts, maxSize, 37362);
        System.out.write(me.makeTestMessage());
    }

    public byte[] makeTestMessage() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(40960);
        baos.write(("Content-Type: multipart/related; boundary=\"" + new String(boundary) + "\"").getBytes());
        baos.write(CRLF);
        baos.write(CRLF);
        byte[] preamble = new byte[random.nextInt(1024)];
        baos.write("This preamble should be ignored by a SwA parser:\r\n".getBytes());
        random.nextBytes(preamble);
        baos.write(preamble);
        baos.write("\r\nThat about wraps it up for preamble.\r\n".getBytes());
        for (int i = 0; i < numParts; i++) {
            int trailingBlanks = random.nextInt(5);
            baos.write(CRLF);
            baos.write("--".getBytes());
            baos.write(boundary);
            baos.write(CRLF);

            baos.write(("Content-Length: " + (partSize + trailingBlanks * 2)).getBytes());
            baos.write(CRLF);
            baos.write(("Content-Type: application/octet-stream").getBytes());
            baos.write(CRLF);
            baos.write(("X-L7-Trailing-blank-lines: " + trailingBlanks).getBytes());
            baos.write(CRLF);
            baos.write(CRLF);

            baos.write(randomBinary(partSize));
            for (int j = 0; j < trailingBlanks; j++) {
                baos.write(CRLF);
            }
        }

        baos.write(CRLF);
        baos.write("--".getBytes());
        baos.write(boundary);
        baos.write("--".getBytes());
        baos.write(CRLF);
        return baos.toByteArray();
    }

    /**
     * @return the boundary used by the last message generated, not including leading dashes or any CRLF.  Never null.
     * @throws IllegalStateException if no message has been generated yet  
     */
    public byte[] getBoundary() {
        if (boundary == null) throw new IllegalStateException("No message has been generated yet");
        return boundary;
    }

    private final int numParts;
    private final int partSize;
    private final byte[] boundary;
    private final Random random;
    public static final byte[] CRLF = "\r\n".getBytes();
}
