package com.l7tech.server.security.keystore.ncipher;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.security.keystore.KeyAccessFilter;
import com.l7tech.server.security.keystore.KeystoreFile;
import com.l7tech.server.security.keystore.KeystoreFileManager;
import com.l7tech.server.security.keystore.SsgKeyMetadataManager;
import com.l7tech.util.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.KeyStore;
import java.security.KeyStoreException;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Test nCipher SSG Key Store when or after its initialization.
 */
@RunWith(MockitoJUnitRunner.class)
public class NcipherSsgKeyStoreTest {
    @Mock
    private KeystoreFileManager kem;
    @Mock
    private KeyAccessFilter keyAccessFilter;
    @Mock
    private SsgKeyMetadataManager metadataManager;
    @Mock
    private KeyStore testingKeyStore;
    @Mock
    private NcipherKeyStoreData ncipherKeyStoreData;
    private int countOfCallingBytesToKeyStore;

    @Test
    public void testUpdatingKeyStoreAndKeystoreFileVersion() throws KeyStoreException, FindException {
        //////////////////////////////////////////////////////////////////////////////////////
        // Check keystore and keystore version after the NcipherSsgKeyStore initialization. //
        //////////////////////////////////////////////////////////////////////////////////////

        final TestableNcipherSsgKeyStore testableNcipherSsgKeyStore =
            new TestableNcipherSsgKeyStore(new Goid("00000000000000000000000000000004"), "nCipher HSM", kem, keyAccessFilter, metadataManager);
        assertNull("When NcipherSsgKeyStore is initialized, keystore is null.", testableNcipherSsgKeyStore.getKeystoreObject());
        assertTrue("When NcipherSsgKeyStore is initialized, keystoreVersion is -1.", -1 == testableNcipherSsgKeyStore.getKeystoreVersion());

        // Create a testing KeyStoreFile
        final KeystoreFile testingKeyStoreFile = new KeystoreFile();
        testingKeyStoreFile.setDatabytes("Sample Testing Data".getBytes());
        testingKeyStoreFile.setFormat(NcipherSsgKeyStore.DB_FORMAT);
        testingKeyStoreFile.setVersion(1); // Set a new DB version as 1.
        when(kem.findByPrimaryKey(testableNcipherSsgKeyStore.getGoid())).thenReturn(testingKeyStoreFile);

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Check keystore and keystore version after the first method call to keystore() will update keystore and keystore version. //
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        KeyStore keyStore = testableNcipherSsgKeyStore.keyStore();
        // Verify keystore
        assertNotNull("After calling keystore() at first time, keystore will be generated.", keyStore);
        assertEquals(keyStore, testingKeyStore);
        // Verify keystoreFile version
        assertTrue(testableNcipherSsgKeyStore.getKeystoreVersion() == testingKeyStoreFile.getVersion());
        // Verify how many times to call bytesToKeyStore()
        assertTrue("bytesToKeyStore() has been called once so far.", countOfCallingBytesToKeyStore == 1);


        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Check keystore and keystore version after more method calls to keystore() will not update keystore and keystore version. //
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        keyStore = testableNcipherSsgKeyStore.keyStore();
        // Verify keystore
        assertNotNull("After calling keystore() at a second time, keystore should not be null.", keyStore);
        assertEquals(keyStore, testingKeyStore);
        // Verify keystoreFile version
        assertTrue(testableNcipherSsgKeyStore.getKeystoreVersion() == testingKeyStoreFile.getVersion());
        // Verify how many times to call bytesToKeyStore()
        assertTrue("The count number should not be changed.  That is, bytesToKeyStore() should not be called again, " +
            "because keystore has been created and keystoreVersion is the same as the keystoreFile DB version.",
            countOfCallingBytesToKeyStore == 1);
    }

    /**
     * Creating a testable NcipherSsgKeyStore to simply return a dummy pair of <NcipherKeyStoreData, KeyStore> and count
     * the method how many times to call bytesToKeyStore(), which will trigger to save bytes in niCipher local disk.
     */
    private class TestableNcipherSsgKeyStore extends NcipherSsgKeyStore {

        private TestableNcipherSsgKeyStore(Goid id, String name, KeystoreFileManager kem, KeyAccessFilter keyAccessFilter, SsgKeyMetadataManager metadataManager) throws KeyStoreException {
            super(id, name, kem, keyAccessFilter, metadataManager);
        }

        @Override
        synchronized Pair<NcipherKeyStoreData, KeyStore> bytesToKeyStore(byte[] bytes) throws KeyStoreException {
            countOfCallingBytesToKeyStore++;
            return new Pair<>(ncipherKeyStoreData, testingKeyStore);
        }
    }
}
