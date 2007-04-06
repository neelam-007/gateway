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
     * Get the private key and certificate chain for a given alias, in the form of an SsgKeyEntry instance.
     *
     * @param alias  the alias. Required.
     * @return the SsgKeyEntry for this alias.   Never null.
     * @throws KeyStoreException if this alias doesn't exist or doesn't have a cert chain or private key,
     *                           or if there is a problem reading the underlying key store.
     */
    SsgKeyEntry getCertificateChain(String alias) throws KeyStoreException;
}
