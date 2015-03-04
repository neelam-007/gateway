package com.l7tech.server.security.keystore;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import org.jetbrains.annotations.NotNull;
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
     * @throws ObjectNotFoundException if the requested ID could not be found or is not available on this system
     * @throws FindException if there was some other problem finding the requested key finder
     * @throws java.security.KeyStoreException if there is a problem with the format of some keystore data
     */
    @Transactional(readOnly=true)
    SsgKeyFinder findByPrimaryKey(Goid id) throws FindException, KeyStoreException;

    /**
     * Find a single private key, along with its cert chain, by searching for a key with the specified alias.
     * <p/>
     * If the specified alias is not found in the preferred keystore, all other keystores will be scanned for it.
     * If the key cannot be found in any key store, FindException is thrown.
     *
     * @param keyAlias  the alias of the key to find.  Required.
     * @param preferredKeystoreId  the ID of a keystore to look in first, or PersistentEntity.DEFAULT_GOID to scan all key stores.
     *                             For compatibility with systems ugpraded from pre-5.0, an ID of zero is treated like -1.
     * @return a SignerInfo instance containing a private key and cert chain.  Never null.
     * @throws ObjectNotFoundException if the requested alias could not be found in any keystore
     * @throws FindException if there is a problem reading key data from the DB.
     * @throws java.security.KeyStoreException if there is a problem with the format of some keystore data
     */
    @Transactional(readOnly=true)
    @NotNull
    SsgKeyEntry lookupKeyByKeyAlias(String keyAlias, Goid preferredKeystoreId) throws FindException, KeyStoreException;
}
