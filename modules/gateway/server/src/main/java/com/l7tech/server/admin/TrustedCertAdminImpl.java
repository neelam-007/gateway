package com.l7tech.server.admin;

import com.l7tech.common.io.*;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.admin.LicenseRuntimeException;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.security.MultipleAliasesException;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.security.SpecialKeyType;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.security.password.SecurePassword.SecurePasswordType;
import com.l7tech.objectmodel.*;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.security.keys.PemUtils;
import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.prov.RsaSignerEngine;
import com.l7tech.security.prov.bc.BouncyCastleRsaSignerEngine;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.identity.cert.RevocationCheckPolicyManager;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyMetadataManager;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.server.event.AdminInfo.find;

public class TrustedCertAdminImpl extends AsyncAdminMethodsImpl implements ApplicationEventPublisherAware, TrustedCertAdmin {

    private final DefaultKey defaultKey;
    private final LicenseManager licenseManager;
    private final SsgKeyStoreManager ssgKeyStoreManager;
    private final SsgKeyMetadataManager ssgKeyMetadataManager;
    private final SecurePasswordManager securePasswordManager;
    private final ClusterPropertyManager clusterPropertyManager;
    private ApplicationEventPublisher applicationEventPublisher;

    public TrustedCertAdminImpl(TrustedCertManager trustedCertManager,
                                RevocationCheckPolicyManager revocationCheckPolicyManager,
                                DefaultKey defaultKey,
                                LicenseManager licenseManager,
                                SsgKeyStoreManager ssgKeyStoreManager,
                                @NotNull SsgKeyMetadataManager ssgKeyMetadataManager,
                                SecurePasswordManager securePasswordManager,
                                ClusterPropertyManager clusterPropertyManager)
    {
        super(120 * 60);
        this.trustedCertManager = trustedCertManager;
        if (trustedCertManager == null) {
            throw new IllegalArgumentException("trusted cert manager is required");
        }
        this.revocationCheckPolicyManager = revocationCheckPolicyManager;
        if (revocationCheckPolicyManager == null) {
            throw new IllegalArgumentException("revocation check policy manager is required");
        }
        this.defaultKey = defaultKey;
        if (defaultKey == null)
            throw new IllegalArgumentException("defaultKey is required");
        this.licenseManager = licenseManager;
        if (licenseManager == null)
            throw new IllegalArgumentException("License manager is required");
        this.ssgKeyStoreManager = ssgKeyStoreManager;
        if (ssgKeyStoreManager == null)
            throw new IllegalArgumentException("SsgKeyStoreManager is required");
        this.securePasswordManager = securePasswordManager;
        if (securePasswordManager == null)
            throw new IllegalArgumentException("securePasswordManager is required");
        this.clusterPropertyManager = clusterPropertyManager;
        if (clusterPropertyManager == null)
            throw new IllegalArgumentException("clusterPropertyManager is required");
        this.ssgKeyMetadataManager = ssgKeyMetadataManager;
    }

    private void checkLicenseHeavy() {
        try {
            licenseManager.requireFeature( GatewayFeatureSets.SERVICE_TRUSTSTORE);
        } catch (LicenseException e) {
            // New exception to conceal original stack trace from LicenseManager
            throw new LicenseRuntimeException(e);
        }
    }

    private void checkLicenseKeyStore() {
        try {
            licenseManager.requireFeature(GatewayFeatureSets.SERVICE_KEYSTORE);
        } catch ( LicenseException e) {
            // New exception to conceal original stack trace from LicenseManager
            throw new LicenseRuntimeException(e);
        }
    }

    @Override
    public List<TrustedCert> findAllCerts() throws FindException {
        return new ArrayList<TrustedCert>(getManager().findAll());
    }

    @Override
    public TrustedCert findCertByPrimaryKey(final long oid) throws FindException {
        return getManager().findByPrimaryKey(oid);
    }

    @Override
    public Collection<TrustedCert> findCertsBySubjectDn(final String dn) throws FindException {
        return getManager().findBySubjectDn(CertUtils.formatDN(dn));
    }

