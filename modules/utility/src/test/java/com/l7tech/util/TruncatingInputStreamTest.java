package com.l7tech.util;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.ByteArrayInputStream;

/**
 * Unit tests for truncating input stream
 */
public class TruncatingInputStreamTest {

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConstruction() {
        new TruncatingInputStream( new ByteArrayInputStream( new byte[10] ), -1L );
    }

    @Test
    public void testNonTruncating() throws Exception {
        final ByteArrayInputStream in = new ByteArrayInputStream( new byte[1000] );
        final TruncatingInputStream tin = new TruncatingInputStream(in);
        final int read = tin.read( new byte[1000] );
        assertEquals( "Data read", 1000L, (long)read );
        assertEquals( "Position", 1000L, tin.getPosition() );

        final int read2 = tin.read( new byte[1000] );
        assertEquals( "Data read", -1L, (long)read2 );
        assertEquals( "Position", -1L, tin.getPosition() );
    }

    @Test
    public void testTruncating() throws Exception {
        final ByteArrayInputStream in = new ByteArrayInputStream( new byte[1000] );
        final TruncatingInputStream tin = new TruncatingInputStream(in, 100L);
        final int read = tin.read( new byte[1000] );
        assertEquals( "Data read", 100L, (long)read );
        assertEquals( "Position", 100L, tin.getPosition() );

        final int read2 = tin.read( new byte[1000] );
        assertEquals( "Data read", -1L, (long)read2 );
        assertEquals( "Position", 100L, tin.getPosition() );
    }

    @Test
    public void testTruncatingWithSmallRead() throws Exception {
        final ByteArrayInputStream in = new ByteArrayInputStream( new byte[1000] );
        final TruncatingInputStream tin = new TruncatingInputStream(in, 100L);

        for ( int i=0; i<100; i++ ) {
            int read = tin.read();
            assertEquals( "Data read", 0L, (long)read );
            assertEquals( "Position", (long)i+1L, tin.getPosition() );
        }

        final int read = tin.read();
        assertEquals( "Data read", -1L, (long)read );
        assertEquals( "Position", 100L, tin.getPosition() );
    }

    @Test
    public void testSkipAndAvailable() throws Exception {
        final ByteArrayInputStream in = new ByteArrayInputStream( new byte[1000] );
        final TruncatingInputStream tin = new TruncatingInputStream(in, 100L);
        assertEquals( "Available", 100L, (long)tin.available() );
        assertEquals( "Position", 0L, tin.getPosition() );

        final long skipped = tin.skip( 120L );
        assertEquals( "Skipped", 100L, skipped );
        assertEquals( "Available", 0L, (long)tin.available() );
        assertEquals( "Position", 100L, tin.getPosition() );
    }
}
