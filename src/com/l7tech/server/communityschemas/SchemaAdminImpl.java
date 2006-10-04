/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 3, 2005<br/>
 */
package com.l7tech.server.communityschemas;

import com.l7tech.common.LicenseException;
import com.l7tech.common.LicenseManager;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.xml.schema.SchemaAdmin;
import com.l7tech.common.xml.schema.SchemaEntry;
import com.l7tech.objectmodel.*;
import com.l7tech.server.GatewayFeatureSets;

import java.rmi.RemoteException;
import java.util.Collection;

/**
 * Serverside implementation of the SchemaAdmin interface.
 *
 * @author flascelles@layer7-tech.com
 */
public class SchemaAdminImpl implements SchemaAdmin {
    private final LicenseManager licenseManager;
    private SchemaEntryManager schemaEntryManager;
    //private final Logger logger = Logger.getLogger(SchemaAdminImpl.class.getName());

    public SchemaAdminImpl(LicenseManager licenseManager) {
        this.licenseManager = licenseManager;
    }

    private void checkLicense() throws RemoteException {
        try {
            licenseManager.requireFeature(GatewayFeatureSets.SERVICE_ADMIN);
        } catch (LicenseException e) {
            // New exception to conceal original stack trace from LicenseManager
            throw new RemoteException(ExceptionUtils.getMessage(e), new LicenseException(e.getMessage()));
        }
    }

    public void setSchemaEntryManager(SchemaEntryManager schemaEntryManager) {
        this.schemaEntryManager = schemaEntryManager;
    }

    public Collection<SchemaEntry> findAllSchemas() throws RemoteException, FindException {
        checkLicense();
        return schemaEntryManager.findAll();
    }

    public void deleteSchemaEntry(SchemaEntry existingSchema) throws RemoteException, DeleteException {
        checkLicense();
        schemaEntryManager.delete(existingSchema);
    }

    public Collection<SchemaEntry> findByName(String schemaName) throws RemoteException, FindException {
        checkLicense();
        return schemaEntryManager.findByName(schemaName);
    }

    public Collection<SchemaEntry> findByTNS(String tns) throws RemoteException, FindException {
        checkLicense();
        return schemaEntryManager.findByTNS(tns);
    }

    public long saveSchemaEntry(SchemaEntry entry) throws RemoteException, SaveException {
        checkLicense();
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
}
