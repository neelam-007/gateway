package com.l7tech.common.io;

import com.l7tech.util.Charsets;
import static org.junit.Assert.*;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.util.logging.Logger;

/**
 * Unit tests for HexLogger and associated streams
 */
public class HexLoggerTest {

    @Test
    public void testBasicLogging() {
        final String[] outputHolder = new String[1];
        final HexLogger logger = new HexLogger( Logger.getAnonymousLogger(), "prefix\n" ) {
            @Override
            protected void outputHex( final String hex ) {
                outputHolder[0] = hex;
            }
        };
        logger.trace( "This is the traced output".getBytes( Charsets.UTF8 ) );
        logger.close();

        assertNotNull( "No output", outputHolder[0] );
        assertEquals( "Traced output",
                "000000: 54 68 69 73 20 69 73 20 74 68 65 20 74 72 61 63  This is the trac\n" +
                "000010: 65 64 20 6F 75 74 70 75 74                       ed output",
                outputHolder[0] );
    }

    @Test
    public void testLargeData() {
        final int[] outputCount = new int[1];
        final String[] outputHolder = new String[1];
        final HexLogger logger = new HexLogger( Logger.getAnonymousLogger(), "prefix\n", 4096, false ) {
            @Override
            protected void outputHex( final String hex ) {
                outputCount[0]++;
                outputHolder[0] = hex;
            }
        };
        for (int i=0; i<5; i++) logger.trace( new byte[ 1024 ] );
        logger.trace( new byte[11*1024] );

        logger.trace( "This is the traced output".getBytes( Charsets.UTF8 ) );
        logger.close();

        assertNotNull( "No output", outputHolder[0] );
        assertEquals( "Output count", 5L, outputCount[0] );
        assertEquals( "Traced output",
                "004000: 54 68 69 73 20 69 73 20 74 68 65 20 74 72 61 63  This is the trac\n" +
                "004010: 65 64 20 6F 75 74 70 75 74                       ed output",
                outputHolder[0] );
    }

    @Test
    public void testHexOutputStream() throws IOException {
        final int[] outputCount = new int[1];
        final String[] outputHolder = new String[1];
        final HexLogger logger = new HexLogger( Logger.getAnonymousLogger(), "prefix\n", 4096, false ) {
            @Override
            protected void outputHex( final String hex ) {
                outputCount[0]++;
                outputHolder[0] = hex;
            }

            @Override
            public boolean traceEnabled() {
                return true;
            }
        };
        final HexLoggingOutputStream hos = new HexLoggingOutputStream( logger, new NullOutputStream() );

        for (int i=0; i<5; i++) hos.write( new byte[ 1024 ], 0, 1024 );
        hos.flush();
        hos.write( new byte[11 * 1024] );
        hos.flush();

        byte[] finalBytes = "This is the traced output".getBytes( Charsets.UTF8 );
        for ( final byte finalByte : finalBytes ) {
            hos.write( (int) finalByte );
            hos.flush();
        }
        hos.close();

        assertNotNull( "No output", outputHolder[0] );
        assertEquals( "Output count", 5L, outputCount[0] );
        assertEquals( "Traced output",
                "004000: 54 68 69 73 20 69 73 20 74 68 65 20 74 72 61 63  This is the trac\n" +
                "004010: 65 64 20 6F 75 74 70 75 74                       ed output",
                outputHolder[0] );
    }

    @Test
    public void testHexInputStream() throws IOException {
        final int[] outputCount = new int[1];
        final String[] outputHolder = new String[1];
        final HexLogger logger = new HexLogger( Logger.getAnonymousLogger(), "prefix\n", 4096, false ) {
            @Override
            protected void outputHex( final String hex ) {
                outputCount[0]++;
                outputHolder[0] = hex;
            }

            @Override
            public boolean traceEnabled() {
                return true;
            }
        };
        final byte[] finalBytes = "This is the traced output".getBytes( Charsets.UTF8 );
        final HexLoggingInputStream his = new HexLoggingInputStream( logger, new SequenceInputStream(
                new ByteArrayInputStream(new byte[1024*16]),
                new ByteArrayInputStream(finalBytes) ) );

        assertEquals("Mark supported", false, his.markSupported());
        his.mark( 0 ); // should be ignored
        assertEquals("Available 1", 1024L*16L, (long)his.available());
        for (int i=0; i<5; i++) {
            assertEquals( "Read small buffer", 1024L, (long) his.read( new byte[1024], 0, 1024 ) );
        }
        assertEquals( "Skip nothing", 0L, his.skip( 0L ) );
        assertEquals( "Read large buffer", 11L * 1024L, (long) his.read( new byte[11 * 1024] ) );

        for ( final byte finalByte : finalBytes ) {
            assertEquals( "Read final byte", (long)finalByte, (long)his.read() );
        }
        assertEquals("Available 2", (long)0, (long)his.available());
        his.close();

        assertNotNull( "No output", outputHolder[0] );
        assertEquals( "Output count", 5L, outputCount[0] );
        assertEquals( "Traced output",
                "004000: 54 68 69 73 20 69 73 20 74 68 65 20 74 72 61 63  This is the trac\n" +
                "004010: 65 64 20 6F 75 74 70 75 74                       ed output",
                outputHolder[0] );
    }

    @Test
    public void testFlushOnUse() {
        final StringBuilder output = new StringBuilder();
        final HexLogger logger = new HexLogger( Logger.getAnonymousLogger(), "prefix\n" ) {
            @Override
            protected void outputHex( final String hex ) {
                output.append( hex ).append( "\n" );
            }
        };
        logger.trace( "T".getBytes( Charsets.UTF8 ) );
        logger.trace( "h".getBytes( Charsets.UTF8 ) );
        logger.trace( "i".getBytes( Charsets.UTF8 ) );
        logger.trace( "s".getBytes( Charsets.UTF8 ) );
        logger.trace( " ".getBytes( Charsets.UTF8 ) );
        logger.trace( "is the traced".getBytes( Charsets.UTF8 ) );
        logger.trace( " output.".getBytes( Charsets.UTF8 ) );
        logger.trace( " Output of small odd length packets".getBytes( Charsets.UTF8 ) );
        logger.trace( " will cause output to break across many lines".getBytes( Charsets.UTF8 ) );
        logger.close();

        assertFalse( "No output", output.length()==0 );
        assertEquals( "Traced output",
                "000000: 54 68 69 73 20 69 73 20 74 68 65 20 74 72 61 63  This is the trac\n" +
                "000010: 65 64                                            ed\n" +
                "000010:       20 6F 75 74 70 75 74 2E 20 4F 75 74 70 75     output. Outpu\n" +
                "000020: 74 20 6F 66 20 73 6D 61 6C 6C 20 6F 64 64 20 6C  t of small odd l\n" +
                "000030: 65 6E 67 74 68 20 70 61 63 6B 65 74 73           ength packets\n" +
                "000030:                                        20 77 69                wi\n" +
                "000040: 6C 6C 20 63 61 75 73 65 20 6F 75 74 70 75 74 20  ll cause output \n" +
                "000050: 74 6F 20 62 72 65 61 6B 20 61 63 72 6F 73 73 20  to break across \n" +
                "000060: 6D 61 6E 79 20 6C 69 6E 65 73                    many lines\n",
                output.toString() );
    }
}
