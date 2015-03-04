package com.l7tech.server.security.keystore;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.security.prov.CertificateRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.SignatureException;
import java.util.List;


/**
 * KeyStore-like interface implemented by SSG components that provide access to certificates with private keys.
 */
public interface SsgKeyFinder extends NamedEntity {
    enum SsgKeyStoreType {
        OTHER,
        PKCS12_SOFTWARE,
        PKCS11_HARDWARE,
        LUNA_HARDWARE,
        NCIPHER_HARDWARE,
        GENERIC,
    }

    Goid getGoid();

    /** @return the SsgKeyStoreType of this keystore instance. */
    SsgKeyStoreType getType();

    /** @return true iff. getKeyStore would return a non-null value. */
    boolean isMutable();

    /** @return false if attempts to export private keys from this keystore are certain to fail. */
    boolean isKeyExportSupported();

    /** @return a mutable SsgKeyStore interface to this KeyFinder, or null if this KeyFinder is read-only. */
    SsgKeyStore getKeyStore();

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
     * @throws ObjectNotFoundException if this alias doesn't exist or doesn't have a cert chain or private key
     * @throws KeyStoreException if there is a problem reading the underlying key store.
     */
    @Transactional(propagation=Propagation.SUPPORTS, readOnly=true)
    @NotNull
    SsgKeyEntry getCertificateChain(String alias) throws ObjectNotFoundException, KeyStoreException;

    /**
     * Generate a new PKCS#10 certificate request for the key pair specified by its alias, using a certificate with a DN
     * in the form "CN=username".  The contents of the keystore are not changed in any way by this operation --
     * it just makes and signs a new CSR with the private key, and returns the CSR.
     *
     * @param alias thye alias of the key pair whose public key to embed in the CSR and whose private key to use to sign it.  Required.
     * @param certGenParams parameters describing the certificate to create.  Required.
     *                      This can be used to override the dn from the cert request, if desired.
     * @return a CertificateRequest that can be exported as bytes and sent to a CA service.  Never null.
     * @throws java.security.InvalidKeyException  if the key cannot be used for this purpose
     * @throws java.security.SignatureException   if there was a problem signing the CSR
     * @throws java.security.KeyStoreException  if there is a problem reading the key store
     */
    @Transactional(propagation=Propagation.SUPPORTS, readOnly=true)
    CertificateRequest makeCertificateSigningRequest(String alias, CertGenParams certGenParams) throws InvalidKeyException, SignatureException, KeyStoreException;
}
