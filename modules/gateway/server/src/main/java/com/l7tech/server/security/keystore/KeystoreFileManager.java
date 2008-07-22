package com.l7tech.server.security.keystore;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.Functions;

/**
 * Interface that provides the ability to do CRUD operations on KeystoreFile rows in the database.
 */
public interface KeystoreFileManager extends EntityManager<KeystoreFile, EntityHeader> {
    /**
     * Atomically update the data bytes for the specific keystore file.
     * The most up-to-date data will be fetched, mutated per the caller's mutator, and then saved back,
     * all within a single transaction.
     *
     * @param id  the ID of the keystore whose data bytes are to be updated.  Required.
     * @param mutator  code that will be given the latest, up-to-date data bytes for this keystore, and
     *                 is expected to produce a mutated version containing the intended new data.
     *                 <p/>
     *                 Mutator may throw RuntimeException to roll back the transaction; this will be
     *                 reported back up as an UpdateException.
     * @return the updated KeystoreFile instance, containing the latest version number.  Never null.
     * @throws UpdateException if the update could not be performed due to DB connectivity or the mutator throwing
     *                         a RuntimeException.
     */
    KeystoreFile updateDataBytes(long id, Functions.Unary<byte[], byte[]> mutator) throws UpdateException;
}
