/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.communityschemas;

import com.l7tech.common.xml.schema.SchemaEntry;
import com.l7tech.objectmodel.*;

import java.util.Collection;

/**
 * @author mike
 */
public interface SchemaEntryManager extends EntityManager<SchemaEntry, EntityHeader> {
    Collection<SchemaEntry> findByTNS(String tns) throws FindException;

    void delete(SchemaEntry existingSchema) throws DeleteException;

    Collection<SchemaEntry> findByName(String schemaName) throws FindException;

    void update(SchemaEntry entry) throws UpdateException;

    long save(SchemaEntry entry) throws SaveException;
}
