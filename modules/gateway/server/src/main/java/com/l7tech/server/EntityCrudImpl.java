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
import java.sql.SQLException;

/**
 * Generic CRUD for persistent entities.  Reads delegated to {@link EntityFinderImpl} unless some {@link EntityManager}
 * actually needs to interfere.
 *
 * @author alex
 */
@Transactional(propagation = Propagation.REQUIRED)
public class EntityCrudImpl extends HibernateDaoSupport implements EntityCrud {
    private final EntityFinder entityFinder;
    private final Map<Class<? extends Entity>, EntityManager<? extends Entity, ? extends EntityHeader>> managersByClass;

    public EntityCrudImpl(EntityFinder entityFinder, EntityManager... managers) {
        this.entityFinder = entityFinder;

        final Map<Class<? extends Entity>, EntityManager<? extends Entity, ? extends EntityHeader>> managersByClass = new HashMap<Class<? extends Entity>, EntityManager<? extends Entity,? extends EntityHeader>>();
        for (EntityManager manager : managers) {
            managersByClass.put(manager.getImpClass(), manager);
        }
        this.managersByClass = Collections.unmodifiableMap(managersByClass);
    }

    public EntityHeaderSet<EntityHeader> findAll(Class<? extends Entity> entityClass) throws FindException {
        EntityManager manager = getManager(entityClass);
        if (manager != null) return new EntityHeaderSet<EntityHeader>((EntityHeader[])manager.findAllHeaders().toArray(new EntityHeader[0]));
        return entityFinder.findAll(entityClass);
    }

    public Entity find(EntityHeader header) throws FindException {
        EntityManager manager = getManager(EntityTypeRegistry.getEntityClass(header.getType()));
        if (manager != null) return manager.findByPrimaryKey(header.getOid());
        return entityFinder.find(header);
    }

    public <ET extends Entity> ET find(Class<ET> clazz, Serializable pk) throws FindException {
        EntityManager manager = getManager(clazz);
        if (manager != null)
            return (ET)manager.findByPrimaryKey(Long.valueOf(pk.toString()));
        return entityFinder.find(clazz, pk);
    }

    public EntityHeader findHeader(EntityType etype, Serializable pk) throws FindException {
        EntityManager manager = getManager(EntityTypeRegistry.getEntityClass(etype));
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

    private EntityManager getManager(Class<? extends Entity> clazz) {
        final EntityManager<? extends Entity, ? extends EntityHeader> manager = managersByClass.get(clazz);
        if (manager != null && !(PersistentEntity.class.isAssignableFrom(clazz)))
            throw new IllegalArgumentException(clazz.getSimpleName() + " is not a PersistentEntity");
        return manager;
    }
}
