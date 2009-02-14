package com.l7tech.server;

import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.identity.User;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * RBAC aware delegating EntityCrud implementation
 */
public class SecuredEntityCrud implements EntityCrud {

    //- PUBLIC

    public SecuredEntityCrud( final RbacServices services,
                              final SecurityFilter securityFilter,
                              final EntityCrud entityCrud ) {
        this.services = services;
        this.securityFilter = securityFilter;
        this.entityCrud = entityCrud;
    }

    @Override
    public void delete(Entity entity) throws DeleteException {
        checkPermitted( OperationType.DELETE, entity, DeleteException.class );
        entityCrud.delete(entity);
    }

    @Override
    public void evict(Entity entity) {
        entityCrud.evict(entity);
    }

    @Override
    public <ET extends Entity> ET find(Class<ET> clazz, Serializable pk) throws FindException {
        ET entity = entityCrud.find(clazz, pk);
        checkPermitted( OperationType.READ, entity, FindException.class );
        return entity;
    }

    @Override
    public Entity find(EntityHeader header) throws FindException {
        Entity entity = entityCrud.find(header);
        checkPermitted( OperationType.READ, entity, FindException.class );
        return entity;
    }

    @Override
    public EntityHeaderSet<EntityHeader> findAll(Class<? extends Entity> entityClass) throws FindException {
        return (EntityHeaderSet<EntityHeader>) securityFilter.filter( entityCrud.findAll(entityClass), getUser(), OperationType.READ, null );
    }

    @Override
    public EntityHeaderSet<EntityHeader> findAll(Class<? extends Entity> entityClass, String filter, int offset, int max) throws FindException {
        return (EntityHeaderSet<EntityHeader>) securityFilter.filter(  entityCrud.findAll(entityClass, filter, offset, max), getUser(), OperationType.READ, null );
    }

    @Override
    public EntityHeaderSet<EntityHeader> findAllInScope(Class<? extends Entity> entityClass, EntityHeader header, String filter, int offset, int max) throws FindException {
        return (EntityHeaderSet<EntityHeader>) securityFilter.filter(  entityCrud.findAllInScope(entityClass, header, filter, offset, max), getUser(), OperationType.READ, null );
    }

    @Override
    public EntityHeader findHeader(EntityType etype, Serializable pk) throws FindException {
        return entityCrud.findHeader(etype, pk);
    }

    @Override
    public Serializable save(Entity entity) throws SaveException {
        checkPermitted( OperationType.CREATE, entity, SaveException.class );
        return entityCrud.save(entity);
    }

    @Override
    public void update(Entity entity) throws UpdateException {
        checkPermitted( OperationType.UPDATE, entity, UpdateException.class );
        entityCrud.update(entity);
    }

    //- PRIVATE

    private final RbacServices services;
    private final SecurityFilter securityFilter;
    private final EntityCrud entityCrud;

    private <T extends ObjectModelException> void checkPermitted( final OperationType operation, final Entity entity, final Class<T> exceptionType ) throws T {
        boolean permitted;
        try {
            permitted  = services.isPermittedForEntity( getUser(), entity, operation, null );
        } catch ( FindException fe ) {
            throw new RuntimeException( "Error in permission check.", fe );
        }

        if ( !permitted ) {
            try {
                try {
                    Constructor<T> constructor = exceptionType.getConstructor(String.class);
                    throw constructor.newInstance( "Permission Denied" );
                } catch ( NoSuchMethodException nsme ) {
                    throw exceptionType.newInstance();
                } catch (InvocationTargetException e) {
                    throw exceptionType.newInstance();
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException( "Error in permission check.", e );
            } catch (InstantiationException e) {
                throw new RuntimeException( "Error in permission check.", e );
            }
        }
    }

    private User getUser() {
        return JaasUtils.getCurrentUser();
    }

}
