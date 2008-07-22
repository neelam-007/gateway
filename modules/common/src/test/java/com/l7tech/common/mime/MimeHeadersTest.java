/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.ByteArrayInputStream;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class MimeHeadersTest extends TestCase {
    private static Logger log = Logger.getLogger(MimeHeadersTest.class.getName());

    public MimeHeadersTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(MimeHeadersTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private static String CONTENT = "This is some content";
    public static final String MESSAGE = "Content-Type: multipart/related;\n boundary\n =\"bugger\"\n" +
            "Content-Length: 289273\n" +
            "Content-ID: <hugeFile>\n\n" +
            CONTENT;

    public void testParseHeaders() throws Exception {
        final ByteArrayInputStream stream = new ByteArrayInputStream(MESSAGE.getBytes("UTF-8"));
        MimeHeaders headers = MimeUtil.parseHeaders(stream);
        headers.write(System.err);

        // Make sure the blank line between headers and body is consumed
        byte[] contentBuffer = new byte[4096];
        int got = stream.read(contentBuffer);
        assertEquals(new String(contentBuffer, 0, got, "UTF-8"), CONTENT);
    }
}
