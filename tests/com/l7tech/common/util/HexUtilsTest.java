/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;
import java.util.Arrays;
import java.security.MessageDigest;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 *
 * User: mike
 * Date: Sep 4, 2003
 * Time: 12:15:53 PM
 */
public class HexUtilsTest extends TestCase {
    private static Logger log = Logger.getLogger(HexUtilsTest.class.getName());

    public HexUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(HexUtilsTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testEncodeMd5Digest() throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.reset();
        md5.update("484736327827227".getBytes());
        md5.update("-1".getBytes());
        md5.update("TheseAreSomeCertificateBytes".getBytes());
        md5.update("alice:myrealm:secret".getBytes());
        String result = HexUtils.encodeMd5Digest(md5.digest());
        log.info("result = " + result);
        assertTrue(result != null);
        assertTrue(result.equals("de615f787075c54bd19ba64da4128553"));
    }

    public void testHexDump() {
        assertTrue(HexUtils.hexDump(new byte[] { (byte)0xAB, (byte)0xCD }).equals("abcd"));
        assertTrue(HexUtils.hexDump(new byte[] { (byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF }).equals("deadbeef"));
    }

    public void testUnhexDump() throws IOException {
        assertTrue(Arrays.equals( HexUtils.unHexDump( "abcd" ), new byte[] { (byte)0xAB, (byte)0xCD }));
        assertTrue(Arrays.equals( HexUtils.unHexDump( "deadbeef" ), new byte[] { (byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF } ));
        assertTrue( HexUtils.hexDump( HexUtils.unHexDump( "de615f787075c54bd19ba64da4128553" )).equals("de615f787075c54bd19ba64da4128553"));
    }

    public void testSlurpStream() throws Exception {
        String teststring = "alsdkfhasdfhasdflskdfalksdflakflaksflasdlaksdflaksflaskdslkqpweofihqpwoef";

        {   // Test raw read into large-enough block
            ByteArrayInputStream bais = new ByteArrayInputStream(teststring.getBytes());
            byte[] hold32k = new byte[32768];
            int gotLen = HexUtils.slurpStream(bais, hold32k);
            assertTrue(gotLen == teststring.length());
            assertTrue(new String(hold32k, 0, gotLen).equals(teststring));
        }

        {   // Test raw read into too-small block
            ByteArrayInputStream bais = new ByteArrayInputStream(teststring.getBytes());
            byte[] hold16 = new byte[16];
            int gotLen = HexUtils.slurpStream(bais, hold16);
            assertTrue(gotLen == 16);
            assertTrue(new String(hold16, 0, gotLen).equals(teststring.substring(0, 16)));
        }

        {
            // Test raw read into exactly-right block
            ByteArrayInputStream bais = new ByteArrayInputStream(teststring.getBytes());
            byte[] holdlen = new byte[teststring.length()];
            int gotLen = HexUtils.slurpStream(bais, holdlen);
            assertTrue(gotLen == teststring.length());
            assertTrue(new String(holdlen).equals(teststring));
        }

        {
            // Test raw read of empty stream
            ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
            byte[] hold32k = new byte[32768];
            int num = HexUtils.slurpStream(bais, hold32k);
            assertTrue(num == 0);
        }

        {   // Test size-copied read with large-enough cutoff size
            ByteArrayInputStream bais = new ByteArrayInputStream(teststring.getBytes());
            byte[] got = HexUtils.slurpStream(bais, 32768);
            assertTrue(new String(got).equals(teststring));
        }

        {   // Test size-copied read with too-small cutoff size  (truncated)
            ByteArrayInputStream bais = new ByteArrayInputStream(teststring.getBytes());
            byte[] got = HexUtils.slurpStream(bais, 10);
            assertTrue(new String(got).equals(teststring.substring(0, 10)));
        }

        {
            // Test size-copied read with exactly-right cutoff size
            ByteArrayInputStream bais = new ByteArrayInputStream(teststring.getBytes());
            byte[] got = HexUtils.slurpStream(bais, teststring.length());
            assertTrue(new String(got).equals(teststring));
        }

        {
            // Test size-copied read of empty stream
            ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
            byte[] got = HexUtils.slurpStream(bais, 32768);
            assertTrue(got != null);
            assertTrue(got.length == 0);
        }
    }
}
