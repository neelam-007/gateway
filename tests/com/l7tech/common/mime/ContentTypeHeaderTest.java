package com.l7tech.common.mime;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests for ContentTypeHeader
 *
 * @author Steve Jones
 */
public class ContentTypeHeaderTest extends TestCase {

    public ContentTypeHeaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ContentTypeHeaderTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * Test to ensure that trailing ";" is allowed (bug 3610) 
     */
    public void testParseContentTypeHeader() throws Exception {
        ContentTypeHeader cth = ContentTypeHeader.parseValue("text/xml; charset=UTF-8;");

        assertEquals("Charset", "UTF-8", cth.getEncoding());
        assertEquals("Type", "text", cth.getType());
        assertEquals("Subtype", "xml", cth.getSubtype());
    }

}
