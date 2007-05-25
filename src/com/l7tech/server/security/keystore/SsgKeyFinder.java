package com.l7tech.server.security.keystore;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.List;

/**
 * KeyStore-like interface implemented by SSG components that provide access to certificates with private keys.
 */
@Transactional(propagation= Propagation.SUPPORTS, rollbackFor=Throwable.class)
public interface SsgKeyFinder {
    enum SsgKeyStoreType {
        OTHER,
        PKCS12_SOFTWARE,
        PKCS11_HARDWARE
    }

    /** @return ID of this key store.  Only guaranteed unique on a particular SSG node. */
    @Transactional(propagation=Propagation.SUPPORTS, readOnly=true)
    long getId();

    /** @return the display name of this key store.  Not necessarily unique.  Never null. */
    @Transactional(propagation=Propagation.SUPPORTS, readOnly=true)
    String getName();

    /** @return the SsgKeyStoreType of this keystore instance. */
    @Transactional(propagation=Propagation.SUPPORTS, readOnly=true)
    SsgKeyStoreType getType();

    /** @return true iff. getKeyStore would return a non-null value. */
    @Transactional(propagation=Propagation.SUPPORTS, readOnly=true)
    boolean isMutable();

    /** @return a mutable SsgKeyStore interface to this KeyFinder, or null if this KeyFinder is read-only. */
    @Transactional(propagation=Propagation.SUPPORTS, readOnly=true)
    SsgKeyStore getKeyStore();

    /**
     * Get the aliases available.
     *
     * @return a list of aliases.  May be empty but never null.
     * @throws KeyStoreException if there is a problem obtaining the list
     */
    @Transactional(propagation=Propagation.SUPPORTS, readOnly=true)
    List<String> getAliases() throws KeyStoreException;

    /**
     * Get the private key and certificate chain for a given alias, in the form of an SsgKeyEntry instance.
     *
     * @param alias  the alias. Required.
     * @return the SsgKeyEntry for this alias.   Never null.
     * @throws KeyStoreException if this alias doesn't exist or doesn't have a cert chain or private key,
     *                           or if there is a problem reading the underlying key store.
     */
    @Transactional(propagation=Propagation.SUPPORTS, readOnly=true)
    SsgKeyEntry getCertificateChain(String alias) throws KeyStoreException;
}
