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
    Collection<SchemaEntry> findByTNS(String tns) throws FindException;
    Collection<SchemaEntry> findByName(String schemaName) throws FindException;
}
