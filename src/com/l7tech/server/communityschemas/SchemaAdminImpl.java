/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 3, 2005<br/>
 */
package com.l7tech.server.communityschemas;

import com.l7tech.admin.AccessManager;
import com.l7tech.common.Feature;
import com.l7tech.common.LicenseException;
import com.l7tech.common.LicenseManager;
import com.l7tech.common.xml.schema.SchemaAdmin;
import com.l7tech.common.xml.schema.SchemaEntry;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;

import java.rmi.RemoteException;
import java.util.Collection;

/**
 * Serverside implementation of the SchemaAdmin interface.
 *
 * @author flascelles@layer7-tech.com
 */
public class SchemaAdminImpl implements SchemaAdmin {
    private final AccessManager accessManager;
    private final LicenseManager licenseManager;
    private CommunitySchemaManager communitySchemaManager;
    //private final Logger logger = Logger.getLogger(SchemaAdminImpl.class.getName());

    public SchemaAdminImpl(AccessManager accessManager, LicenseManager licenseManager) {
        this.accessManager = accessManager;
        this.licenseManager = licenseManager;
    }

    private void checkLicense() throws RemoteException {
        try {
            licenseManager.requireFeature(Feature.ADMIN);
        } catch (LicenseException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    public void setCommunitySchemaManager(CommunitySchemaManager communitySchemaManager) {
        this.communitySchemaManager = communitySchemaManager;
    }

    public Collection findAllSchemas() throws RemoteException, FindException {
        checkLicense();
        return communitySchemaManager.findAll();
    }

    public void deleteSchemaEntry(SchemaEntry existingSchema) throws RemoteException, DeleteException {
        accessManager.enforceAdminRole();
        checkLicense();
        communitySchemaManager.delete(existingSchema);
    }

    public Collection findByName(String schemaName) throws RemoteException, FindException {
        checkLicense();
        return communitySchemaManager.findByName(schemaName);
    }

    public Collection findByTNS(String tns) throws RemoteException, FindException {
        checkLicense();
        return communitySchemaManager.findByTNS(tns);
    }

    public long saveSchemaEntry(SchemaEntry entry) throws RemoteException, SaveException, UpdateException {
        accessManager.enforceAdminRole();
        checkLicense();
        if (entry.getOid() != -1) {
            communitySchemaManager.update(entry);
            return entry.getOid();
        } else {
            return communitySchemaManager.save(entry);
        }
    }
}
