package com.l7tech.server.ems.util;

import com.l7tech.util.Charsets;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 *
 */
public class PgpUtilTest {

    @Test
    public void testRoundTrip() throws Exception {
        doRoundTrip( false, false );
        doRoundTrip( false, true );
        doRoundTrip( true, false );
        doRoundTrip( true, true );
    }

    @Test(expected = PgpUtil.PgpException.class)
    public void testBadEncryptedData() throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream( );
        PgpUtil.decrypt(
                new ByteArrayInputStream( "plain text data".getBytes( Charsets.UTF8 ) ),
                out,
                "password".toCharArray()
                );
    }

    private void doRoundTrip( final boolean asciiArmoured, final boolean integrityProtected ) throws IOException, PgpUtil.PgpException {
        final String dataStr = "Test text, test text, test text, test text, test text";
        final byte[] data = dataStr.getBytes( Charsets.UTF8 );

        final ByteArrayOutputStream out = new ByteArrayOutputStream( );
        PgpUtil.encrypt(
                new ByteArrayInputStream( data ),
                out,
                "file.txt",
                123000, // milliseconds not preserved
                "password".toCharArray(),
                asciiArmoured,
                integrityProtected
        );

        final ByteArrayOutputStream out2 = new ByteArrayOutputStream( );
        final PgpUtil.DecryptionMetadata meta = PgpUtil.decrypt(
                new ByteArrayInputStream( out.toByteArray() ),
                out2,
                "password".toCharArray()
                );

        assertArrayEquals("clear data", data, out2.toByteArray());
        assertEquals( "Integrity protected", integrityProtected, meta.isIntegrityChecked() );
        assertEquals( "Armoured", asciiArmoured, meta.isAsciiArmoured() );
        assertEquals( "Filename", "file.txt", meta.getFilename() );
        assertEquals( "File modified", 123000, meta.getFileModified() );
    }

}
