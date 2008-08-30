package com.l7tech.server.security.keystore;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyStoreException;
import java.util.Arrays;
import java.util.List;

/**
 * @since SecureSpan 4.0
 * @author rmak
 */
public class SsgKeyStoreManagerStub implements SsgKeyStoreManager {
    final SsgKeyFinder ssgKeyFinder = new SsgKeyFinderStub();

    @Transactional(readOnly = true)
    public List<SsgKeyFinder> findAll() throws FindException, KeyStoreException {
        return Arrays.asList(ssgKeyFinder);
    }

    @Transactional(readOnly = true)
    public SsgKeyFinder findByPrimaryKey(long id) throws FindException, KeyStoreException, ObjectNotFoundException {
        throw new ObjectNotFoundException("Not found");
    }

    @Transactional(readOnly = true)
    public SsgKeyEntry lookupKeyByKeyAlias(String keyAlias, long preferredKeystoreId) throws FindException, KeyStoreException, ObjectNotFoundException {
        throw new ObjectNotFoundException("Not found");
    }
}
