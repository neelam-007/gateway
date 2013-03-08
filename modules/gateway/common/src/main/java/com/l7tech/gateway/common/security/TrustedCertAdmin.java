/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security;

import com.l7tech.common.io.AliasNotFoundException;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import com.l7tech.security.cert.TrustedCert;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.l7tech.gateway.common.security.PrivateKeySecured.PreCheck.*;
import static com.l7tech.gateway.common.security.PrivateKeySecured.ReturnCheck.*;
import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;
import static com.l7tech.objectmodel.EntityType.*;

/**
 * Remote interface to get/save/delete certs trusted by the gateway.
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
@Administrative
@Secured(types=TRUSTED_CERT)
public interface TrustedCertAdmin extends AsyncAdminMethods {
    public static final String CSR_PROP_SUBJECT_DN = "subject.dn";
    public static final String CSR_PROP_KEY_TYPE = "public.key.type";
    public static final String CSR_PROP_KEY_SIZE = "rsa.public.key.size";
    public static final String CSR_PROP_MODULUS = "rsa.public.key.modulus";
    public static final String CSR_PROP_EXPONENT = "rsa.public.key.public.exponent";
    public static final String CSR_PROP_CURVE_NAME = "ec.public.key.curve.name";
    public static final String CSR_PROP_CURVE_SIZE = "ec.public.key.curve.size";
    public static final String CSR_PROP_CURVE_POINT_W_X = "ec.public.key.curve.point.w.x.coord";
    public static final String CSR_PROP_CURVE_POINT_W_Y = "ec.public.key.curve.point.w.y.coord";

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
     * Get the certificate that should be used for verifying signed audit records.
     * <p/>
     * This will be the same as the gateway's SSL cert unless a special audit signing key has been explicitly designated by the administrator.
     *
     * @return the Gateway's audit signing certificate. Never null.
     * @throws IOException if the certificate cannot be retrieved
     * @throws CertificateException if the certificate cannot be retrieved
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    public X509Certificate getSSGAuditSigningCert() throws IOException, CertificateException;

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
    @Secured(stereotype=FIND_HEADERS, types=SSG_KEYSTORE)
    public List<KeystoreFileEntityHeader> findAllKeystores(boolean includeHardware) throws IOException, FindException, KeyStoreException;

    /**
     * Retrieves all SsgKeyEntry instances available in the specified keystore.
     *
     * @param keystoreId the key store in which to find the key entries.
     * @param includeRestrictedAccessKeys true if restricted access keys (eg, the audit viewer private key) should be included in the returned list.
     * @return a List of SsgKeyEntry.  May be empty but never null.
     * @throws IOException if there is a problem reading necessary keystore data
     * @throws CertificateException if the keystore contents are corrupt
     * @throws FindException if there is a problem getting info from the database
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    @Secured(customInterceptor="com.l7tech.server.admin.PrivateKeyRbacInterceptor")
    @PrivateKeySecured(preChecks={NO_PRE_CHECK}, returnCheck=FILTER_RETURN)
    public List<SsgKeyEntry> findAllKeys(long keystoreId, boolean includeRestrictedAccessKeys) throws IOException, CertificateException, FindException;

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
    @Secured(customInterceptor="com.l7tech.server.admin.PrivateKeyRbacInterceptor")
    @PrivateKeySecured(preChecks={NO_PRE_CHECK}, returnCheck=CHECK_RETURN)
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
    @Secured(customInterceptor="com.l7tech.server.admin.PrivateKeyRbacInterceptor")
    @PrivateKeySecured(preChecks={CHECK_ARG_OPERATION}, argOp=OperationType.DELETE)
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
    @Secured(customInterceptor="com.l7tech.server.admin.PrivateKeyRbacInterceptor")
    @PrivateKeySecured(preChecks={CHECK_ARG_OPERATION}, argOp=OperationType.CREATE, returnCheck=NO_RETURN_CHECK)
    JobId<X509Certificate> generateKeyPair(long keystoreId, String alias, X500Principal dn, int keybits, int expiryDays, boolean makeCaCert, String sigAlg) throws FindException, GeneralSecurityException;

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
     * @return the job identifier of the key generation job.  Call {@link #getJobStatus(com.l7tech.gateway.common.AsyncAdminMethods.JobId)} to poll for job completion
     *         and {@link #getJobResult(JobId)} to pick up the result in the form of a self-signed X509Certificate.
     * @throws FindException if there is a problem getting info from the database
     * @throws java.security.GeneralSecurityException if there is a problem generating or signing the cert
     * @throws IllegalArgumentException if the curveName is unrecognized or if the dn is improperly specified
     */
    @Transactional(propagation=Propagation.REQUIRED)
    @Secured(customInterceptor="com.l7tech.server.admin.PrivateKeyRbacInterceptor")
    @PrivateKeySecured(preChecks={CHECK_ARG_OPERATION}, argOp=OperationType.CREATE, returnCheck=NO_RETURN_CHECK)
    JobId<X509Certificate> generateEcKeyPair(long keystoreId, String alias, X500Principal dn, String curveName, int expiryDays, boolean makeCaCert, String sigAlg) throws FindException, GeneralSecurityException;

    /**
     * Generate a new PKCS#10 Certification Request (aka Certificate Signing Request) using the specified private key,
     * identified by keystoreId and alias, and containing the specified DN.
     *
     * @param keystoreId the ID of the key store in which the private key can be found.  Required.
     * @param alias the alias of the private key with which to sign the CSR.  Required.
     * @param dn the DN to include in the new CSR, ie "CN=mymachine.example.com".  Required.
     * @param sigAlg signature algorithm for the CSR, ie "SHA1withRSA", or null to select one automatically based on the private key type.
     * @param sigHash signature hash for the CSR, ie "SHA384", or null to select one automatically. If both the sigAlg and sigHash is specified the sigHash is ignored.
     * @return the bytes of the encoded form of this certificate request, in PKCS#10 format.
     * @throws FindException if there is a problem getting info from the database
     */
    @Transactional(readOnly=true)
    @Secured(customInterceptor="com.l7tech.server.admin.PrivateKeyRbacInterceptor")
    @PrivateKeySecured(preChecks={CHECK_ARG_OPERATION}, argOp=OperationType.READ)
    byte[] generateCSR(long keystoreId, String alias, X500Principal dn, @Nullable String sigAlg, String sigHash) throws FindException;

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
     * @param hashAlg hash algorithm such as SHA-1, SHA-256, SHA-384, or SHA-512.  If sigAlg is missed, then hashAlg plus key algorithm info will be used to derive the signature algorithm.
     * @return a PEM-encoded certificate chain including the newly-signed cert.  Never null.
     * @throws FindException if there is a problem getting info from the database
     * @throws java.security.GeneralSecurityException if there is a problem signing the specified certificate.
     */
    @Transactional(propagation=Propagation.REQUIRED)
    @Secured(customInterceptor="com.l7tech.server.admin.PrivateKeyRbacInterceptor")
    @PrivateKeySecured(preChecks={CHECK_ARG_OPERATION}, argOp=OperationType.READ, returnCheck=NO_RETURN_CHECK)
    String[] signCSR(long keystoreId, String alias, byte[] csrBytes, X500Principal subjectDn, int expiryDays, String sigAlg, String hashAlg) throws FindException, GeneralSecurityException;

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
    @Secured(customInterceptor="com.l7tech.server.admin.PrivateKeyRbacInterceptor")
    @PrivateKeySecured(preChecks={CHECK_ARG_OPERATION}, argOp=OperationType.UPDATE)
    void assignNewCert(long keystoreId, String alias, String[] pemChain) throws UpdateException, CertificateException;

    /**
     * Import an RSA or ECC private key and certificate chain into the specified keystore ID under the specified alias,
     * from the specified PKCS#12 or JKS file (passed in as a byte array), decrypted with the specified passphrase.
     * <p/>
     * If a pkcs12alias is not provided, this method will expect there to be exactly one private key entry
     * in the provided PKCS#12 or JKS file.
     * <p/>
     * This method will take care to destroy all of its copies of the passphrase and keystore bytes after
     * completion, regardless of whether the import succeeds or fails.
     *
     * @param keystoreId   the target keystore ID.  Required.
     * @param alias        the target alias within the keystore.  Required.
     * @param keyStoreBytes  the bytes of the keystore file.  Required.
     * @param keyStoreType   the type of keystore, such as "PKCS12" or "JKS".  Required.
     * @param keyStorePass   the pass phrase for the keystore file.  May be null to pass null as the second argument to KeyStore.load().
     * @param entryPass      the pass phrase to use when retrieving the relevant private key entry.  
     *                          A null value may be provided to pass null as the second argument to KeyStore.getKey().
     * @param entryAlias  the alias of the key entry within the kesytore file to import, or null to
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
    @Secured(customInterceptor="com.l7tech.server.admin.PrivateKeyRbacInterceptor")
    @PrivateKeySecured(preChecks={CHECK_ARG_OPERATION}, argOp=OperationType.CREATE, returnCheck=NO_RETURN_CHECK)
    SsgKeyEntry importKeyFromKeyStoreFile(long keystoreId,
                                          String alias,
                                          byte[] keyStoreBytes,
                                          String keyStoreType,
                                          @Nullable char[] keyStorePass,
                                          @Nullable char[] entryPass,
                                          String entryAlias)
            throws FindException, SaveException, KeyStoreException, MultipleAliasesException, AliasNotFoundException;

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
    @Secured(customInterceptor="com.l7tech.server.admin.PrivateKeyRbacInterceptor")
    @PrivateKeySecured(preChecks={CHECK_ARG_OPERATION, CHECK_ARG_EXPORT_KEY}, argOp=OperationType.READ)
    byte[] exportKey(long keystoreId, String alias, String p12alias, char[] p12passphrase) throws ObjectNotFoundException, FindException, KeyStoreException, UnrecoverableKeyException;


    /**
     * Look up the current default SSL or CA key.
     *
     * @param keyType  whether to find the SSL or CA key.  Required.
     * @return the current default SSL or CA key, or null if there is no key currently designated with the specified keyType.
     * @throws KeyStoreException if there is a problem reading a keystore.
     */
    @Transactional(propagation=Propagation.REQUIRED)
    @Secured(customInterceptor="com.l7tech.server.admin.PrivateKeyRbacInterceptor")
    @PrivateKeySecured(preChecks={NO_PRE_CHECK}, returnCheck=FILTER_RETURN)
    SsgKeyEntry findDefaultKey(SpecialKeyType keyType) throws KeyStoreException;

    /**
     * Check whether the default key can be changed by editing the appropriate cluster property.
     *
     * @param keyType whether to inquire about the SSL or CA key.  Required.
     * @return true if the specified default key type can be changed by editing its cluster property.
     */
    @Transactional(propagation=Propagation.SUPPORTS, readOnly=true)
    @Secured(customInterceptor="com.l7tech.server.admin.PrivateKeyRbacInterceptor")
    @PrivateKeySecured(preChecks={CHECK_UPDATE_ALL_KEYSTORES_NONFATAL}, allowNonFatalPreChecks=true)
    boolean isDefaultKeyMutable(SpecialKeyType keyType);

    /**
     * Change an assigned default key.
     *
     * @param keyType the key type to change.  Required.
     * @param keystoreId the ID of the key store of the private key to be use for the specified role. Required.  Must be a real keystore ID and not a wildcard (-1).
     * @param alias      the alias of the key entry to designate.  Required.  A key with this alias must exist in the specified keystore.
     * @throws com.l7tech.objectmodel.UpdateException if the default key could not be changed.
     */
    @Transactional(propagation=Propagation.REQUIRED)
    @Secured(customInterceptor="com.l7tech.server.admin.PrivateKeyRbacInterceptor")
    @PrivateKeySecured(preChecks={CHECK_ARG_OPERATION, CHECK_UPDATE_ALL_KEYSTORES_NONFATAL}, argOp=OperationType.READ, keystoreOidArg=1, keyAliasArg=2, allowNonFatalPreChecks=false)
    void setDefaultKey(SpecialKeyType keyType, long keystoreId, String alias) throws UpdateException;

    /**
     * Retrieves all {@link SecurePassword} entity headers from the database.
     *
     * @return a {@link List} of {@link EntityHeader} instances, one per SecurePassword.
     * @throws FindException if there was a server-side problem accessing the requested information
     */
    @Transactional(propagation=Propagation.REQUIRED, readOnly=true)
    @Secured(types=SECURE_PASSWORD,stereotype=FIND_ENTITIES)
    List<SecurePassword> findAllSecurePasswords() throws FindException;

    /**
     * Retrieves the public PEM key for PEM private key stored password.
     *
     * @param securePasswordOid the objectid of the SecurePassword to access. Required.
     * @return the PEM encoded public key
     * @throws ObjectNotFoundException If the referenced secure password could not be found.
     * @throws FindException If there was an error accessing the password
     */
    @Transactional(propagation=Propagation.REQUIRED, readOnly=true)
    @Secured(types=SECURE_PASSWORD,stereotype=GET_PROPERTY_BY_ID,relevantArg=0)
    String getSecurePasswordPublicKey( long securePasswordOid ) throws FindException;

    /**
     * Save a new or updated SecurePassword.
     *
     * @param securePassword the new or updated SecurePassword to save.  Required.
     *                       If this has the objectid {@link SecurePassword#DEFAULT_OID}, this will be saved as a new SecurePassword.
     *                       Otherwise, it will attempt to update an existing password.
     *                       Any encodedPassword field present in the securePassword will be ignored.  To set or change
     *                       the actual raw password characters, use the {@link #setSecurePassword(long, char[])} method.
     * @return the objectid of the securePassword instance that was saved or updated.
     * @throws UpdateException if there is a problem updating an existing entity.
     * @throws FindException if there is a problem locating an existing entity in order to update it.
     * @throws SaveException if there is a problem saving a new entity.
     */
    @Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
    @Secured(types=SECURE_PASSWORD,stereotype=SAVE_OR_UPDATE)
    long saveSecurePassword(SecurePassword securePassword) throws UpdateException, SaveException, FindException;

    /**
     * Set or update the password field for the specified SecurePassword objectid.
     * As a side-effect, this will set the lastUpdate timestamp.
     *
     * @param securePasswordOid the objectid of the SecurePassword to modify. Required.
     * @param newPassword the new plaintext password to assign for this SecurePassword instance.  Required.
     * @throws FindException if there is a problem locating the specified secure password.
     * @throws UpdateException if there is a problem updating the secure password.
     */
    // TODO need an annotation to note that this methods arguments must never be persisted in any debug or audit traces
    @Transactional(propagation=Propagation.REQUIRED)
    @Secured(types=SECURE_PASSWORD,stereotype=SET_PROPERTY_BY_ID,relevantArg=0)
    void setSecurePassword(long securePasswordOid, char[] newPassword) throws FindException, UpdateException;

    /**
     * Set or update the password fields for the specified SecurePassword.
     *
     * <p>Generates an RSA key and assigns to the secure password. As a
     * side-effect, this will set the lastUpdate timestamp.</p>
     *
     * @param securePasswordOid the objectid of the SecurePassword to modify. Required.
     * @param keybits The size in bits of the RSA key to generate
     * @return The asynchronous job identifier
     * @throws FindException If the referenced secure password could not be found.
     * @throws UpdateException If the referenced secure password is not of the expected type.
     */
    public JobId<Boolean> setGeneratedSecurePassword( final long securePasswordOid, final int keybits ) throws FindException, UpdateException;

    /**
     * Destroy a SecurePassword instance.
     *
     * @param oid the objectid of the SecurePassword to delete.  Required.
     * @throws FindException if there is a problem locating the specified secure password.
     * @throws ConstraintViolationException if the secure password cannot be deleted because it is in use.
     * @throws DeleteException if there is some other problem deleting the SecurePassword.
     */
    @Transactional(propagation=Propagation.REQUIRED)
    @Secured(types=SECURE_PASSWORD,stereotype=DELETE_BY_ID, relevantArg=0)
    void deleteSecurePassword(long oid) throws ConstraintViolationException, DeleteException, FindException;

    /**
     * Get the properties of a CSR, such as Subject DN and Public Key details (key type, key size, and other parameters).
     *
     * @param csrBytes raw CSR bytes
     * @return a map containing CSR properties.
     */
    Map<String, String> getCsrProperties(byte[] csrBytes);

    /**
     * Check if the private key is too short key for signature algorithm
     * @param keystoreId: the ID of the key store in which the private key can be found.  Required.
     * @param alias: the alias of the private key to use the key for processing this CSR.  Required.
     * @return  true if the private key uses a short key
     * @throws FindException: thrown if there is a problem getting info from the database
     * @throws KeyStoreException: thrown if there is a problem reading the underlying key store.
     */
    @Secured(customInterceptor="com.l7tech.server.admin.PrivateKeyRbacInterceptor")
    @PrivateKeySecured(preChecks={CHECK_ARG_OPERATION}, argOp=OperationType.READ)
    boolean isShortSigningKey(long keystoreId, String alias) throws FindException, KeyStoreException;
}
