package com.l7tech.util;

import static com.l7tech.util.ArrayUtils.*;
import static com.l7tech.util.CollectionUtils.list;
import static com.l7tech.util.CollectionUtils.toList;
import org.junit.*;
import static org.junit.Assert.*;

import java.util.Collections;

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
    byte[] iploc = new byte[] { (byte)127, (byte)0, (byte)0, (byte)1 };
    byte[] ip1 = new byte[] { (byte)192, (byte)168, (byte)1, (byte)177 };
    byte[] ip2 = new byte[] { (byte)192, (byte)168, (byte)1, (byte)238 };
    byte[] ip3a = new byte[] { (byte)-64, (byte)-88, (byte)-30, (byte)1 };
    byte[] ip3b = new byte[] { (byte)192, (byte)168, (byte)226, (byte)1 };
    byte[] ip_class_a_1 = new byte[] { (byte)10, (byte)99, (byte)247, (byte)131 };
    byte[] ip_class_a_2 = new byte[] { (byte)10, (byte)99, (byte)247, (byte)120 };

    @Test
    public void testCompareArraysNull() {
        assertEquals(0, compareArrays(null, null));
        assertEquals(-1, compareArrays(null, new byte[0]));
        assertEquals(1, compareArrays(new byte[0], null));
        assertEquals(0, compareArrays(ip3a, ip3b));
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
        assertNegative(ip1, ip2);

        // For signed comparison, loopback addresses sort above all other addresses
        assertNegative(ip1, iploc);
        assertNegative(ip2, iploc);
        assertNegative(ip3a, iploc);
        assertNegative(ip_class_a_1, iploc);
        assertNegative(ip_class_a_2, iploc);
    }

    @Test
    public void testUnsignedCompareArraysNull() {
        assertEquals(0, compareArraysUnsigned(null, null));
        assertEquals(-1, compareArraysUnsigned(null, new byte[0]));
        assertEquals(1, compareArraysUnsigned(new byte[0], null));
        assertEquals(0, compareArraysUnsigned(ip3a, ip3b));
    }

    @Test
    public void testUnsignedCompareArraysReferenceEqual() {
        assertEquals(0, compareArraysUnsigned(empty, empty));
        assertEquals(0, compareArraysUnsigned(lazydog, lazydog));
    }

    @Test
    public void testUnsignedCompareArraysSameLengthSameContents() {
        assertSameUnsigned(empty);
        assertSameUnsigned(single0);
        assertSameUnsigned(singleneg);
        assertSameUnsigned(singlepos);
        assertSameUnsigned(double00);
        assertSameUnsigned(double01);
        assertSameUnsigned(lazydog);
        assertSameUnsigned(lazygod);
    }

    @Test
    public void testUnsignedCompareArraysEmpty() {
        assertNegativeUnsigned(empty, single0);
        assertNegativeUnsigned(empty, lazydog);
        assertNegativeUnsigned(empty, singleneg);
    }

    @Test
    public void testUnsignedCompareArraysDiffert() {
        assertNegativeUnsigned(single0, single1);
        assertNegativeUnsigned(single0, double00);
        assertNegativeUnsigned(singlepos, singleneg);  // reverse of signed test
        assertNegativeUnsigned(lazydog, lazygod);
        assertNegativeUnsigned(double00, double01);
        assertNegativeUnsigned(double01, singlepos);
        assertNegativeUnsigned(ip1, ip2);

        // For unsigned comparison, loopback addresses sort above Class A addresses and below other addresses
        assertNegativeUnsigned(iploc, ip1);
        assertNegativeUnsigned(iploc, ip2);
        assertNegativeUnsigned(iploc, ip3a);
        assertNegativeUnsigned(ip_class_a_1, iploc);
        assertNegativeUnsigned(ip_class_a_2, iploc);
    }

    static void assertSame(byte[] arr) {
        assertEquals(0, compareArrays(arr, ArrayUtils.copy(arr)));
    }

    static void assertNegative(byte[] left, byte[] right) {
        assertTrue(compareArrays(left, right) < 0);
        assertTrue(compareArrays(right, left) > 0);
    }

    static void assertSameUnsigned(byte[] arr) {
        assertEquals(0, compareArraysUnsigned(arr, ArrayUtils.copy(arr)));
    }

    static void assertNegativeUnsigned(byte[] left, byte[] right) {
        assertTrue(compareArraysUnsigned(left, right) < 0);
        assertTrue(compareArraysUnsigned(right, left) > 0);
    }

    @Test
    public void testBox() {
        assertArrayEquals( "Boxed longs", new Long[]{ 1L, 2L, 3L }, box( new long[]{ 1L, 2L, 3L } ) );
        assertArrayEquals( "Unbox longs", new long[]{ 1L, 2L, 3L }, unbox( new Long[]{ 1L, 2L, 3L } ) );
        assertArrayEquals( "Boxed chars", new Character[]{ 'a' }, box( new char[]{ 'a' } ) );
        assertArrayEquals( "Unbox chars", new char[]{ 'a' }, unbox( new Character[]{ 'a' } ) );
    }

    @Test
    public void testZipI() {
        final Iterable<Pair<String,String>> zipNull = zipI( (String[]) null, (String[]) null );
        assertEquals( "zipNull", Collections.<Pair<String,String>>emptyList(), toList( zipNull ) );

        final Iterable<Pair<String,String>> zipLeftNull = zipI( (String[])null, new String[]{ "a" } );
        assertEquals( "zipLeftNull", list( new Pair<String, String>( null, "a" ) ), toList( zipLeftNull ) );

        final Iterable<Pair<String,String>> zipRightNull = zipI( new String[]{ "a" }, (String[]) null );
        assertEquals( "zipRightNull", list( new Pair<String, String>( "a", null ) ), toList( zipRightNull ) );

        final Iterable<Pair<String,String>> zip1 = zipI( new String[]{ "1" }, new String[]{ "a" } );
        assertEquals( "zip1", list( new Pair<String, String>( "1", "a" ) ), toList( zip1 ) );

        final Iterable<Pair<String,String>> zip1_2 = zipI( new String[]{ "1" }, new String[]{ "a", "b" } );
        assertEquals( "zip1_2", list(
                new Pair<String, String>( "1", "a" ),
                new Pair<String, String>( null, "b" )
        ), toList( zip1_2 ) );

        final Iterable<Pair<String,String>> zip5_3 = zipI( new String[]{ "1" , "2", "3", "4", "5" }, new String[]{"a", "b", "c"} );
        assertEquals( "zip5_3", list(
                new Pair<String, String>( "1", "a" ),
                new Pair<String, String>( "2", "b" ),
                new Pair<String, String>( "3", "c" ),
                new Pair<String, String>( "4", null ),
                new Pair<String, String>( "5", null )
        ), toList( zip5_3 ) );
    }
}
