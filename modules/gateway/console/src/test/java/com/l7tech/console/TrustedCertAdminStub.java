package com.l7tech.console;

import com.l7tech.common.io.AliasNotFoundException;
import com.l7tech.common.io.CertGenParams;
import com.l7tech.gateway.common.security.MultipleAliasesException;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.security.SpecialKeyType;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.*;
import com.l7tech.security.cert.TrustedCert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 *
 */
public class TrustedCertAdminStub implements TrustedCertAdmin {
    public Map<Goid,SecurePassword> securePasswords = new LinkedHashMap<>();
    public int nextId = 5501;

    @Override
    public List<TrustedCert> findAllCerts() throws FindException {
        return null;
    }

    @Override
    public TrustedCert findCertByPrimaryKey(Goid goid2) throws FindException {
        return null;
    }

    @Override
    public Collection<TrustedCert> findCertsBySubjectDn(String dn) throws FindException {
        return null;
    }

    @Override
    public Goid saveCert(TrustedCert cert) throws SaveException, UpdateException, VersionException {
        return new Goid(0, 0);
    }

    @Override
    public void deleteCert(Goid goid) throws FindException, DeleteException, ConstraintViolationException {
    }

    @Override
    public List<RevocationCheckPolicy> findAllRevocationCheckPolicies() throws FindException {
        return null;
    }

    @Override
    public RevocationCheckPolicy findRevocationCheckPolicyByPrimaryKey(Goid oid) throws FindException {
        return null;
    }

    @Override
    public Goid saveRevocationCheckPolicy(RevocationCheckPolicy revocationCheckPolicy) throws SaveException, UpdateException, VersionException {
        return new Goid(0, 0);
    }

    @Override
    public void deleteRevocationCheckPolicy(Goid oid) throws FindException, DeleteException, ConstraintViolationException {
    }

    @Override
    public X509Certificate[] retrieveCertFromUrl(String url) throws IOException, HostnameMismatchException {
        return new X509Certificate[0];
    }

    @Override
    public X509Certificate[] retrieveCertFromUrl(String url, boolean ignoreHostname) throws IOException, HostnameMismatchException {
        return new X509Certificate[0];
    }

    @Override
    public JobId<X509Certificate[]> retrieveCertFromUrlAsync(String url, boolean ignoreHostname) {
        return null;
    }

    @Override
    public X509Certificate getSSGRootCert() throws IOException, CertificateException {
        return null;
    }

    @Override
    public X509Certificate getSSGSslCert() throws IOException, CertificateException {
        return null;
    }

    @Override
    public X509Certificate getSSGAuditSigningCert() throws IOException, CertificateException {
        return null;
    }

    @Override
    public List<KeystoreFileEntityHeader> findAllKeystores(boolean includeHardware) throws IOException, FindException, KeyStoreException {
        return null;
    }

    @Override
    public List<SsgKeyEntry> findAllKeys(Goid keystoreId, boolean includeRestrictedAccessKeys) throws IOException, CertificateException, FindException {
        return null;
    }

    @Override
    public SsgKeyEntry findKeyEntry(String keyAlias, Goid preferredKeystoreOid) throws FindException, KeyStoreException {
        return null;
    }

    @Override
    public void deleteKey(Goid keystoreId, String keyAlias) throws IOException, CertificateException, DeleteException {
    }

    @Override
    public JobId<X509Certificate> generateKeyPair(Goid keystoreId, String alias, @Nullable SsgKeyMetadata metadata, X500Principal dn, int keybits, int expiryDays, boolean makeCaCert, String sigAlg) throws FindException, GeneralSecurityException {
        return null;
    }

    @Override
    public JobId<X509Certificate> generateEcKeyPair(Goid keystoreId, String alias, @Nullable SsgKeyMetadata metadata, X500Principal dn, String curveName, int expiryDays, boolean makeCaCert, String sigAlg) throws FindException, GeneralSecurityException {
        return null;
    }

    @Override
    public byte[] generateCSR(Goid keystoreId, String alias, CertGenParams params) throws FindException {
        return new byte[0];
    }

    @Override
    public String[] signCSR(Goid keystoreId, String alias, byte[] csrBytes, X500Principal subjectDn, int expiryDays, String sigAlg, String hashAlg) throws FindException, GeneralSecurityException {
        return new String[0];
    }

    @Override
    public void updateKeyEntry(SsgKeyEntry keyEntry) throws UpdateException {
    }

    @Override
    public SsgKeyEntry importKeyFromKeyStoreFile(Goid keystoreId, String alias, @Nullable SsgKeyMetadata metadata, byte[] keyStoreBytes, String keyStoreType, char[] keyStorePass, char[] entryPass, String entryAlias) throws FindException, SaveException, KeyStoreException, MultipleAliasesException, AliasNotFoundException {
        return null;
    }

    @Override
    public byte[] exportKey(Goid keystoreId, String alias, String p12alias, char[] p12passphrase) throws FindException, KeyStoreException, UnrecoverableKeyException {
        return new byte[0];
    }

    @Override
    public SsgKeyEntry findDefaultKey(SpecialKeyType keyType) throws KeyStoreException {
        // not currently implemented in stub
        return null;
    }

    @Override
    public boolean isDefaultKeyMutable(SpecialKeyType keyType) {
        return false;
    }

    @Override
    public void setDefaultKey(SpecialKeyType keyType, Goid keystoreId, String alias) {
        // not currently implemented in stub
    }

    @Override
    public List<SecurePassword> findAllSecurePasswords() throws FindException {
        return new ArrayList<>(securePasswords.values());
    }

    @Override
    public SecurePassword findSecurePasswordById(Goid id) throws FindException {
        return null;
    }

    @Override
    public SecurePassword findSecurePasswordByName(String name) throws FindException {
        return null;
    }

    @Override
    public String getSecurePasswordPublicKey( final Goid securePasswordOid ) throws FindException {
        return null;
    }

    @Override
    public Goid saveSecurePassword(SecurePassword securePassword) throws UpdateException, SaveException, FindException {
        if (Goid.isDefault(securePassword.getGoid()))
            securePassword.setGoid(new Goid(0,nextId++));
        securePasswords.put(securePassword.getGoid(), securePassword);
        return securePassword.getGoid();
    }

    @Override
    public void setSecurePassword(Goid securePasswordOid, char[] newPassword) throws FindException, UpdateException {
    }

    @Override
    public JobId<Boolean> setGeneratedSecurePassword( final Goid securePasswordOid, final int keybits ) throws FindException, UpdateException {
        return null;
    }

    @Override
    public void deleteSecurePassword(Goid oid) throws DeleteException, FindException {
    }

    @Override
    public Map<String, Object> getCsrProperties(byte[] csrBytes) {
        return null;
    }

    @Override
    public boolean isShortSigningKey(Goid keystoreId, String alias) throws FindException, KeyStoreException {
        return false;
    }

    @Override
    public SsgKeyMetadata findKeyMetadata(Goid metadataOid) throws FindException {
        return null;
    }

    @Override
    public Goid saveOrUpdateMetadata(@NotNull SsgKeyMetadata metadata) throws SaveException {
        return new Goid(0,0);
    }

    @Override
    public <OUT extends Serializable> String getJobStatus(JobId<OUT> jobId) {
        return null;
    }

    @Override
    public <OUT extends Serializable> JobResult<OUT> getJobResult(JobId<OUT> jobId) throws UnknownJobException, JobStillActiveException {
        return null;
    }

    @Override
    public <OUT extends Serializable> void cancelJob(JobId<OUT> jobId, boolean interruptIfRunning) {
    }
}
