package com.l7tech.server.admin;

import com.l7tech.common.TestDocuments;
import com.l7tech.gateway.common.security.SpecialKeyType;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyMetadataManager;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.util.Functions.UnaryVoid;
import com.l7tech.util.IOUtils;
import com.l7tech.util.NotFuture;
import org.apache.commons.lang.ObjectUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;

import static com.l7tech.util.CollectionUtils.foreach;
import static com.l7tech.util.CollectionUtils.list;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PrivateKeyAdminHelper
 */
@RunWith(MockitoJUnitRunner.class)
public class PrivateKeyAdminHelperTest {
    private static final String ALIAS = "alias";
    private static final Goid GOID = new Goid(0,1234L);
    private static final String KEY_STORE_TYPE = "PKCS12";
    private static final char[] password = "password".toCharArray();
    private static final String alias = "6e0e88f36ebb8744d470f62f604d03ea4ebe5094";
    @Mock
    private SsgKeyMetadataManager metadataManager;
    @Mock
    private SsgKeyStore keystore;
    @Mock
    private SsgKeyStoreManager keystoreManager;
    @Mock
    private SsgKeyFinder keyFinder;
    private PrivateKeyAdminHelper helper;
    private SsgKeyMetadata metadata;
    private byte[] keyStoreBytes;


    @Before
    public void setup() throws Exception {
        helper = new PrivateKeyAdminHelper(null, keystoreManager, null);
        metadata = new SsgKeyMetadata();
        keyStoreBytes = IOUtils.slurpStream(TestDocuments.getInputStream(TestDocuments.WSSKEYSTORE_ALICE));
    }

    @Test
    public void testClusterPropertyNamesForKeyTypes() {
        foreach(list(SpecialKeyType.values()), true, new UnaryVoid<SpecialKeyType>() {
            @Override
            public void call(final SpecialKeyType specialKeyType) {
                assertNotNull(
                        "Property for key type " + specialKeyType,
                        PrivateKeyAdminHelper.getClusterPropertyForSpecialKeyType(specialKeyType));
            }
        });
    }

    @Test
    public void doUpdateKeyMetadata() throws Exception {
        when(keystoreManager.findByPrimaryKey(GOID)).thenReturn(keyFinder);
        when(keyFinder.getKeyStore()).thenReturn(keystore);

        helper.doUpdateKeyMetadata(GOID, ALIAS, metadata);
        verify(keystore).updateKeyMetadata(GOID, ALIAS, metadata);
    }

    @Test(expected = UpdateException.class)
    public void doUpdateKeyMetadataUpdateError() throws Exception {
        when(keystoreManager.findByPrimaryKey(GOID)).thenReturn(keyFinder);
        when(keyFinder.getKeyStore()).thenReturn(keystore);
        doThrow(new UpdateException("mocking exception")).when(keystore).updateKeyMetadata(any(Goid.class), anyString(), any(SsgKeyMetadata.class));

        try {
            helper.doUpdateKeyMetadata(GOID, ALIAS, metadata);
            fail("Expected UpdateException");
        } catch (final UpdateException e) {
            verify(keystore).updateKeyMetadata(GOID, ALIAS, metadata);
            throw e;
        }
    }

    @Test(expected = UpdateException.class)
    public void doUpdateKeyMetadataCannotFindKeyFinder() throws Exception {
        when(keystoreManager.findByPrimaryKey(GOID)).thenThrow(new ObjectNotFoundException("mocking exception"));
        try {
            helper.doUpdateKeyMetadata(GOID, ALIAS, metadata);
            fail("Expected UpdateException");
        } catch (final UpdateException e) {
            verify(keystore, never()).updateKeyMetadata(GOID, ALIAS, new SsgKeyMetadata());
            throw e;
        }
    }

    @Test(expected = UpdateException.class)
    public void doUpdateKeyMetadataKeystoreException() throws Exception {
        when(keystoreManager.findByPrimaryKey(GOID)).thenThrow(new KeyStoreException("mocking exception"));
        try {
            helper.doUpdateKeyMetadata(GOID, ALIAS, metadata);
            fail("Expected UpdateException");
        } catch (final UpdateException e) {
            verify(keystore, never()).updateKeyMetadata(GOID, ALIAS, new SsgKeyMetadata());
            throw e;
        }
    }

