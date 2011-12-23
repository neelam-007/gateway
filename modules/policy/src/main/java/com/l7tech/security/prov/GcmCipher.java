package com.l7tech.security.prov;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Wrapper interface for doing a GCM block cipher, for easily shimming in ncipher-specific (nCore) implementation
 * without having to create a provider.
 */
public interface GcmCipher {

    void init(int cipherMode, Key key, AlgorithmParameterSpec spec) throws InvalidKeyException, InvalidAlgorithmParameterException;

    byte[] update(byte[] bytes, int offset, int len);

    byte[] doFinal() throws IllegalBlockSizeException, BadPaddingException;
}
