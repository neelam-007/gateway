/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.communityschemas;

import com.l7tech.common.xml.schema.SchemaEntry;
import com.l7tech.objectmodel.*;

import java.util.Collection;
import java.util.Collections;

/**
 * @author mike
 */
public class SchemaEntryManagerStub extends EntityManagerStub<SchemaEntry> implements SchemaEntryManager {
    public Collection<SchemaEntry> findByTNS(String tns) throws FindException {
        return Collections.emptySet();
    }

    public void delete(SchemaEntry existingSchema) throws DeleteException {
        return;
    }

    public Collection<SchemaEntry> findByName(String schemaName) throws FindException {
        return Collections.emptySet();
    }

    public void update(SchemaEntry entry) throws UpdateException {
        return;
    }

    public long save(SchemaEntry entry) throws SaveException {
        return 0;
    }

    public SchemaEntry getSchemaEntryFromSystemId(String systemId) {
        return null;
    }

    public SchemaHandle getCachedSchemaHandleByOid(long oid) {
        return null;
    }

    public SchemaHandle getCachedSchemaHandleByTns(String tns) {
        return null;
    }
}
