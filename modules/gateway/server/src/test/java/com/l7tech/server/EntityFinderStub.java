/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.objectmodel.*;
import com.l7tech.policy.DesignTimeEntityProvider;
import com.l7tech.policy.assertion.UsesEntitiesAtDesignTime;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Stub implementation of {@link EntityFinder} that delegates to an {@link com.l7tech.objectmodel.EntityManager} selected based on the
 * manager's declared {@link com.l7tech.objectmodel.EntityManager#getImpClass}.
 *  
 * @author alex
 */
public class EntityFinderStub implements EntityFinder, DesignTimeEntityProvider {
    private final Map<Class<? extends Entity>, EntityManager> entityManagers;

    public EntityFinderStub() {
        this.entityManagers = new HashMap<Class<? extends Entity>, EntityManager>();
    }

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
    public Entity find(@NotNull EntityHeader header) throws FindException {
        return entityManagers.get(EntityTypeRegistry.getEntityClass(header.getType())).findByPrimaryKey(header.getGoid());
    }

    @Override
    public <ET extends Entity> ET find(Class<ET> clazz, Serializable pk) throws FindException {
        return (ET)entityManagers.get(clazz).findByPrimaryKey(Goid.parseGoid(pk.toString()));
    }

    @Override
    public EntityHeader findHeader(EntityType etype, Serializable pk) throws FindException {
        Entity e = entityManagers.get(EntityTypeRegistry.getEntityClass(etype)).findByPrimaryKey(Goid.parseGoid(pk.toString()));
        return EntityHeaderUtils.fromEntity(e);
    }

    @Override
    public void provideNeededEntities(@NotNull UsesEntitiesAtDesignTime entityUser, @Nullable Functions.BinaryVoid<EntityHeader, FindException> errorHandler) throws FindException {
    }

    @Override
    public Collection<EntityHeader> findByEntityTypeAndSecurityZoneGoid(@NotNull EntityType type, Goid securityZoneGoid) throws FindException {
        return null;
    }
}
