/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.communityschemas;

import com.l7tech.common.xml.tarari.TarariSchemaResolver;
import com.l7tech.common.xml.schema.SchemaEntry;
import com.l7tech.objectmodel.*;
import org.w3c.dom.ls.LSResourceResolver;
import org.w3c.dom.ls.LSInput;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.util.Collection;
import java.io.IOException;

/**
 * @author alex
 */
public class CommunitySchemaManagerStub extends EntityManagerStub<SchemaEntry> implements CommunitySchemaManager {
    public Collection findByTNS(String tns) throws FindException {
        throw new UnsupportedOperationException();
    }

    public EntityResolver communityEntityResolver() {
        return new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                throw new UnsupportedOperationException();
            }
        };
    }

    public LSResourceResolver communityLSResourceResolver() {
        return new LSResourceResolver() {
            public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
                throw new UnsupportedOperationException();
            }
        };
    }

    public TarariSchemaResolver communitySchemaResolver() {
        return new TarariSchemaResolver() {
            public byte[] resolveSchema(String tns, String location, String baseURI) {
                throw new UnsupportedOperationException();
            }
        };
    }

    public void delete(SchemaEntry existingSchema) throws DeleteException {
        throw new UnsupportedOperationException();
    }

    public Collection<SchemaEntry> findByName(String schemaName) throws FindException {
        throw new UnsupportedOperationException();
    }

    public void update(SchemaEntry entry) throws UpdateException {
        throw new UnsupportedOperationException();
    }

    public long save(SchemaEntry entry) throws SaveException {
        throw new UnsupportedOperationException();
    }
}
