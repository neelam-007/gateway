package com.l7tech.gateway.common.security.password;

import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.SyspropUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;

/**
 * Low level implementation of a password hashing algorithm that works with arbitrary hash functions
 * and supports unlimited-length passwords and salts and has a configuration exponential work factor.
 */
public class CryptL7 {
    // Minimum work factor to allow when computing a new password hash or checking an existing one.
    // Work factor 3 results in 256 work rounds and is computed pretty much instantly by my workstation.
    // This is about as low as we should consider permitting, initially.
    // If this value is raised then passwords hashed using lower values can no longer be authenticated.  We should avoid this course of action unless serious security concerns require it.
    public static final int MIN_WORK_FACTOR = SyspropUtil.getInteger("com.l7tech.CryptL7.minWorkFactor", 3);

    // Maximum work factor to allow when computing a new password hash or checking an existing one.
    // Work factor 10 results in 4194304 work rounds and takes about 8 CPU seconds with 64-bit JDK 6 on my current workstation (Core i5 750 @ 2.67 GHz, quad core, circa 2011).
    // This is about as high as we should currently consider allowing, in order to avoid a CPU DoS if a hostile hashed password is encountered.
    // This can be raised in the future with no backward-compatibility consequences.
    public static final int MAX_WORK_FACTOR = SyspropUtil.getInteger("com.l7tech.CryptL7.maxWorkFactor", 10);

    // This is the minimum size of underlying hash
    public static final int MIN_HASH_OUTPUT_SIZE = SyspropUtil.getInteger("com.l7tech.CryptL7.minHashOutputSize", 16);

    /*
The CryptL7 algorithm is intended to work with PCI-approved hash algorithms (ie, SHA-2) but to be computationally
infeasable to brute force, and to be annoying to implement a brute forcer in hardware (FPGA, GPU shader, or custom).

It uses a work factor which quickly scales up to run an enormous number of rounds of the hash algorithm.  In
between each round, the previous hash output is treated as mini-bytecode describing additional bytes to fetch from
various places (the salt or one of the previous round outputs) and mix into the hash input for the next round.

Input:
  password: byte string of arbitrary length
  salt: byte string of arbitrary length
  work_factor: small positive integer between MIN_WORK_FACTOR and MAX_WORK_FACTOR
  H: cryptographic hash that takes as input a byte string of arbitrary length and outputs a byte string of fixed length, ie SHA-256

Setup:
  ROUNDS = 2^(2 * (work_factor + 1))
  HS  = H(salt)
  H0 := H(password . salt)
  H1 := H(H0)
  H2 := H(H1)
  H3 := H(H2)
  H4 := H(H3)
  H5 := H(H4)
  H6 := H(H5)
  H7 := H(H6)
  H8 := H(H7)
  H9 := H(H8)
  H10:= H(H9)
  H11:= H(H10)
  H12:= H(H11)
  H13:= H(H12)
  H14:= H(H13)
  H15:= H(H14)
  HN:= H(H15)

Repeat ROUNDS times:
  BUF:= empty buffer
  For each byte i of HN:
    Break HN[i] into bits:  ooaaabbb  (rightmost b is least significant bit)
      ArgumentA = aaa:
        000: H0[i]
        001: H1[i]
        010: H2[i]
        011: H3[i]
        100: H4[i]
        101: H5[i]
        110: H6[i]
        111: H7[i]
      ArgumentB = bbb:
        000: H8[i]
        001: H9[i]
        010: H10[i]
        011: H11[i]
        100: H12[i]
        101: H13[i]
        110: H14[i]
        111: H15[i]
      OPERATION = oo:
        00 = ArgumentA[i] XOR HS[i]
        01 = ArgumentA[i] XOR ArgumentB[i]
        10 = (ArgumentA[i] unsigned+ ArgumentB[i]) mod 256
        11 = ArgumentB[i] XOR HN[i]
    Append result of OPERATION to BUF
  Append HN to BUF
  H0 := H1
  H1 := H2
  H2 := H3
  H3 := H4
  H4 := H5
  H5 := H6
  H6 := H7
  H7 := H8
  H8 := H9
  H9 := H10
  H10:= H11
  H11:= H12
  H12:= H13
  H13:= H14
  H14:= H15
  H15:= HN
  HN := H(BUF)

Output:
  HN: hashed password
     */

    /**
     * Interface implemented to provide a particular underlying hash algorithm to CryptL7.
     */
    public interface Hasher {
        /**
         * @return the fixed output size of this hasher, in bytes.  Must be positive.
         */
        int getOutputSizeInBytes();

        /**
         * Process a single byte array as hash input and return the hashed result.
         *
         * @param inputs  one or more byte arrays to hash in.  Must be non-null and contain at least one byte array.  Each contained byte array must be non-null but may be any length (including empty).  Required.
         * @return the result of hashing all input byte arrays in left-to-right order.  Always {@link #getOutputSizeInBytes()} bytes long.  Never null.
         */
        byte[] hash(byte[]... inputs);
    }

    /**
     * A Hasher implementation that uses the specified underlying MessageDigest implementation as its hash.
     * <p/>
     * Only one thread at a time may use an instance of this class.
     */
    public static class SingleThreadedJceMessageDigestHasher implements Hasher {
        private final MessageDigest md;
        private final int outputSize;

        /**
         * Create a Hasher that will take ownership of the specified MessageDigest instance and use it to
         * hash future inputs.
         *
         * @param md a MessageDigest instance for this Hasher's exclusive use.  Must be non-null.  Must have a positive digestLength.
         */
        public SingleThreadedJceMessageDigestHasher(MessageDigest md) {
            this(md, 0);
        }

