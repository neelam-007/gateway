/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.objectmodel.folder.FolderedEntityManager;
import com.l7tech.identity.IdentityProviderConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.hibernate.Session;
import org.hibernate.HibernateException;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.sql.SQLException;

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
        EntityManager manager = getManager(EntityTypeRegistry.getEntityClass(header.getType()));
        Entity ent = manager != null ? manager.findByHeader(header) : entityFinder.find(header);

        if (ent instanceof IdentityProviderConfig)
            return new IdentityProviderConfig((IdentityProviderConfig)ent);
        else
            return ent;

        // todo: make entities clonable, so that we can return ent.copyOf();
    }

    @Override
    public <ET extends Entity> ET find(final Class<ET> clazz, final Serializable pk) throws FindException {
        ReadOnlyEntityManager manager = getReadOnlyManager(clazz);
        if (manager != null)
            return (ET)manager.findByPrimaryKey(Long.valueOf(pk.toString()));
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
            Entity ent = manager.findByPrimaryKey(Long.valueOf(pk.toString()));
            return ent == null ? null : EntityHeaderUtils.fromEntity(ent);
        }
        return entityFinder.findHeader(etype, pk);
    }

    @Override
    public Serializable save(final Entity e) throws SaveException {
        final EntityManager manager = getManager(e.getClass());
        if ( manager != null ) {
            Serializable key = manager.save((PersistentEntity)e);
            if ( manager instanceof RoleAwareEntityManager ) {
                ((RoleAwareEntityManager)manager).createRoles( (PersistentEntity)e );
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
    public void update(final Entity e) throws UpdateException {
        final EntityManager manager = getManager(e.getClass());
        if (manager != null) {
            PersistentEntity pe = (PersistentEntity) e;
            if (e instanceof HasFolder && manager instanceof FolderedEntityManager) {
                ((FolderedEntityManager)manager).updateWithFolder(pe);
            } else {
                manager.update(pe);
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
        final EntityManager manager = getManager(e.getClass());
        if (manager != null) {
            manager.delete((PersistentEntity)e);
            if ( manager instanceof RoleAwareEntityManager ) {
                ((RoleAwareEntityManager)manager).deleteRoles( ((PersistentEntity) e).getOid() );
            }
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
        if ( entity instanceof PersistentEntity ) {
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
    public Collection<Entity> findByEntityTypeAndSecurityZoneOid(@NotNull final EntityType type, final long securityZoneOid) throws FindException {
        return entityFinder.findByEntityTypeAndSecurityZoneOid(type, securityZoneOid);
    }

    @Override
    public void setSecurityZoneForEntities(@Nullable final Long securityZoneOid, @NotNull final EntityType entityType, @NotNull final Collection<Long> entityOids) throws UpdateException {
        if (!entityType.isSecurityZoneable()) {
            throw new IllegalArgumentException("Entity type must be security zoneable");
        }
        try{
            final SecurityZone securityZone = securityZoneOid != null ? find(SecurityZone.class, securityZoneOid) : null;
                if (securityZoneOid == null || (securityZoneOid != null && securityZone != null)) {
                    for (final Long entityOid : entityOids) {
                        final Entity entity = find(entityType.getEntityClass(), entityOid);
                        if (entity instanceof ZoneableEntity) {
                            final ZoneableEntity zoneable = (ZoneableEntity) entity;
                            zoneable.setSecurityZone(securityZone);
                            update(entity);
                        } else {
                            throw new UpdateException(entityType.getName() + " with oid " + entityOid + " does not exist or is not security zoneable");
                        }
                    }
                } else {
                    throw new UpdateException("Security zone with oid " + securityZoneOid + " does not exist");
                }
        } catch (final FindException e) {
            throw new UpdateException("Unable to set security zone for entities: " + e.getMessage(), e);
        }
    }

    private ReadOnlyEntityManager getReadOnlyManager(Class<? extends Entity> clazz) {
        return managersByClass.get(clazz);
    }

    private EntityManager getManager(Class<? extends Entity> clazz) {
        final ReadOnlyEntityManager<? extends Entity, ? extends EntityHeader> manager = managersByClass.get(clazz);
        if (manager != null && !(PersistentEntity.class.isAssignableFrom(clazz)))
            return null;
        if (!(manager instanceof EntityManager))
            return null;
        return (EntityManager)manager;
    }
}
