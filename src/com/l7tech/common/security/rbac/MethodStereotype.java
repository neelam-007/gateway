/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

/**
 * The known stereotypes that persistence-related admin APIs conform to.  Admin APIs that do not conform
 * to any of the stereotypes listed below cannot currently be secured with RBAC, so the SSG's RBAC enforcement
 * layer will throw a RuntimeException if they are encountered.
 */
public enum MethodStereotype {
    /**
     * The method returns one or more {@link EntityHeader}s.
     *
     * If the method's return type is a {@link java.util.Collection} or an array of
     * {@link com.l7tech.objectmodel.EntityHeader}, the contents of the collection will be filtered so that only
     * headers for which the caller has {@link OperationType#READ} permission on the corresponding ntity will be
     * returned.
     *
     * If the method's return type is not a collection or an array, the caller must have {@link OperationType#READ}
     * permission on <em>all</em> entities with the specified {@link EntityType}.
     */
    FIND_HEADERS,

    /**
     * The method returns one or more {@link com.l7tech.objectmodel.Entity}s.
     *
     * If the method's return type is a {@link java.util.Collection} or an array of
     * {@link com.l7tech.objectmodel.Entity}, the contents of the collection will be filtered so that only entities for
     * which the caller has {@link OperationType#READ} permission will be returned.
     *
     * If the method's return type is not a collection or an array, the caller must have {@link OperationType#READ}
     * permission on <em>all</em> entities with the specified {@link EntityType}.
     */
    FIND_ENTITIES,

    /**
     * The method returns one {@link com.l7tech.objectmodel.Entity}, and takes a primary key (e.g. a long)
     * as a {@link Secured#relevantArg} argument.  Caller must hold {@link OperationType#READ} permission
     * on the returned entity.
     */
    FIND_BY_PRIMARY_KEY,

    /**
     * The method returns one {@link com.l7tech.objectmodel.Entity}, and takes an attribute value as a
     * {@link Secured#relevantArg} argument.  Caller must hold {@link OperationType#READ} permission
     * on the returned entity.
     */
    FIND_ENTITY_BY_ATTRIBUTE,

    /**
     * A {@link Secured#relevantArg} argument must be an {@link com.l7tech.objectmodel.Entity}, and the caller must hold 
     * {@link OperationType#CREATE} permission on it.
     */
    SAVE,

    /**
     * A {@link Secured#relevantArg} argument must be an {@link com.l7tech.objectmodel.Entity}, and the caller must hold
     * {@link OperationType#UPDATE} permission on it.
     */
    UPDATE,

    /**
     * If a {@link Secured#relevantArg} argument is an {@link com.l7tech.objectmodel.Entity}, the caller must hold
     * {@link OperationType#CREATE} permission on it, if {@link com.l7tech.objectmodel.Entity#getId} is null or
     * {@link com.l7tech.objectmodel.PersistentEntity#DEFAULT_OID}, or {@link OperationType#UPDATE} permission if not.
     */
    SAVE_OR_UPDATE,

    /**
     * A {@link Secured#relevantArg} argument must be a <code>long</code>, and the caller must hold
     * {@link OperationType#DELETE} permission on the corresonding {@link com.l7tech.objectmodel.Entity}.
     */
    DELETE_BY_ID,

    /**
     * A {@link Secured#relevantArg} argument must be a <code>long</code>, and the caller must hold
     * {@link OperationType#DELETE} permission on the corresonding {@link com.l7tech.objectmodel.Entity}.
     */
    DELETE_IDENTITY_BY_ID,

    DELETE_BY_UNIQUE_ATTRIBUTE,

    /**
     * A {@link Secured#relevantArg} argument must be an {@link com.l7tech.objectmodel.Entity}, and the caller must hold
     * {@link OperationType#DELETE} permission on it.
     */
    DELETE_ENTITY,

    /**
     * The arguments can be anything, but the caller must hold {@link OperationType#DELETE} permission for all entities
     * of the specified type.
     */
    DELETE_MULTI,

    /**
     * A {@link Secured#relevantArg} argument must be an {@link com.l7tech.objectmodel.Entity}, and the caller must hold
     * {@link OperationType#READ} permission on it.
     */
    GET_PROPERTY_OF_ENTITY,

    /**
     * A {@link Secured#relevantArg} argument must be a {@link Long} or {@link String}, and the caller must hold
     * {@link OperationType#READ} permission on the corresponding {@link com.l7tech.objectmodel.Entity}.
     */
    GET_IDENTITY_PROPERTY_BY_ID,

    /**
     * A {@link Secured#relevantArg} argument must be an {@link com.l7tech.objectmodel.Entity}, and the caller must hold
     * {@link OperationType#UPDATE} permission on it.
     */
    SET_PROPERTY_OF_ENTITY,

    /**
     * A {@link Secured#relevantArg} argument argument must be a {@link Long} or {@link String}, and the caller must
     * hold {@link OperationType#UPDATE} permission on the corresponding {@link com.l7tech.objectmodel.Entity}.
     */
    SET_PROPERTY_BY_ID,

    /**
     * Caller must hold {@link OperationType#UPDATE} permission on all entities of the declared type (we are currently
     * unable to find entities by unique attributes in a generic way)  
     */
    SET_PROPERTY_BY_UNIQUE_ATTRIBUTE,

    /**
     * The method does not conform to any known stereotype.
     *
     * Not currently supported (will throw an exception at runtime).
     */
    NONE,
    ;
}
