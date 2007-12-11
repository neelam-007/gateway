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
public class SchemaEntryManagerStub extends EntityManagerStub<SchemaEntry,EntityHeader> implements SchemaEntryManager {
    public Collection<SchemaEntry> findByTNS(String tns) throws FindException {
        return Collections.emptySet();
    }

    public Collection<SchemaEntry> findByName(String schemaName) throws FindException {
        return Collections.emptySet();
    }
}
