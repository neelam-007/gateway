/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class MimeHeaderTest extends TestCase {
    private static Logger log = Logger.getLogger(MimeHeaderTest.class.getName());

    public MimeHeaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(MimeHeaderTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private static String[][] failingContentTypeValues = new String[][] {
        {"missing type", "foo\n  bar"},
        {"missing type delimiter", "foo"},
        {"missing subtype", "foo/"},
    };

    public void testParseContentTypeHeaderValue() throws Exception {
        // Make sure failers fail
        for (int i = 0; i < failingContentTypeValues.length; i++) {
            String[] f = failingContentTypeValues[i];
            String msg = f[0];
            String ctype = f[1];

            try {
                ContentTypeHeader.parseValue(ctype);
                fail("Expected exception not thrown: IOException on " + msg);
            } catch (IOException e) {
                // ok
            }
        }

        String boundary1 = "-------#&^@%!\\\"*&#&^*@&^@%jkahsdf*   &@^";
        ContentTypeHeader h = ContentTypeHeader.parseValue("multipart\n /\nrelated;\n boundary\n =\n \"" + boundary1 + "\"");
        assertEquals(boundary1.replaceAll("\\\\", ""), h.getParam("boundary"));
    }


    public void testBadContentLength() throws IOException {
        try {
            MimeHeader h = MimeHeader.parseValue("Content-Length", "foo");
            fail("Content-Length header was allowed with bogus value 'foo'");
        } catch (IOException e) {
            log.info("Correct exception thrown on attempt to parse bogus content-length header: " + e.getMessage());
        }

        MimeHeader h = MimeHeader.parseValue("Content-Length", "2893472");
    }

}
