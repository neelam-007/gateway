package com.l7tech.gateway.common.transport;

import static org.junit.Assert.*;
import org.junit.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 */
public class InterfaceTagTest {
    @Test(expected=NullPointerException.class)
    public void testNullName() throws Exception {
        new InterfaceTag(null, Collections.<String>emptySet());
    }

    @Test(expected=NullPointerException.class)
    public void testNullPatterns() throws Exception {
        new InterfaceTag("foo", null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testEmptyName() throws Exception {
        new InterfaceTag("", Collections.<String>emptySet());
    }

    @Test
    public void testToStringSingle() throws Exception {
        assertEquals("foo()", new InterfaceTag("foo", pats()).toString());
        assertEquals("foo(127)", new InterfaceTag("foo", pats("127")).toString());
        assertEquals("foo(127.0.0.1,40.55.255/24,48.22.44)", new InterfaceTag("foo", pats("127.0.0.1", "40.55.255/24", "48.22.44")).toString());
    }

    @Test
    public void testToStringMulti() throws Exception {
        InterfaceTag a = new InterfaceTag("foo", pats());
        InterfaceTag b = new InterfaceTag("blat", pats("43.55/31", "10.20"));
        InterfaceTag c = new InterfaceTag("murble", pats("44.33", "127", "4/16"));

        assertEquals("blat(43.55/31,10.20)", InterfaceTag.toString(tags(b)));
        assertEquals("foo();murble(44.33,127,4/16)", InterfaceTag.toString(tags(a, c)));
        assertEquals("foo();blat(43.55/31,10.20);murble(44.33,127,4/16)", InterfaceTag.toString(tags(a, b, c)));
    }

    @Test
    public void testParseSingle() throws Exception {
        assertEquals(new InterfaceTag("foo", pats()), InterfaceTag.parseSingle("foo()"));
        assertEquals(new InterfaceTag("murble", pats("23", "55/31", "83.33.44.11/32")), InterfaceTag.parseSingle("murble(23,55/31,83.33.44.11/32)"));
    }

    @Test
    public void testParseMulti() throws Exception {
        InterfaceTag a = new InterfaceTag("foo", pats());
        InterfaceTag b = new InterfaceTag("blat", pats("43.55/31", "10.20"));
        InterfaceTag c = new InterfaceTag("murble", pats("44.33", "127", "4/16"));

        assertEquals(tags(a), InterfaceTag.parseMultiple(InterfaceTag.toString(tags(a))));
        assertEquals(tags(a, c), InterfaceTag.parseMultiple(InterfaceTag.toString(tags(a, c))));
        assertEquals(tags(a, b, c), InterfaceTag.parseMultiple(InterfaceTag.toString(tags(a, b, c))));
    }

    private Set<String> pats(String... in) {
        return new LinkedHashSet<String>(Arrays.asList(in));
    }

    private Set<InterfaceTag> tags(InterfaceTag... in) {
        return new LinkedHashSet<InterfaceTag>(Arrays.asList(in));
    }
}
