/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import static com.l7tech.common.security.rbac.EntityType.ANY;
import com.l7tech.common.security.rbac.*;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    public boolean isPermittedForAllEntities(User user, com.l7tech.common.security.rbac.EntityType type, OperationType operation) throws FindException {
        if (user == null || type == null) throw new NullPointerException();
        logger.log(Level.FINE, "Checking for permission to {0} any {1}", new Object[] { operation.getName(), type.getName()});
        for (Role role : getAssignedRoles(user)) {
            for (Permission perm : role.getPermissions()) {
                if (perm.getScope() != null && !perm.getScope().isEmpty()) continue; // This permission is too restrictive
                if (perm.getOperation() != operation) continue; // This permission is for a different operation
                com.l7tech.common.security.rbac.EntityType ptype = perm.getEntityType();
                if (ptype == ANY || ptype == type) return true;
            }
        }
        return false;
    }

    @Override
    public void update(Role role) throws UpdateException {
        if (role.getOid() == Role.ADMIN_ROLE_OID && role.getUserAssignments().isEmpty())
            throw new UpdateException(RoleManager.ADMIN_REQUIRED);
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


    public void deleteEntitySpecificRole(Entity entity, PermissionMatchCallback callback) throws DeleteException {
        try {
            for (Role role : findAll()) {
                boolean match = !role.getPermissions().isEmpty();
                for (Permission perm : role.getPermissions()) {
                    match = match && callback.matches(perm);
                    if (!match) break;
                }

                if (match) {
                    logger.info("Deleting obsolete Role #" + role.getOid() + " (" + role.getName() + ")");
                    delete(role);
                    break;
                }
            }
        } catch (FindException e) {
            throw new DeleteException("Couldn't find Roles for this Entity", e);
        }
    }

    public void deleteEntitySpecificRole(final Entity entity) throws DeleteException {
        deleteEntitySpecificRole(entity, new PermissionMatchCallback() {
            public boolean matches(Permission permission) {
                if (!permission.getEntityType().getEntityClass().isAssignableFrom(entity.getClass()))
                    return false;

                Set<ScopePredicate> scope = permission.getScope();
                if (scope == null || scope.isEmpty()) return false;
                if (scope.size() > 1) return false;
                ScopePredicate pred = scope.iterator().next();
                if (pred instanceof ObjectIdentityPredicate) {
                    ObjectIdentityPredicate oip = (ObjectIdentityPredicate) pred;
                    if (oip.getTargetEntityId().equals(entity.getId())) return true;
                }
                return false;
            }
        });
    }
}
