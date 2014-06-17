/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.gateway.common;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.objectmodel.EntityType;

import java.util.Collection;

/**
 * The <code>Authorizer</code> abstract class provide authorization methods for
 * roles and access permissions. The concrete implementaiton implement the abstract
 * method {@link Authorizer#getUserPermissions()}
 *
 * @author emil
 * @version Sep 2, 2004
 */
public abstract class Authorizer {

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

            if (attempted instanceof AttemptedOtherSpecific) {
                AttemptedOtherSpecific attemptedOtherSpecific = (AttemptedOtherSpecific) attempted;
                if (perm.getOtherOperationName() == null || !perm.getOtherOperationName().equals(attemptedOtherSpecific.getOtherOperationName())) {
                    continue;
                }
            }

            if (attempted instanceof AttemptedEntityOperation) {
                // Permission grants read access to anything with matching type
                if (perm.getScope() == null || perm.getScope().size() == 0) return true;

                final AttemptedEntityOperation attemptedEntityOperation = (AttemptedEntityOperation) attempted;
                if ( attemptedEntityOperation.getEntity() != null ) {
                    boolean allPredicatesMatch = true;
                    for (final ScopePredicate scopePredicate : perm.getScope()) {
                        if (scopePredicate instanceof ScopeEvaluator && !((ScopeEvaluator)scopePredicate).matches(attemptedEntityOperation.getEntity())) {
                            allPredicatesMatch = false;
                            break;
                        }
                    }
                    if (allPredicatesMatch) {
                        return true;
                    }
                }
            } else if (attempted instanceof AttemptedCreate) {
                // CREATE doesn't support any scope yet, only a type
                return true;
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
}