package com.l7tech.util;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for {@link RandomUtil}.
 */
public class RandomUtilTest {
    private static final boolean PRINT_HISTOGRAMS = SyspropUtil.getBoolean( "com.l7tech.test.printHistograms", false );
    private static final int MAX_HISTOGRAM_LINES = SyspropUtil.getInteger( "com.l7tech.test.maxHistogramLines", 300 );
    private static final int HISTOGRAM_AVG_COLS = SyspropUtil.getInteger( "com.l7tech.test.histogramAvgColumns", 70 );


    @Test
    public void testNextChars() {
        final char FIRST_CHAR = '!';
        final char LAST_CHAR = '~';

        // Print a nice 32 character password string
        System.out.println( "A random ASCII password: " + new String( generateRandomCharacters( 64, FIRST_CHAR, LAST_CHAR ) ) );
        testDistribution( 65536, FIRST_CHAR, LAST_CHAR );
    }

    @Test
    public void testNextChars_coinFlips() {
        testDistribution( 1024, '0', '1' );
        testDistribution( 1024 * 4, '0', '1' );
        testDistribution( 1024 * 16, '0', '1' );
        testDistribution( 1024 * 64, '0', '1' );
        testDistribution( 1024 * 128, '0', '1' );
    }

    @Test
    public void testNextChars_digits() {
        testDistribution( 1024 * 2, '0', '9' );
    }

    @Test
    public void testNextChars_alpha() {
        testDistribution( 1024 * 2, 'A', 'Z' );
    }

    @Test
    public void testNextChars_UCS2() {
        testDistribution( 65536 * 16, Character.MIN_VALUE, (char)( Character.MIN_SURROGATE - 1 ) );
    }

    @Test( expected = IllegalArgumentException.class )
    public void testNextChars_badRange() {
        char[] chars = new char[0];
        RandomUtil.nextChars( chars, 'A', 'A' );
    }

    @Test( expected = IllegalArgumentException.class )
    public void testNextChars_badRange2() {
        char[] chars = new char[0];
        RandomUtil.nextChars( chars, 'B', 'A' );
    }

    @Test
    public void testNextChars_emptyOutput() {
        char[] chars = new char[0];
        RandomUtil.nextChars( chars, 'A', 'Z' );
    }

    @Test
    public void testNextChars_singleChar() {
        char[] chars = new char[1];
        RandomUtil.nextChars( chars, 'A', 'Z' );
        assertTrue( chars[0] >= 'A' && chars[0] <= 'Z' );
    }

    @Test
    @Ignore( "developer test - for tuning computeAcceptableMinimumCount() so it doesn't produce too many spurious failures" )
    public void testVarianceThreshold() {
        // Current values:
        //      1/100000 failures  for 1024,'0','9'
        //    260/100000 failures  for  256,'0','9'
        //   3600/100000 failures  for  128,'0','9'
        int numFails = 0;
        int total = 100000;

        int count = 1024;
        char lo = '0';
        char hi = '9';
        for ( int i = 0; i < total; ++i ) {
            char[] chars = generateRandomCharacters( count, lo, hi );
            int[] hist = histogram( chars, hi );
            try {
                assertHistogramIsFlat( hist, count, lo, hi );
            } catch ( AssertionError e ) {
                numFails++;
            }
        }

        System.out.println( "FAILURES: " + numFails + "/" + total );
    }

    private static void testDistribution( int count, char lo, char hi ) {
        char[] chars = generateRandomCharacters( count, lo, hi );
        int[] hist = histogram( chars, hi );
        if ( PRINT_HISTOGRAMS )
            printHistogram( hist, count, lo, hi );
        assertHistogramIsFlat( hist, count, lo, hi );
    }

    private static void assertHistogramIsFlat( int[] hist, int totalSamples, char lo, char hi ) {
        // What we would see if distro was totally flat
        int minimum = computeAcceptableMinimumCount( totalSamples, lo, hi );

        int totalCount = 0;
        for ( int i = 0; i < hist.length; i++ ) {
            int count = hist[i];
            totalCount += count;
            if ( i >= lo && i <= hi ) {
                assertTrue( "Saw too few of character " + i + ": expected to see at least " + minimum + ", only saw " + count,
                        count >= minimum );
            }
        }

        assertEquals( "Histogram did exactly the reported number of samples", totalSamples, totalCount );
    }

    private static int computeFairDivision( int totalSamples, char lo, char hi ) {
        return totalSamples / ( hi - lo + 1 );
    }

    private static int computeAcceptableMinimumCount( int totalSamples, char lo, char hi ) {
        // TODO: find a proper statistical test that is less than (say) 1 in a 100,000 likely to occur by chance
        //       if the distribution is truly flat.  For now I will use this hacky thing that seems to work ok
        int fair = computeFairDivision( totalSamples, lo, hi );
        int acceptableVariation =  (int)( Math.cbrt( fair ) * fair / ( Math.sqrt( fair ) ) );
        int min = fair - acceptableVariation;

        // Hack off edges of my curve that start to produce nonsensical results
        if ( min > ( fair / 2 ) && min > 1 )
            min = 1;
        if ( fair < 40 )
            min = 2;
        if ( fair < 20 )
            min = 1;
        if ( fair < 10 )
            min = 0;

        return min;
    }

    private static void printHistogram( int[] hist, int totalSamples, char lo, char hi ) {
        int even = computeFairDivision( totalSamples, lo, hi );
        int minimum = computeAcceptableMinimumCount( totalSamples, lo, hi );

        System.out.println( "\nHistogram:" );
        System.out.println( " Even=" + even + "  Min=" + minimum );
        for ( int j = lo; j <= hi; j++ ) {
            int count = hist[j];
            String bar = repeat( '*', ( count * HISTOGRAM_AVG_COLS ) / even );
            System.out.println( j + "\t" + (char)j + "\t" + count + "\t" + bar );
            if ( j >= MAX_HISTOGRAM_LINES ) {
                System.out.println( "[Truncated]" );
                break;
            }
        }
    }

    private static String repeat( char c, int count ) {
        char[] chars = new char[count];
        Arrays.fill( chars, c );
        return new String( chars );
    }

    private static int[] histogram( char[] chars, char hi ) {
        int[] hist = new int[ hi + 1 ];
        for ( char c : chars ) {
            hist[c]++;
        }
        return hist;
    }

    private static char[] generateRandomCharacters( int count, char lo, char hi ) {
        char[] ret = new char[ count ];
        RandomUtil.nextChars( ret, lo, hi );
        return ret;
    }
}
