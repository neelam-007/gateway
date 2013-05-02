package com.l7tech.server.security.keystore;

import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.objectmodel.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Entity manager for persistent {@link SsgKeyMetadata} instances.
 */
public interface SsgKeyMetadataManager extends EntityManager<SsgKeyMetadata, EntityHeader> {
    /**
     * Finds (but does not attach) metadata for the specified key entry.
     *
     * @param keystoreOid keystore OID.  Required.
     * @param alias       key alias.  Required.
     * @return key metadata, or null if none found for the specified key entry.
     */
    @Nullable
    SsgKeyMetadata findMetadata(long keystoreOid, @NotNull String alias) throws FindException;

    /**
     * Update metadata for a key entry in the specified keystore.
     * This will avoid saving a metadata row if none is required for a key, but will not delete existing metadata if it is no longer required
     * (to avoid transaction/sync issues).
     *
     * @param keystoreOid keystore OID.  Required.
     * @param alias       key alias.  Required.
     * @param newMeta     new metadata to save, or null if none is required.  If null, will avoid creating metadata if it doesn't already exist for this key.
     *                    If any existing metadata is present for key, will update it to contain default values (null security zone, etc).
     * @return the OID assigned to the new or updated metadata instance, or -1 if newMeta was null and there was no existing metadata.
     * @throws FindException   if there is a DB error checking for existing metadata.
     * @throws UpdateException if there is a DB error updating existing metadata.
     * @throws SaveException   if there is a DB error saving new metadata.
     */
    long updateMetadataForKey(long keystoreOid, @NotNull String alias, @Nullable SsgKeyMetadata newMeta) throws FindException, SaveException, UpdateException;

    /**
     * Deletes metadata for a key entry in the specified keystore.
     *
     * If no metadata exists for the key entry in the specified keystore, nothing will happen.
     *
     * @param keystoreOid the oid of the keystore. Required.
     * @param alias       the key alias that identifies the key entry. Required.
     * @throws DeleteException if a DB error occurred while deleting the metadata.
     */
    void deleteMetadataForKey(long keystoreOid, @NotNull String alias) throws DeleteException;
}