    @Override
    public long saveCert(final TrustedCert cert) throws SaveException, UpdateException {
        checkLicenseHeavy();
        long oid;

        // validate settings
        if (cert.getRevocationCheckPolicyType() == null) {
            cert.setRevocationCheckPolicyType(TrustedCert.PolicyUsageType.USE_DEFAULT);
        } else if (cert.getRevocationCheckPolicyType() == TrustedCert.PolicyUsageType.SPECIFIED &&
                cert.getRevocationCheckPolicyOid()==null) {
            if (cert.getOid() == TrustedCert.DEFAULT_OID) {
                throw new SaveException("A revocation checking policy must be specified for this revocation checking type.");
            } else {
                throw new UpdateException("A revocation checking policy must be specified for this revocation checking type.");
            }
        }

        if (cert.getOid() == TrustedCert.DEFAULT_OID) {
            oid = getManager().save(cert);
        } else {
            getManager().update(cert);
            oid = cert.getOid();
        }
        return oid;
    }

    @Override
    public void deleteCert(final long oid) throws FindException, DeleteException {
        checkLicenseHeavy();
        getManager().delete(oid);
    }

    @Override
    public List<RevocationCheckPolicy> findAllRevocationCheckPolicies() throws FindException {
        return new ArrayList<RevocationCheckPolicy>(getRevocationCheckPolicyManager().findAll());
    }

    @Override
    public RevocationCheckPolicy findRevocationCheckPolicyByPrimaryKey(long oid) throws FindException {
        return getRevocationCheckPolicyManager().findByPrimaryKey(oid);
    }

    @Override
    public long saveRevocationCheckPolicy(RevocationCheckPolicy revocationCheckPolicy) throws SaveException, UpdateException, VersionException {
        checkLicenseHeavy();

        long oid;
        RevocationCheckPolicyManager manager = getRevocationCheckPolicyManager();
        if (revocationCheckPolicy.getOid() == RevocationCheckPolicy.DEFAULT_OID) {
            oid = manager.save(revocationCheckPolicy);
        } else {
            manager.update(revocationCheckPolicy);
            oid = revocationCheckPolicy.getOid();
        }

        return oid;
    }

    @Override
    public void deleteRevocationCheckPolicy(long oid) throws FindException, DeleteException {
        checkLicenseHeavy();
        getRevocationCheckPolicyManager().delete(oid);
    }

    @Override
    public X509Certificate[] retrieveCertFromUrl(String purl) throws IOException, HostnameMismatchException {
        checkLicenseHeavy();
        return retrieveCertFromUrl(purl, false);
    }

    @Override
    public X509Certificate[] retrieveCertFromUrl(String purl, boolean ignoreHostname)
      throws IOException, HostnameMismatchException {
        checkLicenseHeavy();
        try {
            return SslCertificateSniffer.retrieveCertFromUrl(purl, ignoreHostname);
        } catch (SslCertificateSniffer.HostnameMismatchException e) {
            throw new HostnameMismatchException(e.getCertname(), e.getHostname());
        }
    }

    @Override
    public X509Certificate getSSGRootCert() throws IOException, CertificateException {
        SsgKeyEntry caInfo = defaultKey.getCaInfo();
        if (caInfo == null)
            throw new IOException("No default CA certificate is currently designated on this Gateway");
        return caInfo.getCertificate();
    }

    @Override
    public X509Certificate getSSGSslCert() throws IOException, CertificateException {
        return defaultKey.getSslInfo().getCertificate();
    }

    @Override
    public X509Certificate getSSGAuditSigningCert() throws IOException, CertificateException {
        final SsgKeyEntry info = defaultKey.getAuditSigningInfo();
        return info != null ? info.getCertificate() : getSSGSslCert();
    }

