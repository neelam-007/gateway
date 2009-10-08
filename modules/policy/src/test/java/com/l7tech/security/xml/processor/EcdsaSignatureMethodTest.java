package com.l7tech.security.xml.processor;

import static org.junit.Assert.*;
import org.junit.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

/**
 *
 */
public class EcdsaSignatureMethodTest {
    /**
     * Ensure that our encoding the (r,s) values in ASN.1 works the same as xss4j's encoder,
     * at least for values that are the same length as those xss4j's code supports (DSA, 40 byte raw sig value)
     */
    @Test
    public void testEncodeAsn1BehavesLikeXss4jDsa() {
        final int rslength = 40;
        doTestEncode(rslength);
    }

    /**
     * Ensure that our decoding of (r,s) values from ASN.1 works the same as xss4j's decoder,
     * at least for values that are the same length as those xss4j's code supports (DS, 40 byte raw sig value)
     */
    @Test
    public void testDecodeAsn1BehavesLikeXss4jDsa() {
        doTestDecode(40);
    }

    private void doTestEncode(int rslength) {
        Random random = new Random(1L);

        for (int i = 0; i < 512; ++i) {
            // Produce fake (r,s) values, sized to match DSA (40 bytes total)
            byte[] rs = new byte[rslength];
            random.nextBytes(rs);

            byte[] xasn = xss4j_dsa_only_rawEncodeRSinASN1(rs);
            byte[] asn = EcdsaSignatureMethod.encodeAsn1(rs);
            assertTrue("Iteration " + i + ": encoded values shall be identical", Arrays.equals(xasn, asn));
        }
    }

    private void doTestDecode(int rslength) {
        Random random = new Random(1L);

        for (int i = 0; i < 512; ++i) {
            // Produce fake (r,s) values, encode them with xss4j encoder, then test decoding them with both
            byte[] rs = new byte[rslength];
            random.nextBytes(rs);
            byte[] asn = xss4j_dsa_only_rawEncodeRSinASN1(rs);

            byte[] xrs = xss4j_dsa_only_rawDecodeRSfromASN1(asn);
            byte[] our_rs = EcdsaSignatureMethod.decodeAsn1(asn);
            assertTrue("Iteration " + i + ": decoded values shall be identical", Arrays.equals(xrs, our_rs));
            assertTrue("Iteration " + i + ": decoded values shall match original value", Arrays.equals(rs, our_rs));
        }
    }

    @Test(expected = NullPointerException.class)
    public void testDecodeNull() {
        EcdsaSignatureMethod.decodeAsn1(null);
    }

    @Test(expected = NullPointerException.class)
    public void testEncodeNull() {
        EcdsaSignatureMethod.encodeAsn1(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeEmpty() {
        EcdsaSignatureMethod.decodeAsn1(new byte[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncodeEmpty() {
        EcdsaSignatureMethod.encodeAsn1(new byte[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeOdd() {
        EcdsaSignatureMethod.decodeAsn1(new byte[41]);
    }

    @Test
    public void testEncodeOdd() {
        // This actually Just Works
        EcdsaSignatureMethod.encodeAsn1(new byte[41]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeWayTooLong() throws IOException {
        final byte[] blah = new byte[2048];
        new Random(1L).nextBytes(blah);
        final byte[] longasn = EcdsaSignatureMethod.encodeAsn1(blah);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(longasn);
        baos.write(longasn);
        baos.write(longasn);
        baos.write(longasn);
        baos.write(longasn);
        baos.write(longasn);
        baos.write(longasn);
        baos.write(longasn);
        EcdsaSignatureMethod.decodeAsn1(baos.toByteArray());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncodeWayTooLong() {
        EcdsaSignatureMethod.encodeAsn1(new byte[8192]);
    }

    // The asn.1 encoding procedure used by XSS4j
    static byte[] xss4j_dsa_only_rawEncodeRSinASN1(byte rs[]) {
        int s_index = rs.length / 2;

        int rlen = rs[0] >= 0 ? 20 : 21;
        int slen = rs[s_index] >= 0 ? 20 : 21;
        byte result[] = new byte[rlen + slen + 6];
        result[0] = 48;
        result[1] = (byte)(rlen + slen + 4);
        result[2] = 2;
        result[3] = (byte)rlen;
        if(rs[0] < 0) {
            result[4] = 0;
            System.arraycopy(rs, 0, result, 5, 20);
        } else {
            System.arraycopy(rs, 0, result, 4, 20);
        }
        result[4 + rlen] = 2;
        result[5 + rlen] = (byte)slen;
        if(rs[s_index] < 0) {
            result[6 + rlen] = 0;
            System.arraycopy(rs, s_index, result, 7 + rlen, rs.length - s_index);
        } else {
            System.arraycopy(rs, s_index, result, 6 + rlen, rs.length - s_index);
        }
        return result;
    }

    // The asn.1 decoding procedure used by XSS4J
    static byte[] xss4j_dsa_only_rawDecodeRSfromASN1(byte asn[]) {
        int rlength = asn[3];
        int slength = asn[4 + rlength + 1];
        byte result[] = new byte[40];

        if(rlength < 20) {
            for(int gap = 0; gap < 20 - rlength; gap++)
                result[gap] = 0;
            System.arraycopy(asn, 4, result, 20 - rlength, rlength);
        } else {
            System.arraycopy(asn, (4 + rlength) - 20, result, 0, 20);
        }
        if(slength < 20) {
            for(int gap = 0; gap < 20 - slength; gap++)
                result[20 + gap] = 0;
            System.arraycopy(asn, 4 + rlength + 2, result, 40 - slength, slength);
        } else {
            System.arraycopy(asn, (4 + rlength + 2 + slength) - 20, result, 20, 20);
        }
        return result;
    }
}
