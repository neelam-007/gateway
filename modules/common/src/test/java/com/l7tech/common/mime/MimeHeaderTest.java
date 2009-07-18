/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import com.l7tech.test.BugNumber;
import com.l7tech.util.HexUtils;
import static org.junit.Assert.*;
import org.junit.*;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class MimeHeaderTest {
    private static Logger log = Logger.getLogger(MimeHeaderTest.class.getName());

    /** Delta for comparing double values. */
    private static final double DELTA = 0.00000005d;

    private static String[][] failingContentTypeValues = new String[][] {
        {"missing type", "foo\n  bar"},
        {"missing type delimiter", "foo"},
        {"missing subtype", "foo/"},
    };

    @Test
    public void testParseContentTypeHeaderValue() throws Exception {
        // Make sure failers fail
        for (String[] failer : failingContentTypeValues) {
            String msg = failer[0];
            String ctype = failer[1];

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

    @Test
    public void testBadContentLength() throws IOException {
        try {
            MimeHeader.parseValue("Content-Length", "foo");
            fail("Content-Length header was allowed with bogus value 'foo'");
        } catch (IOException e) {
            log.info("Correct exception thrown on attempt to parse bogus content-length header: " + e.getMessage());
        }

        MimeHeader.parseValue("Content-Length", "2893472");
    }

    @Test
    public void testContentTypeWithUnquotedMetacharacters() throws Exception {
        ContentTypeHeader ct = ContentTypeHeader.parseValue("multipart/related;boundary=----=_Part_314443_680334604.1130361033168");
        assertEquals(ct.getMultipartBoundary(), "----=_Part_314443_680334604.1130361033168");

        ContentTypeHeader ct2 = ContentTypeHeader.parseValue("multipart/related;boundary=----=/'23!$$#@=Part_314443_680334604.1130361033168");
        assertEquals(ct2.getMultipartBoundary(), "----=/'23!$$#@=Part_314443_680334604.1130361033168");

        // This shouldn't really be legal since it has a space in the boundary.  Our parser will flatten the spaces.
        ContentTypeHeader ct3 = ContentTypeHeader.parseValue("multipart/related;boundary=----=/'23!$$#@=Part_314443_680334604.1130361033168 blee=blah");
        assertEquals(ct3.getMultipartBoundary(), "----=/'23!$$#@=Part_314443_680334604.1130361033168blee=blah");
    }

    @Test
    public void testContentTypePatternMatching() throws Exception {
        ContentTypeHeader textXml     = ContentTypeHeader.parseValue("tExt/xML; charset=utf8");
        ContentTypeHeader textPlain   = ContentTypeHeader.parseValue("text/PlAIn; charset=us-ascii");
        ContentTypeHeader textEnriched= ContentTypeHeader.parseValue("text/enriched");
        ContentTypeHeader textHtml    = ContentTypeHeader.parseValue("tEXt/html; charset=latin1");
        ContentTypeHeader octetstream = ContentTypeHeader.parseValue("appliCation/octet-stream");
        ContentTypeHeader imageGif    = ContentTypeHeader.parseValue("image/gif");
        ContentTypeHeader imageJpeg   = ContentTypeHeader.parseValue("image/jpeg");

        assertTrue(imageGif.matches("*", "*"));
        assertTrue(octetstream.matches("*", "*"));
        assertTrue(octetstream.matches("*", "octet-stream"));  // debatable if this is a good idea
        assertTrue(imageGif.matches("image", "*"));
        assertTrue(imageJpeg.matches("image", "*"));
        assertFalse(textXml.matches("iMAge", "*"));
        assertTrue(imageGif.matches("image", "gif"));
        assertFalse(imageJpeg.matches("image", "gif"));
        assertTrue(textHtml.matches("text", "*"));
        assertTrue(textPlain.matches("tEXt", "enRIChed"));
        assertTrue(textEnriched.matches("text", "enriched"));
        assertFalse(textXml.matches("text", "enriched"));
    }

    @Test
    public void testAuthorization() throws Exception {
        String name = "Authorization";
        String value = "Basic " + HexUtils.encodeBase64("login:password".getBytes());
        MimeHeader mh = MimeHeader.parseValue(name, value);
        assertEquals(mh.getName(), "Authorization");
        assertEquals(mh.getMainValue(), value);
    }

    @Test
    @BugNumber(7353)
    public void testParseNumericValue() throws Exception {
        assertEquals(0d, MimeHeader.parseNumericValue("0"), DELTA);
        assertEquals(8347348734d, MimeHeader.parseNumericValue("8347348734"), DELTA);
        assertEquals(23498798327539857d, MimeHeader.parseNumericValue("23498798327539857,23498798327539857"), DELTA);
        assertEquals(634, MimeHeader.parseNumericValue("  \t634   ,634, 634 , 634 "), DELTA);
    }

    @Test(expected=NumberFormatException.class)
    @BugNumber(7343)
    public void testParseNumberValue_doubleFormat() throws Exception {
        MimeHeader.parseNumericValue("-634.55   ,-634.55, -634.55 , -634.55");
    }

    @Test(expected=NumberFormatException.class)
    @BugNumber(7343)
    public void testParseNumberValue_disagreeFirst() throws Exception {
        MimeHeader.parseNumericValue("1,2,2,2");
    }

    @Test(expected=NumberFormatException.class)
    @BugNumber(7343)
    public void testParseNumberValue_disagreeLast() throws Exception {
        MimeHeader.parseNumericValue("44,45");
    }

    @Test(expected=NumberFormatException.class)
    @BugNumber(7343)
    public void testParseNumberValue_empty() throws Exception {
        MimeHeader.parseNumericValue("");
    }

    @Test(expected=NumberFormatException.class)
    @BugNumber(7343)
    public void testParseNumberValue_whitespaceonly() throws Exception {
        MimeHeader.parseNumericValue("  \t ");
    }

    @Test(expected=NumberFormatException.class)
    @BugNumber(7343)
    public void testParseNumberValue_whitespacewithcommas() throws Exception {
        MimeHeader.parseNumericValue("  \t, ");
    }

    @Test(expected=NullPointerException.class)
    @BugNumber(7343)
    public void testParseNumberValue_null() throws Exception {
        MimeHeader.parseNumericValue(null);
    }
}
