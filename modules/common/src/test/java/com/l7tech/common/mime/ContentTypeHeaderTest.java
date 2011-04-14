package com.l7tech.common.mime;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.test.BugNumber;
import static org.junit.Assert.*;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

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
        final ContentTypeHeader contentTypeHeader1 = ContentTypeHeader.parseValue(contentType, true);
        final ContentTypeHeader contentTypeHeader2 = ContentTypeHeader.parseValue(contentType, false);
        assertFalse( "Format different", contentTypeHeader1.getFullValue().equals( contentTypeHeader2.getFullValue()) );
        assertEquals( "Format preserved", contentType, contentTypeHeader1.getFullValue());
        assertArrayEquals( "Format preserved serialized", serialize( contentType ), contentTypeHeader1.getSerializedBytes() );
    }

    private void ensureFormattingPreserved( final String description, final String contentType ) throws Exception {
        final ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue(contentType);
        assertEquals( description + " full value", contentType, contentTypeHeader.getFullValue());
        assertArrayEquals( description + " serialized", serialize( contentType ), contentTypeHeader.getSerializedBytes() );
    }

    private byte[] serialize( final String contentType ) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream( );
        out.write( (HttpConstants.HEADER_CONTENT_TYPE + ": " + contentType).getBytes( ContentTypeHeader.ENCODING ) );
        out.write( MimeHeader.CRLF );
        return out.toByteArray();
    }
}
