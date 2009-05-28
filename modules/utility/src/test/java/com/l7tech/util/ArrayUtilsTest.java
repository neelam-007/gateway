package com.l7tech.util;

import org.junit.*;
import static org.junit.Assert.*;
import static com.l7tech.util.ArrayUtils.compareArrays;

/**
 *
 */
public class ArrayUtilsTest {
    byte[] empty = new byte[0];
    byte[] single0 = new byte[] { 0 };
    byte[] single1 = new byte[] { 1 };
    byte[] singleneg = new byte[] { -128 };
    byte[] singlepos = new byte[] { 127 };
    byte[] double00 = new byte[] { 0, 0 };
    byte[] double01 = new byte[] { 0, 1 };
    byte[] lazydog = "the quick brown fox jumps over the lazy dog".getBytes();
    byte[] lazygod = "the quick brown fox jumps over the lazy god".getBytes();

    @Test
    public void testCompareArraysNull() {
        assertEquals(0, compareArrays(null, null));
        assertEquals(-1, compareArrays(null, new byte[0]));
        assertEquals(1, compareArrays(new byte[0], null));
    }
    
    @Test
    public void testCompareArraysReferenceEqual() {
        assertEquals(0, compareArrays(empty, empty));
        assertEquals(0, compareArrays(lazydog, lazydog));
    }

    @Test
    public void testCompareArraysSameLengthSameContents() {
        assertSame(empty);
        assertSame(single0);
        assertSame(singleneg);
        assertSame(singlepos);
        assertSame(double00);
        assertSame(double01);
        assertSame(lazydog);
        assertSame(lazygod);
    }

    @Test
    public void testCompareArraysEmpty() {
        assertNegative(empty, single0);
        assertNegative(empty, lazydog);
        assertNegative(empty, singleneg);
    }

    @Test
    public void testCompareArraysDiffert() {
        assertNegative(single0, single1);
        assertNegative(single0, double00);
        assertNegative(singleneg, singlepos);
        assertNegative(lazydog, lazygod);
        assertNegative(double00, double01);
        assertNegative(double01, singlepos);
    }

    static void assertSame(byte[] arr) {
        assertEquals(0, compareArrays(arr, ArrayUtils.copy(arr)));
    }

    static void assertNegative(byte[] left, byte[] right) {
        assertTrue(compareArrays(left, right) < 0);
        assertTrue(compareArrays(right, left) > 0);
    }
}
