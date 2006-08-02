/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.common.security.rbac.Permission;
import com.l7tech.common.security.rbac.Role;
import com.l7tech.common.security.rbac.UserRoleAssignment;
import static com.l7tech.common.security.rbac.EntityType.*;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.identity.UserManager;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.sql.SQLException;
import java.util.*;

/**
 * @author alex
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor = Throwable.class)
public class RoleManagerImpl
        extends HibernateEntityManager<Role, EntityHeader>
        implements RoleManager
{
    private IdentityProviderFactory identityProviderFactory;

    public RoleManagerImpl(IdentityProviderFactory ipf) {
        this.identityProviderFactory = ipf;
    }

    protected RoleManagerImpl() { }

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
    public Collection<User> getAssignedUsers(final Role role) throws FindException {
        Set<User> users = new HashSet<User>();
        List assignments = (List) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
            public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Criteria assignmentsForRole = session.createCriteria(UserRoleAssignment.class);
                assignmentsForRole.add(Restrictions.eq("role", role));
                return assignmentsForRole.list();
            }
        });
        if (assignments.isEmpty()) return users;

        for (Iterator i = assignments.iterator(); i.hasNext();) {
            UserRoleAssignment ura = (UserRoleAssignment) i.next();
            IdentityProvider idp = identityProviderFactory.getProvider(ura.getProviderId());
            UserManager uman = idp.getUserManager();
            users.add(uman.findByPrimaryKey(ura.getUserId()));
        }

        return users;
    }

    @Transactional(readOnly=true)
    public Collection<UserRoleAssignment> getAssignments(final User user) throws FindException {
        //noinspection unchecked
        return (Collection<UserRoleAssignment>) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
            public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Set<UserRoleAssignment> assignments = new HashSet<UserRoleAssignment>();
                Criteria userAssignmentQuery = session.createCriteria(UserRoleAssignment.class);
                userAssignmentQuery.add(Restrictions.eq("providerId", user.getProviderId()));
                userAssignmentQuery.add(Restrictions.eq("userId", user.getUniqueIdentifier()));
                List directUserAssignments = userAssignmentQuery.list();
                for (Object assignment : directUserAssignments) {
                    assignments.add((UserRoleAssignment) assignment);
                }
                return assignments;
            }
        });
    }

    @Transactional(readOnly=true)
    public Collection<Role> getAssignedRoles(final User user) throws FindException {
        //noinspection unchecked
        return (Collection<Role>) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
            public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Set<Role> roles = new HashSet<Role>();
                Criteria userAssignmentQuery = session.createCriteria(UserRoleAssignment.class);
                userAssignmentQuery.add(Restrictions.eq("userId", user.getUniqueIdentifier()));
                userAssignmentQuery.add(Restrictions.eq("providerId", user.getProviderId()));
                List uras = userAssignmentQuery.list();
                for (Iterator i = uras.iterator(); i.hasNext();) {
                    UserRoleAssignment ura = (UserRoleAssignment) i.next();
                    roles.add(ura.getRole());
                }

                return roles;
            }
        });
    }

    public void update(Role role) throws UpdateException {
        try {
            getHibernateTemplate().saveOrUpdate(role);
        } catch (Exception e) {
            throw new UpdateException("Couldn't save Role", e);
        }
    }

    public void assignUser(Role role, User user) throws UpdateException {
        try {
            getHibernateTemplate().save(new UserRoleAssignment(role, user.getProviderId(), user.getUniqueIdentifier()));
        } catch (DataAccessException e) {
            throw new UpdateException(ExceptionUtils.getMessage(e), e);
        }
    }

    @Transactional(readOnly=true)
    public boolean isPermittedForAllEntities(User user, com.l7tech.common.security.rbac.EntityType type, OperationType operation) throws FindException {
        if (user == null || type == null) throw new NullPointerException();
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

    @Transactional(readOnly=true)
    public boolean isPermittedForEntity(User user, Entity entity, OperationType operation, String otherOperationName) throws FindException {
        if (user == null || entity == null || operation == null) throw new NullPointerException();
        if (operation == OperationType.OTHER && otherOperationName == null) throw new IllegalArgumentException("otherOperationName must be specified when operation == OTHER");

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

    public void deleteAssignment(final User user, final Role role) {
        getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria findAssignment = session.createCriteria(UserRoleAssignment.class);
                findAssignment.add(Restrictions.eq("role", role));
                findAssignment.add(Restrictions.eq("userId", user.getUniqueIdentifier()));
                findAssignment.add(Restrictions.eq("providerId", user.getProviderId()));
                for (Object o : findAssignment.list()) {
                    UserRoleAssignment ura = (UserRoleAssignment)o;
                    session.delete(ura);
                }
                return null;
            }
        });
    }

}