    @Test(expected = UpdateException.class)
    public void doUpdateKeyMetadataKeyDoesNotExist() throws Exception {
        when(keystoreManager.findByPrimaryKey(GOID)).thenReturn(keyFinder);
        when(keyFinder.getCertificateChain(ALIAS)).thenThrow(new ObjectNotFoundException("mocking exception"));

        try {
            helper.doUpdateKeyMetadata(GOID, ALIAS, metadata);
            fail("Expected UpdateException");
        } catch (final UpdateException e) {
            verify(keystore, never()).updateKeyMetadata(GOID, ALIAS, new SsgKeyMetadata());
            throw e;
        }
    }

    @Test
    public void doUpdateKeyMetadataReadOnlyKeyStore() throws Exception {
        when(keystoreManager.findByPrimaryKey(GOID)).thenReturn(keyFinder);
        // key store is read only
        when(keyFinder.getKeyStore()).thenReturn(null);

        helper.doUpdateKeyMetadata(GOID, ALIAS, metadata);
        verify(keystore, never()).updateKeyMetadata(GOID, ALIAS, metadata);
    }

    @Test
    public void doImportKeyFromKeyStoreFileAttachesMetadata() throws Exception {
        final SsgKeyEntry keyEntry = createDummyKeyEntry();
        when(keystoreManager.findByPrimaryKey(GOID)).thenReturn(keyFinder);
        when(keyFinder.getKeyStore()).thenReturn(keystore);
        when(keystore.storePrivateKeyEntry(any(Runnable.class), any(SsgKeyEntry.class), eq(false))).thenReturn(new NotFuture<Boolean>(true));
        when(keystore.getCertificateChain(alias)).thenReturn(keyEntry);

        final SsgKeyEntry result = helper.doImportKeyFromKeyStoreFile(GOID, alias, metadata, keyStoreBytes, KEY_STORE_TYPE, password, password, alias);

        assertEquals(keyEntry, result);
        // ensure key entry is sent to keystore with metadata attached
        verify(keystore).storePrivateKeyEntry(any(Runnable.class), argThat(isKeyEntryWithMetadata(metadata)), eq(false));
    }

    @Test
    public void doImportKeyFromKeyStoreFileDoesNotAttachNullMetadata() throws Exception {
        final SsgKeyEntry keyEntry = createDummyKeyEntry();
        when(keystoreManager.findByPrimaryKey(GOID)).thenReturn(keyFinder);
        when(keyFinder.getKeyStore()).thenReturn(keystore);
        when(keystore.storePrivateKeyEntry(any(Runnable.class), any(SsgKeyEntry.class), eq(false))).thenReturn(new NotFuture<Boolean>(true));
        when(keystore.getCertificateChain(alias)).thenReturn(keyEntry);

        final SsgKeyEntry result = helper.doImportKeyFromKeyStoreFile(GOID, alias, null, keyStoreBytes, KEY_STORE_TYPE, password, password, alias);

        assertEquals(keyEntry, result);
        // ensure key entry is sent to keystore with null metadata
        verify(keystore).storePrivateKeyEntry(any(Runnable.class), argThat(isKeyEntryWithMetadata(null)), eq(false));
    }

    private SsgKeyEntry createDummyKeyEntry() throws GeneralSecurityException {
        return new SsgKeyEntry(GOID, ALIAS, new X509Certificate[]{new TestCertificateGenerator().subject("CN=test").generate()},
                    new TestCertificateGenerator().getPrivateKey());
    }

    private KeyEntryMatcher isKeyEntryWithMetadata(final SsgKeyMetadata metadata) {
        return new KeyEntryMatcher(metadata);
    }

    private class KeyEntryMatcher extends ArgumentMatcher<SsgKeyEntry> {
        private final SsgKeyMetadata metadata;

        private KeyEntryMatcher(final SsgKeyMetadata metadata) {
            this.metadata = metadata;
        }

        @Override
        public boolean matches(final Object o) {
            boolean matches = false;
            if (o != null && o instanceof SsgKeyEntry) {
                final SsgKeyEntry keyEntry = (SsgKeyEntry) o;
                matches = ObjectUtils.equals(keyEntry.getKeyMetadata(), metadata);
            }
            return matches;
        }
    }
}
