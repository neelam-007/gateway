package com.l7tech.server;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.ZoneUpdateSecurityChecker;
import com.l7tech.server.util.JaasUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;

/**
 * RBAC aware delegating EntityCrud implementation
 */
public class SecuredEntityCrud implements EntityCrud {
    private static final String ERROR_IN_PERMISSION_CHECK = "Error in permission check.";

    //- PUBLIC

    public SecuredEntityCrud( final RbacServices services,
                              final SecurityFilter securityFilter,
                              final EntityCrud entityCrud,
                              @NotNull final ZoneUpdateSecurityChecker zoneUpdateSecurityChecker) {
        this.services = services;
        this.securityFilter = securityFilter;
        this.entityCrud = entityCrud;
        this.zoneUpdateSecurityChecker = zoneUpdateSecurityChecker;
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
    public Entity find(@NotNull EntityHeader header) throws FindException {
        Entity entity = entityCrud.find(header);
        checkPermitted( OperationType.READ, entity, FindException.class );
        return entity;
    }

    @Override
    public EntityHeaderSet<EntityHeader> findAll(Class<? extends Entity> entityClass) throws FindException {
        return (EntityHeaderSet<EntityHeader>) securityFilter.filter( entityCrud.findAll(entityClass), getUser(), OperationType.READ, null );
    }

    @Override
    public EntityHeaderSet<EntityHeader> findAll(Class<? extends Entity> entityClass, Map<String,String> filters, int offset, int max) throws FindException {
        return (EntityHeaderSet<EntityHeader>) securityFilter.filter(  entityCrud.findAll(entityClass, filters, offset, max), getUser(), OperationType.READ, null );
    }

    @Override
    public EntityHeaderSet<EntityHeader> findAllInScope(Class<? extends Entity> entityClass, EntityHeader header, Map<String,String> filters, int offset, int max) throws FindException {
        return (EntityHeaderSet<EntityHeader>) securityFilter.filter(  entityCrud.findAllInScope(entityClass, header, filters, offset, max), getUser(), OperationType.READ, null );
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

    @Override
    public Collection<ZoneableEntityHeader> findByEntityTypeAndSecurityZoneOid(@NotNull EntityType type, long securityZoneOid) throws FindException {
        return securityFilter.filter(entityCrud.findByEntityTypeAndSecurityZoneOid(type, securityZoneOid), getUser(), OperationType.READ, null);
    }

    @Override
    public void setSecurityZoneForEntities(@Nullable final Long securityZoneOid, @NotNull final EntityType entityType, @NotNull final Collection<Long> entityOids) throws UpdateException {
        try {
            zoneUpdateSecurityChecker.checkBulkUpdatePermitted(getUser(), securityZoneOid, entityType, entityOids);
            entityCrud.setSecurityZoneForEntities(securityZoneOid, entityType, entityOids);
        } catch (final FindException e) {
            throw new UpdateException(ERROR_IN_PERMISSION_CHECK, e);
        }
    }

    //- PRIVATE

    private final RbacServices services;
    private final SecurityFilter securityFilter;
    private final EntityCrud entityCrud;
    private final ZoneUpdateSecurityChecker zoneUpdateSecurityChecker;

    private <T extends ObjectModelException> void checkPermitted( final OperationType operation, final Entity entity, final Class<T> exceptionType ) throws T {
        if (entity == null) return;
        String deniedMsg = null;
        try {
            User user = getUser();
            if (! services.isPermittedForEntity( user, entity, operation, null ) ) {
                deniedMsg = "Permission Denied";
            } else if ( entity instanceof HasFolder && OperationType.UPDATE == operation &&
                        FolderSupportHibernateEntityManager.changesExistingFolder( (HasFolder)entityCrud.find(EntityHeaderUtils.fromEntity(entity)), (HasFolder) entity ) &&
                        ! services.isPermittedForAnyEntityOfType(user, OperationType.DELETE, EntityType.FOLDER) ) {
                deniedMsg = "Folder Update Denied";
            }
        } catch ( FindException fe ) {
            throw new RuntimeException(ERROR_IN_PERMISSION_CHECK, fe );
        }

        if ( deniedMsg != null ) {
            try {
                try {
                    Constructor<T> constructor = exceptionType.getConstructor(String.class);
                    throw constructor.newInstance( deniedMsg );
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
