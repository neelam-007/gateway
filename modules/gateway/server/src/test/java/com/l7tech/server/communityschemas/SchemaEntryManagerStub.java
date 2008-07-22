/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.communityschemas;

import com.l7tech.objectmodel.*;
import com.l7tech.gateway.common.schema.SchemaEntry;
import com.l7tech.server.EntityManagerStub;

import java.util.Collection;
import java.util.Collections;

/**
 * @author mike
 */
public class SchemaEntryManagerStub extends EntityManagerStub<SchemaEntry,EntityHeader> implements SchemaEntryManager {
    public Collection<SchemaEntry> findByTNS(String tns) throws FindException {
        return Collections.emptySet();
    }

    public Collection<SchemaEntry> findByName(String schemaName) throws FindException {
        return Collections.emptySet();
    }
}
