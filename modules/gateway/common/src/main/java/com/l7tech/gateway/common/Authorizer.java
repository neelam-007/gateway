/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.gateway.common;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>Authorizer</code> abstract class provide authorization methods for
 * roles and access permissions. The concrete implementaiton implement the abstract
 * method {@link Authorizer#getUserPermissions()}
 *
 * @author emil
 * @version Sep 2, 2004
 */
public abstract class Authorizer {
    private static final Logger logger = Logger.getLogger(Authorizer.class.getName());

    //- PUBLIC

    public boolean hasPermission( final Class<?> permissionTarget ) {
        return hasPermission( permissionTarget == null ? null : permissionTarget.getAnnotation( RequiredPermissionSet.class ) );
    }

    public boolean hasPermission( final RequiredPermissionSet permissionSet ) {
        boolean permitted = false;

        if ( permissionSet != null ) {
            if ( RequiredPermissionSet.Type.ANY == permissionSet.type() ) {
                for ( RequiredPermission permission : permissionSet.requiredPermissions() ) {
                    if ( hasPermission( asAttemptedOperation(permission) ) ) {
                        permitted = true;
                        break;
                    }
                }
            } else {
                boolean granted = true;
                for ( RequiredPermission permission : permissionSet.requiredPermissions() ) {
                    if ( !hasPermission( asAttemptedOperation(permission) ) ) {
                        granted = false;
                        break;
                    }
                }
                permitted = granted;
            }
        }

        return permitted;
    }

    public boolean hasPermission(AttemptedOperation attempted) {
        Collection<Permission> perms = getUserPermissions();
        if (attempted == null || perms == null || perms.isEmpty()) return false;

        for ( com.l7tech.gateway.common.security.rbac.Permission perm : perms) {
            if (perm.getEntityType() != EntityType.ANY && perm.getEntityType() != attempted.getType()) continue;
            if (perm.getOperation() != attempted.getOperation()) {
                if (attempted instanceof AttemptedAnyOperation) {
                    return true;
                } else {
                    continue;
                }
            }

            if (attempted instanceof AttemptedEntityOperation) {
                // Permission grants read access to anything with matching type
                if (perm.getScope() == null || perm.getScope().size() == 0) return true;

                AttemptedEntityOperation attemptedEntityOperation = (AttemptedEntityOperation) attempted;
                if ( attemptedEntityOperation.getEntity() != null ) {
                    if ( perm.getScope().size() == 1 ) {
                        ScopePredicate pred = perm.getScope().iterator().next();
                        if ( pred instanceof ScopeEvaluator ) {
                            try {
                                if (((ScopeEvaluator)pred).matches( attemptedEntityOperation.getEntity() ))
                                    return true;
                            } catch ( Exception e ) {
                                // check other permissions
                            }
                        }
                    }
                }
            } else if (attempted instanceof AttemptedCreate) {
                // give the benefit of the doubt unless you are sure they won't be able to create the entity
                boolean canCreate = true;
                if (attempted.getType() != EntityType.ANY) {
                    try {
                        final Collection<EntityType> zonePermittedTypes = getZonePermittedTypes(OperationType.CREATE);
                        if (zonePermittedTypes != null) {
                            // if user permissions only have security zone predicates, we can accurately determine if
                            // they can create this entity type by looking at the permitted entity types on the zone(s)
                            canCreate = zonePermittedTypes.contains(attempted.getType());
                        } else {
                            // no zone predicates to check against
                        }
                    } catch (final ComplexScopeException e) {
                        logger.log(Level.FINE, "Permission scope is too complex to determine authorization for " + attempted);
                    }
                }
                return canCreate;
            } else if (attempted instanceof AttemptedReadAny) {
                // EntityType and Operation already match
                return true;
            } else if (attempted instanceof AttemptedReadAll) {
                if ( perm.getScope().isEmpty() ) return true;
            } else if (attempted instanceof AttemptedDeleteAll) {
                if ( perm.getScope().isEmpty() ) return true;
            } else if (attempted instanceof AttemptedUpdateAny){
                // EntityType and Operation already match
                return true;
            } else if (attempted instanceof AttemptedUpdateAll) {
                if ( perm.getScope().isEmpty() ) return true;
            } else if(attempted instanceof AttemptedOther){
                AttemptedOther attemptedOther = (AttemptedOther) attempted;
                if (perm.getOtherOperationName()!=null && perm.getOtherOperationName().equals(attemptedOther.getOtherOperationName())){
                    return true;
                }
            }
        }
        return false;
    }

    public abstract Collection<Permission> getUserPermissions() throws RuntimeException;

    //- PRIVATE

    private AttemptedOperation asAttemptedOperation( final RequiredPermission permission ) {
        AttemptedOperation operation = null;

        switch ( permission.operationType() ) {
            case CREATE:
                operation = new AttemptedCreate( permission.entityType() );
                break;
            case READ:
                operation = new AttemptedReadAny( permission.entityType() );
                break;
            case UPDATE:
                operation = new AttemptedUpdateAny( permission.entityType() );
                break;
            case DELETE:
                operation = new AttemptedDeleteAll( permission.entityType() );
                break;
        }

        return operation;
    }

    /**
     * Scope is too complex to determine authorization.
     */
    protected class ComplexScopeException extends Exception {
        private ComplexScopeException(final String message) {
            super(message);
        }
    }

    /**
     * Get a collection of EntityType that the user is allowed to operate on due to the presence of one/more SecurityZonePredicate on their permissions.
     *
     * @param operation the relevant OperationType.
     * @return a collection of EntityType that the user is allowed to operate on due to the presence of one/more
     *         SecurityZonePredicate on their permissions or null if the user does not have any SecurityZonePredicate on their permissions.
     * @throws ComplexScopeException if the user permissions contains predicates other than SecurityZonePredicate
     *                               which require more information to determine if the user has entity type permission for the operation.
     */
    @Nullable
    private Collection<EntityType> getZonePermittedTypes(final OperationType operation) throws ComplexScopeException {
        Set<EntityType> permittedTypes = null;
        for (final Permission permission : getUserPermissions()) {
            if (permission.getOperation() == operation) {
                for (final ScopePredicate predicate : permission.getScope()) {
                    if (predicate instanceof SecurityZonePredicate) {
                        if (permittedTypes == null) {
                            permittedTypes = new HashSet<>();
                        }
                        final SecurityZonePredicate zonePredicate = (SecurityZonePredicate) predicate;
                        final Set<EntityType> typesOnZone = zonePredicate.getRequiredZone().getPermittedEntityTypes();
                        if (typesOnZone.contains(EntityType.ANY)) {
                            permittedTypes.addAll(Arrays.asList(EntityType.values()));
                        } else {
                            permittedTypes.addAll(typesOnZone);
                        }
                    } else {
                        throw new ComplexScopeException("Permissions contain predicates other than SecurityZonePredicates.");
                    }
                }
            }
        }
        return permittedTypes;
    }
}