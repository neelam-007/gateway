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

/**
 * @author alex
 * @version $Revision$
 */
public class SwaTestcaseFactory {
    private SwaTestcaseFactory(int num, int max) {
        this.numParts = num;
        this.maxSize = max;
        this.boundary = randomBoundary();
    }

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

    private byte[] randomBinary(int max) {
        byte[] bytes = new byte[max];
        int oopsPos = random.nextInt(max);
        int oopsLen = random.nextInt(boundary.length-1)+1;
        random.nextBytes(bytes);
        for (int i = 0; i < oopsLen; i++) {
            bytes[oopsPos + i] = boundary[i];
        }
        return bytes;
    }

    public static void main(String[] args) throws IOException {
        int numParts = Integer.parseInt(args[0]);
        int maxSize = Integer.parseInt(args[1]);
        SwaTestcaseFactory me = new SwaTestcaseFactory(numParts, maxSize);
        System.out.write(me.doIt());
    }

    private byte[] doIt() throws IOException {
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

            baos.write(("Content-Length: " + maxSize).getBytes());
            baos.write(CRLF);
            baos.write(("Content-Type: application/octet-stream").getBytes());
            baos.write(CRLF);
            baos.write(("X-L7-Trailing-blank-lines: " + trailingBlanks).getBytes());
            baos.write(CRLF);
            baos.write(CRLF);

            baos.write(randomBinary(maxSize));
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

    private final int numParts;
    private final int maxSize;
    private final byte[] boundary;
    private final Random random = new Random();
    public static final byte[] CRLF = "\r\n".getBytes();
}
