/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import static com.l7tech.objectmodel.EntityType.ANY;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.OrganizationHeader;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.event.EntityInvalidationEvent;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/** @author alex */
public class RbacServicesImpl implements RbacServices, InitializingBean, ApplicationListener {
    private static final Logger logger = Logger.getLogger(RbacServicesImpl.class.getName());

    private RoleManager roleManager;

    public RbacServicesImpl(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    public RbacServicesImpl() {
    }

    @Override
    public boolean isPermittedForEntitiesOfTypes(User authenticatedUser, OperationType requiredOperation, Set<EntityType> requiredTypes)
        throws FindException {
        if (authenticatedUser == null) throw new IllegalArgumentException();
        if (requiredTypes == null || requiredTypes.isEmpty()) throw new IllegalArgumentException();
        if (requiredOperation == null || !OperationType.ALL_CRUD.contains(requiredOperation)) throw new IllegalArgumentException();

        final Map<EntityType, Boolean> permittedTypes = new HashMap<EntityType, Boolean>();

        for (Role role : roleManager.getAssignedRoles(authenticatedUser)) {
            for (Permission perm : role.getPermissions()) {
                if (perm.getScope() != null && !perm.getScope().isEmpty()) continue; // This permission is too restrictive
                if (perm.getOperation() != requiredOperation) continue; // This permission is for a different operation
                EntityType ptype = perm.getEntityType();

                if (ptype == ANY) return true; // Permitted against all types
                permittedTypes.put(ptype, true); // Permitted for this type
            }
        }

        if (permittedTypes.isEmpty()) return false; // Not permitted on any type

        for (EntityType requiredType : requiredTypes) {
            Boolean permittedType = permittedTypes.get(requiredType);
            if (permittedType == null) return false; // Required type is not permitted
        }

        return true;
    }

    @Override
    public boolean isPermittedForAnyEntityOfType(User authenticatedUser, OperationType requiredOperation, EntityType requiredType)
        throws FindException {
        if (authenticatedUser == null || requiredType == null) throw new NullPointerException();
        logger.log(Level.FINE, "Checking for permission to {0} any {1}", new Object[] { requiredOperation.getName(), requiredType.getName()});
        for (Role role : roleManager.getAssignedRoles(authenticatedUser)) {
            for (Permission perm : role.getPermissions()) {
                if (perm.getScope() != null && !perm.getScope().isEmpty()) continue; // This permission is too restrictive
                if (perm.getOperation() != requiredOperation) continue; // This permission is for a different operation
                EntityType ptype = perm.getEntityType();
                if (ptype == ANY || ptype == requiredType) return true;
            }
        }
        return false;
    }

    @Override
    public boolean isPermittedForEntity(User user, Entity entity, OperationType operation, String otherOperationName)
        throws FindException {
        if (user == null || entity == null || operation == null) throw new NullPointerException();
        if (operation == OperationType.OTHER && otherOperationName == null) throw new IllegalArgumentException("otherOperationName must be specified when operation == OTHER");
        logger.log(Level.FINE, "Checking for permission to {0} {1} #{2}", new Object[] { operation.getName(), entity.getClass().getSimpleName(), entity.getId()});

        Collection<Role> assignedRoles = roleManager.getAssignedRoles(user);
        for (Role role : assignedRoles) {
            for (Permission perm : role.getPermissions()) {
                if (perm.matches(entity) && perm.getOperation() == operation) {
                    if (operation != OperationType.OTHER && operation != OperationType.NONE) {
                        return true;
                    } else {
                        if (otherOperationName.equals(perm.getOtherOperationName())) return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public <T extends OrganizationHeader> Iterable<T> filterPermittedHeaders(User authenticatedUser, OperationType requiredOperation, Iterable<T> headers, EntityFinder entityFinder)
        throws FindException {
        if (authenticatedUser == null) throw new IllegalArgumentException();
        if (requiredOperation == null || !OperationType.ALL_CRUD.contains(requiredOperation)) throw new IllegalArgumentException();

        // If we already have blanket permission for this type, just return the original collection
        // however as the SSM now shows services and policies together, blanket only applies if the user has it on
        // all entites which can be shown in the tree
        if (isPermittedForAnyEntityOfType(authenticatedUser, requiredOperation, EntityType.SERVICE)
                && isPermittedForAnyEntityOfType(authenticatedUser, requiredOperation, EntityType.POLICY)) return headers;

        // Do this outside the loop so performance isn't appalling
        final Collection<Role> userRoles = roleManager.getAssignedRoles(authenticatedUser);
        if (userRoles.isEmpty()) return Collections.emptyList();

        final List<T> result = new LinkedList<T>();
        for (final T header : headers) {
            final Entity entity;
            try {
                entity = entityFinder.find(header);
                if (entity == null) continue;
            } catch (FindException e) {
                logger.log(Level.WARNING, MessageFormat.format("Unable to find entity for header: {0}; skipping", header), e);
                continue;
            }

            if (isPermitted(userRoles, entity, requiredOperation, null))
                result.add(header);
        }

        return result;
    }

    private boolean isPermitted(Collection<Role> assignedRoles, Entity entity, OperationType operation, String otherOperationName) {
        for (Role role : assignedRoles) {
            for (Permission perm : role.getPermissions()) {
                if (perm.matches(entity) && perm.getOperation() == operation) {
                    if (operation != OperationType.OTHER && operation != OperationType.NONE) {
                        return true;
                    } else {
                        if (otherOperationName.equals(perm.getOtherOperationName())) return true;
                    }
                }
            }
        }
        return false;
    }

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (roleManager == null) throw new IllegalStateException("RoleManager is required");
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent eie = (EntityInvalidationEvent) event;
            if (Role.class.isAssignableFrom(eie.getEntityClass())) {
                for (long oid : eie.getEntityIds()) {
                    try {
                        // Refresh cached roles, if they still exist
                        roleManager.getCachedEntity(oid, 0);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Couldn't refresh cached Role", e);
                    }
                }
            }
        }
    }

}
