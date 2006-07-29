/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.skunkworks;

import com.l7tech.common.ApplicationContexts;
import com.l7tech.common.security.rbac.*;
import com.l7tech.identity.User;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.server.security.rbac.RoleManager;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Set;

/**
 * @author alex
 */
public class TestCreateStuff {
    private final SessionFactory sessionFactory;
    private final ApplicationContext spring;

    private TestCreateStuff() {
        spring = ApplicationContexts.getProdApplicationContext();
        this.sessionFactory = (SessionFactory) spring.getBean("sessionFactory");
    }

    private void doIt() throws Exception {
        Session s = sessionFactory.openSession();

        Transaction tx = s.beginTransaction();

        Query q = s.createQuery("FROM u IN CLASS " + InternalUser.class.getName() + " WHERE u.login = 'admin'");
        InternalUser adminUser = (InternalUser) q.uniqueResult();

        Role adminRole;
        q = s.createQuery("FROM r IN CLASS " + Role.class.getName() + " WHERE r.name = 'Gateway Administrator'");
        adminRole = (Role) q.uniqueResult();
        if (adminRole == null) {
            adminRole = new Role();
            adminRole.setName("Gateway Administrator");
            Set<Permission> adminPerms = adminRole.getPermissions();
            adminPerms.add(new Permission(adminRole, OperationType.CREATE, EntityType.ANY));
            adminPerms.add(new Permission(adminRole, OperationType.READ, EntityType.ANY));
            adminPerms.add(new Permission(adminRole, OperationType.UPDATE, EntityType.ANY));
            adminPerms.add(new Permission(adminRole, OperationType.DELETE, EntityType.ANY));
            s.save(adminRole);
        }

        Role operRole;
        q = s.createQuery("FROM r IN CLASS " + Role.class.getName() + " WHERE r.name = 'Gateway Operator'");
        operRole = (Role) q.uniqueResult();
        if (operRole == null) {
            operRole = new Role();
            operRole.setName("Gateway Operator");
            Set<Permission> operPerms = operRole.getPermissions();
            operPerms.add(new Permission(operRole, OperationType.READ, EntityType.ANY));
            s.save(operRole);
        }

        Criteria c = s.createCriteria(UserRoleAssignment.class);
        c.add(Restrictions.eq("providerId", adminUser.getProviderId()));
        c.add(Restrictions.eq("userId", adminUser.getUniqueIdentifier()));
        UserRoleAssignment ura = (UserRoleAssignment) c.uniqueResult();
        if (ura == null) {
            ura = new UserRoleAssignment(adminRole, adminUser.getProviderId(), adminUser.getUniqueIdentifier());
            s.save(ura);
        }

        tx.commit();

        RoleManager rm = (RoleManager) spring.getBean("roleManager");
        Collection<Role> rolesIncludingGroup = rm.getAssignedRoles(adminUser);
        System.out.println("Roles for adminUser: " + rolesIncludingGroup);

        Collection<User> users = rm.getAssignedUsers(adminRole);
        System.out.println("Users in adminRole:  " + users);
    }

    public static void main(String[] args) throws Exception {
        TestCreateStuff me = new TestCreateStuff();
        me.doIt();
    }
}
