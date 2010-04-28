/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.communityschemas;

import com.l7tech.gateway.common.schema.SchemaEntry;
import com.l7tech.objectmodel.*;

import java.util.Collection;

/**
 * @author mike
 */
public interface SchemaEntryManager extends EntityManager<SchemaEntry, EntityHeader> {
    /**
     * Find a schema by targetNamespace.
     *
     * <p>The targetNamespace is not unique in the database and two different
     * schemas could have the same targetNamespace. All found will be
     * returned.</p>
     *
     * @param tns String targetNamespace to find all matching SchemaEntry's for (may be null)
     * @return Collection&lt;SchemaEntry&gt; all matching schema entries (may be empty, never null)
     * @throws FindException If an error occurs
     */
    Collection<SchemaEntry> findByTNS(String tns) throws FindException;

    /**
     * Find a schema by name / system identifier.
     * 
     * @param schemaName The name / system identifier to use (must not be null)
     * @return The collection of matching schema entries (may be empty, never null)
     * @throws FindException If an error occurs
     */
    Collection<SchemaEntry> findByName(String schemaName) throws FindException;
}
