package com.l7tech.server.admin;

import com.l7tech.gateway.common.security.SpecialKeyType;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyMetadataManager;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.util.Functions.UnaryVoid;
import com.l7tech.util.NotFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.KeyStoreException;

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
    private static final long OID = 1234L;
    private PrivateKeyAdminHelper helper;
    @Mock
    private SsgKeyMetadataManager metadataManager;
    @Mock
    private SsgKeyStore keystore;
    @Mock
    private SsgKeyStoreManager keystoreManager;
    @Mock
    private SsgKeyFinder keyFinder;
    private SsgKeyMetadata metadata;


    @Before
    public void setup() {
        helper = new PrivateKeyAdminHelper(null, keystoreManager, null);
        metadata = new SsgKeyMetadata();
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
        when(keystoreManager.findByPrimaryKey(OID)).thenReturn(keyFinder);
        when(keyFinder.getKeyStore()).thenReturn(keystore);

        helper.doUpdateKeyMetadata(OID, ALIAS, metadata);
        verify(keystore).updateKeyMetadata(OID, ALIAS, metadata);
    }

    @Test(expected = UpdateException.class)
    public void doUpdateKeyMetadataUpdateError() throws Exception {
        when(keystoreManager.findByPrimaryKey(OID)).thenReturn(keyFinder);
        when(keyFinder.getKeyStore()).thenReturn(keystore);
        doThrow(new UpdateException("mocking exception")).when(keystore).updateKeyMetadata(anyLong(), anyString(), any(SsgKeyMetadata.class));

        try {
            helper.doUpdateKeyMetadata(OID, ALIAS, metadata);
            fail("Expected UpdateException");
        } catch (final UpdateException e) {
            verify(keystore).updateKeyMetadata(OID, ALIAS, metadata);
            throw e;
        }
    }

    @Test(expected = UpdateException.class)
    public void doUpdateKeyMetadataCannotFindKeyFinder() throws Exception {
        when(keystoreManager.findByPrimaryKey(OID)).thenThrow(new ObjectNotFoundException("mocking exception"));
        try {
            helper.doUpdateKeyMetadata(OID, ALIAS, metadata);
            fail("Expected UpdateException");
        } catch (final UpdateException e) {
            verify(keystore, never()).updateKeyMetadata(OID, ALIAS, new SsgKeyMetadata());
            throw e;
        }
    }

    @Test(expected = UpdateException.class)
    public void doUpdateKeyMetadataKeystoreException() throws Exception {
        when(keystoreManager.findByPrimaryKey(OID)).thenThrow(new KeyStoreException("mocking exception"));
        try {
            helper.doUpdateKeyMetadata(OID, ALIAS, metadata);
            fail("Expected UpdateException");
        } catch (final UpdateException e) {
            verify(keystore, never()).updateKeyMetadata(OID, ALIAS, new SsgKeyMetadata());
            throw e;
        }
    }

    @Test(expected = UpdateException.class)
    public void doUpdateKeyMetadataKeyDoesNotExist() throws Exception {
        when(keystoreManager.findByPrimaryKey(OID)).thenReturn(keyFinder);
        when(keyFinder.getCertificateChain(ALIAS)).thenThrow(new ObjectNotFoundException("mocking exception"));

        try {
            helper.doUpdateKeyMetadata(OID, ALIAS, metadata);
            fail("Expected UpdateException");
        } catch (final UpdateException e) {
            verify(keystore, never()).updateKeyMetadata(OID, ALIAS, new SsgKeyMetadata());
            throw e;
        }
    }

    @Test
    public void doUpdateKeyMetadataReadOnlyKeyStore() throws Exception {
        when(keystoreManager.findByPrimaryKey(OID)).thenReturn(keyFinder);
        // key store is read only
        when(keyFinder.getKeyStore()).thenReturn(null);

        helper.doUpdateKeyMetadata(OID, ALIAS, metadata);
        verify(keystore, never()).updateKeyMetadata(OID, ALIAS, metadata);
    }
}
