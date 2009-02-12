/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.util.BufferPoolByteArrayOutputStream;
import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.common.AsyncAdminMethodsImpl;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.spring.remoting.RemoteUtils;
import com.l7tech.gateway.common.admin.LicenseRuntimeException;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.objectmodel.*;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.prov.bc.BouncyCastleRsaSignerEngine;
import com.l7tech.server.event.admin.KeyExportedEvent;
import com.l7tech.server.identity.cert.RevocationCheckPolicyManager;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SslCertificateSniffer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TrustedCertAdminImpl extends AsyncAdminMethodsImpl implements ApplicationEventPublisherAware, TrustedCertAdmin {
    private final DefaultKey defaultKey;
    private final LicenseManager licenseManager;
    private final SsgKeyStoreManager ssgKeyStoreManager;
    private ApplicationEventPublisher applicationEventPublisher;

    public TrustedCertAdminImpl(TrustedCertManager trustedCertManager,
                                RevocationCheckPolicyManager revocationCheckPolicyManager,
                                DefaultKey defaultKey,
                                LicenseManager licenseManager,
                                SsgKeyStoreManager ssgKeyStoreManager)
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

    public List<TrustedCert> findAllCerts() throws FindException {
        return new ArrayList<TrustedCert>(getManager().findAll());
    }

    public TrustedCert findCertByPrimaryKey(final long oid) throws FindException {
        return getManager().findByPrimaryKey(oid);
    }

    public Collection<TrustedCert> findCertsBySubjectDn(final String dn) throws FindException {
        return getManager().findBySubjectDn(dn);
    }

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

    public void deleteCert(final long oid) throws FindException, DeleteException {
        checkLicenseHeavy();
        getManager().delete(oid);
    }

    public List<RevocationCheckPolicy> findAllRevocationCheckPolicies() throws FindException {
        return new ArrayList<RevocationCheckPolicy>(getRevocationCheckPolicyManager().findAll());
    }

    public RevocationCheckPolicy findRevocationCheckPolicyByPrimaryKey(long oid) throws FindException {
        return getRevocationCheckPolicyManager().findByPrimaryKey(oid);
    }

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

    public void deleteRevocationCheckPolicy(long oid) throws FindException, DeleteException {
        checkLicenseHeavy();
        getRevocationCheckPolicyManager().delete(oid);
    }

    public X509Certificate[] retrieveCertFromUrl(String purl) throws IOException, HostnameMismatchException {
        checkLicenseHeavy();
        return retrieveCertFromUrl(purl, false);
    }

    public X509Certificate[] retrieveCertFromUrl(String purl, boolean ignoreHostname)
      throws IOException, HostnameMismatchException {
        checkLicenseHeavy();
        try {
            return SslCertificateSniffer.retrieveCertFromUrl(purl, ignoreHostname);
        } catch (SslCertificateSniffer.HostnameMismatchException e) {
            throw new HostnameMismatchException(e.getCertname(), e.getHostname());
        }
    }

    public X509Certificate getSSGRootCert() throws IOException, CertificateException {
        SsgKeyEntry caInfo = defaultKey.getCaInfo();
        if (caInfo == null)
            throw new IOException("No default CA certificate is currently designated on this Gateway");
        return caInfo.getCertificate();
    }

    public X509Certificate getSSGSslCert() throws IOException, CertificateException {
        return defaultKey.getSslInfo().getCertificate();
    }

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

    public List<SsgKeyEntry> findAllKeys(long keystoreId) throws IOException, CertificateException, FindException {
        try {
            SsgKeyFinder keyFinder = ssgKeyStoreManager.findByPrimaryKey(keystoreId);

            List<SsgKeyEntry> list = new ArrayList<SsgKeyEntry>();
            List<String> aliases = keyFinder.getAliases();
            for (String alias : aliases) {
                SsgKeyEntry entry = keyFinder.getCertificateChain(alias);
                list.add(entry);
            }

            return list;
        } catch (KeyStoreException e) {
            throw new CertificateException(e);
        } catch (ObjectNotFoundException e) {
            throw new FindException("No keystore found with ID " + keystoreId);
        }
    }

    public void deleteKey(long keystoreId, String keyAlias) throws IOException, CertificateException, DeleteException {
        checkLicenseKeyStore();
        try {
            SsgKeyFinder keyFinder = ssgKeyStoreManager.findByPrimaryKey(keystoreId);
            SsgKeyStore store = keyFinder.getKeyStore();
            if (store == null)
                throw new DeleteException("Unable to delete key: keystore id " + keystoreId + " is read-only");

            //check if key is in use
            if (HttpTransportModule.isKeyActive(RemoteUtils.getHttpServletRequest(), keyAlias))
                throw new DeleteException("Key '" + keyAlias + "' is in use");

            Future<Boolean> result = store.deletePrivateKeyEntry(keyAlias);
            // Force it to be synchronous (Bug #3852)
            result.get();

        } catch (KeyStoreException e) {
            throw new CertificateException(e);
        } catch (FindException e) {
            throw new DeleteException("Unable to find keystore: " + ExceptionUtils.getMessage(e), e);
        } catch (ExecutionException e) {
            throw new DeleteException("Unable to find keystore: " + ExceptionUtils.getMessage(e), e);
        } catch (InterruptedException e) {
            throw new DeleteException("Unable to find keystore: " + ExceptionUtils.getMessage(e), e);
        } 
    }

    public JobId<X509Certificate> generateKeyPair(long keystoreId, String alias, String dn, int keybits, int expiryDays, boolean makeCaCert) throws FindException, GeneralSecurityException {
        checkLicenseKeyStore();
        if (alias == null) throw new NullPointerException("alias is null");
        if (alias.length() < 1) throw new IllegalArgumentException("alias is empty");
        if (dn == null) throw new NullPointerException("dn is null");
        if (dn.length() < 1) throw new IllegalArgumentException("dn is empty");

        // Ensure that Sun certificate parser will like this dn
        new X500Principal(dn).getEncoded();

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

        return registerJob(keystore.generateKeyPair(alias, new X500Principal(dn), keybits, expiryDays, makeCaCert), X509Certificate.class);
    }

    public byte[] generateCSR(long keystoreId, String alias, String dn) throws FindException {
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
            CertificateRequest res = keystore.makeCertificateSigningRequest(alias, dn);
            return res.getEncoded();
        } catch (Exception e) {
            logger.log(Level.WARNING, "error getting keystore", e);
            throw new FindException("error making CSR", e);
        }
    }

    public String[] signCSR(long keystoreId, String alias, byte[] csrBytes) throws FindException, GeneralSecurityException {
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

        BouncyCastleRsaSignerEngine signer =
                new BouncyCastleRsaSignerEngine(entry.getPrivateKey(), entry.getCertificate(), JceProvider.getAsymmetricJceProvider().getName());

        X509Certificate cert;
        try {
            byte[] decodedCsrBytes = CertUtils. csrPemToBinary(csrBytes);            
            cert = (X509Certificate) signer.createCertificate(decodedCsrBytes, null);
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

    public void assignNewCert(long keystoreId, String alias, String[] pemChain) throws UpdateException, CertificateException {
        checkLicenseKeyStore();
        X509Certificate[] safeChain = CertUtils.parsePemChain(pemChain);

        SsgKeyFinder keyFinder;
        try {
            keyFinder = ssgKeyStoreManager.findByPrimaryKey(keystoreId);
        } catch (KeyStoreException e) {
            logger.log(Level.WARNING, "error setting new cert", e);
            throw new UpdateException("error getting keystore", e);
        } catch (FindException e) {
            throw new UpdateException("error getting keystore", e);
        } 

        SsgKeyStore keystore = keyFinder.getKeyStore();
        if (keystore == null)
            throw new UpdateException("error: keystore ID " + keystoreId + " is read-only");

        try {
            Future<Boolean> future = keystore.replaceCertificateChain(alias, safeChain);
            // Force it to be synchronous (Bug #3852)
            future.get();
        } catch (Exception e) {
            logger.log(Level.WARNING, "error setting new cert", e);
            throw new UpdateException("error setting new cert", e);
        }
    }

    public void importKey(long keystoreId, String alias, String[] pemChain, final byte[] privateKeyPkcs8)
            throws SaveException, CertificateException, InvalidKeyException {
        checkLicenseKeyStore();
        X509Certificate[] safeChain = CertUtils.parsePemChain(pemChain);

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

        // Ensure all certs are instances that have come from the default certificate factory
        try {
            PrivateKey rsaPrivateKey = (PrivateKey)KeyFactory.getInstance("RSA").translateKey(new PrivateKey() {
                public String getAlgorithm() {
                    return "RSA";
                }

                public String getFormat() {
                    return "PKCS#8";
                }

                public byte[] getEncoded() {
                    return privateKeyPkcs8;
                }
            });
            SsgKeyEntry entry = new SsgKeyEntry(keystoreId, alias, safeChain, rsaPrivateKey);

            Future<Boolean> result = keystore.storePrivateKeyEntry(entry, false);
            // Force it to be synchronous (Bug #3924)
            result.get();
        } catch (NoSuchAlgorithmException e) {
            throw new SaveException("error setting new cert: " + ExceptionUtils.getMessage(e), e);
        } catch (KeyStoreException e) {
            logger.log(Level.WARNING, "error setting new cert", e);
            throw new SaveException("Error setting new cert: " + ExceptionUtils.getMessage(e), e);
        } catch (ExecutionException e) {
            throw new SaveException("Error setting new cert: " + ExceptionUtils.getMessage(e), e);
        } catch (InterruptedException e) {
            throw new SaveException("Error setting new cert: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public byte[] exportKey(long keystoreId, String alias, String p12alias, char[] p12passphrase) throws ObjectNotFoundException, FindException, KeyStoreException, UnrecoverableKeyException {
        checkLicenseKeyStore();

        SsgKeyEntry entry = ssgKeyStoreManager.lookupKeyByKeyAlias(alias, keystoreId);
        if (!entry.isPrivateKeyAvailable())
            throw new UnrecoverableKeyException("Private Key for alias " + alias + " cannot be exported.");

        if (p12alias == null)
            p12alias = alias;

        PrivateKey privateKey = entry.getPrivateKey();
        Certificate[] certChain = entry.getCertificateChain();        

        BufferPoolByteArrayOutputStream baos = new BufferPoolByteArrayOutputStream();
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

    private String getSubjectDN(SsgKeyEntry entry) {
        X509Certificate cert = entry.getCertificate();
        if (cert == null) return null;
        return cert.getSubjectDN().getName();
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

    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}
