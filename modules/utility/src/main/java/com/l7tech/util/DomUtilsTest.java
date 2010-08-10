package com.l7tech.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link DomUtils}.
 */
public class DomUtilsTest {
    private static final String[] VALID_NAMES = new String[]{
            "a",
            "test",
            "foo::sdl4-asdf-",
            "hi",
            "xmlns",
            "xmlns:foo",
            "xmlns:foo:bar:baz:blatch",
            "a_b",
            "a-b",
            "_foo",
            "::",
            ":",
            ":foo",
    };

    private static final String[] INVALID_NAMES = new String[]{
            "3232",
            "3",
            "-foo",
            "ab&out",
            "ab#out",
            "4asdf",
            "test%test",
            "---",
            "JDI#",
            "",
    };

    private static final String[] VALID_NCNAMES = new String[]{
            "asdf",
            "test",
            "asd3",
            "a-b",
    };

    private static final String[] INVALID_NCNAMES = new String[]{
            ":",
            "foo:bar",
            "asdflkj__%$@",
    };

    private static final String[] VALID_QNAMES = new String[]{
            "asdf",
            "a:foo",
            "foo",
            "xmlns:asdfasdf-asdf",
            "a:b",
    };

    private static final String[] INVALID_QNAMES = new String[]{
            "foo:bar:baz",
            ":",
            "::",
            "",
    };

    @Test(expected = NullPointerException.class)
    public void testNullName() {
        DomUtils.isValidXmlName(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullNCName() {
        DomUtils.isValidXmlNcName(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullQName() {
        DomUtils.isValidXmlQname(null);
    }


    @Test
    public void testValidNames() throws Exception {
        for (String s : VALID_NAMES) {
            assertTrue("Should have passed as Name: " + s, DomUtils.isValidXmlName(s));
        }
    }

    @Test
    public void testInvalidNames() throws Exception {
        for (String s : INVALID_NAMES) {
            assertFalse("Should have failed as Name: " + s, DomUtils.isValidXmlName(s));
        }
    }

    @Test
    public void testValidNCNames() throws Exception {
        for (String s : VALID_NCNAMES) {
            assertTrue("Should have passed as NCName: " + s, DomUtils.isValidXmlNcName(s));
        }
    }

    @Test
    public void testInvalidNCNames() throws Exception {
        for (String s : INVALID_NCNAMES) {
            assertFalse("Should have failed as NCName: " + s, DomUtils.isValidXmlNcName(s));
        }
    }

    @Test
    public void testValidQNames() throws Exception {
        for (String s : VALID_QNAMES) {
            assertTrue("Should have passed as QName: " + s, DomUtils.isValidXmlQname(s));
        }

        // All valid NCNames are automatically valid as QNames as well
        for (String s : VALID_NCNAMES) {
            assertTrue("Should have passed as QName: " + s, DomUtils.isValidXmlQname(s));
        }
    }

    @Test
    public void testInvalidQNames() throws Exception {
        for (String s : INVALID_QNAMES) {
            assertFalse("Should have failed as QName: " + s, DomUtils.isValidXmlQname(s));
        }

        // All invalid Names are automatically invalid as QNames as well
        for (String s : INVALID_NAMES) {
            assertFalse("Should have failed as QName: " + s, DomUtils.isValidXmlQname(s));
        }
    }
}
