package com.l7tech.common.mime;

import com.l7tech.test.BugNumber;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests for ContentTypeHeader
 *
 * @author Steve Jones
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
        Assert.assertTrue("Wrong type found", jsonHeader.isJson());

        String xmlType = "text/xml; charset=utf-8";
        final ContentTypeHeader xmlHeader = ContentTypeHeader.parseValue(xmlType);

        String madeUpType = "application/ijustmadeitup; charset=utf-8";
        final ContentTypeHeader madeUpHeader = ContentTypeHeader.parseValue(madeUpType);

        Assert.assertFalse("Type should not be textual as it is unknown", madeUpHeader.isTextualContentType());
        ContentTypeHeader.setConfigurableTextualContentTypes(madeUpHeader);
        
        Assert.assertTrue("Type should be found in list of other textual types", madeUpHeader.isTextualContentType());

        ContentTypeHeader.setConfigurableTextualContentTypes(jsonHeader, xmlHeader);
        Assert.assertFalse("Type should not be found in list of other textual types", madeUpHeader.isTextualContentType());
    }
}
