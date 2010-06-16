/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import static com.l7tech.objectmodel.EntityType.ANY;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.event.EntityInvalidationEvent;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/** @author alex */
public class RbacServicesImpl implements RbacServices, InitializingBean, ApplicationListener {
    private static final Logger logger = Logger.getLogger(RbacServicesImpl.class.getName());
              
    private final Map<Class<? extends ScopePredicate>, ScopeEvaluatorFactory> scopeEvaluatorFactories = new HashMap<Class<? extends ScopePredicate>, ScopeEvaluatorFactory>() {{
        put(EntityFolderAncestryPredicate.class, new EntityFolderAncestryEvaluatorFactory());
    }};
    private final Map<ScopePredicate, ScopeEvaluator> evaluatorCache = new ConcurrentHashMap<ScopePredicate, ScopeEvaluator>();

    private RoleManager roleManager;
    private EntityFinder entityFinder;

    public RbacServicesImpl(RoleManager roleManager, EntityFinder entityFinder) {
        this.roleManager = roleManager;
        this.entityFinder = entityFinder;
    }

    public RbacServicesImpl() { }

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
        return isPermittedForEntityOfType( authenticatedUser, requiredOperation, requiredType, true );
    }

    @Override
    public boolean isPermittedForSomeEntityOfType(User authenticatedUser, OperationType requiredOperation, EntityType requiredType)
        throws FindException {
        if (authenticatedUser == null || requiredType == null) throw new NullPointerException();
        logger.log(Level.FINE, "Checking for permission to {0} some {1}", new Object[] { requiredOperation.getName(), requiredType.getName()});
        return isPermittedForEntityOfType( authenticatedUser, requiredOperation, requiredType, false );
    }

    private boolean isPermittedForEntityOfType( final User authenticatedUser,
                                                final OperationType requiredOperation,
                                                final EntityType requiredType,
                                                final boolean allEntities ) throws FindException {
        for ( final Role role : roleManager.getAssignedRoles(authenticatedUser)) {
            for ( final Permission perm : role.getPermissions() ) {
                if (allEntities && perm.getScope() != null && !perm.getScope().isEmpty()) continue; // This permission is too restrictive
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
        logger.log(Level.FINE, String.format("Checking for permission to %s %s #%s", operation.getName(), entity.getClass().getSimpleName(), entity.getId()));

        return isPermitted(roleManager.getAssignedRoles(user), entity, operation, otherOperationName);
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

    private boolean isPermitted(final Collection<Role> assignedRoles,
                                final Entity entity,
                                final OperationType attemptedOperation,
                                final String otherOperationName)
    {
        for (Role role : assignedRoles) {
            for (Permission perm : role.getPermissions()) {
                if (!perm.matches(entity) || perm.getOperation() != attemptedOperation)
                    continue;

                if (!checkScope(entity, perm)) continue;

                if (attemptedOperation == OperationType.OTHER || attemptedOperation == OperationType.NONE) {
                    if (otherOperationName.equals(perm.getOtherOperationName())) return true;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkScope(final Entity entity, final Permission perm) {
        if (perm.getScope().isEmpty())
            return true;

        boolean allmatch = true;

        for (ScopePredicate predicate : perm.getScope()) {
            allmatch &= eval(predicate, entity);
        }

        return allmatch;
    }

    private boolean eval(final ScopePredicate predicate, final Entity entity) {
        ScopeEvaluator evaluator;
        if (predicate instanceof ScopeEvaluator) {
            evaluator = (ScopeEvaluator) predicate;
        } else {
            evaluator = evaluatorCache.get(predicate);
            if (evaluator == null) {
                final ScopeEvaluatorFactory evaluatorFactory = scopeEvaluatorFactories.get(predicate.getClass());
                if (evaluatorFactory == null) throw new IllegalStateException("No evaluator factory for " + predicate.getClass().getName());

                //noinspection unchecked
                evaluator = evaluatorFactory.makeEvaluator(predicate);
                if (evaluator == null) throw new IllegalStateException(evaluatorFactory.getClass().getName() + " produced a null evaluator");

                evaluatorCache.put(predicate, evaluator);
            }
        }

        if (evaluator.matches(entity))
            return true;

        logger.fine(String.format("%s did not match the entity", evaluator.getClass().getSimpleName()));
        return false;
    }

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    public void setEntityFinder(EntityFinder entityFinder) {
        this.entityFinder = entityFinder;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (roleManager == null) throw new IllegalStateException("RoleManager is required");
        if (entityFinder == null) throw new IllegalStateException("EntityFinder is required");
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

    private class EntityAncestryEvaluator implements ScopeEvaluator {
        private final EntityFolderAncestryPredicate predicate;

        public EntityAncestryEvaluator(EntityFolderAncestryPredicate predicate) {
            this.predicate = predicate;
        }

        @Override
        public boolean matches(final Entity subjectEntity) {
            if (!(subjectEntity instanceof Folder)) return false;

            final EntityType type = predicate.getEntityType();
            final String id = predicate.getEntityId();
            final Entity targetEntity;
            try {
                targetEntity = entityFinder.find(EntityTypeRegistry.getEntityClass(type), id);
            } catch (FindException e) {
                logger.log(Level.WARNING, String.format("Unable to find target %s #%s", type, id), e);
                return false;
            }

            if (targetEntity == null) {
                logger.log(Level.INFO, String.format("Target %s #%s no longer exists", type, id));
                return false;
            } else if (!(targetEntity instanceof HasFolder)) {
                logger.log(Level.INFO, String.format("Target %s #%s has no folder", type, id));
                return false;
            }

            // Start from the target entity's folder and work upward. If the subject folder is encountered
            // in the entity's ancestry, it's permitted.
            final Folder subjectFolder = (Folder) subjectEntity;

            Folder nextFolder = ((HasFolder) targetEntity).getFolder();
            while (nextFolder != null) {
                if (subjectFolder.getOid() == nextFolder.getOid()) return true;
                nextFolder = nextFolder.getFolder();
            }
            return false;
        }
    }

    private class EntityFolderAncestryEvaluatorFactory implements ScopeEvaluatorFactory<EntityFolderAncestryPredicate> {
        @Override
            public ScopeEvaluator makeEvaluator(final EntityFolderAncestryPredicate predicate) {
            return new EntityAncestryEvaluator(predicate);
        }
    }
}
