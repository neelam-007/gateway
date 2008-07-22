package com.l7tech.server.security.keystore;

import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyStoreException;
import java.util.List;

/**
 * @since SecureSpan 4.0
 * @author rmak
 */
public class SsgKeyStoreManagerStub implements SsgKeyStoreManager {
    @Transactional(readOnly = true)
    public List<SsgKeyFinder> findAll() throws FindException, KeyStoreException {
        return null;
    }

    @Transactional(readOnly = true)
    public SsgKeyFinder findByPrimaryKey(long id) throws FindException, KeyStoreException {
        return null;
    }

    @Transactional(readOnly = true)
    public SsgKeyEntry lookupKeyByKeyAlias(String keyAlias, long preferredKeystoreId) throws FindException, KeyStoreException {
        throw new FindException("Not found");
    }
}
