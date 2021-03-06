/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.FolderedEntityManager;
import com.l7tech.objectmodel.folder.HasFolder;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;

/**
 * Generic CRUD for persistent entities.  Reads delegated to {@link EntityFinderImpl} unless some {@link com.l7tech.objectmodel.EntityManager}
 * actually needs to interfere.
 *
 * @author alex
 */
@SuppressWarnings({"unchecked"})
@Transactional(propagation = Propagation.REQUIRED, rollbackFor=Throwable.class)
public class EntityCrudImpl extends HibernateDaoSupport implements EntityCrud {
    private final EntityFinder entityFinder;
    private final Map<Class<? extends Entity>, ReadOnlyEntityManager<? extends Entity, ? extends EntityHeader>> managersByClass = new LinkedHashMap<>();

    public EntityCrudImpl( final EntityFinder entityFinder, final List<ReadOnlyEntityManager<? extends Entity, ? extends EntityHeader>> managers) {
        this.entityFinder = entityFinder;
        for (ReadOnlyEntityManager manager : managers) {
            this.managersByClass.put(manager.getImpClass(), manager);
        }
    }

    @Override
    public void addEntityManagers(final List<ReadOnlyEntityManager<? extends Entity, ? extends EntityHeader>> managers) {
        for (ReadOnlyEntityManager manager : managers) {
            this.managersByClass.put(manager.getImpClass(), manager);
        }
    }

    @Override
    public EntityHeaderSet<EntityHeader> findAll(final Class<? extends Entity> entityClass) throws FindException {
        ReadOnlyEntityManager manager = getReadOnlyManager(entityClass);
        if (manager != null){
            Collection<EntityHeader> headers = manager.findAllHeaders();
            return new EntityHeaderSet<EntityHeader>((EntityHeader[])headers.toArray(new EntityHeader[headers.size()]));
        }
        return entityFinder.findAll(entityClass);
    }

    @Override
    public EntityHeaderSet<EntityHeader> findAll(final Class<? extends Entity> entityClass, final Map<String,String> filters, final int offset, final int max) throws FindException {
        ReadOnlyEntityManager manager = getReadOnlyManager(entityClass);
        if ( manager != null ){
            Collection<EntityHeader> headers;
            if ( manager instanceof PropertySearchableEntityManager ) {
                headers = ((PropertySearchableEntityManager)manager).findHeaders( offset, max, filters );
            } else {
                headers = manager.findAllHeaders( offset, max );
            }
            return new EntityHeaderSet<EntityHeader>((EntityHeader[])headers.toArray(new EntityHeader[headers.size()]));
        }
        return entityFinder.findAll(entityClass);
    }

    @Override
    public <ET extends Entity> List<ET> findAll(Class<ET> entityClass, Map<String, List<Object>> filters, int offset, int max, Boolean ascending, String sortKey) throws FindException {
        EntityManager manager = getEntityManager(entityClass);
        if ( manager != null ){
            return manager.findPagedMatching(offset, max, sortKey, ascending, filters);
        } else {
            throw new FindException("Could not find entity manager for entity type: " + entityClass.getName());
        }
    }

    @Override
    public EntityHeaderSet<EntityHeader> findAllInScope(final Class<? extends Entity> entityClass, EntityHeader scope, final Map<String,String> filters, final int offset, final int max) throws FindException {
        ReadOnlyEntityManager manager = getReadOnlyManager(entityClass);
        if ( manager != null ){
            Collection<EntityHeader> headers;
            if ( scope != null && manager instanceof ScopedSearchableEntityManager ) {
                headers = ((ScopedSearchableEntityManager)manager).findHeadersInScope( offset, max, scope, filters );
            } else if ( manager instanceof PropertySearchableEntityManager ) {
                headers = ((PropertySearchableEntityManager)manager).findHeaders( offset, max, filters );
            } else {
                headers = manager.findAllHeaders( offset, max );
            }
            return new EntityHeaderSet<EntityHeader>((EntityHeader[])headers.toArray(new EntityHeader[headers.size()]));
        }
        return entityFinder.findAll(entityClass);
    }

    @Override
    public Entity find(@NotNull final EntityHeader header) throws FindException {
        final EntityManager manager = getEntityManager(EntityTypeRegistry.getEntityClass(header.getType()));
        Entity ent = manager != null ? manager.findByHeader(header) : entityFinder.find(header);

        return ent;

        // todo: make entities clonable, so that we can return ent.copyOf();
    }

    @Override
    public <ET extends Entity> ET find(final Class<ET> clazz, final Serializable pk) throws FindException {
        ReadOnlyEntityManager manager = getReadOnlyManager(clazz);
        if (manager != null) {
                return (ET) manager.findByPrimaryKey((pk instanceof Goid) ? (Goid) pk : Goid.parseGoid(pk.toString()));
        }
        ET ent = entityFinder.find(clazz, pk);

        return ent;
    }

    @Override
    public EntityHeader findHeader(final EntityType etype, final Serializable pk) throws FindException {
        ReadOnlyEntityManager manager = getReadOnlyManager(EntityTypeRegistry.getEntityClass(etype));
        if (manager != null) {
            Entity ent = manager.findByPrimaryKey((pk instanceof Goid) ? (Goid) pk : Goid.parseGoid(pk.toString()));
            return ent == null ? null : EntityHeaderUtils.fromEntity(ent);
        }
        return entityFinder.findHeader(etype, pk);
    }

