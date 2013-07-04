package com.l7tech.gateway.common.custom;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.policy.assertion.ext.message.CustomContentType;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test ContentTypeHeaderToCustomConverter
 */
public class ContentTypeHeaderToCustomConverterTest {

    static private final String MULTIPART_SOURCE = "multipart/related; type=\"text/xml\"; " +
            "boundary=\"----=Part_-763936460.00306951464153826\"; start=\"-76394136.13454\"";

    @Before
    public void setUp() throws Exception {
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullSupport() {
        //noinspection ConstantConditions
        new ContentTypeHeaderToCustomConverter(null);
        fail("This message should not have been displayed");

    }

    /**
     * Make sure all methods are called correctly.
     */
    @Test
    public void testProperties() throws Exception {
        final ContentTypeHeader header = ContentTypeHeader.parseValue(MULTIPART_SOURCE);
        final CustomContentType customType = new ContentTypeHeaderToCustomConverter(header);

        assertEquals("Type", header.getType(), customType.getType());
        assertEquals("Subtype", header.getSubtype(), customType.getSubtype());
        assertEquals("FullValue", header.getFullValue(), customType.getFullValue());
        assertEquals("Encoding", header.getEncoding(), customType.getEncoding());
        assertEquals("MultipartBoundary", header.getMultipartBoundary(), customType.getMultipartBoundary());
        assertEquals("isApplication", header.isApplication(), customType.isApplication());
        assertEquals("isApplicationFormUrlEncoded", header.isApplicationFormUrlEncoded(), customType.isApplicationFormUrlEncoded());
        assertEquals("isHtml", header.isHtml(), customType.isHtml());
        assertEquals("isJson", header.isJson(), customType.isJson());
        assertEquals("isMultipart", header.isMultipart(), customType.isMultipart());
        assertEquals("isSoap12", header.isSoap12(), customType.isSoap12());
        assertEquals("isText", header.isText(), customType.isText());
        assertEquals("isXml", header.isXml(), customType.isXml());
        assertTrue("matches", customType.matches(header.getType(), header.getSubtype()));
    }
}
