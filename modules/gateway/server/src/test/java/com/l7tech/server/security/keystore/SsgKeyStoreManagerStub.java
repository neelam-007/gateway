package com.l7tech.server.security.keystore;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.common.TestDocuments;

import java.security.KeyStoreException;
import java.util.Arrays;
import java.util.List;

/**
 * @since SecureSpan 4.0
 * @author rmak
 */
public class SsgKeyStoreManagerStub implements SsgKeyStoreManager {
    final SsgKeyFinder ssgKeyFinder = new SsgKeyFinderStub();

    @Override
    public List<SsgKeyFinder> findAll() throws FindException, KeyStoreException {
        return Arrays.asList(ssgKeyFinder);
    }

    @Override
    public SsgKeyFinder findByPrimaryKey(long id) throws FindException, KeyStoreException {
        throw new ObjectNotFoundException("Not found");
    }

    @Override
    public SsgKeyEntry lookupKeyByKeyAlias(String keyAlias, long preferredKeystoreId) throws FindException, KeyStoreException {
        if ( "alice".equalsIgnoreCase(keyAlias) ) {
            try {
                return new SsgKeyEntry(
                    preferredKeystoreId,
                    keyAlias,
                    TestDocuments.getWssInteropAliceChain(),
                    TestDocuments.getWssInteropAliceKey()
                );
            } catch ( Exception e ) {
                throw new FindException("Error getting test key/cert.", e);
            }

        }
        throw new ObjectNotFoundException("Not found");
    }
}
