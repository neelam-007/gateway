package com.l7tech.server.security.keystore;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
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
    final SsgKeyFinder ssgKeyFinder;

    public SsgKeyStoreManagerStub() {
        this(null);
    }

    public SsgKeyStoreManagerStub(SsgKeyFinder keyFinder) {
        this.ssgKeyFinder = keyFinder != null ? keyFinder : new SsgKeyFinderStub();
    }

    @Override
    public List<SsgKeyFinder> findAll() throws FindException, KeyStoreException {
        return Arrays.asList(ssgKeyFinder);
    }

    @Override
    public SsgKeyFinder findByPrimaryKey(Goid id) throws FindException, KeyStoreException {
        if ( ssgKeyFinder != null && Goid.equals(ssgKeyFinder.getGoid(), id) ) return ssgKeyFinder;
        throw new ObjectNotFoundException("Not found");
    }

    @Override
    public SsgKeyEntry lookupKeyByKeyAlias(String keyAlias, Goid preferredKeystoreId) throws FindException, KeyStoreException {
        if (ssgKeyFinder != null) {
            try {
                SsgKeyEntry entry = ssgKeyFinder.getCertificateChain(keyAlias);
                if (entry != null)
                    return entry;
            } catch (ObjectNotFoundException e) {
                // Fallthrough and check for "alice"
            }
        }

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
