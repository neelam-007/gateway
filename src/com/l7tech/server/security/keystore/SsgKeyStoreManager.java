package com.l7tech.server.security.keystore;

import com.l7tech.objectmodel.FindException;
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
    SsgKeyFinder findByPrimaryKey(long id) throws FindException, KeyStoreException;
}
