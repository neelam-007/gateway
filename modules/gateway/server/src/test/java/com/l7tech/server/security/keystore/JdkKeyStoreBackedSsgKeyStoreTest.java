package com.l7tech.server.security.keystore;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.KeyGenParams;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.util.NotFuture;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.security.auth.x500.X500Principal;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JdkKeyStoreBackedSsgKeyStoreTest {
    private static final Goid KEYSTORE_ID = new Goid(0, 1234L);
    private static final String ALIAS = "alias";
    private JdkKeyStoreBackedSsgKeyStore keyStore;
    @Mock
    private KeyAccessFilter keyAccessFilter;
    @Mock
    private SsgKeyMetadataManager metadataManager;
    @Mock
    private KeyStoreSpi keyStoreSpi;
    private KeyStore delegate;
    private SsgKeyMetadata metadata;
    private SsgKeyEntry entry;
    private CertGenParams certGenParams;

    @Before
    public void setup() throws Exception {
        keyStore = new TestableJdkKeyStoreBackedSsgKeyStore(keyAccessFilter, metadataManager);
        delegate = new KeyStoreStub();
        metadata = new SsgKeyMetadata();
        final X509Certificate[] chain = new X509Certificate[]{new TestCertificateGenerator().subject("CN=test").generate()};
        final PrivateKey privateKey = new TestCertificateGenerator().getPrivateKey();
        entry = new SsgKeyEntry(KEYSTORE_ID, ALIAS, chain, privateKey);
        certGenParams = new CertGenParams(new X500Principal("CN=test"), 1, false, null);
    }

    @Test
    public void storePrivateKeyEntryWithMetadata() throws Exception {
        entry.attachMetadata(metadata);
        final Future<Boolean> future = keyStore.storePrivateKeyEntry(null, entry, false);
        assertTrue(future.get());
        verify(metadataManager).updateMetadataForKey(KEYSTORE_ID, ALIAS, metadata);
    }

    @Test
    public void storePrivateKeyEntryWithNullMetadata() throws Exception {
        final Future<Boolean> future = keyStore.storePrivateKeyEntry(null, entry, false);
        assertTrue(future.get());
        verify(metadataManager).updateMetadataForKey(KEYSTORE_ID, ALIAS, null);
    }

    @Test(expected = KeyStoreException.class)
    public void storePrivateKeyEntryErrorSavingMetadata() throws Exception {
        entry.attachMetadata(metadata);
        when(metadataManager.updateMetadataForKey(any(Goid.class), anyString(), any(SsgKeyMetadata.class))).thenThrow(new UpdateException("mocking exception"));
        keyStore.storePrivateKeyEntry(null, entry, false);
    }

    @Test
    public void generateKeyPairWithMetadata() throws Exception {
        final Future<X509Certificate> future = keyStore.generateKeyPair(null, ALIAS, new KeyGenParams(), certGenParams, metadata);
        assertNotNull(future.get());
        verify(metadataManager).updateMetadataForKey(KEYSTORE_ID, ALIAS, metadata);
    }

    @Test
    public void generateKeyPairWithNullMetadata() throws Exception {
        final Future<X509Certificate> future = keyStore.generateKeyPair(null, ALIAS, new KeyGenParams(), certGenParams, null);
        assertNotNull(future.get());
        verify(metadataManager).updateMetadataForKey(KEYSTORE_ID, ALIAS, null);
    }

    @Test(expected = KeyStoreException.class)
    public void generateKeyPairErrorSavingMetadata() throws Exception {
        when(metadataManager.updateMetadataForKey(any(Goid.class), anyString(), any(SsgKeyMetadata.class))).thenThrow(new UpdateException("mocking exception"));
        keyStore.generateKeyPair(null, ALIAS, new KeyGenParams(), certGenParams, metadata);
    }

    @Test
    public void updateKeyMetadata() throws Exception {
        keyStore.updateKeyMetadata(KEYSTORE_ID, ALIAS, metadata);
        verify(metadataManager).updateMetadataForKey(KEYSTORE_ID, ALIAS, metadata);
    }

    @Test
    public void updateKeyMetadataNull() throws Exception {
        keyStore.updateKeyMetadata(KEYSTORE_ID, ALIAS, null);
        verify(metadataManager).updateMetadataForKey(KEYSTORE_ID, ALIAS, null);
    }

    @Test(expected = UpdateException.class)
    public void updateKeyMetadataError() throws Exception {
        when(metadataManager.updateMetadataForKey(any(Goid.class), anyString(), any(SsgKeyMetadata.class))).thenThrow(new UpdateException("mocking exception"));
        keyStore.updateKeyMetadata(KEYSTORE_ID, ALIAS, metadata);
    }

    @Test
    public void deletePrivateKeyEntryDeletesMetadata() throws Exception {
        keyStore.deletePrivateKeyEntry(null, ALIAS);
        verify(metadataManager).deleteMetadataForKey(KEYSTORE_ID, ALIAS);
    }

    @Test(expected = KeyStoreException.class)
    public void deletePrivateKeyEntryErrorDeletingMetadata() throws Exception {
        doThrow(new DeleteException("mocking exception")).when(metadataManager).deleteMetadataForKey(any(Goid.class), anyString());
        keyStore.deletePrivateKeyEntry(null, ALIAS);
    }

    private class TestableJdkKeyStoreBackedSsgKeyStore extends JdkKeyStoreBackedSsgKeyStore {

        protected TestableJdkKeyStoreBackedSsgKeyStore(@NotNull KeyAccessFilter keyAccessFilter, @NotNull SsgKeyMetadataManager metadataManager) {
            super(keyAccessFilter, metadataManager);
        }

        @Override
        protected KeyStore keyStore() throws KeyStoreException {
            return delegate;
        }

        @Override
        protected String getFormat() {
            return null;
        }

        @Override
        protected Logger getLogger() {
            return null;
        }

        @Override
        protected char[] getEntryPassword() {
            return "password".toCharArray();
        }

        @Override
        protected <OUT> Future<OUT> mutateKeystore(final boolean useCurrentThread, Runnable transactionCallback, Callable<OUT> mutator) throws KeyStoreException {
            OUT out;
            try {
                out = mutator.call();
            } catch (final Exception e) {
                throw new KeyStoreException(e);
            }
            return new NotFuture<OUT>(out);
        }

        @Override
        public Goid getGoid() {
            return KEYSTORE_ID;
        }

        @Override
        public SsgKeyStoreType getType() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }
    }

    /**
     * Have to stub KeyStore instead of mocking because its methods are final.
     */
    private class KeyStoreStub extends KeyStore {
        protected KeyStoreStub() {
            super(keyStoreSpi, null, null);
            try {
                load(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
