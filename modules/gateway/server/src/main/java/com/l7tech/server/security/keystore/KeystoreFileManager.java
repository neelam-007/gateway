package com.l7tech.server.security.keystore;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.Functions;

/**
 * Interface that provides the ability to do CRUD operations on KeystoreFile rows in the database.
 */
public interface KeystoreFileManager extends EntityManager<KeystoreFile, EntityHeader> {
    /**
     * Atomically update the specified KeystoreFile entity in the database.
     * The most up-to-date entity will be fetched, mutated per the caller's mutator, and then saved back,
     * all within a single transaction.
     *
     * @param id  the ID of the keystore whose KeystoreFile is to be updated.  Required.
     * @param mutator  code that will be given the latest, up-to-date KeystoreFile instance,
     *                 is expected to produce a mutated version containing the intended new data.
     *                 <p/>
     *                 Mutator may throw RuntimeException to roll back the transaction; this will be
     *                 reported back up as an UpdateException.
     * @return the updated KeystoreFile instance, containing the latest version number.  Never null.
     * @throws UpdateException if the update could not be performed due to DB connectivity or the mutator throwing
     *                         a RuntimeException.
     */
    KeystoreFile mutateKeystoreFile(final Goid id, final Functions.UnaryVoid<KeystoreFile> mutator) throws UpdateException;

    void initializeHsmKeystorePasswordFromFile() throws UpdateException;
}
