package com.l7tech.server.security.keystore;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyStoreException;
import java.util.List;

/**
 * Interface implemented by central bean that manages keystores on the Gateway node.
 */
@Transactional(propagation= Propagation.SUPPORTS, rollbackFor=Throwable.class)
public interface SsgKeyStoreManager {
    /**
     * Finds all SsgKeyFinder instances available on this Gateway node.
     * <p/>
     * Some of these SsgKeyFinder instances may be mutable.
     *
     * @return a List of SsgKeyFinder instances.  Never null.  Guaranteed to contain at least one SsgKeyFinder.
     * @throws com.l7tech.objectmodel.FindException if there is a problem getting keystore data or metadata from the database
     * @throws java.security.KeyStoreException if there is a problem with the format of some keystore data
     */
    @Transactional(readOnly=true)
    List<SsgKeyFinder> findAll() throws FindException, KeyStoreException;

    /**
     * Find the specified SsgKeyFinder.
     *
     * @param id  the ID to find.
     * @return the requested SsgKeyFinder.  Never null.
     * @throws FindException if the requested ID could not be found or is not available on this system
     * @throws java.security.KeyStoreException if there is a problem with the format of some keystore data
     */
    @Transactional(readOnly=true)
    SsgKeyFinder findByPrimaryKey(long id) throws FindException, KeyStoreException, ObjectNotFoundException;

    /**
     * Find a single private key, along with its cert chain, by searching for a key with the specified alias.
     * <p/>
     * If the specified alias is not found in the preferred keystore, all other keystores will be scanned for it.
     * If the key cannot be found in any key store, FindException is thrown.
     *
     * @param keyAlias  the alias of the key to find.  Required.
     * @param preferredKeystoreId  the ID of a keystore to look in first, or -1 to scan all key stores.
     * @return a SignerInfo instance containing a private key and cert chain.  Never null.
     * @throws ObjectNotFoundException if the requested alias could not be found in any keystore
     * @throws FindException if there is a problem reading key data from the DB.
     * @throws java.security.KeyStoreException if there is a problem with the format of some keystore data
     */
    @Transactional(readOnly=true)
    SsgKeyEntry lookupKeyByKeyAlias(String keyAlias, long preferredKeystoreId) throws FindException, KeyStoreException, ObjectNotFoundException;
}
