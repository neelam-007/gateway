package com.l7tech.util;

import static org.junit.Assert.*;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

/**
 *
 */
public class IOUtilsTest {

    /**
     * Test using output streams as input stream filters.
     */
    @Test
    public void testInputStreamFilter() throws Exception {
        byte[] data1 = new byte[0];
        byte[] read1 = IOUtils.slurpStream( IOUtils.toInputStream( data1, new Functions.Unary<OutputStream, OutputStream>(){
            @Override
            public OutputStream call( final OutputStream outputStream ) {
                return outputStream; // identity transform
            }
        } ) );

        assertArrayEquals( "No data", data1, read1 );

        byte[] data2 = new byte[]{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 };
        byte[] read2 = IOUtils.slurpStream( IOUtils.toInputStream( data2, new Functions.Unary<OutputStream, OutputStream>(){
            @Override
            public OutputStream call( final OutputStream outputStream ) {
                return outputStream; // identity transform
            }
        } ) );

        assertArrayEquals( "Short data", data2, read2 );

        byte[] data3 = new byte[1024];
        for ( int i=0; i<data3.length; i++ ) {
            data3[i] = (byte)(i & 0xFF);
        }
        byte[] read3 = IOUtils.slurpStream( IOUtils.toInputStream( data3, new Functions.Unary<OutputStream, OutputStream>(){
            @Override
            public OutputStream call( final OutputStream outputStream ) {
                return outputStream; // identity transform
            }
        } ) );

        assertArrayEquals( "Long data", data3, read3 );

        byte[] expect4 = new byte[3072];
        for ( int i=0; i<1024; i++ ) {
            expect4[(i*3)  ] = (byte)(i & 0xFF);
            expect4[(i*3)+1] = (byte)(i & 0xFF);
            expect4[(i*3)+2] = (byte)(i & 0xFF);
        }
        byte[] read4 = IOUtils.slurpStream( IOUtils.toInputStream( data3, new Functions.Unary<OutputStream, OutputStream>(){
            @Override
            public OutputStream call( final OutputStream outputStream ) {
                return new FilterOutputStream( outputStream ){
                    @Override
                    public void write( final int b ) throws IOException {
                        out.write( b ); // expand data x3
                        out.write( b );
                        out.write( b );
                    }
                }; // identity transform
            }
        } ) );

        assertArrayEquals( "Expanding data", expect4, read4 );
    }

    /**
     * Test that direct use of an encoder give the same result as converting to an input stream.
     */
    @Test
    public void testEncodingInputStream() throws Exception {
        // Create raw byte data
        final byte[] raw = new byte[12345];
        for ( int i=0; i<256; i++ ) {
            raw[i] = (byte)(i & 0xFF);
        }
        long seed = System.currentTimeMillis();
        System.out.println("Seeding with " + seed);
        Random random = new Random(seed);
        for ( int i=256; i<raw.length; i++ ) {
            raw[i] = (byte)(random.nextInt() & 0xFF);
        }

        // Encode on read via converted stream
        final byte[] read = IOUtils.slurpStream( IOUtils.toInputStream( raw, new Functions.Unary<OutputStream, OutputStream>(){
            @Override
            public OutputStream call( final OutputStream outputStream ) {
                return new Base64OutputStream( outputStream );
            }
        } ) );

        // Encode directly
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final OutputStream out = new Base64OutputStream( byteArrayOutputStream );
        out.write( raw );
        out.close();
        byte[] encoded = byteArrayOutputStream.toByteArray();

        //System.out.println( new String(encoded) );

        assertArrayEquals( "Long data", encoded, read );
    }
}
