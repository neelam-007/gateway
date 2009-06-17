package com.l7tech.server.security.keystore;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.x500.X500Principal;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.concurrent.Future;

/**
 * Interface implemented by SSG components that own and manage certificates with private keys on a particular
 * SSG node.
 */
@Transactional(propagation= Propagation.SUPPORTS, rollbackFor=Throwable.class)
public interface SsgKeyStore extends SsgKeyFinder {
    /**
     * Generate a new RSA key pair and self-signed certificate within this keystore.
     *
     * @param transactionCallback Optional callback to invoke inside the transaction, or null.
     *                            Can be used for more detailed auditing.
     * @param alias   the alias for the new key entry.  Required.  Must not collide with any existing alias.
     * @param dn      the DN for the new self-signed certificate.  Required.
     * @param keybits the number of bits the new RSA key should contain, ie 512, 768, 1024 or 2048.  Required.
     * @param expiryDays  the number of days before the new self-signed certificate will expire.  Required.
     * @param makeCaCert    true if the new certificate is intended to be used to sign other certs.  Normally false.
     *                      If this is true, the new certificate will have the "cA" basic constraint and the "keyCertSign" key usage.
     * @return immediately returns a Future which returns the new self-signed certificate, already added to this key store.  Never null.
     * @throws GeneralSecurityException if there is a problem generating, signing, or saving the new certificate or key pair
     */
    @Transactional(propagation=Propagation.REQUIRED)
    Future<X509Certificate> generateKeyPair(Runnable transactionCallback, String alias, X500Principal dn, int keybits, int expiryDays, boolean makeCaCert) throws GeneralSecurityException;

    /**
     * Generate a new elliptic curve key pair and self-signed certificate within this keystore.
     *
     * @param transactionCallback Optional callback to invoke inside the transaction, or null.
     *                            Can be used for more detailed auditing.
     * @param alias   the alias for the new key entry.  Required.  Must not collide with any existing alias.
     * @param dn      the DN for the new self-signed certificate.  Required.
     * @param curveName a recognized ECC curve name, ie "secp384r1".   Required.
     * @param expiryDays  the number of days before the new self-signed certificate will expire.  Required.
     * @param makeCaCert    true if the new certificate is intended to be used to sign other certs.  Normally false.
     *                      If this is true, the new certificate will have the "cA" basic constraint and the "keyCertSign" key usage.
     * @return immediately returns a Future which returns the new self-signed certificate, already added to this key store.  Never null.
     * @throws GeneralSecurityException if there is a problem generating, signing, or saving the new certificate or key pair
     */
    @Transactional(propagation=Propagation.REQUIRED)
    Future<X509Certificate> generateEcKeyPair(Runnable transactionCallback, String alias, X500Principal dn, String curveName, int expiryDays, boolean makeCaCert) throws GeneralSecurityException;

    /**
     * Replace the certificate for the specified alias with a new certificate based on the same key pair.
     * This could be used to replace a placeholder self-signed certificate with the real cartel certificate
     * once it comes back from the CA.
     *
     * @param transactionCallback Optional callback to invoke inside the transaction, or null.
     *                            Can be used for more detailed auditing.
     * @param alias   the alias whose certificate to replace.  Required.   There must be a key pair at this alias.
     * @param chain   the certificate chain to use instead.  Required.  Must contain at least one certificate,
     *                the first one in the array being the subject certificate.
     *                The subject public key must match the public key of the current subject.
     * @return immediately returns a Future which returns Boolean.TRUE when successful.
     * @throws InvalidKeyException if the public key does not match the public key of the existing key pair
     * @throws KeyStoreException  if there is a problem reading or writing the keystore
     */
    @Transactional(propagation=Propagation.REQUIRED)
    Future<Boolean> replaceCertificateChain(Runnable transactionCallback, String alias, X509Certificate[] chain) throws InvalidKeyException, KeyStoreException;

    /**
     * Add an entry to this key store.  Any previous entry with this alias can optionally be overwritten.  However, due to
     * key caching, callers should not count on this taking effect immediately.  Instead, it is recommended to
     * save changed keys under a new alias.
     *
     * @param transactionCallback Optional callback to invoke inside the transaction, or null.
     *                            Can be used for more detailed auditing.
     * @param entry             The entry to store.  Required.  Must contain the private key.
     *                          Must also contain a non-null alias; if overwriteExisting is false, this must not match
     *                          the alias of any existing entry.  Must also contain the private key.
     * @param overwriteExisting if true, any existing entry with this alias will be overwritten.
     * @return immediately returns a Future which returns Boolean.TRUE when successful.
     * @throws KeyStoreException  if there is a problem storing this entry
     */
    @Transactional(propagation=Propagation.REQUIRED)
    Future<Boolean> storePrivateKeyEntry(Runnable transactionCallback, SsgKeyEntry entry, boolean overwriteExisting) throws KeyStoreException;

    /**
     * Delete an entry from this key store.
     *
     * @param transactionCallback Optional callback to invoke inside the transaction, or null.
     *                            Can be used for more detailed auditing.
     * @param keyAlias   the alias of the entry to delete.  Required.
     * @return immediately returns a Future which returns Boolean.TRUE when successful.
     * @throws KeyStoreException  if there is a problem deleting this entry
     */
    @Transactional(propagation=Propagation.REQUIRED)
    Future<Boolean> deletePrivateKeyEntry(Runnable transactionCallback, String keyAlias) throws KeyStoreException;
}
