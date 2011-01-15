package com.l7tech.security.xml;

/**
 * Interface implemented by components that can look up secret key material based on EncryptedKeySHA1 strings.
 */
public interface EncryptedKeyResolver {
    /**
     * Look up an EncryptedKey by its EncryptedKeySHA1.
     *
     * @param encryptedKeySha1 the identifier to look up.  Never null or empty.
     * @return the matching EncryptedKey token, or null if no match was found.  The returned token is unmodifiable.
     * @see com.l7tech.security.xml.processor.WssProcessorUtil#makeEncryptedKey
     */
    byte[] getSecretKeyByEncryptedKeySha1(String encryptedKeySha1);
}
