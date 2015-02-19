package com.l7tech.common.mime;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import static org.junit.Assert.*;

/**
 * Tests for ContentTypeHeader
 */
public class ContentTypeHeaderTest {
    /*
     * Test to ensure that trailing ";" is allowed (bug 3610) 
     */
    @Test
    @BugNumber(3610)
    public void testParseContentTypeHeader() throws Exception {
        ContentTypeHeader cth = ContentTypeHeader.parseValue("text/xml; charset=UTF-8;");

        assertEquals("Charset", Charset.forName("UTF-8").name(), cth.getEncoding().name());
        assertEquals("Type", "text", cth.getType());
        assertEquals("Subtype", "xml", cth.getSubtype());
    }

    @Test
    public void testDuplicateParameter() {
        try {
            ContentTypeHeader.parseValue("text/xml; charset=UTF-8; charset=Windows-1252");
            fail("Duplicate parameter name should have triggered parse exception");
        } catch (IOException e) {
            // Ok
        }
    }

    /**
     * Test that JSON is a textual type and support for configurable textual content types
     * @throws Exception
     */
    @Test
    @BugNumber(8884)
    public void testJsonIsTextualOtherContentTypes() throws Exception{
        String jsonType = "application/json; charset=utf-8";
        final ContentTypeHeader jsonHeader = ContentTypeHeader.parseValue(jsonType);
        assertTrue( "Wrong type found", jsonHeader.isJson() );

        String xmlType = "text/xml; charset=utf-8";
        final ContentTypeHeader xmlHeader = ContentTypeHeader.parseValue(xmlType);

        String madeUpType = "application/ijustmadeitup; charset=utf-8";
        final ContentTypeHeader madeUpHeader = ContentTypeHeader.parseValue(madeUpType);

        assertFalse( "Type should not be textual as it is unknown", madeUpHeader.isTextualContentType() );
        ContentTypeHeader.setConfigurableTextualContentTypes(madeUpHeader);
        
        assertTrue( "Type should be found in list of other textual types", madeUpHeader.isTextualContentType() );

        ContentTypeHeader.setConfigurableTextualContentTypes(jsonHeader, xmlHeader);
        assertFalse( "Type should not be found in list of other textual types", madeUpHeader.isTextualContentType() );
    }

    @Test
    @BugNumber(10243)
    public void testFormattingPreserved() throws Exception {
        ensureFormattingPreserved( "JSON type with extra whitespace", "application/json;  charset=utf-8" );
        ensureFormattingPreserved( "XML type with many parameters", "text/xml; charset=utf-8; z=0; a=1; b=2; c=3; d=4; e=5; f=6; g=7; h=8" );
        ensureFormattingPreserved( "Multipart", "Multipart/Related;boundary=MIME_boundary; type=\"application/xop+xml\"; start=\"<mainpart>\"; start-info=\"text/xml\"" );
    }

    @Test
    public void testFormattingOption() throws Exception {
        final String contentType = "a/b;  c=d";
        final ContentTypeHeader contentTypeHeader1 = ContentTypeHeader.parseValue(contentType);
        assertEquals( "Format preserved", contentType, contentTypeHeader1.getFullValue());
        assertArrayEquals( "Format preserved serialized", serialize( contentType ), contentTypeHeader1.getSerializedBytes() );
    }

    @Test
    @BugNumber(9118)
    public void testAllowButFlagWhitespace() throws Exception {
        ContentTypeHeader ch = ContentTypeHeader.create("text / html");
        try {
            ch.validate();
            fail("IOException should have been thrown when validating content type with spaces");
        } catch (IOException e) {
            // Ok
        }
    }

    @Test
    public void testLazyParams() throws Exception {
        ContentTypeHeader ch = ContentTypeHeader.create("foo/bar; a=1; b=3");
        assertEquals("1", ch.getParam("a"));
        assertEquals("3", ch.getParam("b"));
    }

    @Test
    public void testValidate() throws Exception {
        String[] good = {
                "text/xml",
                "a/b;",
                "a/b; c=d",
                "a/b;c=\"d\"",
                "  a/b  ; c=d  ; e=\"f \" ;",
                "application/xop+xml;\n" +
                        "   charset=UTF-8;\n" +
                        "   type=\"text/xml\"",
                "a/b; c==="
        };
        for (String s : good) {
            ContentTypeHeader.create(s).validate();
        }

        String[] bad = {
                "",
                "a",
                "a/b/c",
                "a /b",
                "a/ b;",
                "a/b;c",
                "a/b ; c<="
        };
        for (String s : bad) {
            try {
                ContentTypeHeader.create(s).validate();
                fail("Expected exception not thrown for invalid content type format: " + s);
            } catch (IOException e) {
                // Ok
            }
        }
    }

    @Test
    public void testDefaultCharset() throws Exception {
        assertEquals("utf-8", ContentTypeHeader.XML_DEFAULT.getParam("charset"));
        assertEquals("utf-8", ContentTypeHeader.TEXT_DEFAULT.getParam("charset"));
        assertEquals("utf-8", ContentTypeHeader.SOAP_1_2_DEFAULT.getParam("charset"));
        assertEquals("utf-8", ContentTypeHeader.APPLICATION_JSON.getParam("charset"));
    }

    @Test
    @BugId( "SSG-10718" )
    public void testTypeSpecificDefaultCharsets() throws Exception {
        assertEquals("ISO-8859-1", ContentTypeHeader.parseValue( "text/plain" ).getEncoding().toString() );
        assertEquals("ISO-8859-1", ContentTypeHeader.parseValue( "text/xml" ).getEncoding().toString() );
        assertEquals("ISO-8859-1", ContentTypeHeader.parseValue( "application/blah" ).getEncoding().toString() );
        assertEquals("UTF-32LE", ContentTypeHeader.parseValue( "application/blah; charset=utf-32le" ).getEncoding().toString() );
        assertEquals("UTF-8", ContentTypeHeader.parseValue( "application/json" ).getEncoding().toString() );
        assertEquals("UTF-16", ContentTypeHeader.parseValue( "application/json; charset=utf-16" ).getEncoding().toString() );
        assertEquals("US-ASCII", ContentTypeHeader.parseValue( "application/json; charset=ASCII" ).getEncoding().toString() );
        assertEquals("UTF-8", ContentTypeHeader.parseValue( "application/json; charset=INVALID" ).getEncoding().toString() );
    }

    private void ensureFormattingPreserved( final String description, final String contentType ) throws Exception {
        final ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue(contentType);
        assertEquals( description + " full value", contentType, contentTypeHeader.getFullValue());
        assertArrayEquals( description + " serialized", serialize( contentType ), contentTypeHeader.getSerializedBytes() );
    }

    private byte[] serialize( final String contentType ) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream( );
        out.write( (HttpConstants.HEADER_CONTENT_TYPE + ": " + contentType).getBytes( MimeUtil.ENCODING ) );
        out.write( MimeHeader.CRLF );
        return out.toByteArray();
    }
}