    @Override
    public List<KeystoreFileEntityHeader> findAllKeystores(boolean includeHardware) throws IOException, FindException, KeyStoreException {
        List<SsgKeyFinder> finders = ssgKeyStoreManager.findAll();
        List<KeystoreFileEntityHeader> list = new ArrayList<KeystoreFileEntityHeader>();
        for (SsgKeyFinder ssgKeyFinder : finders) {
            if (!includeHardware && ssgKeyFinder.getType() == SsgKeyFinder.SsgKeyStoreType.PKCS11_HARDWARE) {
                continue;   // skip
            }
            list.add(new KeystoreFileEntityHeader(
                    ssgKeyFinder.getOid(),
                    ssgKeyFinder.getName(),
                    ssgKeyFinder.getType().toString(),
                    !ssgKeyFinder.isMutable()));
        }
        return list;
    }

    @Override
    public List<SsgKeyEntry> findAllKeys(long keystoreId, boolean includeRestrictedAccessKeys) throws IOException, CertificateException, FindException {
        try {
            SsgKeyFinder keyFinder = ssgKeyStoreManager.findByPrimaryKey(keystoreId);

            List<SsgKeyEntry> list = new ArrayList<SsgKeyEntry>();
            List<String> aliases = keyFinder.getAliases();
            for (String alias : aliases) {
                SsgKeyEntry entry = keyFinder.getCertificateChain(alias);
                if (includeRestrictedAccessKeys || !entry.isRestrictedAccess())
                    list.add(entry);
            }

            return list;
        } catch (KeyStoreException e) {
            throw new CertificateException(e);
        } catch (ObjectNotFoundException e) {
            throw new FindException("No keystore found with ID " + keystoreId);
        }
    }

    /**
     * Find a key entry using the rules assertions and connectors would follow.
     *
     * @param keyAlias  key alias to find, or null to find default SSL key.
     * @param preferredKeystoreOid preferred keystore OID to look in, or -1 to search all keystores (if permitted).
     * @return the requested private key, or null if it wasn't found.
     * @throws FindException if there is a database problem (other than ObjectNotFoundException)
     * @throws KeyStoreException if there is a problem reading a keystore
     */
    @Override
    public SsgKeyEntry findKeyEntry(String keyAlias, long preferredKeystoreOid) throws FindException, KeyStoreException {
        return getPrivateKeyAdminHelper().doFindKeyEntry( keyAlias, preferredKeystoreOid );
    }

