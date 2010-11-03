package com.l7tech.xml;

import java.util.Map;
import java.util.Set;

/**
 * Interface implemented by assertions that support the "migrate namespaces" functionality.
 */
public interface NamespaceMigratable {
    /**
     * Perform namespace migration on this assertion.  Any configured namespace URIs matching one of the source namespace URIs
     * in the provided map will be replaced with the corresponding destination URI.
     *
     * @param nsUriSourceToDest a Map from source URI to destination (replacement) URI.  Required.
     */
    void migrateNamespaces(Map<String, String> nsUriSourceToDest);

    /**
     * Find all namespace URIs that are used by this migratable.
     * <p/>
     * It should be understood that this information may take some time to prepare,
     * so callers should avoid calling this method more often than necessary.
     *
     * @return a list of all namespace URIs currently actively used by this assertion.  May be empty but never null.
     *         The returned Set belongs to the caller and may be modified.
     */
    Set<String> findNamespaceUrisUsed();
}
