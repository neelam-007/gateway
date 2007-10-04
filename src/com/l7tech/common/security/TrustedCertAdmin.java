/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security;

import com.l7tech.common.AsyncAdminMethods;
import com.l7tech.common.security.keystore.SsgKeyEntry;
import static com.l7tech.common.security.rbac.EntityType.*;
import com.l7tech.common.security.rbac.MethodStereotype;
import static com.l7tech.common.security.rbac.MethodStereotype.*;
import com.l7tech.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Remote interface to get/save/delete certs trusted by the gateway.
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
@Secured(types=TRUSTED_CERT)
public interface TrustedCertAdmin extends AsyncAdminMethods {
    /**
     * Retrieves all {@link TrustedCert}s from the database.
     * @return a {@link List} of {@link TrustedCert}s
     * @throws FindException if there was a server-side problem accessing the requested information
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    public List<TrustedCert> findAllCerts() throws FindException;

    /**
     * Retrieves the {@link TrustedCert} with the specified oid.
     * @param oid the oid of the {@link TrustedCert} to retrieve
     * @return the TrustedCert or null if no cert for that oid
     * @throws FindException if there was a server-side problem accessing the requested information
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_BY_PRIMARY_KEY)
    public TrustedCert findCertByPrimaryKey(long oid) throws FindException;

    /**
     * Retrieves the {@link TrustedCert} with the specified subject DN.
     * @param dn the Subject DN of the {@link TrustedCert} to retrieve
     * @return the TrustedCert or null if no cert for that oid
     * @throws FindException if there is a problem finding the cert
     */
    @Transactional(readOnly=true)
    @Secured(stereotype= FIND_ENTITY_BY_ATTRIBUTE)
    public TrustedCert findCertBySubjectDn(String dn) throws FindException;

    /**
     * Saves a new or existing {@link TrustedCert} to the database.
     * @param cert the {@link TrustedCert} to be saved
     * @return the object id (oid) of the newly saved cert
     * @throws SaveException if there was a server-side problem saving the cert
     * @throws UpdateException if there was a server-side problem updating the cert
     * @throws VersionException if the updated cert was not up-to-date (updating an old version)
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    public long saveCert(TrustedCert cert) throws SaveException, UpdateException, VersionException;

    /**
     * Removes the specified {@link TrustedCert} from the database.
     * @param oid the oid of the {@link TrustedCert} to be deleted
     * @throws FindException if the {@link TrustedCert} cannot be found
     * @throws DeleteException if the {@link TrustedCert} cannot be deleted
     */
    @Secured(stereotype= DELETE_BY_ID)
    public void deleteCert(long oid) throws FindException, DeleteException;

    /**
     * Retrieves all {@link RevocationCheckPolicy}s from the database.
     *
     * @return a {@link List} of {@link RevocationCheckPolicy RevocationCheckPolicies}
     * @throws FindException if there was a server-side problem accessing the requested information
     */
    @Transactional(readOnly=true)
    @Secured(types=REVOCATION_CHECK_POLICY,stereotype=FIND_ENTITIES)
    public List<RevocationCheckPolicy> findAllRevocationCheckPolicies() throws FindException;

    /**
     * Retrieves the {@link RevocationCheckPolicy} with the specified oid.
     *
     * @param oid the oid of the {@link RevocationCheckPolicy} to retrieve
     * @return the RevocationCheckPolicy or null if no policy exists with the given oid
     * @throws FindException if there was a server-side problem accessing the requested information
     */
    @Transactional(readOnly=true)
    @Secured(types=REVOCATION_CHECK_POLICY,stereotype=FIND_BY_PRIMARY_KEY)
    public RevocationCheckPolicy findRevocationCheckPolicyByPrimaryKey(long oid) throws FindException;

    /**
     * Saves a new or existing {@link RevocationCheckPolicy} to the database.
     *
     * <p>If the given policy is flagged as the default policy then all other, policies are
     * updated so they are not the default.</p>
     *
     * @param revocationCheckPolicy the {@link RevocationCheckPolicy} to be saved
     * @return the object id (oid) of the newly saved policy
     * @throws SaveException if there was a server-side problem saving the policy
     * @throws UpdateException if there was a server-side problem updating the policy
     * @throws VersionException if the updated policy was not up-to-date (updating an old version)
     */
    @Secured(types=REVOCATION_CHECK_POLICY,stereotype=SAVE_OR_UPDATE)
    public long saveRevocationCheckPolicy(RevocationCheckPolicy revocationCheckPolicy) throws SaveException, UpdateException, VersionException;

