package com.l7tech.security.keys;

import javax.crypto.SecretKey;

/**
 * Key Used by the xml mangler.
 */
public class DesKey implements SecretKey {
    private byte[] bytes;

    public DesKey(byte[] bytes, int keyBits) {
        if (keyBits != 64 && keyBits != 192)
            throw new IllegalArgumentException("Unsupported DES key length " + keyBits + ".  Supported lengths are 64 or 192");
        if (bytes.length * 8 < keyBits)
            throw new IllegalArgumentException("Byte array is too short for DES with " + keyBits + " bit key");
        this.bytes = new byte[keyBits / 8];
        System.arraycopy(bytes, 0, this.bytes, 0, this.bytes.length);
    }

    public String getAlgorithm() {
        return bytes.length > 8 ? "DESede" : "DES";
    }

    public String getFormat() {
        return "RAW";
    }

    public byte[] getEncoded() {
        return bytes;
    }
}
