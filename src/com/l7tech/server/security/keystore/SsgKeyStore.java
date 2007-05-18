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
     * Add an entry to this key store.  Any previous entry with this alias will be overwritten.  However, due to
     * key caching, callers should not count on this taking effect immediately.  Instead, it is recommended to
     * save changed keys under a new alias.
     * @param entry             The entry to store.  Required.  Must contain a non-null alias that does not match
     *                          the alias of any existing entry.  Must also contain the private key.
     * @throws KeyStoreException  if there is a problem storing this entry
     */
    void storePrivateKeyEntry(SsgKeyEntry entry) throws KeyStoreException;

    /**
     * Generate a new RSA KeyPair whose PrivateKey will be suitable for storage into this key store
     * using storePrivateKeyEntry.
     *
     * @param keyBits  number of bits, or zero to default to 1024 bit RSA.
     * @return a new RSA key pair, whose private key may be locked within the hardware if this is a hardware key store.
     * @throws InvalidAlgorithmParameterException if there is a problem generating a new RSA key pair with this key size.
     */
    KeyPair generateRsaKeyPair(int keyBits) throws InvalidAlgorithmParameterException, KeyStoreException;

    /**
     * Generate a new PKCS#10 certificate request for the specified key pair, using a certificate with a DN
     * in the form "CN=username".
     *
     * @param dn  DN to use in the CSR.  Must contain valid X.509 fields.  Required.
     * @param keyPair key pair whose public key to embed in the CSR and whose private key to use to sign it.  Required.
     * @return a CertificateRequest that can be exported as bytes and sent to a CA service.  Never null.
     * @throws InvalidKeyException  if the key cannot be used for this purpose
     * @throws SignatureException   if there was a problem signing the CSR
     */
    CertificateRequest makeCsr(LdapName dn, KeyPair keyPair) throws InvalidKeyException, SignatureException, KeyStoreException;
}