    /**
     * Removes the specified {@link RevocationCheckPolicy} from the database.
     *
     * @param oid the oid of the {@link RevocationCheckPolicy} to be deleted
     * @throws FindException if the {@link RevocationCheckPolicy} cannot be found
     * @throws DeleteException if the {@link RevocationCheckPolicy} cannot be deleted
     */
    @Secured(types=REVOCATION_CHECK_POLICY,stereotype= DELETE_BY_ID)
    public void deleteRevocationCheckPolicy(long oid) throws FindException, DeleteException, ConstraintViolationException;


    public static class HostnameMismatchException extends Exception {
        public HostnameMismatchException(String certName, String hostname) {
            super("SSL Certificate with DN '" + certName + "' does not match the expected hostname '" + hostname + "'");
        }
    }

    /**
     * Retrieves the {@link X509Certificate} chain from the specified URL.
     * @param url the url from which to retrieve the cert.
     * @return an {@link X509Certificate} chain.
     * @throws IOException if the certificate cannot be retrieved for whatever reason.
     * @throws IllegalArgumentException if the URL does not start with "https://"
     * @throws HostnameMismatchException if the hostname did not match the cert's subject
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    public X509Certificate[] retrieveCertFromUrl(String url) throws IOException, HostnameMismatchException;

    /**
     * Retrieves the {@link X509Certificate} chain from the specified URL.
     * @param url the url from which to retrieve the cert.
     * @param ignoreHostname whether or not the hostname match should be ignored when doing ssl handshake
     * @return an {@link X509Certificate} chain.
     * @throws IOException if the certificate cannot be retrieved for whatever reason.
     * @throws HostnameMismatchException if the hostname did not match the cert's subject
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    public X509Certificate[] retrieveCertFromUrl(String url, boolean ignoreHostname) throws IOException, HostnameMismatchException;

    /**
     * Get the gateway's root cert.
     * @return the gateway's root cert
     * @throws IOException if the certificate cannot be retrieved
     * @throws CertificateException if the certificate cannot be retrieved
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    public X509Certificate getSSGRootCert() throws IOException, CertificateException;

    /**
     * Get the gateway's SSL cert.
     * @return the gateway's SSL cert
     * @throws IOException if the certificate cannot be retrieved
     * @throws CertificateException if the certificate cannot be retrieved
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    public X509Certificate getSSGSslCert() throws IOException, CertificateException;

    /**
     * Represents general information about a Keystore instance available on this Gateway.
     */
    public static class KeystoreInfo implements Serializable {
        private static final long serialVersionUID = 2340872398471981L;
        public final long id;
        public final String name;
        public final String type;
        public final boolean readonly;

