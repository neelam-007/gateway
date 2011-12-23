package com.l7tech.security.prov;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;

/**
 * A {@link GcmCipher} that just delegates to a JCE Cipher object.
 */
public class DefaultGcmCipher implements GcmCipher {
    private final Cipher cipher;

    public DefaultGcmCipher(Cipher cipher) {
        this.cipher = cipher;
    }

    @Override
    public void init(int cipherMode, Key key, AlgorithmParameterSpec spec) throws InvalidKeyException, InvalidAlgorithmParameterException {
        cipher.init(cipherMode, key, spec);
    }

    @Override
    public byte[] update(byte[] bytes, int offset, int len) {
        return cipher.update(bytes, offset, len);
    }

    @Override
    public byte[] doFinal() throws IllegalBlockSizeException, BadPaddingException {
        return cipher.doFinal();
    }
}
