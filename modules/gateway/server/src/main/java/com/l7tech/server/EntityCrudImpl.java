/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.objectmodel.*;
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
@Transactional(propagation = Propagation.REQUIRED)
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
    public EntityHeaderSet<EntityHeader> findAll(final Class<? extends Entity> entityClass, final String filter, final int offset, final int max) throws FindException {
        ReadOnlyEntityManager manager = getReadOnlyManager(entityClass);
        if ( manager != null ){
            Collection<EntityHeader> headers;
            if ( manager instanceof SearchableEntityManager ) {
                headers = ((SearchableEntityManager)manager).findHeaders( offset, max, filter );
            } else {
                headers = manager.findAllHeaders( offset, max );
            }
            return new EntityHeaderSet<EntityHeader>((EntityHeader[])headers.toArray(new EntityHeader[headers.size()]));
        }
        return entityFinder.findAll(entityClass);
    }

    @Override
    public Entity find(final EntityHeader header) throws FindException {
        ReadOnlyEntityManager manager = getReadOnlyManager(EntityTypeRegistry.getEntityClass(header.getType()));
        if (manager != null) return manager.findByPrimaryKey(header.getOid());
        return entityFinder.find(header);
    }

    @Override
    public <ET extends Entity> ET find(final Class<ET> clazz, final Serializable pk) throws FindException {
        ReadOnlyEntityManager manager = getReadOnlyManager(clazz);
        if (manager != null)
            return (ET)manager.findByPrimaryKey(Long.valueOf(pk.toString()));
        return entityFinder.find(clazz, pk);
    }

    @Override
    public EntityHeader findHeader(final EntityType etype, final Serializable pk) throws FindException {
        ReadOnlyEntityManager manager = getReadOnlyManager(EntityTypeRegistry.getEntityClass(etype));
        if (manager != null) {
            return EntityHeaderUtils.fromEntity(manager.findByPrimaryKey(Long.valueOf(pk.toString())));
        }
        return entityFinder.findHeader(etype, pk);
    }

    @Override
    public Serializable save(final Entity e) throws SaveException {
        final EntityManager manager = getManager(e.getClass());
        if (manager != null)
            return manager.save((PersistentEntity)e);

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
            manager.update((PersistentEntity)e);
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

    private ReadOnlyEntityManager getReadOnlyManager(Class<? extends Entity> clazz) {
        return managersByClass.get(clazz);
    }

    private EntityManager getManager(Class<? extends Entity> clazz) {
        final ReadOnlyEntityManager<? extends Entity, ? extends EntityHeader> manager = managersByClass.get(clazz);
        if (manager != null && !(PersistentEntity.class.isAssignableFrom(clazz)))
            throw new IllegalArgumentException(clazz.getSimpleName() + " is not a PersistentEntity");
        if (!(manager instanceof EntityManager))
            throw new IllegalArgumentException("Writeable EntityManager not available.");
        return (EntityManager)manager;
    }
}
