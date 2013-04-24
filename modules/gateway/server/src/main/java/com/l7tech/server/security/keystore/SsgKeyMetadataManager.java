package com.l7tech.server.security.keystore;

import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.objectmodel.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Entity manager for persistent {@link SsgKeyMetadata} instances.
 */
public interface SsgKeyMetadataManager extends EntityManager<SsgKeyMetadata, EntityHeader>, SsgKeyMetadataFinder {
    /**
     * Record metadata for a key entry in the specified keystore.
     * <p/>
     * If there was any existing (orphaned) metadata for a key with this alias in this keystore ID it will be
     * deleted before the new metadata for this key (if any) is recorded.
     *
     * @param keystoreOid keystore OID.  Required.
     * @param alias      key alias.  Required.
     * @param newMeta    new metadata instance, or null if no metadata is required for this key entry.
     *                   If metadata is provided, the keystore OID and alias must match.
     * @return the OID assigned to the new metadata instance, or -1 if newMeta was null.
     * @throws FindException if there is a DB error checking for existing metadata.
     * @throws DeleteException if there is a DB error deleting existing metadata.
     * @throws SaveException if there is a DB error saving new metadata.
     */
    long setMetadataForNewKey(long keystoreOid, @NotNull String alias, @Nullable SsgKeyMetadata newMeta) throws FindException, DeleteException, SaveException;

    /**
     * Update metadata for a key entry in the specified keystore.
     * This will avoid saving a metadata row if none is required for a key, but will not delete existing metadata if it is no longer required
     * (to avoid transaction/sync issues).
     *
     * @param keystoreOid keystore OID.  Required.
     * @param alias      key alias.  Required.
     * @param newMeta new metadata to save, or null if none is required.  If null, will avoid creating metadata if it doesn't already exist for this key.
     *                               If any existing metadata is present for key, will update it to contain default values (null security zone, etc).
     * @return the OID assigned to the new or updated metadata instance, or -1 if newMeta was null and there was no existing metadata.
     * @throws FindException if there is a DB error checking for existing metadata.
     * @throws UpdateException if there is a DB error updating existing metadata.
     * @throws SaveException if there is a DB error saving new metadata.
     */
    long updateMetadataForKey(long keystoreOid, @NotNull String alias, @Nullable SsgKeyMetadata newMeta) throws FindException, SaveException, UpdateException;
}