    @Override
    public void deleteKey(long keystoreId, String keyAlias) throws IOException, CertificateException, DeleteException {
        checkLicenseKeyStore();
        if (keyAlias == null) throw new NullPointerException("keyAlias");
        try {
            SsgKeyFinder keyFinder = ssgKeyStoreManager.findByPrimaryKey(keystoreId);
            SsgKeyStore store = keyFinder.getKeyStore();
            if (store == null)
                throw new DeleteException("Unable to delete key: keystore id " + keystoreId + " is read-only");

            final PrivateKeyAdminHelper helper = getPrivateKeyAdminHelper();
            if (helper.isKeyActive( store, keyAlias ))
                throw new DeleteException("Key '" + keyAlias + "' is in use by the connector for current admin connection");

            helper.doDeletePrivateKeyEntry( store, keyAlias );
        } catch (KeyStoreException e) {
            throw new CertificateException(e);
        } catch (FindException e) {
            throw new DeleteException("Unable to find keystore: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public JobId<X509Certificate> generateKeyPair(long keystoreId, String alias, @Nullable SsgKeyMetadata metadata, X500Principal dn, int keybits, int expiryDays, boolean makeCaCert, String sigAlg) throws FindException, GeneralSecurityException {
        checkLicenseKeyStore();
        final PrivateKeyAdminHelper helper = getPrivateKeyAdminHelper();
        return registerJob(helper.doGenerateKeyPair(keystoreId, alias, metadata, dn, new KeyGenParams(keybits), expiryDays, makeCaCert, sigAlg), X509Certificate.class);
    }

    @Override
    public JobId<X509Certificate> generateEcKeyPair(long keystoreId, String alias, @Nullable SsgKeyMetadata metadata, X500Principal dn, String curveName, int expiryDays, boolean makeCaCert, String sigAlg) throws FindException, GeneralSecurityException {
        checkLicenseKeyStore();
        final PrivateKeyAdminHelper helper = getPrivateKeyAdminHelper();
        return registerJob(helper.doGenerateKeyPair(keystoreId, alias, metadata, dn, new KeyGenParams(curveName), expiryDays, makeCaCert, sigAlg), X509Certificate.class);
    }

    private SsgKeyMetadata makeMeta(long keystoreOid, @NotNull String alias, @Nullable SecurityZone securityZone) {
        // Currently metadata is required only if a security zone needs to be set for the new key entry.
        // TODO if we change metadata to store other stuff (like special key purposes) it may need to be set in more situations than just for a non-null security zone.
        return securityZone == null ? null : new SsgKeyMetadata(keystoreOid, alias, securityZone);
    }

    @Override
    public byte[] generateCSR(long keystoreId, String alias, CertGenParams params) throws FindException {
        checkLicenseKeyStore();
        SsgKeyFinder keyFinder;
        try {
            keyFinder = ssgKeyStoreManager.findByPrimaryKey(keystoreId);
        } catch (KeyStoreException e) {
            logger.log(Level.WARNING, "error getting keystore", e);
            throw new FindException("error getting keystore", e);
        } catch (ObjectNotFoundException e) {
            throw new FindException("error getting keystore", e);
        }
        SsgKeyStore keystore;
        if (keyFinder != null) {
            keystore = keyFinder.getKeyStore();
        } else {
            logger.log(Level.WARNING, "error getting keystore");
            throw new FindException("cannot find keystore");
        }
        try {
            CertificateRequest res = keystore.makeCertificateSigningRequest(alias, params);
            return res.getEncoded();
        } catch (Exception e) {
            logger.log(Level.WARNING, "error getting keystore", e);
            throw new FindException("error making CSR", e);
        }
    }

    @Override
    public String[] signCSR(long keystoreId, String alias, byte[] csrBytes, X500Principal subjectDn, int expiryDays, String sigAlg, String hashAlg) throws FindException, GeneralSecurityException {
        checkLicenseKeyStore();
        SsgKeyFinder keyFinder;
        try {
            keyFinder = ssgKeyStoreManager.findByPrimaryKey(keystoreId);
        } catch (KeyStoreException e) {
            logger.log(Level.WARNING, "error getting keystore", e);
            throw new FindException("error getting keystore", e);
        } catch (ObjectNotFoundException e) {
            throw new FindException("error getting keystore", e);
        }
        SsgKeyStore keystore;
        if (keyFinder != null) {
            keystore = keyFinder.getKeyStore();
        } else {
            logger.log(Level.WARNING, "error getting keystore");
            throw new FindException("cannot find keystore");
        }

        SsgKeyEntry entry = keystore.getCertificateChain(alias);

        RsaSignerEngine signer = JceProvider.getInstance().createRsaSignerEngine(entry.getPrivateKey(), entry.getCertificateChain());

        X509Certificate cert;
        try {
            byte[] decodedCsrBytes;
            try {
                decodedCsrBytes = CertUtils.csrPemToBinary(csrBytes);
            } catch (IOException e) {
                // Try as DER
                decodedCsrBytes = csrBytes;
            }
            final CertGenParams certGenParams = new CertGenParams(subjectDn, expiryDays, false, sigAlg);
            // If sigAlg is not specified, then hashAlg will be used to derived the signature algorithm.
            certGenParams.setHashAlgorithm(hashAlg);

            cert = (X509Certificate) signer.createCertificate(decodedCsrBytes, certGenParams.useUserCertDefaults());
        } catch (GeneralSecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new SignatureException("Unable to sign certificate: " + ExceptionUtils.getMessage(e), e);
        }

        X509Certificate[] caChain = entry.getCertificateChain();
        X509Certificate[] retChain = new X509Certificate[caChain.length + 1];
        System.arraycopy(caChain, 0, retChain, 1, caChain.length);
        retChain[0] = cert;

        try {
            String[] pemChain = new String[retChain.length];
            for (int i = 0; i < retChain.length; i++)
                pemChain[i] = CertUtils.encodeAsPEM(retChain[i]);
            return pemChain;

        } catch (IOException e) {
            // Shouldn't be possible
            throw new SignatureException("Unable to sign certificate: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public void updateKeyEntry(@NotNull final SsgKeyEntry keyEntry) throws UpdateException {
        checkLicenseKeyStore();

        // ensure certs are parsed with the configured certificate factory
        // which may not be the same as the default certificate factory used for serialization
        final X509Certificate[] chain = keyEntry.getCertificateChain();
        for (int i = 0; i < chain.length; i++) {
            X509Certificate certificate = keyEntry.getCertificateChain()[i];
            try {
                chain[i] = CertUtils.decodeCert(certificate.getEncoded());
            } catch (final CertificateException e) {
                logger.log( Level.INFO, ExceptionUtils.getMessage( e ), ExceptionUtils.getDebugException( e ) );
                throw new UpdateException("Unable to decode cert", e);
            }
        }

        try {
            getPrivateKeyAdminHelper().doUpdateCertificateChain( keyEntry.getKeystoreId(), keyEntry.getAlias(), keyEntry.getCertificateChain() );
            getPrivateKeyAdminHelper().doUpdateKeyMetadata( keyEntry.getKeystoreId(), keyEntry.getAlias(),
                    makeMeta(keyEntry.getKeystoreId(), keyEntry.getAlias(), keyEntry.getSecurityZone()));
        } catch ( final UpdateException e ) {
            logger.log( Level.INFO, ExceptionUtils.getMessage( e ), ExceptionUtils.getDebugException( e ) );
            throw e;
        }
    }

    @Override
    public SsgKeyEntry importKeyFromKeyStoreFile(long keystoreId,
                                                 String alias,
                                                 @Nullable SsgKeyMetadata metadata,
                                                 byte[] keyStoreBytes,
                                                 String keyStoreType,
                                                 @Nullable char[] keyStorePass,
                                                 @Nullable char[] entryPass,
                                                 String entryAlias)
            throws FindException, SaveException, KeyStoreException, MultipleAliasesException, AliasNotFoundException
    {
        try {
            checkLicenseKeyStore();

            final PrivateKeyAdminHelper helper = getPrivateKeyAdminHelper();
            return helper.doImportKeyFromKeyStoreFile(keystoreId, alias, metadata, keyStoreBytes, keyStoreType, keyStorePass, entryPass, entryAlias);

        } catch (IOException e) {
            throw new KeyStoreException(ExceptionUtils.getMessage( e ), e);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException(e);
        } catch (CertificateException e) {
            throw new KeyStoreException(e);
        } catch (UnrecoverableKeyException e) {
            throw new KeyStoreException(e);
        } catch (ExecutionException e) {
            throw new KeyStoreException(e);
        } catch (InterruptedException e) {
            throw new KeyStoreException(e);
        } catch (NoSuchProviderException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.WARNING, "Invalid " + PrivateKeyAdminHelper.PROP_PKCS12_PARSING_PROVIDER + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw new KeyStoreException(e);
        } finally {
            if (keyStoreBytes != null)
                ArrayUtils.zero(keyStoreBytes);
            if (keyStorePass != null)
                ArrayUtils.zero(keyStorePass);
            if (entryPass != null)
                ArrayUtils.zero(entryPass);
        }
    }

    @Override
    public byte[] exportKey(long keystoreId, String alias, String p12alias, char[] p12passphrase) throws FindException, KeyStoreException, UnrecoverableKeyException {
        checkLicenseKeyStore();

        final PrivateKeyAdminHelper helper = getPrivateKeyAdminHelper();
        return helper.doExportKeyAsPkcs12( keystoreId, alias, p12alias, p12passphrase );
    }

    @Override
    public SsgKeyEntry findDefaultKey(SpecialKeyType keyType) throws KeyStoreException {
        switch (keyType) {
            case SSL:
                try {
                    return defaultKey.getSslInfo();
                } catch (IOException e) {
                    throw new KeyStoreException("Unable to obtain default SSL key: " + ExceptionUtils.getMessage(e), e);
                }

            case CA:
                return defaultKey.getCaInfo();

            case AUDIT_VIEWER:
                return defaultKey.getAuditViewerInfo();

            case AUDIT_SIGNING:
                return defaultKey.getAuditSigningInfo();

            default:
                throw new IllegalArgumentException("No such keyType: " + keyType);
        }
    }

    @Override
    public boolean isDefaultKeyMutable(SpecialKeyType keyType) {
        // We'll assume a designation is mutable unless it was set via its system property (instead of its cluster property)
        switch (keyType) {
            case SSL:
                return "unset".equals(SyspropUtil.getString("com.l7tech.server.keyStore.defaultSsl.alias", "unset"));

            case CA:
                return "unset".equals(SyspropUtil.getString("com.l7tech.server.keyStore.defaultCa.alias", "unset"));

            case AUDIT_VIEWER:
                return "unset".equals(SyspropUtil.getString("com.l7tech.server.keyStore.auditViewer.alias", "unset"));

            case AUDIT_SIGNING:
                return "unset".equals(SyspropUtil.getString("com.l7tech.server.keyStore.auditSigning.alias", "unset"));

            default:
                return false;
        }
    }

    @Override
    public void setDefaultKey(SpecialKeyType keyType, long keystoreId, String alias) throws UpdateException {
        if (keyType == null)
            throw new NullPointerException("A keyType must be specified");
        if (keystoreId == -1L )
            throw new IllegalArgumentException("A specific keystore ID must be specified.");
        if (!isDefaultKeyMutable(keyType))
            throw new IllegalArgumentException("The " + keyType + " private key cannot be changed on this system.");

        final String clusterPropertyName = PrivateKeyAdminHelper.getClusterPropertyForSpecialKeyType(keyType);
        try {
            SsgKeyEntry entry = ssgKeyStoreManager.lookupKeyByKeyAlias(alias, keystoreId);
            String propValue = entry.getKeystoreId() + ":" + entry.getAlias();

            ClusterProperty prop = clusterPropertyManager.findByUniqueName(clusterPropertyName);
            if (prop == null) {
                prop = new ClusterProperty(clusterPropertyName, propValue);
                clusterPropertyManager.save(prop);
            } else {
                prop.setValue(propValue);
                clusterPropertyManager.update(prop);
            }
        } catch (FindException e) {
            throw new UpdateException(e);
        } catch (SaveException e) {
            throw new UpdateException(e);
        } catch (KeyStoreException e) {
            throw new UpdateException(e);
        }
    }

    @Override
    public List<SecurePassword> findAllSecurePasswords() throws FindException {
        List<SecurePassword> ret = new ArrayList<SecurePassword>();
        Collection<SecurePassword> securePasswords = securePasswordManager.findAll();
        for (SecurePassword securePassword : securePasswords) {
            securePassword.setEncodedPassword(null); // blank password before returning
            ret.add(securePassword);
        }
        return ret;
    }

    @Override
    public long saveSecurePassword(SecurePassword securePassword) throws UpdateException, SaveException, FindException {
        if (securePassword.getOid() == SecurePassword.DEFAULT_OID) {
            return saveNewSecurePassword(securePassword);
        } else {
            return updateExistingSecurePassword(securePassword);
        }
    }

    private long saveNewSecurePassword(SecurePassword securePassword) throws SaveException {
        // Set initial placeholder encoded password
        securePassword.setEncodedPassword( "" );
        securePassword.setLastUpdate( 0L );
        return securePasswordManager.save(securePassword);
    }

    private long updateExistingSecurePassword(SecurePassword securePassword) throws FindException, UpdateException {
        // Preserve existing encoded password, ignoring any from client
        final long oid = securePassword.getOid();
        SecurePassword existing = securePasswordManager.findByPrimaryKey(oid);
        if (existing == null) throw new ObjectNotFoundException("No stored password exists with object ID " + oid);
        securePassword.setEncodedPassword( existing.getEncodedPassword() );
        securePasswordManager.update(securePassword);
        return oid;
    }

    @Override
    public void setSecurePassword(long securePasswordOid, char[] newPassword) throws FindException, UpdateException {
        SecurePassword existing = securePasswordManager.findByPrimaryKey(securePasswordOid);
        if (existing == null) throw new ObjectNotFoundException();
        existing.setEncodedPassword(securePasswordManager.encryptPassword(newPassword));
        existing.setLastUpdate(System.currentTimeMillis());
        securePasswordManager.update( existing );
    }

    @Override
    public JobId<Boolean> setGeneratedSecurePassword( final long securePasswordOid,
                                                      final int keybits ) throws FindException, UpdateException  {
        if ( keybits < 512 || keybits > 16384 ) throw new UpdateException("Invalid key size " + keybits);
        // Verify that password exists and is the correct type
        getSecurePasswordOfType( securePasswordOid, SecurePasswordType.PEM_PRIVATE_KEY );

        final FutureTask<Boolean> keyGenerator = new FutureTask<Boolean>( find( false ).wrapCallable( new Callable<Boolean>(){
            @Override
            public Boolean call() throws Exception {
                final KeyPairGenerator generator = KeyPairGenerator.getInstance( "RSA", new BouncyCastleProvider() );
                generator.initialize( keybits, JceProvider.getInstance().getSecureRandom() );
                final SecurePassword existing = getSecurePasswordOfType( securePasswordOid, SecurePasswordType.PEM_PRIVATE_KEY );
                final String pemKey = PemUtils.doWriteKeyPair( generator.genKeyPair().getPrivate() );
                existing.setEncodedPassword(securePasswordManager.encryptPassword(pemKey.toCharArray()));
                existing.setLastUpdate(System.currentTimeMillis());
                securePasswordManager.update( existing );
                return true;
            }
        } ) );

        Background.scheduleOneShot( new TimerTask(){
            @Override
            public void run() {
                keyGenerator.run();
            }
        }, 0L );

        return registerJob( keyGenerator, Boolean.class );
    }

    private SecurePassword getSecurePasswordOfType( final long securePasswordOid, final SecurePasswordType type ) throws FindException, UpdateException {
        final SecurePassword existing = securePasswordManager.findByPrimaryKey(securePasswordOid);
        if (existing == null) throw new ObjectNotFoundException();
        if (existing.getType() != type) throw new UpdateException("Cannot generate password for type");
        return existing;
    }

    @Override
    public String getSecurePasswordPublicKey( final long securePasswordOid ) throws FindException  {
        final SecurePassword existing = securePasswordManager.findByPrimaryKey(securePasswordOid);
        if (existing == null) throw new ObjectNotFoundException();
        if (existing.getType() != SecurePasswordType.PEM_PRIVATE_KEY) throw new FindException("Unexpected password type");

        try {
            final String encodedPassword = existing.getEncodedPassword();
            if ( encodedPassword != null ) {
                final KeyPair keyPair = PemUtils.doReadKeyPair( new String(securePasswordManager.decryptPassword( encodedPassword ) ) );
                return PemUtils.writeKey( keyPair.getPublic(), true );
            }
        } catch ( Exception e ) {
            throw new FindException( "Error accessing key: " + ExceptionUtils.getMessage(e) );
        }

        throw new FindException( "Key not available" );
    }

    @Override
    public void deleteSecurePassword(long oid) throws DeleteException, FindException {
        securePasswordManager.delete(oid);
    }

    @Override
    public Map<String, String> getCsrProperties(byte[] csrBytes) {
        // The details array will store three pieces of information: Subject DN, Public Key Brief Details, Public Key Full Details
        Map<String, String> csrProps = new HashMap<String, String>();

        byte[] decodedCsrBytes;
        try {
            decodedCsrBytes = CertUtils.csrPemToBinary(csrBytes);
        } catch (IOException e) {
            // Try as DER
            decodedCsrBytes = csrBytes;
        }

        PKCS10CertificationRequest pkcs10 = new PKCS10CertificationRequest(decodedCsrBytes);
        CertificationRequestInfo certReqInfo = pkcs10.getCertificationRequestInfo();

        // Subject DN:
        csrProps.put(CSR_PROP_SUBJECT_DN, certReqInfo.getSubject().toString(true, X509Name.DefaultSymbols));

        // Public Key:
        final PublicKey publicKey;
        try {
            publicKey = BouncyCastleRsaSignerEngine.getPublicKey(pkcs10);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to get the public key from the CSR: " + ExceptionUtils.getMessage(e));
            return csrProps;
        }

        if (publicKey instanceof RSAPublicKey) {
            final RSAPublicKey rsa = (RSAPublicKey) publicKey;
            final BigInteger modulus = rsa.getModulus();

            csrProps.put(CSR_PROP_KEY_TYPE, "RSA public key");
            csrProps.put(CSR_PROP_KEY_SIZE, String.valueOf(modulus.bitLength()));
            csrProps.put(CSR_PROP_MODULUS, modulus.toString(16));
            csrProps.put(CSR_PROP_EXPONENT, rsa.getPublicExponent().toString(16));
        } else if (publicKey instanceof ECPublicKey) {
            final ECPublicKey ec = (ECPublicKey) publicKey;
            final ECParameterSpec params = ec.getParams();
            final ECPoint w = ec.getW();
            final String curveName = CertUtils.guessEcCurveName(publicKey);

            csrProps.put(CSR_PROP_KEY_TYPE, "EC public key");
            if (curveName != null) csrProps.put(CSR_PROP_CURVE_NAME, curveName);
            csrProps.put(CSR_PROP_CURVE_SIZE, String.valueOf(params.getCurve().getField().getFieldSize()));
            csrProps.put(CSR_PROP_CURVE_POINT_W_X, w.getAffineX().toString());
            csrProps.put(CSR_PROP_CURVE_POINT_W_Y, w.getAffineY().toString());
        } else {
            csrProps.put(CSR_PROP_KEY_TYPE, publicKey.getAlgorithm());
        }

        return csrProps;
    }

    @Override
    public boolean isShortSigningKey(long keystoreId, String alias) throws FindException, KeyStoreException {
        checkLicenseKeyStore();
        SsgKeyFinder keyFinder;
        try {
            keyFinder = ssgKeyStoreManager.findByPrimaryKey(keystoreId);
        } catch (KeyStoreException e) {
            logger.log(Level.WARNING, "error getting keystore", e);
            throw new FindException("error getting keystore", e);
        } catch (ObjectNotFoundException e) {
            throw new FindException("error getting keystore", e);
        }
        SsgKeyStore keystore;
        if (keyFinder != null) {
            keystore = keyFinder.getKeyStore();
        } else {
            logger.log(Level.WARNING, "error getting keystore");
            throw new FindException("cannot find keystore");
        }

        final SsgKeyEntry entry = keystore.getCertificateChain(alias);
        final X509Certificate cert = entry.getCertificateChain()[0];
        final PublicKey publicKey = cert.getPublicKey();
        return ParamsCertificateGenerator.isShortKey(publicKey);
    }

    private TrustedCertManager getManager() {
        return trustedCertManager;
    }

    private RevocationCheckPolicyManager getRevocationCheckPolicyManager() {
        return revocationCheckPolicyManager;
    }

    /**
     * Override for unit tests.
     */
    PrivateKeyAdminHelper getPrivateKeyAdminHelper() {
        return new PrivateKeyAdminHelper( defaultKey, ssgKeyStoreManager, applicationEventPublisher );
    }

    private Logger logger = Logger.getLogger(getClass().getName());
    private final TrustedCertManager trustedCertManager;
    private final RevocationCheckPolicyManager revocationCheckPolicyManager;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}
