package com.l7tech.common.security;

import javax.crypto.SecretKey;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell, mike
 * Date: Aug 28, 2003
 * Time: 9:17:45 AM
 * $Id$
 *
 * Key Used by the xml mangler
 */
public class AesKey implements SecretKey {
    private byte[] bytes;

    public AesKey(byte[] bytes) {
        if (bytes.length != 16 && bytes.length != 24 && bytes.length != 32)
            throw new IllegalArgumentException("Byte array is wrong length for AES128, AES192 or AES256");
        this.bytes = bytes;
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