    @Override
    public Serializable save(final Entity e) throws SaveException {
        final EntityManager manager = getEntityManager(e.getClass());
        if (manager != null) {
            Serializable key = manager.save((PersistentEntity) e);
            if (manager instanceof RoleAwareEntityManager) {
                ((RoleAwareEntityManager) manager).createRoles((PersistentEntity) e);
            }
            return key;
        }

        return (Serializable)getHibernateTemplate().execute(new HibernateCallback() {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                return session.save(e);
            }
        });
    }

    @Override
    public void save(final Goid id, final Entity e) throws SaveException {
        final EntityManager manager = getEntityManager(e.getClass());
        if (manager != null) {
            manager.save(id, (PersistentEntity) e);
            if (manager instanceof RoleAwareEntityManager) {
                ((RoleAwareEntityManager) manager).createRoles((PersistentEntity) e);
            }
        } else {
            throw new SaveException("Could not find entity manager for entity type: " + e.getClass().getName());
        }
    }

    @Override
    public void update(final Entity e) throws UpdateException {
        final EntityManager manager = getEntityManager(e.getClass());
        if (manager != null) {
            PersistentEntity persistentEntity = (PersistentEntity) e;
            if (persistentEntity instanceof HasFolder && manager instanceof FolderedEntityManager) {
                ((FolderedEntityManager)manager).updateWithFolder(persistentEntity);
            } else {
                manager.update(persistentEntity);
            }
            return;
        }

        getHibernateTemplate().execute(new HibernateCallback() {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                session.update(e);
                return null;
            }
        });
    }

    @Override
    public void delete(final Entity e) throws DeleteException {
        final EntityManager manager = getEntityManager(e.getClass());
        if (manager != null) {
            manager.delete((PersistentEntity)e);
            return;
        }

        getHibernateTemplate().execute(new HibernateCallback() {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                session.delete(e);
                return null;
            }
        });
    }

    @Override
    public void evict( final Entity entity ) {
        if ( entity instanceof PersistentEntity) {
            getHibernateTemplate().execute(new HibernateCallback() {
                @Override
                public Object doInHibernate( final Session session ) throws HibernateException, SQLException {
                    session.evict( entity );
                    return null;
                }
            });
        }
    }

    @Override
    public Collection<EntityHeader> findByEntityTypeAndSecurityZoneGoid(@NotNull final EntityType type, final Goid securityZoneGoid) throws FindException {
        return entityFinder.findByEntityTypeAndSecurityZoneGoid(type, securityZoneGoid);
    }

    @Override
    public void setSecurityZoneForEntities(@Nullable final Goid securityZoneGoid, @NotNull final EntityType entityType, @NotNull final Collection<Serializable> entityIds) throws UpdateException {
        if (!entityIds.isEmpty()) {
            try {
                setSecurityZoneForEntities(findSecurityZone(securityZoneGoid), entityType, entityIds);
            } catch (final FindException e) {
                throw new UpdateException("Unable to set security zone for entities: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void setSecurityZoneForEntities(@Nullable final Goid securityZoneGoid, @NotNull Map<EntityType, Collection<Serializable>> entityIds) throws UpdateException {
        if (!entityIds.isEmpty()) {
            try {
                final SecurityZone securityZone = findSecurityZone(securityZoneGoid);
                for (final Map.Entry<EntityType, Collection<Serializable>> entry : entityIds.entrySet()) {
                    setSecurityZoneForEntities(securityZone, entry.getKey(), entry.getValue());
                }
            } catch (final FindException e) {
                throw new UpdateException("Unable to set security zone for entities: " + e.getMessage(), e);
            }
        }
    }

    private SecurityZone findSecurityZone(@Nullable final Goid securityZoneGoid) throws FindException {
        final SecurityZone securityZone = securityZoneGoid != null ? find(SecurityZone.class, securityZoneGoid) : null;
        if (securityZone == null && securityZoneGoid != null) {
            throw new FindException("Security zone with goid " + securityZoneGoid + " does not exist");
        }
        return securityZone;
    }

    private void setSecurityZoneForEntities(@Nullable final SecurityZone securityZone, @NotNull final EntityType entityType, @NotNull final Collection<Serializable> entityIds) throws UpdateException, FindException {
        if (!entityType.isSecurityZoneable()) {
            throw new IllegalArgumentException("Entity type must be security zoneable");
        }
        for (final Serializable entityId : entityIds) {
            final Entity entity = find(entityType.getEntityClass(), entityId);
            if (entity instanceof ZoneableEntity) {
                final ZoneableEntity zoneable = (ZoneableEntity) entity;
                zoneable.setSecurityZone(securityZone);
                update(entity);
            } else {
                throw new UpdateException(entityType.getName() + " with id " + entityId + " does not exist or is not security zoneable");
            }
        }
    }

    private ReadOnlyEntityManager getReadOnlyManager(Class<? extends Entity> clazz) {
        return managersByClass.get(clazz);
    }

    private EntityManager getEntityManager(Class<? extends Entity> clazz) {
        //If this is a subclass of identity provider config us the IdentityProviderConfig
        if(IdentityProviderConfig.class.isAssignableFrom(clazz)) {
            clazz = IdentityProviderConfig.class;
        }
        final ReadOnlyEntityManager<? extends Entity, ? extends EntityHeader> manager = managersByClass.get(clazz);
        if (manager != null && !(PersistentEntity.class.isAssignableFrom(clazz)))
            return null;
        if (!(manager instanceof EntityManager))
            return null;
        return (EntityManager)manager;
    }
}
