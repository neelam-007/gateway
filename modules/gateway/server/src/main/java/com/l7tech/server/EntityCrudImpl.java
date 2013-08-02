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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic CRUD for persistent entities.  Reads delegated to {@link EntityFinderImpl} unless some {@link EntityManager}
 * actually needs to interfere.
 *
 * @author alex
 */
@SuppressWarnings({"unchecked"})
@Transactional(propagation = Propagation.REQUIRED, rollbackFor=Throwable.class)
public class EntityCrudImpl extends HibernateDaoSupport implements EntityCrud {
    private final EntityFinder entityFinder;
    private final Map<Class<? extends Entity>, ReadOnlyEntityManager<? extends Entity, ? extends EntityHeader>> managersByClass;

    public EntityCrudImpl( final EntityFinder entityFinder, final ReadOnlyEntityManager... managers) {
        this.entityFinder = entityFinder;

        final Map<Class<? extends Entity>, ReadOnlyEntityManager<? extends Entity, ? extends EntityHeader>> managersByClass = new HashMap<Class<? extends Entity>, ReadOnlyEntityManager<? extends Entity,? extends EntityHeader>>();
        for (ReadOnlyEntityManager manager : managers) {
            managersByClass.put(manager.getImpClass(), manager);
        }
        this.managersByClass = Collections.unmodifiableMap(managersByClass);
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
        Entity ent;
        if (GoidEntity.class.isAssignableFrom(header.getType().getEntityClass())) {
            final GoidEntityManager manager = getGoidEntityManager(EntityTypeRegistry.getEntityClass(header.getType()));
            ent = manager != null ? manager.findByHeader(header) : entityFinder.find(header);
        } else {
            EntityManager manager = getEntityManager(EntityTypeRegistry.getEntityClass(header.getType()));
            ent = manager != null ? manager.findByHeader(header) : entityFinder.find(header);
        }

        if (ent instanceof IdentityProviderConfig)
            return new IdentityProviderConfig((IdentityProviderConfig)ent);
        else
            return ent;

        // todo: make entities clonable, so that we can return ent.copyOf();
    }

    @Override
    public <ET extends Entity> ET find(final Class<ET> clazz, final Serializable pk) throws FindException {
        ReadOnlyEntityManager manager = getReadOnlyManager(clazz);
        if (manager != null) {
            if (GoidEntity.class.isAssignableFrom(clazz)) {
                return (ET) manager.findByPrimaryKey((pk instanceof Goid) ? (Goid) pk : Goid.parseGoid(pk.toString()));
            } else if (PersistentEntity.class.isAssignableFrom(clazz)) {
                return (ET) manager.findByPrimaryKey(Long.valueOf(pk.toString()));
            }
        }
        ET ent = entityFinder.find(clazz, pk);

        if (ent instanceof IdentityProviderConfig)
            return (ET) new IdentityProviderConfig((IdentityProviderConfig)ent);
        else
            return ent;
    }

    @Override
    public EntityHeader findHeader(final EntityType etype, final Serializable pk) throws FindException {
        ReadOnlyEntityManager manager = getReadOnlyManager(EntityTypeRegistry.getEntityClass(etype));
        if (manager != null) {
            if (GoidEntity.class.isAssignableFrom(etype.getEntityClass())) {
                Entity ent = manager.findByPrimaryKey((pk instanceof Goid) ? (Goid) pk : Goid.parseGoid(pk.toString()));
                return ent == null ? null : EntityHeaderUtils.fromEntity(ent);
            } else if (PersistentEntity.class.isAssignableFrom(etype.getEntityClass())) {
                Entity ent = manager.findByPrimaryKey(Long.valueOf(pk.toString()));
                return ent == null ? null : EntityHeaderUtils.fromEntity(ent);
            }
        }
        return entityFinder.findHeader(etype, pk);
    }

    @Override
    public Serializable save(final Entity e) throws SaveException {
        if (e instanceof GoidEntity) {
            final GoidEntityManager manager = getGoidEntityManager(e.getClass());
            if (manager != null) {
                Serializable key = manager.save((GoidEntity) e);
                if (manager instanceof RoleAwareEntityManager) {
                    ((RoleAwareEntityManager) manager).createRoles((PersistentEntity) e);
                }
                return key;
            }
        } else {
            EntityManager manager = getEntityManager(e.getClass());
            if (manager != null) {
                Serializable key = manager.save((PersistentEntity) e);
                if (manager instanceof RoleAwareEntityManager) {
                    ((RoleAwareEntityManager) manager).createRoles((PersistentEntity) e);
                }
                return key;
            }
        }

        return (Serializable)getHibernateTemplate().execute(new HibernateCallback() {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                return session.save(e);
            }
        });
    }

    @Override
    public void update(final Entity e) throws UpdateException {
        if (e instanceof GoidEntity) {
            final GoidEntityManager manager = getGoidEntityManager(e.getClass());
            if (manager != null) {
                GoidEntity goidEntity = (GoidEntity) e;
                if (goidEntity instanceof HasFolder && manager instanceof FolderedEntityManager) {
                    ((FolderedEntityManager)manager).updateWithFolder(goidEntity);
                } else {
                    manager.update(goidEntity);
                }
                return;
            }
        } else {
            EntityManager manager = getEntityManager(e.getClass());
            if (manager != null) {
                manager.update((PersistentEntity) e);
                return;
            }
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
        if (e instanceof GoidEntity) {
            final GoidEntityManager manager = getGoidEntityManager(e.getClass());
            if (manager != null) {
                manager.delete((GoidEntity)e);
                if ( manager instanceof GoidRoleAwareEntityManager ) {
                    ((GoidRoleAwareEntityManager)manager).deleteRoles( ((GoidEntity) e).getGoid() );
                }
                return;
            }
        } else {
            EntityManager manager = getEntityManager(e.getClass());
            if (manager != null) {
                manager.delete((PersistentEntity)e);
                if ( manager instanceof RoleAwareEntityManager ) {
                    ((RoleAwareEntityManager)manager).deleteRoles( ((PersistentEntity) e).getOid() );
                }
                return;
            }
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
        if ( entity instanceof GoidEntity || entity instanceof PersistentEntity ) {
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
        final ReadOnlyEntityManager<? extends Entity, ? extends EntityHeader> manager = managersByClass.get(clazz);
        if (manager != null && !(PersistentEntity.class.isAssignableFrom(clazz)))
            return null;
        if (!(manager instanceof EntityManager))
            return null;
        return (EntityManager)manager;
    }

    private GoidEntityManager getGoidEntityManager(Class<? extends Entity> clazz) {
        final ReadOnlyEntityManager<? extends Entity, ? extends EntityHeader> manager = managersByClass.get(clazz);
        if (manager != null && !(GoidEntity.class.isAssignableFrom(clazz)))
            return null;
        if (!(manager instanceof GoidEntityManager))
            return null;
        return (GoidEntityManager)manager;
    }
}
