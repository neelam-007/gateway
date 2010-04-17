/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 3, 2005<br/>
 */
package com.l7tech.server.communityschemas;

import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.server.service.ServiceDocumentResolver;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.gateway.common.schema.SchemaAdmin;
import com.l7tech.gateway.common.schema.SchemaEntry;
import com.l7tech.objectmodel.*;

import java.io.IOException;
import java.util.Collection;

/**
 * Serverside implementation of the SchemaAdmin interface.
 *
 * @author flascelles@layer7-tech.com
 */
public class SchemaAdminImpl implements SchemaAdmin {
    private SchemaEntryManager schemaEntryManager;
    private ServiceDocumentResolver serviceDocumentResolver;

    //private final Logger logger = Logger.getLogger(SchemaAdminImpl.class.getName());

    public SchemaAdminImpl() {
    }

    public void setSchemaEntryManager(SchemaEntryManager schemaEntryManager) {
        this.schemaEntryManager = schemaEntryManager;
    }

    public void setServiceDocumentResolver(ServiceDocumentResolver serviceDocumentResolver) {
        this.serviceDocumentResolver = serviceDocumentResolver;
    }

    @Override
    public Collection<SchemaEntry> findAllSchemas() throws FindException {
        return schemaEntryManager.findAll();
    }

    @Override
    public void deleteSchemaEntry(SchemaEntry existingSchema) throws DeleteException {
        schemaEntryManager.delete(existingSchema);
    }

    @Override
    public Collection<SchemaEntry> findByName(String schemaName) throws FindException {
        return schemaEntryManager.findByName(schemaName);
    }

    @Override
    public Collection<SchemaEntry> findByTNS(String tns) throws FindException {
        return schemaEntryManager.findByTNS(tns);
    }

    @Override
    public long saveSchemaEntry(SchemaEntry entry) throws SaveException {
        if (entry.getOid() != -1) {
            try {
                schemaEntryManager.update(entry);
            } catch (ObjectModelException e) {
                throw new SaveException("Couldn't save Schema: " + ExceptionUtils.getMessage(e), e);
            }
            return entry.getOid();
        } else {
            return schemaEntryManager.save(entry);
        }
    }

    @Override
    public String resolveSchemaTarget(String url) throws IOException {
        return serviceDocumentResolver.resolveDocumentTarget(url, ServiceAdmin.DownloadDocumentType.SCHEMA);
    }
}
