package com.l7tech.gateway.common.custom;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.policy.assertion.ext.message.CustomContentHeader;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * Test CustomToContentTypeHeaderConverter
 *
 * @author tveninov
 */
public class CustomToContentTypeHeaderConverterTest {

    static private final String MULTIPART_SOURCE = "multipart/related; type=\"text/xml\"; " +
            "boundary=\"----=Part_-763936460.00306951464153826\"; start=\"-76394136.13454\"";

    @Before
    public void setUp() throws Exception {
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullSupport() {
        //noinspection ConstantConditions
        new CustomToContentTypeHeaderConverter(null);
        fail("This message should not have been displayed");

    }

    /**
     * Make sure all methods are called correctly.
     */
    @Test
    public void testProperties() throws Exception {
        final ContentTypeHeader header = ContentTypeHeader.parseValue(MULTIPART_SOURCE);
        final CustomContentHeader customHeader = new CustomToContentTypeHeaderConverter(header);

        assertEquals("Type", header.getType(), customHeader.getType());
        assertEquals("Subtype", header.getSubtype(), customHeader.getSubtype());
        assertEquals("FullValue", header.getFullValue(), customHeader.getFullValue());
        assertEquals("Encoding", header.getEncoding(), customHeader.getEncoding());
        assertEquals("MultipartBoundary", header.getMultipartBoundary(), customHeader.getMultipartBoundary());
        assertEquals("isApplication", header.isApplication(), customHeader.isApplication());
        assertEquals("isApplicationFormUrlEncoded", header.isApplicationFormUrlEncoded(), customHeader.isApplicationFormUrlEncoded());
        assertEquals("isHtml", header.isHtml(), customHeader.isHtml());
        assertEquals("isJson", header.isJson(), customHeader.isJson());
        assertEquals("isMultipart", header.isMultipart(), customHeader.isMultipart());
        assertEquals("isSoap12", header.isSoap12(), customHeader.isSoap12());
        assertEquals("isText", header.isText(), customHeader.isText());
        assertEquals("isXml", header.isXml(), customHeader.isXml());
        assertTrue("matches", customHeader.matches(header.getType(), header.getSubtype()));
    }
}
