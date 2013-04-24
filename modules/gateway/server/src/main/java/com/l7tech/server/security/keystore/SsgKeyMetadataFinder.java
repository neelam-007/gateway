package com.l7tech.server.security.keystore;

import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.objectmodel.FindException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for looking up key metadata for a key entry.
 */
public interface SsgKeyMetadataFinder {

    /**
     * Finds (but does not attach) metadata for the specified key entry.
     *
     * @param keystoreOid keystore OID.  Requied.
     * @param alias key alias.  Required.
     * @return key metadata, or null if none found for the specified key entry.
     */
    @Nullable
    SsgKeyMetadata findMetadata(long keystoreOid, @NotNull String alias) throws FindException;
}
