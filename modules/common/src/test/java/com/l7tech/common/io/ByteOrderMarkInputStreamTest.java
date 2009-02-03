/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */
package com.l7tech.common.io;

import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Unit test for {@link ByteOrderMarkInputStream}.
  */
public class ByteOrderMarkInputStreamTest extends TestCase {
    @SuppressWarnings({"UNUSED_SYMBOL"})
    private static Logger log = Logger.getLogger(ByteOrderMarkInputStreamTest.class.getName());

    public ByteOrderMarkInputStreamTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ByteOrderMarkInputStreamTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private class Result {
        private final ByteOrderMarkInputStream bomis;
        private final byte[] slurpedBytes;

        public Result(ByteOrderMarkInputStream bomis, byte[] slurpedBytes) {
            this.bomis = bomis;
            this.slurpedBytes = slurpedBytes;
        }
    }

    private Result getResult(byte[] input, boolean slurp) throws IOException {
        ByteOrderMarkInputStream bomis = new ByteOrderMarkInputStream(new ByteArrayInputStream(input));
        byte[] slurped = slurp ? IOUtils.slurpStream(bomis) : null;
        return new Result(bomis, slurped);
    }

    private void assertSimpleNoBom(byte[] in) throws Exception {
        Result result = getResult(in, false);
        assertNull(result.bomis.getEncoding());
        assertFalse(result.bomis.isSrippingBom());

        result = getResult(in, true);
        assertNull(result.bomis.getEncoding());
        assertFalse(result.bomis.isSrippingBom());
        assertTrue(Arrays.equals(result.slurpedBytes, in));
    }

    public void testNoBom() throws Exception {
        assertSimpleNoBom(ASCII_DOC);
        assertSimpleNoBom(ASCII_C1);
        assertSimpleNoBom(ASCII_C2);
        assertSimpleNoBom(ASCII_C3);
        assertSimpleNoBom(ASCII_C4);
        assertSimpleNoBom(ASCII_C5);
    }

    private void assertUtf8Bom(byte[] in) throws Exception {
        Result result = getResult(in, false);
        assertEquals(ByteOrderMarkInputStream.UTF8, result.bomis.getEncoding());
        assertTrue(result.bomis.isSrippingBom());

        result = getResult(in, true);
        assertEquals(ByteOrderMarkInputStream.UTF8, result.bomis.getEncoding());
        assertTrue(result.bomis.isSrippingBom());

        // Make sure we slurped the exact bytes we fed in, minus the 3-byte UTF-8 BOM
        byte[] nobom = new byte[in.length - 3];
        System.arraycopy(in, 3, nobom, 0, nobom.length);
        assertTrue(Arrays.equals(result.slurpedBytes, nobom));
    }

    public void testUtf8Bom() throws Exception {
        assertUtf8Bom(UTF8M_DOC);
        assertUtf8Bom(UTF8M_C1);
        assertUtf8Bom(UTF8M_C2);
        assertUtf8Bom(UTF8M_C3);
        assertUtf8Bom(UTF8M_C4);
        assertUtf8Bom(UTF8M_C5);
    }

    private void assertUtf16Bom(byte[] in) throws Exception {
        Result result = getResult(in, false);
        assertEquals(ByteOrderMarkInputStream.UNICODE_BIG_UNMARKED,  result.bomis.getEncoding());
        assertTrue(result.bomis.isSrippingBom());

        result = getResult(in, true);
        assertEquals(ByteOrderMarkInputStream.UNICODE_BIG_UNMARKED,  result.bomis.getEncoding());
        assertTrue(result.bomis.isSrippingBom());

        // Make sure we slurped the exact bytes we fed in, minus the 2-byte UTF-16 BOM
        byte[] nobom = new byte[in.length - 2];
        System.arraycopy(in, 2, nobom, 0, nobom.length);
        assertTrue(Arrays.equals(result.slurpedBytes, nobom));
    }

    public void testUtf16Bom() throws Exception {
        assertUtf16Bom(UTF16M_DOC);
        assertUtf16Bom(UTF16M_C1);
        assertUtf16Bom(UTF16M_C2);
        assertUtf16Bom(UTF16M_C3);
        assertUtf16Bom(UTF16M_C4);
        assertUtf16Bom(UTF16M_C5);
    }

    private static byte[] getBytes(String str, String encoding) {
        try {
            return str.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /** @return bytes of string in encoding, prefixed with prefix (with ints converted to bytes) */
    private static byte[] getBytes(int[] ints, String str, String encoding) {
        byte[] base = getBytes(str, encoding);
        byte[] prefix = new byte[ints.length];
        for (int j = 0; j < ints.length; j++) {
            int i = ints[j];
            prefix[j] = (byte)(0xFF & i);
        }
        byte[] ret = new byte[prefix.length + base.length];
        System.arraycopy(prefix, 0, ret, 0, prefix.length);
        System.arraycopy(base, 0, ret, prefix.length, base.length);
        return ret;
    }

    private static String makeStringFromHex(String hexString, String encoding) {
        try {
            return new String(HexUtils.unHexDump(hexString), encoding);
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    private static final String c1 = "a";
    private static final String c2 = "ab";
    private static final String c3 = "abc";
    private static final String c4 = "abcd";
    private static final String c5 = "abcde";

    /* Unicode string constant that uses unicode */
    private static final String UTF8_STRING_HEX = "d8add8b3d98ad98620d8a8d98620d8b7d984d8a7"; // (arabic: "Hussein bin Talal" King Hussein of Jordan)\
    private static final String UNICODE_STRING = makeStringFromHex(UTF8_STRING_HEX, "UTF-8");

    /* Plain ASCII */
    private static final byte[] ASCII_DOC = getBytes("Normal ascii blah blah blah", "ASCII");
    private static final byte[] ASCII_C1 = getBytes(c1, "ASCII");
    private static final byte[] ASCII_C2 = getBytes(c2, "ASCII");
    private static final byte[] ASCII_C3 = getBytes(c3, "ASCII");
    private static final byte[] ASCII_C4 = getBytes(c4, "ASCII");
    private static final byte[] ASCII_C5 = getBytes(c5, "ASCII");

    /* UTF-8, with BOM */
    private static final int[] BOM_UTF8 = new int[] { 0xEF, 0xBB, 0xBF };
    private static final byte[] UTF8M_DOC = getBytes(BOM_UTF8, "UTF-8 blah blah blah: " + UNICODE_STRING, "UTF-8");
    private static final byte[] UTF8M_C1 = getBytes(BOM_UTF8, c1, "UTF-8");
    private static final byte[] UTF8M_C2 = getBytes(BOM_UTF8, c2, "UTF-8");
    private static final byte[] UTF8M_C3 = getBytes(BOM_UTF8, c3, "UTF-8");
    private static final byte[] UTF8M_C4 = getBytes(BOM_UTF8, c4, "UTF-8");
    private static final byte[] UTF8M_C5 = getBytes(BOM_UTF8, c5, "UTF-8");

    /* UTF-16, probably native byte order, with BOM */
    private static final byte[] UTF16M_DOC = getBytes("UTF-16 blah blah blah: " + UNICODE_STRING, "UTF-16");
    private static final byte[] UTF16M_C1 = getBytes(c1, "UTF-16");
    private static final byte[] UTF16M_C2 = getBytes(c2, "UTF-16");
    private static final byte[] UTF16M_C3 = getBytes(c3, "UTF-16");
    private static final byte[] UTF16M_C4 = getBytes(c4, "UTF-16");
    private static final byte[] UTF16M_C5 = getBytes(c5, "UTF-16");
}
