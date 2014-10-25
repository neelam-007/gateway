/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import static org.junit.Assert.*;

import com.l7tech.util.SyspropUtil;
import org.junit.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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

    @After
    public void afterTest() {
        cleanup();
    }

    @AfterClass
    public static void afterClass() {
        cleanup();
    }


    public static void cleanup() {
        SyspropUtil.clearProperties(
                "com.l7tech.common.mime.values.utf8"
        );
        MimeUtil.updateMimeEncoding();
    }

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

    static final String JAPANESE_FILENAME;
    static final String JAPANESE_FILENAME_UTF8_AS_LATIN1;
    static {
        try {
            JAPANESE_FILENAME = new String( HexUtils.unHexDump( "e697a5e69cace8aa9ee381aee38386e382b9e38388" ), Charsets.UTF8 );
            JAPANESE_FILENAME_UTF8_AS_LATIN1 = new String( JAPANESE_FILENAME.getBytes( Charsets.UTF8 ), Charsets.ISO8859 );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Test
    @BugId( "SSG-9380" )
    public void testInboundLatin1() throws Exception {
        // Client will send a header containing a UTF-8 Japanese filename.  We will by default parse it
        // as Latin1, which is fine as long as we also use Latin1 on the way out (so the outbound request
        // sends out the same byte stream that arrived inbound).
        // client(bytes, interpreted as UTF-8) -> Gateway(bytes, interpreted as Latin1) -> Server(bytes, interpreted as UTF-8)
        String headersStr = "Test-Header: foo=\"" + JAPANESE_FILENAME + "\"\r\n\r\n";
        InputStream stream = new ByteArrayInputStream( headersStr.getBytes( Charsets.UTF8) );

        /// ... bytes sent from client to Gateway ...

        MimeHeaders headers = MimeUtil.parseHeaders( stream );
        MimeHeader mh = headers.get( 0 );

        String value = mh.getFullValue();

        // Within Gateway, value looks like gibberish but byte values will be preserved on output
        assertEquals( "foo=\"" + JAPANESE_FILENAME_UTF8_AS_LATIN1 + "\"", value );
    }

    @Test
    @BugId( "SSG-9380" )
    public void testOutboundLatin1() throws Exception {
        // We have a header containing the UTF-8 bytes of a Japanese filename that has been parsed
        // as Latin1 (which is the default).  We will serialize the header back to bytes as Latin1 (the default)
        // and then decode as UTF-8 which should recover the original undamaged filename.
        // client(bytes, interpreted as UTF-8) -> Gateway(bytes, interpreted as Latin1) -> Server(bytes, interpreted as UTF-8)
        MimeHeader mh = MimeHeader.parseValue( "Test-Header", "foo=\"" + JAPANESE_FILENAME_UTF8_AS_LATIN1 + "\"" );
        byte[] bytes = mh.getSerializedBytes();

        // ... bytes sent from Gateway to server ...

        String serialized = new String( bytes, Charsets.UTF8 );
        assertEquals( "Test-Header: foo=\"" + JAPANESE_FILENAME + "\"\r\n", serialized );
    }

    @Test
    @BugId( "SSG-9380" )
    public void testInboundUtf8() throws Exception {
        try {
            SyspropUtil.setProperty( "com.l7tech.common.mime.values.utf8", "true" );
            MimeUtil.updateMimeEncoding();

            // Client will send a header containing a UTF-8 Japanese filename.  We will (in this non-default configuration) parse it
            // as UTF-8.
            // client(bytes, interpreted as UTF-8) -> Gateway(bytes, interpreted as UTF-8) -> Server(bytes, interpreted as UTF-8)
            String headersStr = "Test-Header: foo=\"" + JAPANESE_FILENAME + "\"\r\n\r\n";
            InputStream stream = new ByteArrayInputStream( headersStr.getBytes( Charsets.UTF8) );

            /// ... bytes sent from client to Gateway ...

            MimeHeaders headers = MimeUtil.parseHeaders( stream );
            MimeHeader mh = headers.get( 0 );

            String value = mh.getFullValue();

            // Within Gateway, value looks like original Japanese characters (rather than Latin1 gibberish)
            assertEquals( "foo=\"" + JAPANESE_FILENAME + "\"", value );
        } finally {
            SyspropUtil.clearProperty( "com.l7tech.common.mime.values.utf8" );
        }
    }

    @Test
    @BugId( "SSG-9380" )
    public void testOutboundUtf8() throws Exception {
        try {
            SyspropUtil.setProperty( "com.l7tech.common.mime.values.utf8", "true" );
            MimeUtil.updateMimeEncoding();

            // We have a header containing the UTF-8 bytes of a Japanese filename that has been parsed
            // as UTF-8 (in this non-default configuration).  We will serialize the header back to bytes as UTF-8
            // (in this non-default configuration)
            // and then decode as UTF-8 which should recover the original undamaged filename.
            // client(bytes, interpreted as UTF-8) -> Gateway(bytes, interpreted as Latin1) -> Server(bytes, interpreted as UTF-8)
            MimeHeader mh = MimeHeader.parseValue( "Test-Header", "foo=\"" + JAPANESE_FILENAME + "\"" );
            byte[] bytes = mh.getSerializedBytes();

            // ... bytes sent from Gateway to server ...

            String serialized = new String( bytes, Charsets.UTF8 );
            assertEquals( "Test-Header: foo=\"" + JAPANESE_FILENAME + "\"\r\n", serialized );

        } finally {
            SyspropUtil.clearProperty( "com.l7tech.common.mime.values.utf8" );
        }
    }
}
