package com.l7tech.security.keys;

import javax.crypto.SecretKey;
import java.security.KeyException;

/**
 * A Key that can dynamically alter its reported algorithm and key length.
 */
public class FlexKey implements SecretKey {
    public static class Alg {
        private final String name;
        private final int size;
        private Alg(String name, int size) {
            this.name = name;
            this.size = size / 8;
        }
    }

    public static final Alg AES128 = new Alg("AES", 128);
    public static final Alg AES192 = new Alg("AES", 192);
    public static final Alg AES256 = new Alg("AES", 256);
    public static final Alg TRIPLEDES = new Alg("DESede", 192);

    private final byte[] bytes;
    private Alg algorithm;
    private byte[] shortbytes;

    /**
     * Create a FlexKey that defaults to AES 128.
     *
     * @param bytes bytes to use as key material.  Must be at least 16 bytes in this array.
     * @throws KeyException if bytes is not at least 16 bytes long
     */
    public FlexKey(byte[] bytes) throws KeyException {
        this.bytes = bytes;
        setAlgorithm(AES128);
    }

    /**
     * Change the algorithm this FlexKey will offer.
     *
     * @param alg one of {@link #AES128}, {@link #AES192}, {@link #AES256}, or {@link #TRIPLEDES}.
     * @throws KeyException if this FlexKey does not contain enough key material bytes to offer the specified algorithm
     */
    public void setAlgorithm(Alg alg) throws KeyException {
        if (algorithm == alg) return;
        if (alg == null) throw new IllegalArgumentException("alg must be provided");
        if (bytes == null || bytes.length < alg.size) throw new KeyException("insufficient key material for " + alg.name + " " + (alg.size * 8) + " key");
        this.shortbytes = new byte[alg.size];
        System.arraycopy(bytes, 0, shortbytes, 0, alg.size);
        this.algorithm = alg;
    }

    public String getAlgorithm() {
        return algorithm.name;
    }

    public String getFormat() {
        return "RAW";
    }

    public byte[] getEncoded() {
        return shortbytes;
    }
}
