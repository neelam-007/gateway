package com.l7tech.security.xml;

/**
 * Interface implemented by objects that can store and retrieve secret key information by EncryptedKeySHA1 string.
 */
public interface EncryptedKeyCache extends EncryptedKeyResolver {

    /**
     * Report that an EncryptedKey was decrypted, so it can be saved for later reuse by its EncryptedKeySHA1.
     *
     * @param encryptedKeySha1 the identifier to store, in the form of an EncryptedKeySHA1 string, which is the base64
     *                         encoded ciphertext of the secret key.  Must not be null or empty.
     * @param secretKey  the unwrapped SecretKey that came from the EncryptedKey with the specified EncryptedKeySha1.
     */
    void putSecretKeyByEncryptedKeySha1(String encryptedKeySha1, byte[] secretKey);
}
