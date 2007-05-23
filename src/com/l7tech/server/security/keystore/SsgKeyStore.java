package com.l7tech.server.security.keystore;

import com.l7tech.common.security.CertificateRequest;

import javax.naming.ldap.LdapName;
import java.security.*;
import java.security.cert.X509Certificate;

/**
 * Interface implemented by SSG components that own and manage certificates with private keys on a particular
 * SSG node.
 */
public interface SsgKeyStore extends SsgKeyFinder {
    /**
     * Generate a new RSA key pair and self-signed certificate within this keystore.
     *
     * @param alias   the alias for the new key entry.  Required.  Must not collide with any existing alias.
     * @param dn      the DN for the new self-signed certificate.  Required.
     * @param keybits the number of bits the new RSA key should contain, ie 512, 768, 1024 or 2048.  Required.
     * @param expiryDays  the number of days before the new self-signed certificate will expire.  Required.
     * @return the new self-signed certificate, already added to this key store.  Never null.
     * @throws GeneralSecurityException if there is a problem generating, signing, or saving the new certificate or key pair
     */
    public X509Certificate generateKeyPair(String alias, LdapName dn, int keybits, int expiryDays) throws GeneralSecurityException;

    /**
     * Replace the certificate for the specified alias with a new certificate based on the same key pair.
     * This could be used to replace a placeholder self-signed certificate with the real cartel certificate
     * once it comes back from the CA.
     *
     * @param alias   the alias whose certificate to replace.  Required.   There must be a key pair at this alias.
     * @param certificate  the certificate to use instead.  Required.  The public key must match the public key of
     *                     the existing key pair.
     * @throws InvalidKeyException if the public key does not match the public key of the existing key pair
     * @throws KeyStoreException  if there is a problem reading or writing the keystore
     */
    void replaceCertificate(String alias, X509Certificate certificate) throws InvalidKeyException, KeyStoreException;

    /**
     * Add an entry to this key store.  Any previous entry with this alias will be overwritten.  However, due to
     * key caching, callers should not count on this taking effect immediately.  Instead, it is recommended to
     * save changed keys under a new alias.
     * @param entry             The entry to store.  Required.  Must contain a non-null alias that does not match
     *                          the alias of any existing entry.  Must also contain the private key.
     * @throws KeyStoreException  if there is a problem storing this entry
     */
    void storePrivateKeyEntry(SsgKeyEntry entry) throws KeyStoreException;

    /**
     * Delete an entry from this key store.
     *
     * @param keyAlias   the alias of the entry to delete.  Required.
     * @throws KeyStoreException  if there is a problem deleting this entry
     */
    void deletePrivateKeyEntry(String keyAlias) throws KeyStoreException;

    /**
     * Generate a new PKCS#10 certificate request for the key pair specified by its alias, using a certificate with a DN
     * in the form "CN=username".
     *
     * @param alias thye alias of the key pair whose public key to embed in the CSR and whose private key to use to sign it.  Required.
     * @param dn  DN to use in the CSR.  Must contain valid X.509 fields.  Required.
     * @return a CertificateRequest that can be exported as bytes and sent to a CA service.  Never null.
     * @throws InvalidKeyException  if the key cannot be used for this purpose
     * @throws SignatureException   if there was a problem signing the CSR
     * @throws java.security.KeyStoreException  if there is a problem reading the key store
     */
    CertificateRequest makeCertificateSigningRequest(String alias, LdapName dn) throws InvalidKeyException, SignatureException, KeyStoreException;
}
