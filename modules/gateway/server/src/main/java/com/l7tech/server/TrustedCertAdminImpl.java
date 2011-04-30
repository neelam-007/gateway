/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.common.io.AliasNotFoundException;
import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.KeyGenParams;
import com.l7tech.gateway.common.AsyncAdminMethodsImpl;
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
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.spring.remoting.RemoteUtils;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.*;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.prov.RsaSignerEngine;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.event.EntityChangeSet;
import com.l7tech.server.event.admin.Created;
import com.l7tech.server.event.admin.Deleted;
import com.l7tech.server.event.admin.KeyExportedEvent;
import com.l7tech.server.event.admin.Updated;
import com.l7tech.server.identity.cert.RevocationCheckPolicyManager;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.util.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TrustedCertAdminImpl extends AsyncAdminMethodsImpl implements ApplicationEventPublisherAware, TrustedCertAdmin {

    /**
     * Provider for parsing PKCS#12 files when someone calls {@link #importKeyFromPkcs12(long, String, byte[], char[], String)}.  Values:
     *   "default" to use the system current most-preferred implementation of KeyStore.PKCS12;  "BC" to use
     *   Bouncy Castle's implementation (note that Bouncy Castle need not be registered as a Security provider
     *   for this to work); or else the name of any registered Security provider that offers KeyStore.PKCS12.
     */
    public static final String PROP_PKCS12_PARSING_PROVIDER = "com.l7tech.keyStore.pkcs12.parsing.provider";

    private final DefaultKey defaultKey;
    private final LicenseManager licenseManager;
    private final SsgKeyStoreManager ssgKeyStoreManager;
    private final SecurePasswordManager securePasswordManager;
    private final ClusterPropertyManager clusterPropertyManager;
    private ApplicationEventPublisher applicationEventPublisher;

    public TrustedCertAdminImpl(TrustedCertManager trustedCertManager,
                                RevocationCheckPolicyManager revocationCheckPolicyManager,
                                DefaultKey defaultKey,
                                LicenseManager licenseManager,
                                SsgKeyStoreManager ssgKeyStoreManager,
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
    }

    private void checkLicenseHeavy() {
        try {
            licenseManager.requireFeature(GatewayFeatureSets.SERVICE_TRUSTSTORE);
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
        return getManager().findBySubjectDn( CertUtils.formatDN(dn) );
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
        try {
            return keyAlias == null ? defaultKey.getSslInfo() : ssgKeyStoreManager.lookupKeyByKeyAlias(keyAlias, preferredKeystoreOid);
        } catch (IOException e) {
            // No default SSL key
            return null;
        } catch (ObjectNotFoundException e) {
            return null;
        }
    }

    /**
     * Check if this thread is processing an admin request that arrived over an SSL connector that appears
     * to be using the specified private key as its SSL server cert.
     * <p/>
     * This method will return false if one of the following is true:
     * <ul>
     * <li> this thread has no active servlet request
     * <li> no active connector can be found for this thread's active servlet request
     * <li> the active connector explicitly specifies a key alias that does not match the specified keyAlias
     * <li> the active connector explicitly specifies a keystore OID that does not match the specified store's OID
     * <li> the specified store does not contain the specified keyAlias
     * <li> the specified key entry's certificate does not exactly match the active connector's SSL server cert
     * <li> there is a database problem checking any of the above information
     * </ul>
     * <p/>
     * Otherwise, this method returns true.
     *
     * @param store  the keystore in which to find the alias.  Required.
     * @param keyAlias  the alias to find.  Required.
     * @return true if the specified key appears to be in use by the current admin connection.
     * @throws KeyStoreException if there is a problem reading a keystore
     */
    boolean isKeyActive(SsgKeyFinder store, String keyAlias) throws KeyStoreException {
        HttpServletRequest req = RemoteUtils.getHttpServletRequest();
        if (null == req)
            return false;
        SsgConnector connector = HttpTransportModule.getConnector(req);
        if (null == connector)
            return false;
        if (connector.getKeyAlias() != null && !keyAlias.equalsIgnoreCase(connector.getKeyAlias()))
            return false;
        Long portStoreOid = connector.getKeystoreOid();
        if (portStoreOid != null && portStoreOid != store.getOid())
            return false;

        final SsgKeyEntry entry;
        try {
            entry = store.getCertificateChain(keyAlias);
        } catch (ObjectNotFoundException e) {
            return false;
        }

        try {
            SsgKeyEntry portEntry = findKeyEntry(connector.getKeyAlias(), portStoreOid != null ? portStoreOid : -1);
            return CertUtils.certsAreEqual(portEntry.getCertificate(), entry.getCertificate());
        } catch (FindException e) {
            return false;
        }
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

            if (isKeyActive(store, keyAlias))
                throw new DeleteException("Key '" + keyAlias + "' is in use by the connector for current admin connection");

            Future<Boolean> result = store.deletePrivateKeyEntry(auditAfterDelete(store, keyAlias), keyAlias);
            // Force it to be synchronous (Bug #3852)
            result.get();

        } catch (KeyStoreException e) {
            throw new CertificateException(e);
        } catch (FindException e) {
            throw new DeleteException("Unable to find keystore: " + ExceptionUtils.getMessage(e), e);
        } catch (ExecutionException e) {
            throw new DeleteException("Unable to delete key: " + ExceptionUtils.getMessage(e), e);
        } catch (InterruptedException e) {
            throw new DeleteException("Unable to find keystore: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public JobId<X509Certificate> generateKeyPair(long keystoreId, String alias, X500Principal dn, int keybits, int expiryDays, boolean makeCaCert, String sigAlg) throws FindException, GeneralSecurityException {
        SsgKeyStore keystore = checkBeforeGenerate(keystoreId, alias, dn, expiryDays);
        return registerJob(keystore.generateKeyPair(auditAfterCreate(keystore, alias, "generated"),
                alias, new KeyGenParams(keybits), new CertGenParams(dn, expiryDays, makeCaCert, sigAlg)), X509Certificate.class);
    }

    @Override
    public JobId<X509Certificate> generateEcKeyPair(long keystoreId, String alias, X500Principal dn, String curveName, int expiryDays, boolean makeCaCert, String sigAlg) throws FindException, GeneralSecurityException {
        SsgKeyStore keystore = checkBeforeGenerate(keystoreId, alias, dn, expiryDays);
        return registerJob(keystore.generateKeyPair(auditAfterCreate(keystore, alias, "generated"),
                alias, new KeyGenParams(curveName), new CertGenParams(dn, expiryDays, makeCaCert, sigAlg)), X509Certificate.class);
    }

    private SsgKeyStore checkBeforeGenerate(long keystoreId, String alias, X500Principal dn, int expiryDays) throws FindException, KeyStoreException {
        checkLicenseKeyStore();
        if (alias == null) throw new NullPointerException("alias is null");
        if (alias.length() < 1) throw new IllegalArgumentException("alias is empty");
        if (dn == null) throw new NullPointerException("dn is null");

        // Ensure that Sun certificate parser will like this dn
        new X500Principal(dn.getName(X500Principal.CANONICAL)).getEncoded();

        SsgKeyFinder keyFinder;
        try {
            keyFinder = ssgKeyStoreManager.findByPrimaryKey(keystoreId);
        } catch (ObjectNotFoundException e) {
            throw new FindException("No keystore found with id " + keystoreId);
        }
        SsgKeyStore keystore = null;
        if (keyFinder != null && keyFinder.isMutable())
            keystore = keyFinder.getKeyStore();
        if (keystore == null)
            throw new FindException("Keystore with id " + keystoreId + " is not mutable");
        if (expiryDays < 1)
            throw new IllegalArgumentException("expiryDays must be positive");
        return keystore;
    }

    @Override
    public byte[] generateCSR(long keystoreId, String alias, X500Principal dn, String sigAlg) throws FindException {
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
            CertificateRequest res = keystore.makeCertificateSigningRequest(alias, new CertGenParams(dn, 365 * 2, false, sigAlg));
            return res.getEncoded();
        } catch (Exception e) {
            logger.log(Level.WARNING, "error getting keystore", e);
            throw new FindException("error making CSR", e);
        }
    }

    @Override
    public String[] signCSR(long keystoreId, String alias, byte[] csrBytes, String sigAlg) throws FindException, GeneralSecurityException {
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
            cert = (X509Certificate) signer.createCertificate(decodedCsrBytes, new CertGenParams(null, 365 * 2, false, sigAlg).useUserCertDefaults());
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
    public void assignNewCert(long keystoreId, String alias, String[] pemChain) throws UpdateException, CertificateException {
        checkLicenseKeyStore();
        X509Certificate[] safeChain = CertUtils.parsePemChain(pemChain);

        SsgKeyFinder keyFinder;
        try {
            keyFinder = ssgKeyStoreManager.findByPrimaryKey(keystoreId);
        } catch (KeyStoreException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.INFO, "error getting keystore to set new cert: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw new UpdateException("Error getting keystore: " + ExceptionUtils.getMessage(e), e);
        } catch (FindException e) {
            throw new UpdateException("Error getting keystore: " + ExceptionUtils.getMessage(e), e);
        }

        SsgKeyStore keystore = keyFinder.getKeyStore();
        if (keystore == null)
            throw new UpdateException("error: keystore ID " + keystoreId + " is read-only");

        try {
            Future<Boolean> future = keystore.replaceCertificateChain(auditAfterUpdate(keystore, alias, "certificateChain", "replaced"), alias, safeChain);
            // Force it to be synchronous (Bug #3852)
            future.get();
        } catch (Exception e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.INFO, "error setting new cert: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw new UpdateException("Error setting new cert: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private KeyStore createKeyStoreForParsingPkcs12() throws KeyStoreException, NoSuchProviderException {
        String p = SyspropUtil.getString(PROP_PKCS12_PARSING_PROVIDER, "BC");
        if (null == p || p.length() < 1 || p.equalsIgnoreCase("default"))
            return KeyStore.getInstance("PKCS12");
        if ("BC".equalsIgnoreCase(p))
            return KeyStore.getInstance("PKCS12", new BouncyCastleProvider());
        return KeyStore.getInstance("PKCS12", p);
    }

    @Override
    public SsgKeyEntry importKeyFromPkcs12(long keystoreId, String alias, byte[] pkcs12bytes, char[] pkcs12pass, String pkcs12alias)
            throws FindException, SaveException, KeyStoreException, MultipleAliasesException, AliasNotFoundException
    {
        try {
            return doImportKeyFromPkcs12(keystoreId, alias, pkcs12bytes, pkcs12pass, pkcs12alias);

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
            logger.log(Level.WARNING, "Invalid " + PROP_PKCS12_PARSING_PROVIDER + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw new KeyStoreException(e);
        } finally {
            ArrayUtils.zero(pkcs12bytes);
            ArrayUtils.zero(pkcs12pass);
        }
    }

    private SsgKeyEntry doImportKeyFromPkcs12(long keystoreId, String alias, byte[] pkcs12bytes, char[] pkcs12pass, String pkcs12alias)
            throws KeyStoreException, NoSuchProviderException, IOException, NoSuchAlgorithmException, CertificateException,
            AliasNotFoundException, MultipleAliasesException, UnrecoverableKeyException, SaveException, InterruptedException, ExecutionException, ObjectNotFoundException
    {
        checkLicenseKeyStore();

        KeyStore inks = createKeyStoreForParsingPkcs12();
        inks.load(new ByteArrayInputStream(pkcs12bytes), pkcs12pass);

        if (pkcs12alias == null) {
            List<String> aliases = new ArrayList<String>(Collections.list(inks.aliases()));
            if (aliases.isEmpty())
                throw new AliasNotFoundException("PKCS#12 file contains no private key entries");
            if (aliases.size() > 1) {
                // Retain private keys and filter out those certificates.
                for (Iterator<String> itr = aliases.iterator(); itr.hasNext();) {
                    if (! inks.isKeyEntry(itr.next())) {
                        itr.remove();
                    }
                }
                throw new MultipleAliasesException(aliases.toArray(new String[aliases.size()]));
            }
            pkcs12alias = aliases.iterator().next();
        }

        Certificate[] chain = inks.getCertificateChain(pkcs12alias);
        Key key = inks.getKey(pkcs12alias, pkcs12pass);
        if (chain == null || key == null)
            throw new AliasNotFoundException("alias not found in PKCS#12 file: " + pkcs12alias);

        X509Certificate[] x509chain = CertUtils.asX509CertificateArray(chain);
        if (!(key instanceof PrivateKey))
            throw new KeyStoreException("Key entry is not a PrivateKey: " + key.getClass());

        SsgKeyStore keystore = getKeyStore(keystoreId);
        SsgKeyEntry entry = new SsgKeyEntry(keystore.getOid(), alias, x509chain, (PrivateKey)key);
        Future<Boolean> future = keystore.storePrivateKeyEntry(auditAfterCreate(keystore, alias, "imported"), entry, false);
        if (!future.get())
            throw new KeyStoreException("Import operation returned false"); // can't happen

        return keystore.getCertificateChain(alias);
    }

    private SsgKeyStore getKeyStore(long keystoreId) throws SaveException {
        SsgKeyFinder keyFinder;
        try {
            keyFinder = ssgKeyStoreManager.findByPrimaryKey(keystoreId);
        } catch (KeyStoreException e) {
            throw new SaveException("error getting keystore: " + ExceptionUtils.getMessage(e), e);
        } catch (FindException e) {
            throw new SaveException("error getting keystore: " + ExceptionUtils.getMessage(e), e);
        }

        SsgKeyStore keystore = keyFinder.getKeyStore();
        if (keystore == null)
            throw new SaveException("error: keystore ID " + keystoreId + " is read-only");
        return keystore;
    }

    @Override
    public byte[] exportKey(long keystoreId, String alias, String p12alias, char[] p12passphrase) throws FindException, KeyStoreException, UnrecoverableKeyException {
        checkLicenseKeyStore();

        SsgKeyFinder ks = ssgKeyStoreManager.findByPrimaryKey(keystoreId);
        if (!ks.isKeyExportSupported())
            throw new UnrecoverableKeyException("Key export not available");

        SsgKeyEntry entry = ssgKeyStoreManager.lookupKeyByKeyAlias(alias, keystoreId);
        if (!entry.isPrivateKeyAvailable())
            throw new UnrecoverableKeyException("Private Key for alias " + alias + " cannot be exported.");

        if (p12alias == null)
            p12alias = alias;

        PrivateKey privateKey = entry.getPrivateKey();
        Certificate[] certChain = entry.getCertificateChain();

        PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream();
        KeyStore keystore = KeyStore.getInstance("PKCS12", new BouncyCastleProvider());
        try {
            keystore.load(null, p12passphrase);
            keystore.setKeyEntry(p12alias, privateKey, p12passphrase, certChain);
            keystore.store(baos, p12passphrase);
            applicationEventPublisher.publishEvent(new KeyExportedEvent(this, entry.getKeystoreId(), entry.getAlias(), getSubjectDN(entry)));
            return baos.toByteArray();
        } catch (IOException e) {
            throw new KeyStoreException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException(e);
        } catch (CertificateException e) {
            throw new KeyStoreException(e);
        } finally {
            baos.close();
        }
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
        if (keystoreId == -1)
            throw new IllegalArgumentException("A specific keystore ID must be specified.");
        if (!isDefaultKeyMutable(keyType))
            throw new IllegalArgumentException("The " + keyType + " private key cannot be changed on this system.");

        final String clusterPropertyName;
        switch (keyType) {
            case SSL:
                clusterPropertyName = "keyStore.defaultSsl.alias";
                break;

            case CA:
                clusterPropertyName = "keyStore.defaultCa.alias";
                break;

            case AUDIT_VIEWER:
                clusterPropertyName = "keyStore.auditViewer.alias";
                break;

            case AUDIT_SIGNING:
                clusterPropertyName = "keyStore.auditSigning.alias";
                break;

            default:
                throw new IllegalArgumentException("No such keyType: " + keyType);
        }


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
        securePassword.setEncodedPassword("");
        securePassword.setLastUpdate(0);
        return securePasswordManager.save(securePassword);
    }

    private long updateExistingSecurePassword(SecurePassword securePassword) throws FindException, UpdateException {
        // Preserve existing encoded password, ignoring any from client
        final long oid = securePassword.getOid();
        SecurePassword existing = securePasswordManager.findByPrimaryKey(oid);
        if (existing == null) throw new ObjectNotFoundException("No stored password exists with object ID " + oid);
        securePassword.setEncodedPassword(existing.getEncodedPassword());
        securePasswordManager.update(securePassword);
        return oid;
    }

    @Override
    public void setSecurePassword(long securePasswordOid, char[] newPassword) throws FindException, UpdateException {
        SecurePassword existing = securePasswordManager.findByPrimaryKey(securePasswordOid);
        if (existing == null) throw new ObjectNotFoundException();
        existing.setEncodedPassword(securePasswordManager.encryptPassword(newPassword));
        existing.setLastUpdate(System.currentTimeMillis());
        securePasswordManager.update(existing);
    }

    @Override
    public void deleteSecurePassword(long oid) throws DeleteException, FindException {
        securePasswordManager.delete(oid);
    }

    private String getSubjectDN(SsgKeyEntry entry) {
        X509Certificate cert = entry.getCertificate();
        if (cert == null) return null;
        return cert.getSubjectDN().getName();
    }

    private Runnable auditAfterCreate(SsgKeyStore keystore, String alias, String note) {
        return publisher(new Created<SsgKeyEntry>(SsgKeyEntry.createDummyEntityForAuditing(keystore.getOid(), alias), note));
    }

    private Runnable auditAfterUpdate(SsgKeyStore keystore, String alias, String property, String note) {
        EntityChangeSet changeset = new EntityChangeSet(new String[] {property}, new Object[] {new Object()}, new Object[] {new Object()});
        return publisher(new Updated<SsgKeyEntry>(SsgKeyEntry.createDummyEntityForAuditing(keystore.getOid(), alias), changeset, note));
    }

    private Runnable auditAfterDelete(SsgKeyStore keystore, String alias) {
        return publisher(new Deleted<SsgKeyEntry>(SsgKeyEntry.createDummyEntityForAuditing(keystore.getOid(), alias)));
    }

    private Runnable publisher(final ApplicationEvent event) {
        return new CallableRunnable<Object>(AdminInfo.find(true).wrapCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                applicationEventPublisher.publishEvent(event);
                return null;
            }
        }));
    }

    private TrustedCertManager getManager() {
        return trustedCertManager;
    }

    private RevocationCheckPolicyManager getRevocationCheckPolicyManager() {
        return revocationCheckPolicyManager;
    }

    private Logger logger = Logger.getLogger(getClass().getName());
    private final TrustedCertManager trustedCertManager;
    private final RevocationCheckPolicyManager revocationCheckPolicyManager;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}
