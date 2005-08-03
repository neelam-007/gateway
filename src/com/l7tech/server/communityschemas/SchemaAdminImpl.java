/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 3, 2005<br/>
 */
package com.l7tech.server.communityschemas;

import com.l7tech.common.xml.schema.SchemaAdmin;
import com.l7tech.common.xml.schema.SchemaEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.admin.AccessManager;
import org.springframework.orm.hibernate.support.HibernateDaoSupport;

import java.util.Collection;
import java.rmi.RemoteException;

/**
 * Serverside implementation of the SchemaAdmin interface.
 *
 * @author flascelles@layer7-tech.com
 */
public class SchemaAdminImpl extends HibernateDaoSupport implements SchemaAdmin {

    private AccessManager accessManager;
    private CommunitySchemaManager communitySchemaManager;
    //private final Logger logger = Logger.getLogger(SchemaAdminImpl.class.getName());

    public SchemaAdminImpl(AccessManager accessManager) {
        this.accessManager = accessManager;
    }

    public void setCommunitySchemaManager(CommunitySchemaManager communitySchemaManager) {
        this.communitySchemaManager = communitySchemaManager;
    }

    public Collection findAllSchemas() throws RemoteException, FindException {
        return communitySchemaManager.findAll();
    }

    public void deleteSchemaEntry(long entryOid) throws RemoteException, DeleteException {
        accessManager.enforceAdminRole();
        // todo
    }

    public long saveSchemaEntry(SchemaEntry entry) throws RemoteException, SaveException, UpdateException {
        accessManager.enforceAdminRole();
        if (entry.getOid() != -1) {
            communitySchemaManager.update(entry);
            return entry.getOid();
        } else {
            return communitySchemaManager.save(entry);
        }
    }
}
