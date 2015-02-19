package com.l7tech.util;

/**
 * Interface for a component capable of locating key material to use to encrypt or decrypt passwords.
 */
public interface SecretEncryptorKeyFinder {
    /**
     * Get the password encryption key from wherever this SecretEncryptorKeyFinder gets it.
     * <p/>
     * This method is poorly-named -- the returned secret needn't be the master password.
     * It can be whatever a particular MasterPasswordManager or SecretEncryptor instance is using
     * as symmetric key material.
     * <p/>
     * Implementors must take care to return a defensive copy rather than any original byte array
     * since callers are expected to zeroize their copy once they are done with it.
     *
     * @return the password encryption key as a newly-allocated byte array.  Never empty or null.
     *         May contain arbitrary binary data (not necessarily a valid UTF-8 string).
     *         Caller should zero the array when finished with it.
     * @throws IllegalStateException if no password encryption key can be found.
     */
    byte[] findMasterPasswordBytes();
}
