package com.l7tech.common.io;

import com.l7tech.util.ArrayUtils;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import org.junit.Test;
import static org.junit.Assert.*;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Test for {@link HexUtils} facilities.
 */
public class HexUtilsTest {
    private static Logger log = Logger.getLogger(HexUtilsTest.class.getName());

    @Test
    public void testEncodeMd5Digest() throws Exception {
        byte[][] toDigest = {
            "484736327827227".getBytes(),
            "-1".getBytes(),
            "TheseAreSomeCertificateBytes".getBytes(),
            "alice:myrealm:secret".getBytes()
        };
        String result = HexUtils.encodeMd5Digest(HexUtils.getMd5Digest(toDigest));
        log.info("result = " + result);
        assertTrue(result != null);
        assertTrue(result.equals("de615f787075c54bd19ba64da4128553"));
    }

    @Test
    public void testHexDump() {
        assertTrue(HexUtils.hexDump(new byte[] { (byte)0xAB, (byte)0xCD }).equals("abcd"));
        assertTrue(HexUtils.hexDump(new byte[] { (byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF }).equals("deadbeef"));
    }

    @Test
    public void testUnhexDump() throws IOException {
        assertTrue(Arrays.equals( HexUtils.unHexDump( "abcd" ), new byte[] { (byte)0xAB, (byte)0xCD }));
        assertTrue(Arrays.equals( HexUtils.unHexDump( "deadbeef" ), new byte[] { (byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF } ));
        assertTrue( HexUtils.hexDump( HexUtils.unHexDump( "de615f787075c54bd19ba64da4128553" )).equals("de615f787075c54bd19ba64da4128553"));
    }
    
    @Test
    public void testBase64RoundTrip() throws Exception {
        byte[] data = new byte[4096];
        int index = 0;
        for ( int c=0; c<16; c++ ) {
            for ( int b=Byte.MIN_VALUE; b<=Byte.MAX_VALUE; b++) {
                data[index++] = (byte) b;
            }
        }
        String certB64 = HexUtils.encodeBase64(data, true);
        assertTrue( "chunking disabled, so no line breaks should be present", !certB64.contains( "\015" ) );
        assertTrue( "chunking disabled, so no line breaks should be present", !certB64.contains( "\012" ) );
        System.out.println(certB64);
        byte[] decoded = HexUtils.decodeBase64(certB64);
        assertTrue(Arrays.equals( decoded, data ));
    }

    @Test
    public void testSlurpStream() throws Exception {
        String teststring = "alsdkfhasdfhasdflskdfalksdflakflaksflasdlaksdflaksflaskdslkqpweofihqpwoef";

        {   // Test raw read into large-enough block
            ByteArrayInputStream bais = new ByteArrayInputStream(teststring.getBytes());
            byte[] hold32k = new byte[32768];
            int gotLen = IOUtils.slurpStream(bais, hold32k);
            assertTrue(gotLen == teststring.length());
            assertTrue(new String(hold32k, 0, gotLen).equals(teststring));
        }

        {   // Test raw read into too-small block
            ByteArrayInputStream bais = new ByteArrayInputStream(teststring.getBytes());
            byte[] hold16 = new byte[16];
            int gotLen = IOUtils.slurpStream(bais, hold16);
            assertTrue(gotLen == 16);
            assertTrue(new String(hold16, 0, gotLen).equals(teststring.substring(0, 16)));
        }

        {
            // Test raw read into exactly-right block
            ByteArrayInputStream bais = new ByteArrayInputStream(teststring.getBytes());
            byte[] holdlen = new byte[teststring.length()];
            int gotLen = IOUtils.slurpStream(bais, holdlen);
            assertTrue(gotLen == teststring.length());
            assertTrue(new String(holdlen).equals(teststring));
        }

        {
            // Test raw read of empty stream
            ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
            byte[] hold32k = new byte[32768];
            int num = IOUtils.slurpStream(bais, hold32k);
            assertTrue(num == 0);
        }

        {   // Test size-copied read with large-enough cutoff size
            ByteArrayInputStream bais = new ByteArrayInputStream(teststring.getBytes());
            byte[] got = IOUtils.slurpStream(bais, 32768);
            assertTrue(new String(got).equals(teststring));
        }

        {   // Test size-copied read with too-small cutoff size  (throws)
            ByteArrayInputStream bais = new ByteArrayInputStream(teststring.getBytes());
            try {
                byte[] got = IOUtils.slurpStream(bais, 10);
                fail("expected exception not thrown");
            } catch (IOException e) {
                // Ok
            }
        }

        {
            // Test size-copied read with exactly-right cutoff size
            ByteArrayInputStream bais = new ByteArrayInputStream(teststring.getBytes());
            byte[] got = IOUtils.slurpStream(bais, teststring.length());
            assertTrue(new String(got).equals(teststring));
        }

        {
            // Test size-copied read of empty stream
            ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
            byte[] got = IOUtils.slurpStream(bais, 32768);
            assertTrue(got != null);
            assertTrue(got.length == 0);
        }
    }

    @Test
    public void testMatchSubarrayOrPrefix() throws Exception {
        byte[] nowisthetime = "Now is the time for all good men to get a latte at starbucks".getBytes();
        byte[] starbucksisgod = "starbucks is godly".getBytes();
        byte[] titimefor = "timtime for".getBytes();
        byte[] timefor = "time for".getBytes();
        byte[] timefoe = "time foe".getBytes();
        byte[] timeflies = "time flies like an array".getBytes();
        byte[] thetimefor = "the time for".getBytes();
        byte[] me = "me".getBytes();
        byte[] poomeemee = "poomeemee".getBytes();
        byte[] ettal = "ettal".getBytes();
        byte[] empty = new byte[0];
        byte[] huge = HexUtils.unHexDump(COMPLEX_SUPERSTRING);
        byte[] boundary = HexUtils.unHexDump(COMPLEX_SUBSTRING);

        // Test red herring followed immediately by real substring
        assertEquals(3, ArrayUtils.matchSubarrayOrPrefix(titimefor, 0, titimefor.length, timefor, 0));

        // Test huge match
        assertEquals(221, ArrayUtils.matchSubarrayOrPrefix(huge, 0, huge.length, boundary, 0));

        // Test simple failed match
        assertEquals(ArrayUtils.matchSubarrayOrPrefix(nowisthetime, 0, nowisthetime.length, ettal, 0), -1);
        assertFalse(ArrayUtils.compareArrays(nowisthetime, 11, ettal, 0, ettal.length));

        // Test simple matched substsring
        assertEquals(ArrayUtils.matchSubarrayOrPrefix(nowisthetime, 0, nowisthetime.length, timefor, 0), 11);
        assertTrue(ArrayUtils.compareArrays(nowisthetime, 11, timefor, 0, timefor.length));

        // Test missed by one byte at the end
        assertEquals(ArrayUtils.matchSubarrayOrPrefix(nowisthetime, 0, nowisthetime.length, timefoe, 0), -1);
        assertFalse(ArrayUtils.compareArrays(nowisthetime, 11, timefoe, 0, timefoe.length));

        // Test red herring search
        assertEquals(ArrayUtils.matchSubarrayOrPrefix(nowisthetime, 0, nowisthetime.length, timeflies, 0), -1);

        // Test leftmost match
        assertEquals(ArrayUtils.matchSubarrayOrPrefix(nowisthetime, 0, nowisthetime.length, me, 0), 13);

        // Test simple prefix match at end
        assertEquals(ArrayUtils.matchSubarrayOrPrefix(nowisthetime, 0, nowisthetime.length, starbucksisgod, 0), 51);

        // Test exact match of entire array
        assertEquals(ArrayUtils.matchSubarrayOrPrefix(timefor, 0, timefor.length, timefor, 0), 0);

        // Test exact match of array slice
        assertEquals(ArrayUtils.matchSubarrayOrPrefix(nowisthetime, 11, timefor.length, timefor, 0), 11);

        // Test exact match of slice using slice
        assertEquals(ArrayUtils.matchSubarrayOrPrefix(nowisthetime, 11, timefor.length, thetimefor, 4), 11);

        // Test suffix match of slice using slice
        assertEquals(ArrayUtils.matchSubarrayOrPrefix(nowisthetime, 7, 8, poomeemee, 3), 13);

        // Test empty search array
        try {
            ArrayUtils.matchSubarrayOrPrefix(nowisthetime, 7, 12, empty, 0);
            fail("Exception not thrown");
        } catch (IllegalArgumentException e) {
            // Ok
        }

        log.info("Leftmost boundary occurs at: " + new String(huge).indexOf(new String(boundary)));
        log.info("Boundary: " + HexUtils.hexDump(boundary));
        log.info("Stuff at 219: " + HexUtils.hexDump(huge, 219, boundary.length));

        // Test huge substringy match
        assertEquals(221, ArrayUtils.matchSubarrayOrPrefix(huge, 0, 230, boundary, 0));
    }

    @Test
    public void testHexAppend() {
        final byte[] data = "hexappend".getBytes( Charsets.UTF8 );

        final StringBuilder out = new StringBuilder();
        HexUtils.hexAppend( out, data, 0, data.length, true, (char) 0 );
        assertEquals( "HEX upper no seperator", "686578617070656E64", out.toString() );

        out.setLength( 0 );
        HexUtils.hexAppend( out, data, 0, data.length, false, (char) 0 );
        assertEquals( "HEX lower no seperator", "686578617070656e64", out.toString() );

        out.setLength( 0 );
        HexUtils.hexAppend( out, data, 0, data.length, true, '_' );
        assertEquals( "HEX upper _ seperator", "68_65_78_61_70_70_65_6E_64", out.toString() );
    }

    @Test
    public void testAsciiAppend() {
        final byte[] text8 = "asciiappend".getBytes( Charsets.UTF8 );
        final byte[] text16 = "\31\0\u20ACtext\u221E".getBytes( Charsets.UTF16 );

        final StringBuilder out = new StringBuilder();
        HexUtils.asciiAppend( out, text8, 0, text8.length );
        assertEquals( "Acscii all", "asciiappend", out.toString() );

        out.setLength( 0 );
        HexUtils.asciiAppend( out, text16, 0, text16.length );
        assertEquals( "HEX lower no seperator", "...... ..t.e.x.t\".", out.toString() );
    }

    private byte[] b( String s ) {
        try {
            return s.getBytes( "UTF-8" );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }

    @Test
    public void testBase64url() throws Exception {
        assertEquals( "", HexUtils.encodeBase64Url( b( "" ) ) );
        assertEquals( "YQ", HexUtils.encodeBase64Url( b( "a" ) ) );
        assertEquals( "YXNkZg", HexUtils.encodeBase64Url( b( "asdf" ) ) );
        assertEquals( "b2l1aHBvaXVoZWg5MjhoYXNiMzlyYg", HexUtils.encodeBase64Url( b( "oiuhpoiuheh928hasb39rb" ) ) );
        assertEquals( "YWl1Z2FsZWlyZ2hhZWlydWdoYWVybGl1Z2hhZWxyaXVnYWVyZ2l1aGFlcmlndWFoYWU",
                HexUtils.encodeBase64Url( b( "aiugaleirghaeirughaerliughaelriugaergiuhaeriguahae" ) ) );
    }

    @Test
    public void testUnbase64url() throws Exception {
        assertTrue( Arrays.equals( b( "" ), HexUtils.decodeBase64Url( "" ) ) );
        assertTrue( Arrays.equals( b( "a" ), HexUtils.decodeBase64Url( "YQ" ) ) );
        assertTrue( Arrays.equals( b( "asdf" ), HexUtils.decodeBase64Url( "YXNkZg" ) ) );
        assertTrue( Arrays.equals( b( "oiuhpoiuheh928hasb39rb" ), HexUtils.decodeBase64Url( "b2l1aHBvaXVoZWg5MjhoYXNiMzlyYg" ) ) );
        assertTrue( Arrays.equals( b( "aiugaleirghaeirughaerliughaelriugaergiuhaeriguahae" ),
                HexUtils.decodeBase64Url( "YWl1Z2FsZWlyZ2hhZWlydWdoYWVybGl1Z2hhZWxyaXVnYWVyZ2l1aGFlcmlndWFoYWU" ) ) );
    }

    @Test
    public void testBase64UrlRoundTrip() throws Exception {
        Random r = new Random( 483L );

        for ( int i = 0; i < 5000; ++i ) {
            int len = r.nextInt( 200 );
            byte[] b = new byte[ len ];
            r.nextBytes( b );
            byte[] out =
                    HexUtils.decodeBase64Url(
                            HexUtils.encodeBase64Url(
                                    HexUtils.decodeBase64Url(
                                            HexUtils.encodeBase64Url( b ) ) ) );
            assertEquals( "Length mismatch for " + HexUtils.encodeBase64( b ), b.length, out.length );
            assertTrue( Arrays.equals( b, out ) );
        }
    }


    public static final String COMPLEX_SUBSTRING = "0d0a2d2d2d2d3d5f7e344b2d596358445e75";
    public static final String COMPLEX_SUPERSTRING = "5468697320707265616d626c652073686f756c642062652069676e6" +
            "f726564206279206120537741207061727365723a0d0a1c6f1b9469e803b4eacb515772328c5111eccfd5f798ad0" +
            "5b66582915dd8d76713ab945e70d0fe2780e0c0e9467f0aaac0bde60693935d68943d971d51036b93a11c77d8a61" +
            "47f702d31369b6d4af92745876bdbce7a97cd3c795297c84d5014adc20a0da04aeef3685cbaa985c61a39ead983e" +
            "1f743f8535695155eac71d9940545560d0a546861742061626f757420777261707320697420757020666f7220707" +
            "265616d626c652e0d0a0d0a2d2d2d2d3d5f7e344b2d596358445e750d0a436f6e74656e742d4c656e6774683a203" +
            "530300d0a436f6e74656e742d547970653a206170706c69636174696f6e2f6f637465742d73747265616d0d0a582" +
            "d4c372d547261696c696e672d626c616e6b2d6c696e65733a20330d0a0d0a1e1adbcab222fa524c10e51a5901062" +
            "6d7accd6d1a36eda06e4c6da689ecdcf0f23fb4db767f760ece5e35301f82726338adefa6290d0a2d2d2d2d3d5f7" +
            "e344b2d596395103e00b34ef0b145c45620eba07b130c1b8ae86e8bc31bd056bb0ae6f871af461f54d09f8543300" +
            "fff79605d6c38e9fde9ad2e8af19af3a60d6d012002f5627a95af3e20ec0a4162fe086b985be76e3c21879d5af88" +
            "486a43288a8395dbc85873c6d013c446111eae19d6d0a12f85b2a6493aa952fb579b44ee434f8cf99c9e92f5fac3" +
            "20f0e8e4d3c7a1ac4985e4582e7863e232d94f968a183bed9c245e7635ce32a8c14dafa679981026dda3bfc25f60" +
            "a66ca0bdc21eb9db52f41c50f6f5b090a2e65def36f48f403445a2816e97e6dcceb42e4c49c4069090623091fcf4" +
            "e4c0cc9c33e64ef083d5858cfb6a1653c3ddab5c15c414b105f104fade542b1c09212b64dc5920432da460ae01c1" +
            "9571bed530037164b08b88ef08a8853531276194bfdae4bda1f4c2244df81d7258bc81668cf7e3d3fd9f538c75e9" +
            "a7021b56e7550f85c6b2d2074e27a7d1ac65899dde6a232b5fc4a1d9f55db40a600f8d2910fe0eb2604cbbc44dca" +
            "6d83fad324b6e9e8c33b906e88de00fa088c3e7ab2ba3a1e0b132b902c12a44bf2f6eb5bccc9a149889aa0f69224" +
            "a44878c63b13cd7b6bbb82d9972094c464171252feab027210d0a0d0a0d0a0d0a2d2d2d2d3d5f7e344b2d5963584" +
            "45e752d2d0d0a";

}
