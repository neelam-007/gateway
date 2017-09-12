/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Generic CRUD access to persistent entities
 *
 * @author alex
 */
public interface EntityCrud extends EntityFinder, SecurityZoneEntityManager {
    @Secured(stereotype=MethodStereotype.SAVE)
    Serializable save(Entity entity) throws SaveException;

    @Secured(stereotype=MethodStereotype.SAVE)
    void save(Goid id, Entity entity) throws SaveException;

    @Secured(stereotype=MethodStereotype.UPDATE)
    void update(Entity entity) throws UpdateException;

    @Secured(stereotype=MethodStereotype.DELETE_ENTITY)
    void delete(Entity entity) throws DeleteException;

    @Secured(stereotype=MethodStereotype.NONE)
    default void addEntityManagers(List<ReadOnlyEntityManager<? extends Entity, ? extends EntityHeader>> managers) {
        throw new RuntimeException("Unexpected call to addEntityManagers on EntityCrud that does not extend from EntityCrudImpl");
    }

    @Secured(stereotype=MethodStereotype.FIND_HEADERS)
    @Override
    EntityHeaderSet<EntityHeader> findAll(Class<? extends Entity> entityClass) throws FindException;

    @Secured(stereotype=MethodStereotype.FIND_HEADERS)
    EntityHeaderSet<EntityHeader> findAll(Class<? extends Entity> entityClass, Map<String,String> filters, int offset, int max) throws FindException;

    @Secured(stereotype=MethodStereotype.FIND_ENTITIES)
    <ET extends Entity> List<ET> findAll(Class<ET> entityClass, Map<String, List<Object>> filters, int offset, int max, Boolean ascending, String sortKey) throws FindException;

    @Secured(stereotype=MethodStereotype.FIND_HEADERS)
    EntityHeaderSet<EntityHeader> findAllInScope(Class<? extends Entity> entityClass, EntityHeader header, Map<String,String> filters, int offset, int max) throws FindException;

    @Secured(stereotype=MethodStereotype.FIND_ENTITIES)
    @Override
    Entity find(@NotNull EntityHeader header) throws FindException;

    @Secured(stereotype=MethodStereotype.FIND_ENTITIES)
    @Override
    <ET extends Entity> ET find(Class<ET> clazz, Serializable pk) throws FindException;

    @Secured(stereotype=MethodStereotype.FIND_HEADERS)
    @Override
    EntityHeader findHeader(EntityType etype, Serializable pk) throws FindException;

    void evict( Entity entity );
}
