/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.communityschemas;

import com.l7tech.common.xml.tarari.TarariSchemaResolver;
import com.l7tech.common.xml.schema.SchemaEntry;
import com.l7tech.objectmodel.*;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;

import java.util.Collection;

public interface CommunitySchemaManager extends EntityManager<SchemaEntry> {
    Collection<SchemaEntry> findByTNS(String tns) throws FindException;
    EntityResolver communityEntityResolver();
    LSResourceResolver communityLSResourceResolver();
    TarariSchemaResolver communitySchemaResolver();

    void delete(SchemaEntry existingSchema) throws DeleteException;

    Collection<SchemaEntry> findByName(String schemaName) throws FindException;

    void update(SchemaEntry entry) throws UpdateException;

    long save(SchemaEntry entry) throws SaveException;
}
