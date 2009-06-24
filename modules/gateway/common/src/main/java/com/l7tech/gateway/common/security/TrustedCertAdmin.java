/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security;

import com.l7tech.common.io.AliasNotFoundException;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import static com.l7tech.objectmodel.EntityType.*;
import com.l7tech.security.cert.TrustedCert;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

/**
 * Remote interface to get/save/delete certs trusted by the gateway.
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
@Administrative
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
    @Secured(stereotype=FIND_ENTITY)
    public TrustedCert findCertByPrimaryKey(long oid) throws FindException;

    /**
     * Retrieves every {@link com.l7tech.security.cert.TrustedCert} with the specified subject DN.
     *
     * @param dn the Subject DN of the {@link com.l7tech.security.cert.TrustedCert} to retrieve
     * @return a list of matching TrustedCert instances.  May be empty but never null.
     * @throws FindException if there is a problem finding the certs
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    public Collection<TrustedCert> findCertsBySubjectDn(String dn) throws FindException;

    /**
     * Saves a new or existing {@link com.l7tech.security.cert.TrustedCert} to the database.
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
     * @throws ConstraintViolationException if the {@link TrustedCert} cannot be deleted
     */
    @Secured(stereotype= DELETE_BY_ID)
    public void deleteCert(long oid) throws FindException, DeleteException, ConstraintViolationException;

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
    @Secured(types=REVOCATION_CHECK_POLICY, stereotype=FIND_ENTITY)
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
     * @throws ConstraintViolationException if the {@link RevocationCheckPolicy} cannot be deleted
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
    public List<KeystoreFileEntityHeader> findAllKeystores(boolean includeHardware) throws IOException, FindException, KeyStoreException;

    /**
     * Retrieves all SsgKeyEntry instances available in the specified keystore.
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
     * Find a key entry using the rules assertions and connectors would follow.
     *
     * @param keyAlias  key alias to find, or null to find default SSL key.
     * @param preferredKeystoreOid preferred keystore OID to look in, or -1 to search all keystores (if permitted).
     * @return the requested private key, or null if it wasn't found.
     * @throws FindException if there is a database problem (other than ObjectNotFoundException)
     * @throws KeyStoreException if there is a problem reading a keystore
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    @Secured(stereotype=FIND_ENTITY, types=SSG_KEY_ENTRY)
    public SsgKeyEntry findKeyEntry(String keyAlias, long preferredKeystoreOid) throws FindException, KeyStoreException;

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
     * @param makeCaCert    true if the new certificate is intended to be used to sign other certs.  Normally false.
     *                      If this is true, the new certificate will have the "cA" basic constraint and the "keyCertSign" key usage.
     * @param sigAlg signature algorithm for the initial self-signed cert, ie "SHA1withRSA", or null to select one automatically.
     * @return the job identifier of the key generation job.  Call {@link #getJobStatus(com.l7tech.gateway.common.AsyncAdminMethods.JobId) getJobStatus} to poll for job completion
     *         and {@link #getJobResult(JobId)} to pick up the result in the form of a self-signed X509Certificate.
     * @throws FindException if there is a problem getting info from the database
     * @throws java.security.GeneralSecurityException if there is a problem generating or signing the cert
     * @throws IllegalArgumentException if the keybits or dn are improperly specified
     */
    @Transactional(propagation=Propagation.REQUIRED)
    @Secured(stereotype=SET_PROPERTY_BY_UNIQUE_ATTRIBUTE, types=SSG_KEY_ENTRY)
    JobId<X509Certificate> generateKeyPair(long keystoreId, String alias, String dn, int keybits, int expiryDays, boolean makeCaCert, String sigAlg) throws FindException, GeneralSecurityException;

    /**
     * Generate a new ECC key pair and self-signed certificate in the specified keystore with the specified
     * settings.
     *
     * @param keystoreId the key store in which to create the new key pair and self-signed cert.
     * @param alias the alias to use when saving the new key pair and self-signed cert.  Required.
     * @param dn the DN to use in the new self-signed cert.  Required.
     * @param curveName the named curve to set the ECC parameters.  Required.
     * @param expiryDays number of days the self-signed cert should be valid.  Required.
     * @param makeCaCert    true if the new certificate is intended to be used to sign other certs.  Normally false.
     *                      If this is true, the new certificate will have the "cA" basic constraint and the "keyCertSign" key usage.
     * @param sigAlg signature algorithm for the initial self-signed cert, ie "SHA384withECDSA", or null to select one automatically.
     * @return the job identifier of the key generation job.  Call {@link #getJobStatus} to poll for job completion
     *         and {@link #getJobResult(JobId)} to pick up the result in the form of a self-signed X509Certificate.
     * @throws FindException if there is a problem getting info from the database
     * @throws java.security.GeneralSecurityException if there is a problem generating or signing the cert
     * @throws IllegalArgumentException if the curveName is unrecognized or if the dn is improperly specified
     */
    @Transactional(propagation=Propagation.REQUIRED)
    @Secured(stereotype=SET_PROPERTY_BY_UNIQUE_ATTRIBUTE, types=SSG_KEY_ENTRY)
    JobId<X509Certificate> generateEcKeyPair(long keystoreId, String alias, String dn, String curveName, int expiryDays, boolean makeCaCert, String sigAlg) throws FindException, GeneralSecurityException;

    /**
     * Generate a new PKCS#10 Certification Request (aka Certificate Signing Request) using the specified private key,
     * identified by keystoreId and alias, and containing the specified DN.
     *
     * @param keystoreId the ID of the key store in which the private key can be found.  Required.
     * @param alias the alias of the private key with which to sign the CSR.  Required.
     * @param dn the DN to include in the new CSR, ie "CN=mymachine.example.com".  Required.
     * @param sigAlg signature algorithm for the CSR, ie "SHA1withRSA", or null to select one automatically based on the private key type.
     * @return the bytes of the encoded form of this certificate request, in PKCS#10 format.
     * @throws FindException if there is a problem getting info from the database
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=SET_PROPERTY_BY_UNIQUE_ATTRIBUTE, types=SSG_KEY_ENTRY)
    byte[] generateCSR(long keystoreId, String alias, String dn, String sigAlg) throws FindException;

    /**
     * Process a PKCS#10 Certificate Signing Request, producing a new signed certificate from it
     * and returning the newly-signed certificate along with the rest of its certificate chain
     * in PEM format.
     * <p/>
     * Caller is responsible for ensuring that the specified CSR ought to be signed.
     *
     * @param keystoreId the ID of the key store in which the CA private key can be found.  Required.
     * @param alias the alias of the private key to use the CA key for processing this CSR.  Required.
     * @param csrBytes binary or PEM encoded PKCS#10 certificate signing request.
     * @param sigAlg signature algorithm, ie "SHA1withRSA", or null to select one automatically based on the CA key type.
     * @return a PEM-encoded certificate chain including the newly-signed cert.  Never null.
     * @throws FindException if there is a problem getting info from the database
     * @throws java.security.GeneralSecurityException if there is a problem signing the specified certificate.
     */
    @Transactional(propagation=Propagation.REQUIRED)
    @Secured(stereotype=FIND_ENTITIES, types=SSG_KEY_ENTRY)
    String[] signCSR(long keystoreId, String alias, byte[] csrBytes, String sigAlg) throws FindException, GeneralSecurityException;

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
    @Secured(stereotype= SET_PROPERTY_BY_UNIQUE_ATTRIBUTE, types=SSG_KEY_ENTRY)
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
    @Secured(stereotype= SET_PROPERTY_BY_UNIQUE_ATTRIBUTE, types=SSG_KEY_ENTRY)
    void importKey(long keystoreId, String alias, String[] pemChain, final byte[] privateKeyPkcs8) throws CertificateException, SaveException, InvalidKeyException;

    /**
     * Import an RSA or ECC private key and certificate chain into the specified keystore ID under the specified alias,
     * from the specified PKCS#12 file (passed in as a byte array), decrypted with the specified passphrase.
     * <p/>
     * If a pkcs12alias is not provided, this method will expect there to be exactly one private key entry
     * in the provided PKCS#12 file.
     * <p/>
     * This method will take care to destroy all of its copies of the passphrase and PKCS#12 bytes after
     * completion, regardless of whether the import succeeds or fails.
     *
     * @param keystoreId   the target keystore ID.  Required.
     * @param alias        the target alias within the keystore.  Required.
     * @param pkcs12bytes  the bytes of the PKCS#12 file.  Required.
     * @param pkcs12pass   the pass phrase for the PKCS#12 file.  Required.
     * @param pkcs12alias  the alias of the key entry within the PKCS#12 file to import, or null to
     *                     import exactly one entry.
     * @return the successfully-imported key entry.  Never null.
     * @throws ObjectNotFoundException if the specified keystore ID does not exist
     * @throws FindException if there is some other problem finding the specified keystore
     * @throws KeyStoreException if there is a problem parsing the specified PKCS#12 file
     * @throws MultipleAliasesException if pkcs12alias is null and there is more than one private key entry
     *                                  in the specified keystore
     * @throws AliasNotFoundException if pkcs12alias is specified, but no key entry with that alias is found in the
     *                                keystore
     * @throws DuplicateObjectException if there is already an entry with alias "alias" in the keystore with ID
     *                                  keystoreId
     * @throws SaveException if there is some other problem saving the imported key entry
     */
    // TODO need an annotation to note that this methods arguments must never be persisted in any debug or audit traces
    @Transactional(propagation=Propagation.REQUIRED)
    @Secured(stereotype= SET_PROPERTY_BY_UNIQUE_ATTRIBUTE, types=SSG_KEY_ENTRY)
    SsgKeyEntry importKeyFromPkcs12(long keystoreId, String alias, byte[] pkcs12bytes, char[] pkcs12pass, String pkcs12alias) throws FindException, SaveException, KeyStoreException, MultipleAliasesException, AliasNotFoundException;

    /**
     * Export a private key and certificate chain as a PKCS#12 file, if the private key is available to be exported.
     *
     * @param keystoreId the ID of the key store of the private key to be exported. Required.
     * @param alias      the alias of the key entry to export.  Required.
     * @param p12alias   the alias to use for the entry in the newly-generated PKCS#12 file, or null to just use the same alias.
     * @param p12passphrase the passphrase to use to encrypt the newly-generated PKCS#12 file.  Required.
     * @return the bytes of a new PKCS#12 file containing the private key and certificate chain, with the alias set to alias,
     *         encrypted with p12passphrase.  Never null.
     * @throws com.l7tech.objectmodel.ObjectNotFoundException if the specified keystore ID does not exist, or if
     *                                                        the specified alias cannot be found
     * @throws FindException if there is a problem getting info from the database
     * @throws KeyStoreException if there is a problem reading the keystore
     * @throws java.security.UnrecoverableKeyException  if the private key for this keystore cannot be exported
     */
    @Transactional(propagation=Propagation.REQUIRED)
    @Secured(stereotype=DELETE_MULTI, types=SSG_KEY_ENTRY)
    byte[] exportKey(long keystoreId, String alias, String p12alias, char[] p12passphrase) throws ObjectNotFoundException, FindException, KeyStoreException, UnrecoverableKeyException;
}
