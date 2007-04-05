package com.l7tech.server.security.keystore;

import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.List;

/**
 * KeyStore-like interface implemented by SSG components that provide access to certificates with private keys.
 */
public interface SsgKeyFinder {
    /**
     * Get the aliases available.
     *
     * @return a list of aliases.  May be empty but never null.
     * @throws KeyStoreException if there is a problem obtaining the list
     */
    List<String> getAliases() throws KeyStoreException;

    /**
     * Get the certificate chain for a given alias.
     *
     * @param alias  the alias. Required.
     * @return the certificate chain.  Never null or empty; always contains at least one entry.
     * @throws KeyStoreException if this alias doesn't exist or doesn't have a cert chain, or if there is a problem
     *                           reading the underlying key store.
     */
    X509Certificate[] getCertificateChain(String alias) throws KeyStoreException;

    /**
     * Get the RSA private key for a given alias.
     *
     * @param alias  the alias. Required.
     * @return the RSA private key for this alias.  Never null or empty.  The key will be usable with JCE
     *         but might restrict the operations that can be performed and might be non-extractable if this
     *         is a hardware keystore.
     * @throws KeyStoreException if this alias doesn't exist or doesn't have a private key, or if there is a problem
     *                           reading the underlying key store.
     */
    RSAPrivateKey getRsaPrivateKey(String alias) throws KeyStoreException;
}
