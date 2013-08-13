package com.l7tech.server.globalresources;

import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.GoidEntityManager;
import com.l7tech.objectmodel.PropertySearchableEntityManager;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Entity manager for resource entries.
 */
public interface ResourceEntryManager extends GoidEntityManager<ResourceEntry, ResourceEntryHeader>, PropertySearchableEntityManager<ResourceEntryHeader> {

    /**
     * Find a resource entry by URI.
     *
     * @param uri The URI for the resource (required)
     * @param type The type for the resource (may be null)
     * @return the resource entry or null
     * @throws FindException if an error occurs
     */
    ResourceEntry findResourceByUriAndType(String uri, @Nullable ResourceType type) throws FindException;

    /**
     * Find resource headers by type.
     *
     * @param type The type for the resource (may be null)
     * @return The collection of matching resource entry headers (may be empty but never null)
     * @throws FindException if an error occurs
     */
    Collection<ResourceEntryHeader> findHeadersByType(@Nullable ResourceType type) throws FindException;

    /**
     * Find a resource entry by URI and type.
     *
     * <p>If a resource is found that matches the URI, but the type does not
     * match then <code>null</null> is returned.</p>
     *
     * @param uri The URI for the resource (required)
     * @param type The type for the resource (may be null)
     * @return the resource entry header or null
     * @throws FindException if an error occurs
     */
    ResourceEntryHeader findHeaderByUriAndType(String uri, @Nullable ResourceType type) throws FindException;

    /**
     * Find a resource resource by type and (optional) generic key.
     *
     * @param key The key to match.
     * @param type The type for the resource (may be null)
     * @return The collection of matching resource entry headers (may be empty but never null)
     * @throws FindException if an error occurs
     */
    Collection<ResourceEntryHeader> findHeadersByKeyAndType(String key, @Nullable ResourceType type) throws FindException;

    /**
     * Find an XML Schema resource by target namespace.
     *
     * @param targetNamespace The target namespace to match (may be null)
     * @return The collection of matching resource entry headers (may be empty but never null)
     * @throws FindException if an error occurs
     */
    Collection<ResourceEntryHeader> findHeadersByTNS(@Nullable String targetNamespace) throws FindException;

    /**
     * Find a DTD resource by public identifier.
     *
     * @param publicIdentifier The public identifier to match.
     * @return The collection of matching resource entry headers (may be empty but never null)
     * @throws FindException if an error occurs
     */
    Collection<ResourceEntryHeader> findHeadersByPublicIdentifier( String publicIdentifier ) throws FindException;
}
