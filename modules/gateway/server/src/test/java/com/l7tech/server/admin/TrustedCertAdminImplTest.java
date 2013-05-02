package com.l7tech.server.admin;

import com.l7tech.common.io.KeyGenParams;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.identity.cert.RevocationCheckPolicyManager;
import com.l7tech.server.security.keystore.SsgKeyMetadataManager;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;

import javax.security.auth.x500.X500Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TrustedCertAdminImplTest {
    private static final long KEYSTORE_ID = 1234L;
    private static final String ALIAS = "alias";
    private static final String SIG_ALGORITHM = "algorithm";
    private static final String DN = "CN=test";
    private TrustedCertAdminImpl admin;
    @Mock
    private DefaultKey defaultKey;
    @Mock
    private LicenseManager licenseManager;
    @Mock
    private SsgKeyStoreManager ssgKeyStoreManager;
    @Mock
    private SsgKeyMetadataManager ssgKeyMetadataManager;
    @Mock
    private SecurePasswordManager securePasswordManager;
    @Mock
    private ClusterPropertyManager clusterPropertyManager;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private TrustedCertManager trustedCertManager;
    @Mock
    private RevocationCheckPolicyManager revocationCheckPolicyManager;
    @Mock
    private PrivateKeyAdminHelper adminHelper;
    private SsgKeyMetadata metadata;
    private X500Principal principal;
    private PrivateKey privateKey;
    private X509Certificate cert;
    private X509Certificate[] chain;

    @Before
    public void setup() throws Exception {
        admin = new TestableTrustedCertAdminImpl(trustedCertManager, revocationCheckPolicyManager, defaultKey, licenseManager,
                ssgKeyStoreManager, ssgKeyMetadataManager, securePasswordManager, clusterPropertyManager);
        metadata = new SsgKeyMetadata();
        principal = new X500Principal(DN);
        privateKey = new TestCertificateGenerator().getPrivateKey();
        cert = new TestCertificateGenerator().subject("CN=test").generate();
        chain = new X509Certificate[]{cert};
    }

    @Test
    public void generateKeyPair() throws Exception {
        final AsyncAdminMethods.JobId<X509Certificate> job = admin.generateKeyPair(KEYSTORE_ID, ALIAS, metadata, principal, 1, 1, false, SIG_ALGORITHM);
        assertNotNull(job);
        verify(adminHelper).doGenerateKeyPair(eq(KEYSTORE_ID), eq(ALIAS), eq(metadata), eq(principal), argThat(isKeyGenParamsWithSize(1)), eq(1), eq(false), eq(SIG_ALGORITHM));
    }

    @Test
    public void generateEcKeyPair() throws Exception {
        final AsyncAdminMethods.JobId<X509Certificate> job = admin.generateEcKeyPair(KEYSTORE_ID, ALIAS, metadata, principal, "curveName", 1, false, SIG_ALGORITHM);
        assertNotNull(job);
        verify(adminHelper).doGenerateKeyPair(eq(KEYSTORE_ID), eq(ALIAS), eq(metadata), eq(principal), argThat(isKeyGenParamsWithNamedParam("curveName")), eq(1), eq(false), eq(SIG_ALGORITHM));
    }

    @Test
    public void importKeyFromKeyStoreFile() throws Exception {
        final byte[] keyStoreBytes = "testFileContents".getBytes();
        final String keyStoreType = "keyStoreType";
        final char[] keyStorePass = "keyStorePass".toCharArray();
        final char[] entryPass = "entryPass".toCharArray();
        final SsgKeyEntry keyEntry = SsgKeyEntry.createDummyEntityForAuditing(KEYSTORE_ID, ALIAS);
        when(adminHelper.doImportKeyFromKeyStoreFile(KEYSTORE_ID, ALIAS, metadata, keyStoreBytes, keyStoreType, keyStorePass, entryPass, ALIAS)).thenReturn(keyEntry);

        final SsgKeyEntry result = admin.importKeyFromKeyStoreFile(KEYSTORE_ID, ALIAS, metadata, keyStoreBytes, keyStoreType, keyStorePass, entryPass, ALIAS);
        assertEquals(keyEntry, result);
        verify(adminHelper).doImportKeyFromKeyStoreFile(KEYSTORE_ID, ALIAS, metadata, keyStoreBytes, keyStoreType, keyStorePass, entryPass, ALIAS);
    }

    @Test
    public void updateKeyEntryWithSecurityZone() throws Exception {
        final SecurityZone zone = new SecurityZone();
        final SsgKeyEntry keyEntry = new SsgKeyEntry(KEYSTORE_ID, ALIAS, chain, privateKey);
        keyEntry.setSecurityZone(zone);
        admin.updateKeyEntry(keyEntry);
        verify(adminHelper).doUpdateCertificateChain(KEYSTORE_ID, ALIAS, chain);
        verify(adminHelper).doUpdateKeyMetadata(KEYSTORE_ID, ALIAS, new SsgKeyMetadata(KEYSTORE_ID, ALIAS, zone));
    }

    @Test
    public void updateKeyEntryWithoutSecurityZone() throws Exception {
        final SsgKeyEntry keyEntry = new SsgKeyEntry(KEYSTORE_ID, ALIAS, chain, privateKey);
        keyEntry.setSecurityZone(null);
        admin.updateKeyEntry(keyEntry);
        verify(adminHelper).doUpdateCertificateChain(KEYSTORE_ID, ALIAS, chain);
        verify(adminHelper).doUpdateKeyMetadata(KEYSTORE_ID, ALIAS, null);
    }

    @Test(expected = UpdateException.class)
    public void updateKeyEntryUpdateChainException() throws Exception {
        doThrow(new UpdateException("mocking exception")).when(adminHelper).doUpdateCertificateChain(anyLong(), anyString(), any(X509Certificate[].class));
        try {
            admin.updateKeyEntry(new SsgKeyEntry(KEYSTORE_ID, ALIAS, chain, privateKey));
            fail("expected UpdateException");
        } catch (final UpdateException e) {
            verify(adminHelper, never()).doUpdateKeyMetadata(KEYSTORE_ID, ALIAS, new SsgKeyMetadata(KEYSTORE_ID, ALIAS, null));
            throw e;
        }
    }

    @Test(expected = UpdateException.class)
    public void updateKeyEntryUpdateMetadataException() throws Exception {
        doThrow(new UpdateException("mocking exception")).when(adminHelper).doUpdateKeyMetadata(anyLong(), anyString(), any(SsgKeyMetadata.class));
        try {
            admin.updateKeyEntry(new SsgKeyEntry(KEYSTORE_ID, ALIAS, chain, privateKey));
            fail("expected UpdateException");
        } catch (final UpdateException e) {
            verify(adminHelper).doUpdateCertificateChain(KEYSTORE_ID, ALIAS, chain);
            throw e;
        }
    }

    private class TestableTrustedCertAdminImpl extends TrustedCertAdminImpl {
        public TestableTrustedCertAdminImpl(TrustedCertManager trustedCertManager, RevocationCheckPolicyManager revocationCheckPolicyManager, DefaultKey defaultKey, LicenseManager licenseManager, SsgKeyStoreManager ssgKeyStoreManager, @NotNull SsgKeyMetadataManager ssgKeyMetadataManager, SecurePasswordManager securePasswordManager, ClusterPropertyManager clusterPropertyManager) {
            super(trustedCertManager, revocationCheckPolicyManager, defaultKey, licenseManager, ssgKeyStoreManager, ssgKeyMetadataManager, securePasswordManager, clusterPropertyManager);
        }

        @Override
        PrivateKeyAdminHelper getPrivateKeyAdminHelper() {
            return adminHelper;
        }
    }

    private KeyGenParamsMatcher isKeyGenParamsWithSize(final int keySizeBits) {
        return new KeyGenParamsMatcher(keySizeBits, null);
    }

    private KeyGenParamsMatcher isKeyGenParamsWithNamedParam(final String namedParam) {
        return new KeyGenParamsMatcher(0, namedParam);
    }

    private class KeyGenParamsMatcher extends ArgumentMatcher<KeyGenParams> {
        private final int keySizeBits;
        private final String namedParam;

        private KeyGenParamsMatcher(final int keySizeBits, final String namedParam) {
            this.keySizeBits = keySizeBits;
            this.namedParam = namedParam;
        }

        @Override
        public boolean matches(Object o) {
            if (o != null) {
                final KeyGenParams params = (KeyGenParams) o;
                return params.getKeySize() == keySizeBits &&
                        namedParam == null ? params.getNamedParam() == null : namedParam.equals(params.getNamedParam());
            }
            return false;
        }
    }
}

