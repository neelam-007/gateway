package com.l7tech.common.xml.schema;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.SaveException;

import java.util.Collection;
import java.rmi.RemoteException;

/**
 * An admin interface that allows indirect interaction with the CommunitySchemaManager through
 * the SSM
 *
 * @author flascelles@layer7-tech.com
 */
public interface SchemaAdmin {
    Collection findAllSchemas() throws RemoteException, FindException;
    void deleteSchemaEntry(SchemaEntry existingSchema) throws RemoteException, DeleteException;
    long saveSchemaEntry(SchemaEntry entry) throws RemoteException, SaveException, UpdateException;
}
