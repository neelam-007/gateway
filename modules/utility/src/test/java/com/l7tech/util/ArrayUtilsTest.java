package com.l7tech.util;

import static com.l7tech.util.ArrayUtils.*;
import static com.l7tech.util.CollectionUtils.list;
import static com.l7tech.util.CollectionUtils.toList;

import com.l7tech.test.BenchmarkRunner;
import org.junit.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

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
        assertEquals( 0, compareArrays( empty, empty ) );
        assertEquals( 0, compareArrays( lazydog, lazydog ) );
    }

    @Test
    public void testCompareArraysSameLengthSameContents() {
        assertSame(empty);
        assertSame(single0);
        assertSame(singleneg);
        assertSame(singlepos);
        assertSame(double00);
        assertSame( double01 );
        assertSame( lazydog );
        assertSame( lazygod );
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
        assertNegative( ip2, iploc );
        assertNegative( ip3a, iploc );
        assertNegative( ip_class_a_1, iploc );
        assertNegative( ip_class_a_2, iploc );
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
        assertEquals( 0, compareArraysUnsigned( empty, empty ) );
        assertEquals( 0, compareArraysUnsigned( lazydog, lazydog ) );
    }

    @Test
    public void testUnsignedCompareArraysSameLengthSameContents() {
        assertSameUnsigned(empty);
        assertSameUnsigned(single0);
        assertSameUnsigned(singleneg);
        assertSameUnsigned(singlepos);
        assertSameUnsigned(double00);
        assertSameUnsigned( double01 );
        assertSameUnsigned( lazydog );
        assertSameUnsigned( lazygod );
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
        assertEquals( 0, compareArrays( arr, ArrayUtils.copy( arr ) ) );
    }

    static void assertNegative(byte[] left, byte[] right) {
        assertTrue(compareArrays(left, right) < 0);
        assertTrue( compareArrays( right, left ) > 0 );
    }

    static void assertSameUnsigned(byte[] arr) {
        assertEquals( 0, compareArraysUnsigned( arr, ArrayUtils.copy( arr ) ) );
    }

    static void assertNegativeUnsigned(byte[] left, byte[] right) {
        assertTrue( compareArraysUnsigned( left, right ) < 0 );
        assertTrue( compareArraysUnsigned( right, left ) > 0 );
    }

    @Test
    public void testBox() {
        assertArrayEquals( "Boxed longs", new Long[]{ 1L, 2L, 3L }, box( new long[]{ 1L, 2L, 3L } ) );
        assertArrayEquals( "Unbox longs", new long[] { 1L, 2L, 3L }, unbox( new Long[] { 1L, 2L, 3L } ) );
        assertArrayEquals( "Boxed chars", new Character[]{ 'a' }, box( new char[]{ 'a' } ) );
        assertArrayEquals( "Unbox chars", new char[] { 'a' }, unbox( new Character[] { 'a' } ) );
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

    @Test
    public void testCopy() {
        final byte[] byteArrayZero = new byte[0];
        final byte[] byteArrayZeroCopy = copy(byteArrayZero);
        assertArrayEquals( "Byte array (empty)", byteArrayZero, byteArrayZeroCopy );
        assertNotSame( "Byte array (empty) not same object", byteArrayZero, byteArrayZeroCopy );

        final byte[] byteArray = new byte[]{0,1,2,3,4,5,6,7,8,9};
        final byte[] byteArrayCopy = copy(byteArray);
        assertArrayEquals( "Byte array", byteArray, byteArrayCopy );
        assertNotSame( "Byte array not same object", byteArray, byteArrayCopy );

        final int[] intArray = new int[]{0,1,2,3,4,5,6,7,8,9};
        final int[] intArrayCopy = copy(intArray);
        assertArrayEquals( "Int array", intArray, intArrayCopy );
        assertNotSame( "Int array not same object", intArray, intArrayCopy );

        final char[] charArray = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        final char[] charArrayCopy = copy(charArray);
        assertArrayEquals( "Char array", charArray, charArrayCopy );
        assertNotSame( "Char array not same object", charArray, charArrayCopy );

        final boolean[] booleanArray = new boolean[]{true};
        final boolean[] booleanArrayCopy = copy( booleanArray );
        assertEquals( "Boolean array size", booleanArray.length, booleanArrayCopy.length );
        assertEquals( "Boolean array value 0", booleanArray[0], booleanArrayCopy[0] );
        assertNotSame( "Boolean array not same object", booleanArray, booleanArrayCopy );

        final String[] stringArray = new String[]{"a","b","c"};
        final String[] stringArrayCopy = copy( stringArray );
        assertArrayEquals( "String array", stringArray, stringArrayCopy );
        assertNotSame( "String array not same object", stringArray, stringArrayCopy );
    }

    @Test
    public void testConcat() {
        final String[] stringArrayOne = new String[]{"a"};
        final String[] stringArrayTwo = new String[]{"b","c"};
        final String[] stringArrayConcat = concat( stringArrayOne, stringArrayTwo );
        assertArrayEquals( "String array", new String[]{"a","b","c"}, stringArrayConcat );
        assertNotSame( "String array one not same object", stringArrayOne, stringArrayConcat );
        assertNotSame( "String array two not same object", stringArrayTwo, stringArrayConcat );

        final byte[] byteArrayOne = new byte[]{ 1 };
        final byte[] byteArrayTwo = new byte[]{ 2 };
        final byte[] byteArrayConcat = concat( byteArrayOne, byteArrayTwo );
        assertArrayEquals( "Byte array", new byte[]{1,2}, byteArrayConcat );
        assertNotSame( "Byte array one not same object", byteArrayOne, byteArrayConcat );
        assertNotSame( "Byte array two not same object", byteArrayTwo, byteArrayConcat );

        final byte[] emptyByteArray = new byte[]{};
        final byte[] emptyByteArrayConcat = concat( emptyByteArray, emptyByteArray );
        assertArrayEquals( "Byte array (empty)", emptyByteArray, emptyByteArrayConcat );
        assertNotSame( "Byte array (empty) not same object", emptyByteArray, emptyByteArrayConcat );
    }

    @Test
    public void testCompareArraysConstantTime() throws Exception {
        assertTrue( ArrayUtils.compareArraysConstantTime( lazydog, lazydog ) );
        assertTrue( ArrayUtils.compareArraysConstantTime( lazydog, Arrays.copyOf( lazydog, lazydog.length ) ) );
        assertFalse( ArrayUtils.compareArraysConstantTime( lazydog, lazygod ) );
        assertFalse( ArrayUtils.compareArraysConstantTime( double00, double01 ) );
        assertFalse( ArrayUtils.compareArraysConstantTime( double00, single0 ) );
    }

    @Test
    public void testCompareArraysConstantTime_SimulatedTimingAttack() throws Exception {
        // Build test runnables
        Runnable shortArrayEarlyDifference = buildComparisonTestRunnable( 1024, 15 );
        Runnable shortArrayLateDifference = buildComparisonTestRunnable( 1024, 910 );
        Runnable longArrayEarlyDifference = buildComparisonTestRunnable( 16384, 200 );
        Runnable longArrayLateDifference = buildComparisonTestRunnable( 16384, 15388 );

        // Now gather timings
        System.out.println( "Simulating timing attack..." );
        long timeSE = new BenchmarkRunner( shortArrayEarlyDifference, 5, 1, "short array, early difference" ).run();
        long timeSL = new BenchmarkRunner( shortArrayLateDifference, 5, 1, "short array, late difference" ).run();
        long timeLE = new BenchmarkRunner( longArrayEarlyDifference, 5, 1, "long array, early difference" ).run();
        long timeLL = new BenchmarkRunner( longArrayLateDifference, 5, 1, "long array, late difference" ).run();

        long diffS = Math.abs( timeSL - timeSE );
        double diffPercentS = (double) diffS / Math.min( timeSL, timeSE ) * 100.0d;
        long diffL = Math.abs( timeLL - timeLE );
        double diffPercentL = (double) diffL / Math.min( timeLL, timeLE ) * 100.0d;

        System.out.println( "time difference for short arrays (nanos): " + diffS + " (" + diffPercentS + "%)" );
        System.out.println( "time difference for long arrays (nanos): " + diffL + " (" + diffPercentL + "%)" );

        // These thresholds are arbitrary but serve to fairly robustly differentiate constant time
        // from short circuiting comparison.

        // On my workstation with short-circuiting compare:
        //    time difference for short arrays (nanos): 34004000 (2199.4825355756793%)
        //    time difference for long arrays (nanos): 572528000 (7099.8015873015875%)

        // On my workstation with constant-time compare:
        //    time difference for short arrays (nanos): 6080000 (7.745222929936306%)
        //    time difference for long arrays (nanos): 3327000 (0.27223876493446875%)

        assertTrue( "Expected below 400% difference for short arrays, was " + diffPercentS, diffPercentS < 400 );
        assertTrue( "Expected below 200% difference for long arrays, was " + diffPercentL, diffPercentL < 200 );
    }

    private Runnable buildComparisonTestRunnable( final int arraySize, final int diffPos ) {
        Random random = new Random( 38374 );

        // Generate two test arrays of the specified length.
        // The arrays will be the same up until diffPos, where they will begin to differ in contents.
        final byte[] a = new byte[ arraySize ];
        final byte[] b = new byte[ arraySize ];

        for ( int i = 0; i < a.length; i++ ) {
            byte bb = (byte) random.nextInt( 256 );
            a[i] = bb;
            b[i] = i < diffPos ? a[i] : (byte) random.nextInt( 256 );
        }

        assertTrue( "Test arrays should differ", 0 != ArrayUtils.compareArrays( a, b ) );
        assertTrue( "Constant time compare should fail as well", !ArrayUtils.compareArraysConstantTime( a, b ) );

        Runnable r = new Runnable() {
            @Override
            public void run() {
                int sum = 0;
                for ( int c = 0; c < 20000; ++c ) {
                    boolean result = ArrayUtils.compareArraysConstantTime( a, b );
                    sum += result ? 1 : 0;
                }
                sink += sum;
            }
        };

        // Do a burn-in run before we return
        r.run();

        return r;
    }

    public static volatile long sink = 0;

    @Test
    public void testConcatMulti() throws Exception {
        byte[] empty = ArrayUtils.concatMulti();
        assertEquals( 0, empty.length );

        byte[] identity = ArrayUtils.concatMulti( "blah blah blah".getBytes( Charsets.ASCII) );
        assertArrayEquals( "blah blah blah".getBytes( Charsets.ASCII ), identity );

        byte[] join2 = ArrayUtils.concatMulti( single0, double00 );
        assertArrayEquals( new byte[] { 0, 0, 0 }, join2 );

        byte[] join3 = ArrayUtils.concatMulti( double01, single0, double01 );
        assertArrayEquals( new byte[] { 0, 1, 0, 0, 1 }, join3 );

        byte[] join4 = ArrayUtils.concatMulti( single1, double00, double01, single0 );
        assertArrayEquals( new byte[] { 1, 0, 0, 0, 1, 0 }, join4 );
    }

    @Test
    public void testUnpack() throws Exception {
        byte[] foo_bar_baz = "foo_bar_baz".getBytes( Charsets.ASCII );

        byte[][] nothings = ArrayUtils.unpack( new byte[0], 0, 0, 0 );
        assertEquals( 3, nothings.length );
        assertEquals( 0, nothings[0].length );
        assertEquals( 0, nothings[1].length );
        assertEquals( 0, nothings[2].length );

        byte[][] empty = ArrayUtils.unpack( foo_bar_baz );
        assertEquals( 0, empty.length );

        byte[][] identity = ArrayUtils.unpack( foo_bar_baz, 11 );
        assertEquals( 1, identity.length );
        assertArrayEquals( foo_bar_baz, identity[0] );

        byte[][] leftovers = ArrayUtils.unpack( foo_bar_baz, 3 );
        assertEquals( 1, leftovers.length );
        assertArrayEquals( "foo".getBytes(), leftovers[0] );

        byte[][] split = ArrayUtils.unpack( foo_bar_baz, 4, 7 );
        assertEquals( 2, split.length );
        assertArrayEquals( "foo_".getBytes(), split[0] );
        assertArrayEquals( "bar_baz".getBytes(), split[1] );

        byte[][] complex = ArrayUtils.unpack( "A longer array with complex breakdown".getBytes(), 3, 0, 2, 30 );
        assertEquals( 4, complex.length );
        assertArrayEquals( "A l".getBytes(), complex[0] );
        assertArrayEquals( "".getBytes(), complex[1] );
        assertArrayEquals( "on".getBytes(), complex[2] );
        assertArrayEquals( "ger array with complex breakdo".getBytes(), complex[3] );
    }

    @Test( expected = ArrayIndexOutOfBoundsException.class )
    public void testUnpack_notEnough() throws Exception {
        byte[][] complex = ArrayUtils.unpack( "A longer array with complex breakdown".getBytes(), 3, 0, 2, 33 );
        assertEquals( 4, complex.length );
        assertArrayEquals( "A l".getBytes(), complex[0] );
        assertArrayEquals( "".getBytes(), complex[1] );
        assertArrayEquals( "on".getBytes(), complex[2] );
        assertArrayEquals( "ger array with complex breakdo".getBytes(), complex[3] );
    }

}
