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
     * Find a schema by targetNamespace. The targetNamespace is not unique in the database and two different schemas
     * could have the same targetNamespace. All found will be returned. 
     *
     * @param tns String targetNamespace to find all matching SchemaEntry's for
     * @return Collection&lt;SchemaEntry&gt; all matching schema entries.
     * @throws FindException
     */
    Collection<SchemaEntry> findByTNS(String tns) throws FindException;
    Collection<SchemaEntry> findByName(String schemaName) throws FindException;
}
