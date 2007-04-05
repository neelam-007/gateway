package com.l7tech.server.security.keystore;

import java.security.PrivateKey;
import java.security.KeyStoreException;
import java.security.KeyPair;
import java.security.InvalidAlgorithmParameterException;
import java.security.cert.X509Certificate;

/**
 * Interface implemented by SSG components that own and manage certificates with private keys on a particular
 * SSG node.
 */
public interface SsgKeyStore extends SsgKeyFinder {
    enum SsgKeyStoreType {
        OTHER,
        PKCS12_SOFTWARE,
        PKCS11_HARDWARE
    }

    /** @return ID of this key store.  Only unique on a particular SSG node. */
    int getId();

    /** @return the display name of this key store.  Not necessarily unique.  Never null. */
    String getName();

    /**
     * Add an entry to this key store.  Any previous entry with this alias will be overwritten.  However, due to
     * key caching, callers should not count on this taking effect immediately.  Instead, it is recommended to
     * save changed keys under a new alias.
     * @param alias             key alias.  Must be non-empty.
     * @param privateKey        the private key.  Must be storable into this keystore.  Required.
     *                          A private key will be storable into this keystore if its private key
     *                          material can be obtained in encoded form, or if the private key came from
     *                          this key store in the first place.
     * @param certificateChain  the certificate chain for this private key.  Must contain at least one entry.
     *                          The zeroth entry must have the public key that corresponds to privateKey.
     * @throws KeyStoreException  if there is a problem storing this entry
     */
    void storePrivateKeyEntry(String alias, PrivateKey privateKey, X509Certificate[] certificateChain) throws KeyStoreException;

    /**
     * Generate a new RSA KeyPair whose PrivateKey will be suitable for storage into this key store
     * using storePrivateKeyEntry.
     *
     * @param keyBits  number of bits, or zero to default to 1024 bit RSA.
     * @return a new RSA key pair, whose private key may be locked within the hardware if this is a hardware key store.
     * @throws InvalidAlgorithmParameterException if there is a problem generating a new RSA key pair with this key size.
     */
    KeyPair generateRsaKeyPair(int keyBits) throws InvalidAlgorithmParameterException;
}
