package com.l7tech.common.security;

import javax.crypto.SecretKey;

/**
 * Key Used by the xml mangler.
 *
 * <br/><br/>
 * Layer 7 technologies, inc.<br/>
 * User: flascell, mike<br/>
 * Date: Aug 28, 2003<br/>
 * $Id$
 */
public class AesKey implements SecretKey {
    private byte[] bytes;

    public AesKey(byte[] bytes, int keyBits) {
        if (keyBits != 128 && keyBits != 192 && keyBits != 256)
            throw new IllegalArgumentException("Unsupported AES key length " + keyBits + ".  Supported lengths are 128, 192 or 256");
        if (bytes.length * 8 < keyBits)
            throw new IllegalArgumentException("Byte array is too short for AES with " + keyBits + " bit key");
        this.bytes = new byte[keyBits / 8];
        System.arraycopy(bytes, 0, this.bytes, 0, this.bytes.length);
    }

    public String getAlgorithm() {
        return "AES";
    }

    public String getFormat() {
        return "RAW";
    }

    public byte[] getEncoded() {
        return bytes;
    }
}
