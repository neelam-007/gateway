/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.common.security.rbac.Permission;
import com.l7tech.common.security.rbac.Role;
import com.l7tech.common.security.rbac.UserRoleAssignment;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.identity.UserManager;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.IdentityProviderFactory;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.*;

/**
 * @author alex
 */
@Transactional(rollbackFor = ObjectModelException.class)
public class RoleManagerImpl
        extends HibernateEntityManager<Role, EntityHeader>
        implements RoleManager
{
    private final IdentityProviderFactory identityProviderFactory;

    public RoleManagerImpl(IdentityProviderFactory ipf) {
        this.identityProviderFactory = ipf;
    }

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
    public Collection<User> getAssignedUsers(Role role) throws FindException {
        Set<User> users = new HashSet<User>();
        Criteria assignmentsForRole = getSession().createCriteria(UserRoleAssignment.class);
        assignmentsForRole.add(Restrictions.eq("role", role));
        List assignments = assignmentsForRole.list();
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
    public Collection<UserRoleAssignment> getAssignments(User user) throws FindException {
        Set<UserRoleAssignment> assignments = new HashSet<UserRoleAssignment>();
        Criteria userAssignmentQuery = getSession().createCriteria(UserRoleAssignment.class);
        userAssignmentQuery.add(Restrictions.eq("providerId", user.getProviderId()));
        userAssignmentQuery.add(Restrictions.eq("userId", user.getUniqueIdentifier()));
        List directUserAssignments = userAssignmentQuery.list();
        for (Object assignment : directUserAssignments) {
            assignments.add((UserRoleAssignment) assignment);
        }
        return assignments;
    }

    @Transactional(readOnly=true)
    public Collection<Role> getAssignedRoles(User user) throws FindException {
        Set<Role> roles = new HashSet<Role>();

        Criteria userAssignmentQuery = getSession().createCriteria(UserRoleAssignment.class);
        userAssignmentQuery.add(Restrictions.eq("userId", user.getUniqueIdentifier()));
        userAssignmentQuery.add(Restrictions.eq("providerId", user.getProviderId()));
        List uras = userAssignmentQuery.list();
        for (Iterator i = uras.iterator(); i.hasNext();) {
            UserRoleAssignment ura = (UserRoleAssignment) i.next();
            roles.add(ura.getRole());
        }

        return roles;
    }

    public void update(Role role) throws UpdateException {
        try {
            getHibernateTemplate().saveOrUpdate(role);
        } catch (DataAccessException e) {
            throw new UpdateException(ExceptionUtils.getMessage(e), e);
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
    public boolean isPermittedOperation(User user, Entity entity, OperationType operation, String otherOperationName) throws FindException {
        if (user == null || entity == null || operation == null) throw new NullPointerException();
        if (operation == OperationType.OTHER && otherOperationName == null) throw new IllegalArgumentException("otherOperationName must be specified when operation == OTHER");

        Collection<Role> assignedRoles = getAssignedRoles(user);
        for (Role role : assignedRoles) {
            for (Permission perm : role.getPermissions()) {
                if (perm.matches(entity) && perm.getOperation() == operation) {
                    if (operation != OperationType.OTHER) {
                        return true;
                    } else {
                        if (otherOperationName.equals(perm.getOtherOperationName())) return true;
                    }
                }
            }
        }
        return false;
    }

    @Transactional
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
