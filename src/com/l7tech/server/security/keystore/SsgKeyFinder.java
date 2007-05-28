package com.l7tech.server.security.keystore;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.security.KeyStoreException;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.List;

import com.l7tech.common.security.CertificateRequest;
import com.l7tech.common.security.keystore.SsgKeyEntry;

import javax.naming.ldap.LdapName;

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

    /**
     * Generate a new PKCS#10 certificate request for the key pair specified by its alias, using a certificate with a DN
     * in the form "CN=username".  The contents of the keystore are not changed in any way by this operation --
     * it just makes and signs a new CSR with the private key, and returns the CSR.
     *
     * @param alias thye alias of the key pair whose public key to embed in the CSR and whose private key to use to sign it.  Required.
     * @param dn  DN to use in the CSR.  Must contain valid X.509 fields.  Required.
     * @return a CertificateRequest that can be exported as bytes and sent to a CA service.  Never null.
     * @throws java.security.InvalidKeyException  if the key cannot be used for this purpose
     * @throws java.security.SignatureException   if there was a problem signing the CSR
     * @throws java.security.KeyStoreException  if there is a problem reading the key store
     */
    @Transactional(propagation=Propagation.SUPPORTS, readOnly=true)
    CertificateRequest makeCertificateSigningRequest(String alias, LdapName dn) throws InvalidKeyException, SignatureException, KeyStoreException;
}
