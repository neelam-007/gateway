package com.l7tech.common.mime;

import com.l7tech.test.BugNumber;
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
}
