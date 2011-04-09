package com.l7tech.common.password;

import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Test suite for CryptL7.
 */
public class CryptL7Test {

    // A hasher for testing that does not do any hashing, and uses a very short block size to make manually-computed test results less painful
    static class NullHasher implements CryptL7.Hasher {
        private final int outputSize = 8;
        ByteArrayOutputStream capture = null;

        @Override
        public int getOutputSizeInBytes() {
            return outputSize;
        }

        private byte[] hashSingle(byte[] input) {
            if (capture != null) {
                CryptL7.writeNoThrow(capture, input);
            }
            if (input.length == outputSize)
                return input;
            byte[] ret = new byte[outputSize];
            if (input.length < 1)
                return ret;
            int pos = 0;
            int ipos = 0;
            for (;;) {
                ret[pos++] = input[ipos++];
                if (pos >= ret.length)
                    break;
                if (ipos >= input.length)
                    ipos = 0;
            }
            return ret;
        }

        @Override
        public byte[] hash(byte[]... input) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (byte[] bytes : input) {
                CryptL7.writeNoThrow(out, bytes);
            }
            return hashSingle(out.toByteArray());
        }
    }

    @Test(expected = NullPointerException.class)
    public void testNullHasherRejectsNull() {
        new NullHasher().hash((byte[][])null);
    }

    @SuppressWarnings({"ImplicitNumericConversion"})
    @Test
    public void testNullHasher() throws Exception {
        CryptL7.Hasher nullHasher = new NullHasher();
        assertEquals(8, nullHasher.getOutputSizeInBytes());
        assertTrue(Arrays.equals(new byte[8], nullHasher.hash(new byte[0])));
        assertTrue(Arrays.equals(new byte[] {1,1,1,1,1,1,1,1}, nullHasher.hash(new byte[] {1})));
        assertTrue(Arrays.equals(new byte[] {1,2,1,2,1,2,1,2}, nullHasher.hash(new byte[] {1,2})));
        assertTrue(Arrays.equals(new byte[] {1,2,3,4,5,1,2,3}, nullHasher.hash(new byte[] {1,2,3,4,5})));
        assertTrue(Arrays.equals(new byte[] {1,2,3,4,5,6,7,8}, nullHasher.hash(new byte[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17})));
        assertTrue(Arrays.equals(new byte[] {1,2,3,4,5,6,7,8}, nullHasher.hash(new byte[] {1,2,3,4,5,6,7,8})));
        assertTrue(Arrays.equals(new byte[] {1,2,3,4,5,6,7,1}, nullHasher.hash(new byte[] {1,2,3,4,5,6,7})));
    }

    // A hasher for testing that uses a real MessageDigest but ignores all but the leftmost 8 bytes of its output, to make manually-computed test results less painful to do.
    static class Truncated8ByteMessageDigestHasher implements CryptL7.Hasher {
        private final int outputSize = 8;
        private final MessageDigest md;

        Truncated8ByteMessageDigestHasher(MessageDigest md) {
            this.md = md;
            if (md.getDigestLength() < outputSize) throw new IllegalArgumentException();
        }

        @Override
        public int getOutputSizeInBytes() {
            return outputSize;
        }

        @Override
        public byte[] hash(byte[]... inputs) {
            md.reset();
            for (byte[] input : inputs) {
                md.update(input);
            }
            byte[] result = md.digest();
            byte[] ret = new byte[getOutputSizeInBytes()];
            System.arraycopy(result, 0, ret, 0, ret.length);
            return ret;
        }
    }

    CryptL7.Hasher nullHasher = new NullHasher();
    @SuppressWarnings({"ImplicitNumericConversion"})
    byte[][] h = {
        { 1, 2, 3, 4, 5, 6, 7, 8}, // H0
        { 9,10,11,12,13,14,15,16}, // H1
        {17,18,19,20,21,22,23,24}, // H2
        {25,26,27,28,29,30,31,32}, // H3
        {33,34,35,36,37,38,39,40}, // H4
        {41,42,43,44,45,46,47,48}, // H5
        {49,50,51,52,53,54,55,56}, // H6
        {57,58,59,60,61,62,63,64}, // H7
        {65,66,67,68,69,70,71,72}, // H8
        {73,74,75,76,77,78,79,80}, // H9
        {81,82,83,84,85,86,87,88}, // H10
        {89,90,91,92,93,94,95,96}, // H11
        {97,98,99,100,101,102,103,104}, // H12
        {105,106,107,108,109,110,111,112}, // H13
        {113,114,115,116,117,118,119,120}, // H14
        {121,122,123,124,125,126,127,(byte)128} // H15
    };
    byte[][] oh = new byte[][] { h[0], h[1], h[2], h[3], h[4], h[5], h[6], h[7], h[8], h[9], h[10], h[11], h[12], h[13], h[14], h[15] };
    @SuppressWarnings({"ImplicitNumericConversion"})
    byte[] hs = {-17,-18,-19,-20,-21,-22,-23,-24};
    @SuppressWarnings({"ImplicitNumericConversion"})
    byte[] hn = {-9,-101,42,85,-86,-64,33,-5};
    @SuppressWarnings({"ImplicitNumericConversion"})
    byte[] expectedFirstRoundUnhashedResultPrefix = {-114, 116, -58, 120, -126, -122, -50, -101}; // Computed tediously by hand

    @Test
    public void testPerformWorkRoundNullHashManualCheck() throws Exception {
        // Peforms a single work round with a null hash (that leaves its input unchanged) and checks the result against a hand-generated test vector, showing its work
        // Also ensures that the h[][] array was rotated correctly by the round function.
        byte[] res = new CryptL7().performWorkRound(nullHasher, 8, hs, h, hn);
        assertNotNull(res);
        assertEquals(8, res.length);

        // Ensure h got rotated correctly
        assertTrue(h[0] == oh[1]);
        assertTrue(h[1] == oh[2]);
        assertTrue(h[2] == oh[3]);
        assertTrue(h[3] == oh[4]);
        assertTrue(h[4] == oh[5]);
        assertTrue(h[5] == oh[6]);
        assertTrue(h[6] == oh[7]);
        assertTrue(h[7] == oh[8]);
        assertTrue(h[8] == oh[9]);
        assertTrue(h[9] == oh[10]);
        assertTrue(h[10] == oh[11]);
        assertTrue(h[11] == oh[12]);
        assertTrue(h[12] == oh[13]);
        assertTrue(h[13] == oh[14]);
        assertTrue(h[14] == oh[15]);
        assertTrue(h[15] == hn);

        // Test vector computed by hand
        int i = 0;

        // HN[0] (-9,   11 110 111)
        //a = h[6][i];  // 49
        //b = h[15][i]; // 121
        //op = b ^ hn[i]; // 121 xor -9 == -114
        assertEquals(-114, res[i]);
        ++i;

        // HN[1] (-101, 10 011 011)
        //a = h[3][i];  // 26
        //b = h[11][i]; // 90
        //op = (a + b) & 0xFF; // (byte)(26 + 90) == 116
        assertEquals(116, res[i]);
        ++i;

        // HN[2] (42,   00 101 010)
        //a = h[5][i];  // 43
        //b = h[10][i]; // 83
        //op = a ^ hs[i]; // 43 xor -19 == -58
        assertEquals(-58, res[i]);
        ++i;

        // HN[3] (85,   01 010 101)
        //a = h[2][i];  // 20
        //b = h[13][i]; // 108
        //op = a ^ b; // 20 xor 108 == 120
        assertEquals(120, res[i]);
        ++i;

        // HN[4] (-86,  10 101 010)
        //a = h[5][i];  // 45
        //b = h[10][i]; // 85
        //op = (a + b) & 0xFF; // (byte)(45 + 85) == -126
        assertEquals(-126, res[i]);
        ++i;

        // HN[5] (-64,  11 000 000)
        //a = h[0][i];  // 6
        //b = h[8][i];  // 70
        //op = b ^ hn[i]; // 70 ^ -64 == -122
        assertEquals(-122, res[i]);
        ++i;

        // HN[6] (33,   00 100 001)
        //a = h[4][i];  // 39
        //b = h[9][i];  // 79
        //op = a ^ hs[i]; // 39 ^ -23 == -50
        assertEquals(-50, res[i]);
        ++i;

        // HN[7] (-5,   11 111 011)
        //a = h[7][i];  // 64
        //b = h[11][i]; // 96
        //op = b ^ hn[i]; // 96 ^ -5 == -101
        assertEquals(-101, res[i]);
        ++i;

        assertEquals(8, i);
    }

    @Test
    public void testPerformWorkRoundNullHashArrayCheck() throws Exception {
        // Peforms a single work round with a null hash (that leaves its input unchanged) and checks the result against a test vector.
        byte[] res = new CryptL7().performWorkRound(nullHasher, 8, hs, h, hn);
        byte[] resFirst8 = new byte[8];
        System.arraycopy(res, 0, resFirst8, 0, resFirst8.length);
        assertTrue(Arrays.equals(expectedFirstRoundUnhashedResultPrefix, resFirst8));
    }

    @Test
    public void testPerformWorkRoundShaHashArrayCheck() throws Exception {
        // Performs a single work round with a real (albeit truncated) hash function and compares the result against the truncated hash using the hand-generated test vector.
        CryptL7.Hasher shaHasher = new Truncated8ByteMessageDigestHasher(MessageDigest.getInstance("SHA-1"));

        byte[] res = new CryptL7().performWorkRound(shaHasher, shaHasher.getOutputSizeInBytes(), hs, h, hn);
        assertNotNull(res);
        assertEquals(8, res.length);

        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        sha.reset();
        sha.update(expectedFirstRoundUnhashedResultPrefix);
        sha.update(hn);
        byte[] expectedFullSize = sha.digest();
        byte[] expected8 = new byte[8];
        System.arraycopy(expectedFullSize, 0, expected8, 0, expected8.length);

        assertTrue(Arrays.equals(expected8, res));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWorkFactorTooLowForImpl() throws Exception {
        new CryptL7().getNumRoundsForWorkFactor(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWorkFactorTooHighForImpl() throws Exception {
        new CryptL7().getNumRoundsForWorkFactor(31);
    }

    @Test
    public void testGetNumRoundsForWorkFactor() throws Exception {
        long[] roundsPerWorkFactor = {
                4,
                16,
                64,
                256,
                1024,
                4096,
                16384,
                65536,
                262144,
                1048576,
                4194304,
                16777216,
                67108864,
                268435456,
                1073741824,
                4294967296L,
                17179869184L,
                68719476736L,
                274877906944L,
                1099511627776L,
                4398046511104L,
                17592186044416L,
                70368744177664L,
                281474976710656L,
                1125899906842624L,
                4503599627370496L,
                18014398509481984L,
                72057594037927936L,
                288230376151711744L,
                1152921504606846976L,
                4611686018427387904L
        };
        for (int i = 0; i < roundsPerWorkFactor.length; ++i) {
            assertEquals(roundsPerWorkFactor[i], new CryptL7().getNumRoundsForWorkFactor(i));
        }
    }

    @Test
    public void findMaxIntegerWorkFactor() {
        for (int i = 0; i < 30; ++i) {
            long rounds = new CryptL7().getNumRoundsForWorkFactor(i);
            if (rounds > (long)Integer.MAX_VALUE) {
                System.out.println("Work factor " + i + " results in " + rounds + " rounds");
                return;
            }
        }
    }

    @Test
    public void testConstantDefaultValues() {
        assertEquals(3, CryptL7.MIN_WORK_FACTOR);
        assertEquals(10, CryptL7.MAX_WORK_FACTOR);
        assertEquals(16, CryptL7.MIN_HASH_OUTPUT_SIZE);
    }

    byte[] password = "sekrit".getBytes(Charsets.UTF8);
    byte[] salt = "blah".getBytes(Charsets.UTF8);
    int workFactor = CryptL7.MIN_WORK_FACTOR;
    CryptL7.Hasher sha256Hasher;

    @Before
    public void prepareSha256Hasher() throws NoSuchAlgorithmException {
        sha256Hasher = new CryptL7.SingleThreadedJceMessageDigestHasher(MessageDigest.getInstance("SHA-256"));
    }

    @Test(expected = NullPointerException.class)
    public void testComputeHashNullPassword() throws Exception {
        new CryptL7().computeHash(null, salt, workFactor, sha256Hasher);
    }

    @Test(expected = NullPointerException.class)
    public void testComputeHashNullSalt() throws Exception {
        new CryptL7().computeHash(password, null, workFactor, sha256Hasher);
    }

    @Test(expected = NullPointerException.class)
    public void testComputeHashNullHasher() throws Exception {
        new CryptL7().computeHash(password, salt, workFactor, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testComputHashLowWorkFactor() throws Exception {
        new CryptL7().computeHash(password, salt, CryptL7.MIN_WORK_FACTOR - 1, sha256Hasher);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testComputHashHighWorkFactor() throws Exception {
        new CryptL7().computeHash(password, salt, CryptL7.MAX_WORK_FACTOR + 1, sha256Hasher);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testComputeHashDigestSizeTooSmall() throws Exception {
        new CryptL7().computeHash(password, salt, workFactor, new CryptL7.Hasher() {
            @Override
            public int getOutputSizeInBytes() {
                return 3;
            }

            @Override
            public byte[] hash(byte[]... inputs) {
                fail("hash invocation attempted"); // shouldn't get this far
                return null;
            }
        });
    }

    @Test
    public void testKnownInput() throws Exception {
        String result = HexUtils.hexDump(new CryptL7().computeHash("sekrit".getBytes(), "blah".getBytes(), 8, sha256Hasher));
        assertEquals("021c1f88594c304b4b4151da6db39b0f230e5049cf62c38cd23bfc9bdee2567e", result);
    }

    @Ignore("Disabled because it is slow; enable in order to test performance with default work factor")
    @Test
    public void testTimingOfDefaultWorkFactor() throws Exception {
        long before = System.currentTimeMillis();
        for (int i = 0; i < 1000; ++i) {
            new CryptL7().computeHash(password, salt, CryptL7.DEFAULT_WORK_FACTOR, sha256Hasher);
        }
        long after = System.currentTimeMillis();
        long totalms = after - before;
        System.out.println("Total time for 1000 hashes with default work factor: " + totalms + " ms");
    }
}
