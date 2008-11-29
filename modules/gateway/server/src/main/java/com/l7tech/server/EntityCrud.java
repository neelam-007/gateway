/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;

import java.io.Serializable;

/**
 * Generic CRUD access to persistent entities
 *
 * @author alex
 */
public interface EntityCrud extends EntityFinder {
    @Secured(stereotype=MethodStereotype.SAVE)
    Serializable save(Entity entity) throws SaveException;

    @Secured(stereotype=MethodStereotype.UPDATE)
    void update(Entity entity) throws UpdateException;

    @Secured(stereotype=MethodStereotype.DELETE_ENTITY)
    void delete(Entity entity) throws DeleteException;

    @Secured(stereotype=MethodStereotype.FIND_HEADERS)
    @Override
    EntityHeaderSet<EntityHeader> findAll(Class<? extends Entity> entityClass) throws FindException;

    @Secured(stereotype=MethodStereotype.FIND_ENTITIES)
    @Override
    Entity find(EntityHeader header) throws FindException;

    @Secured(stereotype=MethodStereotype.FIND_ENTITIES)
    @Override
    <ET extends Entity> ET find(Class<ET> clazz, Serializable pk) throws FindException;

    // TODO [steve] new method sterotype for FIND_HEADER
    //@Secured(stereotype=MethodStereotype.FIND_HEADERS, operation=OperationType.READ, inferType=true)
    @Override
    EntityHeader findHeader(EntityType etype, Serializable pk) throws FindException;
}