        public KeystoreInfo(long id, String name, String type, boolean readonly) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.readonly = readonly;
        }
    }

    /**
     * Find all keystore instances available on this Gateway.
     *
     * @param includeHardware   whether to include hardware keystores
     * @return a List of KeystoreInfo.  Always contains at least one keystore, although it may be read-only.
     * @throws IOException if there is a problem reading necessary keystore data
     * @throws FindException if there is a problem getting info from the database
     * @throws java.security.KeyStoreException if a keystore is corrupt
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    @Secured(stereotype=FIND_ENTITIES)
    public List<KeystoreInfo> findAllKeystores(boolean includeHardware) throws IOException, FindException, KeyStoreException;

    /**
     * Retrieves all SsgKeyEntry instances available on this Gateway node.
     *
     * @param keystoreId the key store in which to find the key entries.
     * @return a List of SsgKeyEntry.  May be empty but never null.
     * @throws IOException if there is a problem reading necessary keystore data
     * @throws CertificateException if the keystore contents are corrupt
     * @throws FindException if there is a problem getting info from the database
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    @Secured(stereotype=FIND_ENTITIES, types=SSG_KEY_ENTRY)
    public List<SsgKeyEntry> findAllKeys(long keystoreId) throws IOException, CertificateException, FindException;


    /**
     * Destroys an SsgKeyEntry identified by its keystore ID and entry alias.
     *
     * @param keystoreId  the keystore in which to destroy an entry.  Required.
     * @param keyAlias    the alias of hte entry which is to be destroyed.  Required.
     * @throws com.l7tech.objectmodel.DeleteException  if the key cannot be found or there is a problem deleting the key
     * @throws java.io.IOException if there is a problem reading necessary keystore data
     * @throws java.security.cert.CertificateException if there is a problem decoding a certificate
     */
    @Transactional(propagation=Propagation.REQUIRED)
    @Secured(stereotype=DELETE_MULTI, types=SSG_KEY_ENTRY)
    void deleteKey(long keystoreId, String keyAlias) throws IOException, CertificateException, DeleteException;

    /**
     * Generate a new RSA key pair and self-signed certificate in the specified keystore with the specified
     * settings.
     *
     * @param keystoreId the key store in which to create the new key pair and self-signed cert.
     * @param alias the alias to use when saving the new key pair and self-signed cert.  Required.
     * @param dn the DN to use in the new self-signed cert.  Required.
     * @param keybits number of bits for the new RSA key, ie 512, 768, 1024 or 2048.  Required.
     * @param expiryDays number of days the self-signed cert should be valid.  Required.
     * @return the job identifier of the key generation job.  Call {@link #getJobStatus(com.l7tech.common.AsyncAdminMethods.JobId) getJobStatus} to poll for job completion
     *         and {@link #getJobResult(JobId)} to pick up the result in the form of a self-signed X509Certificate.
     * @throws FindException if there is a problem getting info from the database
     * @throws java.security.GeneralSecurityException if there is a problem generating or signing the cert
     * @throws IllegalArgumentException if the keybits or dn are improperly specified
     */
    @Transactional(propagation=Propagation.REQUIRED)
    @Secured(stereotype= MethodStereotype.SET_PROPERTY_BY_UNIQUE_ATTRIBUTE, types=SSG_KEY_ENTRY)
    JobId<X509Certificate> generateKeyPair(long keystoreId, String alias, String dn, int keybits, int expiryDays) throws FindException, GeneralSecurityException;

    /**
     * Generate a new PKCS#10 Certification Request (aka Certificate Signing Request) using the specified private key,
     * identified by keystoreId and alias, and containing the specified DN.
     *
     * @param keystoreId the ID of the key store in which the private key can be found.  Required.
     * @param alias the alias of the private key with which to sign the CSR.  Required.
     * @param dn the DN to include in the new CSR, ie "CN=mymachine.example.com".  Required.
     * @return the bytes of the encoded form of this certificate request, in PKCS#10 format.
     * @throws FindException if there is a problem getting info from the database
     */
    @Transactional(readOnly=true)
    byte[] generateCSR(long keystoreId, String alias, String dn) throws FindException;

    /**
     * Replace the certificate chain for the specified private key with a new one whose subject cert
     * uses the same public key.  This can be used to replace a placeholder self-signed cert with the real
     * cert when it arrives back from the certificate authority.
     *
     * @param keystoreId the ID of the key store in which the private key can be found.  Required.
     * @param alias the alias of the private key whose cert chain to replace.  Required.
     * @param pemChain  the new certificate chain to use for this private key.  Must contain at least one cert
     *                  (the subject cert, the first cert in the list).  The new subject cert must contain
     *                  exactly the same public key as the previous subject cert.
     *                  All certificates in the chain must be in X.509 format, and the subject certificate must use
     *                  an RSA public key.
     *
     * @throws UpdateException if there is a problem getting info from the database
     * @throws CertificateException if there is a problem with the PEM chain
     */
    @Transactional(propagation=Propagation.REQUIRED)
    @Secured(stereotype= MethodStereotype.SET_PROPERTY_BY_UNIQUE_ATTRIBUTE, types=SSG_KEY_ENTRY)
    void assignNewCert(long keystoreId, String alias, String[] pemChain) throws UpdateException, CertificateException;

    /**
     * Create a new RSA private key entry in the specified keystore with the specified alias, cert chain,
     * and RSA private key components.
     * <p/>
     * <b>Note:</b> Callers of this method (and implementors, for that matter) must take care not to allow
     * the privateExponent to be persisted, audited, logged, displayed, saved, or even left in memory
     * without being overwritten.
     *
     * @param keystoreId the ID of the key store in which the new private key entry is to be created.  Required.
     * @param alias      the alias that is to be used for the new private key entry.  Required.
     * @param pemChain   the cert chain to store, with each certificate in PEM-encoded (Base64) format.
     *                   Must contain at least one certificate
     *                   (the subject cert, the first cert in the list).
     *                   All certificates in the chain must be in X.509 format, and the subject certificate must use
     *                   an RSA public key.
     *                   The new subject cert must contain
     *                   the RSA public key corresponding to the RSA private key described by modulus and privateExponent.
     * @param privateKeyPkcs8  the PKCS#8 encoded RSA private key.  Required.
     * @throws CertificateException if there is a problem with the PEM chain
     * @throws InvalidKeyException   if a valid RSA key corresponding to the subject cert private key
     *                                 could not be created from privateKeyPkcs8.
     * @throws SaveException if there is some other problem importing the new private key entry
     */
    // TODO need an annotation to note that this methods arguments must never be persisted in any debug or audit traces
    @Transactional(propagation=Propagation.REQUIRED)
    @Secured(stereotype= MethodStereotype.SET_PROPERTY_BY_UNIQUE_ATTRIBUTE, types=SSG_KEY_ENTRY)
    void importKey(long keystoreId, String alias, String[] pemChain, final byte[] privateKeyPkcs8) throws CertificateException, SaveException, InvalidKeyException;
}
