/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

import com.l7tech.objectmodel.Entity;

/**
 * The known stereotypes that persistence-related admin APIs conform to.  Admin APIs that do not conform
 * to any of the stereotypes listed below cannot currently be secured with RBAC, so the SSG's RBAC enforcement
 * layer will throw a RuntimeException if they are encountered.
 */
public enum MethodStereotype {
    /**
     * The method returns one or more {@link EntityHeader}s.
     */
    FIND_HEADERS,

    /**
     * The method returns one or more {@link Entity}s.  If the method's return type is a {@link java.util.Collection},
     * the contents of the collection will be filtered so that only entities for which the caller has a
     * {@link OperationType#READ} permission will be returned. If the method's return type is not a collection, the
     * caller must have {@link OperationType#READ} permission on <em>all</em> entities with the specified
     * {@link EntityType}.
     */
    FIND_ENTITIES,

    /**
     * The method returns one {@link Entity}, and takes a primary key (e.g. a long) as its sole argument.  Caller must
     * hold {@link OperationType#READ} permission on the returned entity.
     */
    FIND_BY_PRIMARY_KEY,

    /**
     * The method returns one {@link Entity}, and takes an attribute value as its sole argument.  Caller must hold
     * {@link OperationType#READ} permission on the returned entity.
     */
    FIND_ENTITY_BY_ATTRIBUTE,

    /**
     * The sole argument must be an {@link Entity}, and the caller must hold {@link OperationType#CREATE} permission on
     * it.
     */
    SAVE,

    /**
     * The sole argument must be an {@link Entity}, and the caller must hold {@link OperationType#UPDATE} permission on
     * it.
     */
    UPDATE,

    /**
     * If the sole argument is an {@link Entity}, the caller must hold either {@link OperationType#CREATE} or
     * {@link OperationType#UPDATE} permission on it, depending on whether
     * {@link Entity#getOid()} == {@link Entity#DEFAULT_OID}.
     */
    SAVE_OR_UPDATE,

    /**
     * The sole argument must be a <code>long</code>, and the caller must hold {@link OperationType#DELETE} permission
     * on the corresonding {@link Entity}.
     */
    DELETE_BY_OID,

    DELETE_BY_UNIQUE_ATTRIBUTE,

    /**
     * The sole argument must be an {@link Entity}, and the caller must hold {@link OperationType#DELETE} permission on
     * it.
     */
    DELETE_ENTITY,

    /**
     * The arguments can be anything, but the caller must hold {@link OperationType#DELETE} permission for all entities
     * of the specified type.
     */
    DELETE_MULTI,

    /**
     * The sole argument must be an {@link Entity}, and the caller must hold {@link OperationType#READ} permission on
     * it.
     */
    GET_PROPERTY_OF_ENTITY,

    /**
     * The sole argument must be a {@link Long} or {@link String}, and the caller must hold {@link OperationType#READ}
     * permission on the
     * corresponding {@link Entity}.
     */
    GET_PROPERTY_BY_OID,

    /**
     * Exactly one argument must be an {@link Entity}, and the caller must hold {@link OperationType#UPDATE} permission
     * on it.
     */
    SET_PROPERTY_OF_ENTITY,

    /**
     * Exactly one argument must be a {@link Long} or {@link String}, and the caller must hold
     * {@link OperationType#UPDATE} permission on the corresponding {@link Entity}.
     */
    SET_PROPERTY_BY_OID,

    SET_PROPERTY_BY_UNIQUE_ATTRIBUTE,

    /**
     * The method does not conform to any known stereotype.  Not currently supported (will throw an exception at
     * runtime).
     */
    NONE,
    ;
}
