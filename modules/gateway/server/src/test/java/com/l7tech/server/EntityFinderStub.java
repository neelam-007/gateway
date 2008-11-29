/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.objectmodel.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Stub implementation of {@link EntityFinder} that delegates to an {@link EntityManager} selected based on the
 * manager's declared {@link EntityManager#getImpClass}.
 *  
 * @author alex
 */
public class EntityFinderStub implements EntityFinder {
    private final Map<Class<? extends Entity>, EntityManager> entityManagers;

    public EntityFinderStub(EntityManager... entityManagers) {
        Map<Class<? extends Entity>, EntityManager> managerMap = new HashMap<Class<? extends Entity>, EntityManager>();
        for (EntityManager entityManager : entityManagers) {
            managerMap.put(entityManager.getImpClass(), entityManager);
        }
        this.entityManagers = managerMap;
    }

    @Override
    public EntityHeaderSet<EntityHeader> findAll(Class<? extends Entity> entityClass) throws FindException {
        return new EntityHeaderSet((EntityHeader[])entityManagers.get(entityClass).findAllHeaders().toArray(new EntityHeader[0]));
    }

    @Override
    public Entity find(EntityHeader header) throws FindException {
        return entityManagers.get(EntityTypeRegistry.getEntityClass(header.getType())).findByPrimaryKey(header.getOid());
    }

    @Override
    public <ET extends Entity> ET find(Class<ET> clazz, Serializable pk) throws FindException {
        return (ET)entityManagers.get(clazz).findByPrimaryKey(Long.valueOf(pk.toString()));
    }

    @Override
    public EntityHeader findHeader(EntityType etype, Serializable pk) throws FindException {
        Entity e = entityManagers.get(EntityTypeRegistry.getEntityClass(etype)).findByPrimaryKey(Long.valueOf(pk.toString()));
        return EntityHeaderUtils.fromEntity(e);
    }
}
