/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.common.security.rbac.EntityType;
import static com.l7tech.common.security.rbac.EntityType.ANY;
import com.l7tech.common.security.rbac.*;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author alex
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor = Throwable.class)
public class RoleManagerImpl
        extends HibernateEntityManager<Role, EntityHeader>
        implements RoleManager
{
    private static final Logger logger = Logger.getLogger(RoleManagerImpl.class.getName());

    public Class getImpClass() {
        return Role.class;
    }

    public Class getInterfaceClass() {
        return Role.class;
    }

    public String getTableName() {
        return "rbac_role";
    }

    @Transactional(readOnly=true)
    public Collection<Role> getAssignedRoles(final User user) throws FindException {
        //noinspection unchecked
        return (Collection<Role>) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
            public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Set<Role> roles = new HashSet<Role>();
                Criteria userAssignmentQuery = session.createCriteria(UserRoleAssignment.class);
                userAssignmentQuery.add(Restrictions.eq("userId", user.getId()));
                userAssignmentQuery.add(Restrictions.eq("providerId", user.getProviderId()));
                List uras = userAssignmentQuery.list();
                //(hibernate results aren't generic)
                //noinspection ForLoopReplaceableByForEach
                for (Iterator i = uras.iterator(); i.hasNext();) {
                    UserRoleAssignment ura = (UserRoleAssignment) i.next();
                    roles.add(ura.getRole());
                }

                return roles;
            }
        });
    }

    @Transactional(readOnly=true)
    public boolean isPermittedForEntitiesOfTypes(final User authenticatedUser,
                                                 final OperationType requiredOperation,
                                                 final Set<EntityType> requiredTypes)
            throws FindException
    {
        if (authenticatedUser == null) throw new IllegalArgumentException();
        if (requiredTypes == null || requiredTypes.isEmpty()) throw new IllegalArgumentException();
        if (requiredOperation == null || !OperationType.ALL_CRUD.contains(requiredOperation)) throw new IllegalArgumentException();

        final Map<EntityType, Boolean> permittedTypes = new HashMap<EntityType, Boolean>();

        for (Role role : getAssignedRoles(authenticatedUser)) {
            for (Permission perm : role.getPermissions()) {
                if (perm.getScope() != null && !perm.getScope().isEmpty()) continue; // This permission is too restrictive
                if (perm.getOperation() != requiredOperation) continue; // This permission is for a different operation
                com.l7tech.common.security.rbac.EntityType ptype = perm.getEntityType();

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


    @Transactional(readOnly=true)
    public boolean isPermittedForAnyEntityOfType(final User authenticatedUser,
                                                 final OperationType requiredOperation,
                                                 final EntityType requiredType)
            throws FindException
    {
        if (authenticatedUser == null || requiredType == null) throw new NullPointerException();
        logger.log(Level.FINE, "Checking for permission to {0} any {1}", new Object[] { requiredOperation.getName(), requiredType.getName()});
        for (Role role : getAssignedRoles(authenticatedUser)) {
            for (Permission perm : role.getPermissions()) {
                if (perm.getScope() != null && !perm.getScope().isEmpty()) continue; // This permission is too restrictive
                if (perm.getOperation() != requiredOperation) continue; // This permission is for a different operation
                com.l7tech.common.security.rbac.EntityType ptype = perm.getEntityType();
                if (ptype == ANY || ptype == requiredType) return true;
            }
        }
        return false;
    }

    // TODO check whether any of the assigned users is internal?
    @Override
    public void update(Role role) throws UpdateException {
        if (role.getOid() == Role.ADMIN_ROLE_OID && role.getUserAssignments().isEmpty())
            throw new UpdateException(RoleManager.ADMIN_REQUIRED);

        // Merge in OIDs for any known user assignments (See bug 4176)
        boolean needsOidMerge = false;
        for ( UserRoleAssignment ura : role.getUserAssignments() ) {
            if ( ura.getOid() == UserRoleAssignment.DEFAULT_OID ) {
                needsOidMerge = true;
                break;
            }
        }

        if ( needsOidMerge ) {
            try {
                Role persistedRole = findByPrimaryKey(role.getOid());
                Set previousAssignments = persistedRole.getUserAssignments();

                for ( UserRoleAssignment ura : role.getUserAssignments() ) {
                    if ( ura.getOid() == UserRoleAssignment.DEFAULT_OID ) {
                        ura.setOid(getOidForAssignment(previousAssignments, ura));
                    }
                }
            } catch (FindException fe) {
                // fail on update below
                logger.log(Level.FINE, "Find error when merging assignments for role", ExceptionUtils.getDebugException(fe));
            }
        }

        super.update(role);
    }

    @Transactional(readOnly=true)
    public boolean isPermittedForEntity(User user, Entity entity, OperationType operation, String otherOperationName) throws FindException {
        if (user == null || entity == null || operation == null) throw new NullPointerException();
        if (operation == OperationType.OTHER && otherOperationName == null) throw new IllegalArgumentException("otherOperationName must be specified when operation == OTHER");
        logger.log(Level.FINE, "Checking for permission to {0} {1} #{2}", new Object[] { operation.getName(), entity.getClass().getSimpleName(), entity.getId()});

        Collection<Role> assignedRoles = getAssignedRoles(user);
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


    public Role findEntitySpecificRole(PermissionMatchCallback callback) throws FindException {
        for (Role role : findAll()) {
            boolean match = !role.getPermissions().isEmpty();
            for (Permission perm : role.getPermissions()) {
                match = match && callback.matches(perm);
                if (!match) break;
            }

            if (match) return role;
        }

        return null;
    }

    public Role findEntitySpecificRole(final EntityType etype, final long entityId) throws FindException {
        return (Role) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
            protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Criteria crit = session.createCriteria(Role.class);
                crit.add(Restrictions.eq("entityTypeName", etype.name()));
                crit.add(Restrictions.eq("entityOid", entityId));
                return crit.uniqueResult();
            }
        });
    }

    public void deleteEntitySpecificRole(PermissionMatchCallback callback) throws DeleteException {
        try {
            Role role = findEntitySpecificRole(callback);
            if (role == null) return;
            logger.info("Deleting obsolete Role #" + role.getOid() + " (" + role.getName() + ")");
            delete(role);
        } catch (FindException e) {
            throw new DeleteException("Couldn't find Roles for this Entity", e);
        }
    }

    public void deleteEntitySpecificRole(EntityType etype, final long entityOid) throws DeleteException {
        try {
            Role role = findEntitySpecificRole(etype, entityOid);
            if (role == null) return;
            logger.info("Deleting obsolete Role #" + role.getOid() + " (" + role.getName() + ")");
            delete(role);
        } catch (FindException e) {
            throw new DeleteException("Couldn't find Roles for this Entity", e);
        }
    }

    public void renameEntitySpecificRole(EntityType entityType, NamedEntityImp entity, Pattern replacePattern) throws FindException, UpdateException {
        Role role = findEntitySpecificRole(entityType, entity.getOid());
        if (role == null) {
            logger.warning(MessageFormat.format("No entity-specific role was found for {0} ''{1}'' (#{2})", entity.getName(), entityType.getName(), entity.getOid()));
            return;
        }
        String name = role.getName();
        Matcher matcher = replacePattern.matcher(name);
        String newName = matcher.replaceAll(entity.getName());
        if (!newName.equals(name)) {
            logger.info(MessageFormat.format("Updating ''{0}'' Role with new name: ''{1}''", role.getName(), newName));
            role.setName(newName);
            update(role);
        }
    }

    private long getOidForAssignment(Set<UserRoleAssignment> userRoleAssignments, UserRoleAssignment assignment) {
        long oid = UserRoleAssignment.DEFAULT_OID;

        for ( UserRoleAssignment ura : userRoleAssignments ) {
            if ( ura.getProviderId()==assignment.getProviderId() &&
                 ura.getUserId().equals(assignment.getUserId())  ) {
                oid = ura.getOid();
                break;
            }
        }

        return oid;
    }
}
