package com.l7tech.util;

import org.junit.Test;

import java.text.ParseException;

import static org.junit.Assert.*;

/**
 *
 */
public class FullQNameTest {
    @Test
    public void testToString() throws Exception {
        assertEquals("foo", new FullQName(null, null, "foo").toString());
        assertEquals("pfx:blah", new FullQName(null, "pfx", "blah").toString());
        assertEquals("{urn:blat}pfx:blah", new FullQName("urn:blat", "pfx", "blah").toString());
        assertEquals("pfx:blah", new FullQName("", "pfx", "blah").toString());
        assertEquals("pfx:blah", new FullQName(null, "pfx", "blah").toString());
        assertEquals("blah", new FullQName(null, "", "blah").toString());
        assertEquals("blah", new FullQName(null, null, "blah").toString());
    }

    @Test
    public void testEquals() throws Exception {
        assertTrue(new FullQName(null, null, null).equals(new FullQName()));
        assertTrue(new FullQName(null, null, "a").equals(new FullQName(null, null, "a")));
        assertTrue(new FullQName(null, "b", null).equals(new FullQName(null, "b", null)));
        assertTrue(new FullQName("u", null, null).equals(new FullQName("u", null, null)));
        assertFalse(new FullQName(null, "", null).equals(new FullQName(null, null, null)));
        assertFalse(new FullQName(null, null, "a").equals(new FullQName(null, "b", "a")));
        assertFalse(new FullQName(null, "b", null).equals(new FullQName(null, "c", null)));
        assertFalse(new FullQName("u", null, null).equals(new FullQName(null, null, null)));
    }

    @Test
    public void testHashCode() throws Exception {
        assertEquals(new FullQName("urn:foo", "pfx", "blah").hashCode(), new FullQName("{urn:foo}pfx:blah").hashCode());
    }

    @Test
    public void testValueOf() throws Exception {
        assertEquals(new FullQName(null, null, "foo"), FullQName.valueOf("foo"));
        assertEquals(new FullQName(null, "pfx", "foo"), FullQName.valueOf("pfx:foo"));
        assertEquals(new FullQName("urn:blah", "pfx", "foo"), FullQName.valueOf("{urn:blah}pfx:foo"));
        assertEquals(new FullQName("urn:blah", null, "foo"), FullQName.valueOf("{urn:blah}foo"));
        assertEquals(new FullQName(null, null, ":"), FullQName.valueOf(":"));
        assertEquals(new FullQName(null, null, "a:"), FullQName.valueOf("a:"));
        assertEquals(new FullQName(null, null, ":b"), FullQName.valueOf(":b"));
        assertEquals(new FullQName(null, "a", "b"), FullQName.valueOf("a:b"));
        assertEquals(new FullQName("", "a", "b"), FullQName.valueOf("{}a:b"));
        assertEquals(new FullQName("u", "a", "b"), FullQName.valueOf("{u}a:b"));
        assertEquals(new FullQName("u", null, ":b"), FullQName.valueOf("{u}:b"));
    }

    @Test(expected = NullPointerException.class)
    public void testInvalidNull() throws Exception {
        FullQName.valueOf(null);
    }

    @Test(expected = ParseException.class)
    public void testInvalidEmpty() throws Exception {
        FullQName.valueOf("");
    }
}