        /**
         * Create a Hasher that will take ownership of the specified MessageDigest instance and use it to
         * hash future inputs.
         *
         * @param md a MessageDigest instance for this Hasher's exclusive use.  Must be non-null.  Must have a positive digestLength unless a positive {@link #outputSize} is specified.
         * @param outputSize hash output size in bytes, or 0 to attempt to obtain this information from the MessageDigest instance.
         */
        public SingleThreadedJceMessageDigestHasher(MessageDigest md, int outputSize) {
            this.md = md;
            this.outputSize = outputSize != 0 ? outputSize : md.getDigestLength();
            if (this.outputSize < 1)
                throw new IllegalArgumentException("Unable to determine digest output size");
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
            return md.digest();
        }
    }

    public byte[] computeHash(byte[] password, byte[] salt, int workFactor, Hasher hasher) {
        if (password == null)
            throw new NullPointerException("A password must be provided");
        if (salt == null)
            throw new NullPointerException("A salt must be provided");
        if (hasher == null)
            throw new NullPointerException("A hasher must be provided");
        if (workFactor < MIN_WORK_FACTOR)
            throw new IllegalArgumentException("Work factor is too low");
        if (workFactor > MAX_WORK_FACTOR)
            throw new IllegalArgumentException("Work factor is too high");

        final int hashSize = hasher.getOutputSizeInBytes();
        if (hashSize < MIN_HASH_OUTPUT_SIZE)
            throw new IllegalArgumentException("The hasher's output size is too small");

        final long rounds = getNumRoundsForWorkFactor(workFactor);
        final byte[] hs = hasher.hash(salt);
        final byte[][] h = new byte[16][];
        h[0] = hasher.hash(password, salt);
        for (int i = 1; i < 16; ++i)
            h[i] = hasher.hash(h[i - 1]);
        byte[] hn = hasher.hash(h[15]);

        for (int round = 0; round < rounds; ++round) {
            hn = performWorkRound(hasher, hashSize, hs, h, hn);
        }

        return hn;
    }

    /**
     * Get the number of work rounds that would be performed when hashing a password given the specified work factor,
     * if it is in range for this implementation.
     *
     * @param workFactor the work factor.  Must be nonnegative and less than 31.
     * @return the number of work rounds that would be performed when computing a CryptL7 hash using the specified work factor.
     * @throws IllegalArgumentException if the work factor is negative or higher than this implementation is capable of honoring (assuming no {@link #MAX_WORK_FACTOR} limit).
     */
    public long getNumRoundsForWorkFactor(int workFactor) {
        // Hardcoded implementation-specific absolute limits -- this implementation returns erroneous results if given input outside this range.
        if (workFactor < 0)
            throw new IllegalArgumentException("Negative work factor");
        if (workFactor > 30)
            throw new IllegalArgumentException("Work factor too high for this implementation");
        return 1L << (2 * (workFactor + 1));
    }

    /**
     * Perform a single work round of the password hashing algorithm.
     * <p/>
     * This will interpret each byte of hn as a kind of bytecode saying what corresponding bytes to combine from what
     * sources (in hs, h, and hn itself) using what mechanism (xor or modular addition) to prepend to hn before
     * running it all through the Hasher.
     * <p/>
     * <b>Note:</b>At the end of the round, this method will modify the two-dimensional byte array h to slide everything
     * up one row, and to insert the input hn as the new bottom row (the previous top row, h[0], is discarded).
     *
     * @param hasher a Hasher to use for hashing the data at the end of this round.  Required.
     * @param hashSize the block size used by the hasher, passed in just in case it is slow to query from the hasher itself.  Required.
     * @param hs the hash of the salt.  Must be same length as hn.  Required.
     * @param h  an array of the 16 previous hash values.  Must contain 16 values, each the same length as hn.  Required.  '''Note:''' two-way parameter, modified by this method; see method description.
     * @param hn the hn value output from the previous round (or the Hasher has of h[15], if this is the initial round).  Must be same length as hs and each row of h.  Required.
     * @return the hn value for the next round, already run though the Hasher.  If this is the final round, this is the final hashed password value.
     */
    byte[] performWorkRound(final Hasher hasher, final int hashSize, final byte[] hs, final byte[][] h, final byte[] hn) {
        PoolByteArrayOutputStream buf = new PoolByteArrayOutputStream(hashSize * 2);
        try {
            for (int i = 0; i < hashSize; ++i) {
                final int b = ((int)hn[i]) & 0xFF;
                assert b >= 0 && b < 256;

                final int opNum = b >> 6;
                assert opNum >= 0 && opNum < 4;

                final int argNumA = (b & 0x38)  >> 3;
                assert argNumA >= 0 && argNumA < 8;

                final int argNumB = (b & 0x07);
                assert argNumB >= 0 && argNumB < 8;

                final int argA = h[argNumA][i];
                final int argB = h[argNumB + 8][i];

                final int out;
                switch (opNum) {
                    case 0:
                        out = argA ^ hs[i];
                        break;
                    case 1:
                        out = argA ^ argB;
                        break;
                    case 2:
                        out = (argA + argB) & 0xFF;
                        break;
                    case 3:
                        out = argB ^ hn[i];
                        break;
                    default:
                        throw new IllegalStateException(); // can't happen
                }

                writeNoThrow(buf, out);
            }

            writeNoThrow(buf, hn);
            System.arraycopy(h, 1, h, 0, 15);
            h[15] = hn;
            return hasher.hash(buf.toByteArray());
        } finally {
            buf.close();
        }
    }

    // Write to output stream that is guaranteed never to throw IOException (ie byte array)
    static void writeNoThrow(OutputStream out, int b) {
        try {
            out.write(b);
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    // Write to output stream that is guaranteed never to throw IOException (ie byte array)
    static void writeNoThrow(OutputStream out, byte[] input) {
        try {
            out.write(input);
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        }
    }
}
