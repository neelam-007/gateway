package com.l7tech.common.xml.schema;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.common.security.rbac.Secured;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.security.rbac.MethodStereotype;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.rmi.RemoteException;
import java.util.Collection;

/**
 * An admin interface that allows indirect interaction with the SchemaEntryManager through
 * the SSM
 *
 * @author flascelles@layer7-tech.com
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
@Secured(types= EntityType.SCHEMA_ENTRY)
public interface SchemaAdmin {
    @Secured(stereotype=MethodStereotype.FIND_ENTITIES)
    Collection<SchemaEntry> findAllSchemas() throws RemoteException, FindException;

    @Secured(stereotype=MethodStereotype.DELETE_ENTITY)
    void deleteSchemaEntry(SchemaEntry existingSchema) throws RemoteException, DeleteException;

    @Secured(stereotype=MethodStereotype.SAVE_OR_UPDATE)
    long saveSchemaEntry(SchemaEntry entry) throws RemoteException, SaveException, UpdateException;

    @Secured(stereotype=MethodStereotype.FIND_ENTITIES)
    Collection<SchemaEntry> findByName(String schemaName) throws RemoteException, FindException;

    @Secured(stereotype=MethodStereotype.FIND_ENTITIES)
    Collection<SchemaEntry> findByTNS(String tns) throws RemoteException, FindException;
}
