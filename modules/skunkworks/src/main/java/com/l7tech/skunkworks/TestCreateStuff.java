/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.skunkworks;

import com.l7tech.objectmodel.Goid;
import com.l7tech.server.identity.IdProvConfManagerServer;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.security.rbac.AttributePredicate;
import com.l7tech.gateway.common.security.rbac.ScopePredicate;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.ObjectIdentityPredicate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.context.ApplicationContext;

/**
 * @author alex
 */
public class TestCreateStuff {
//    private final SessionFactory sessionFactory;
//    private final ApplicationContext spring;

    private TestCreateStuff() {
//        spring = ApplicationContexts.getProdApplicationContext();
//        this.sessionFactory = (SessionFactory) spring.getBean("sessionFactory");
    }

    private void doIt() throws Exception {
        Session s = null;//sessionFactory.openSession();

        Transaction tx = s.beginTransaction();

        Role role = createManageInternalUsersAndGroups();
        s.save(role);
        tx.commit();
    }

    private void addPermission( Role role, OperationType operation, EntityType etype, String attributeNameOrOid, String attributeValue) {
        Permission perm = new Permission(role, operation, etype);
        ScopePredicate pred;
        if (attributeValue == null) {
            pred = new ObjectIdentityPredicate(perm, attributeNameOrOid);
        } else {
            pred = new AttributePredicate(perm, attributeNameOrOid, attributeValue);
        }
        perm.getScope().add(pred);
        role.getPermissions().add(perm);
    }

    private Role createManageInternalUsersAndGroups() {
        final String iipOid = Goid.toString(IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_GOID);

        Role manageInternalRole = new Role();
        manageInternalRole.setName("Manage Internal Users and Groups");
        addPermission(manageInternalRole, OperationType.READ, EntityType.ID_PROVIDER_CONFIG, iipOid, null);

        addPermission(manageInternalRole, OperationType.CREATE, EntityType.GROUP, "providerId", iipOid);
        addPermission(manageInternalRole, OperationType.READ, EntityType.GROUP, "providerId", iipOid);
        addPermission(manageInternalRole, OperationType.UPDATE, EntityType.GROUP, "providerId", iipOid);
        addPermission(manageInternalRole, OperationType.DELETE, EntityType.GROUP, "providerId", iipOid);

        addPermission(manageInternalRole, OperationType.CREATE, EntityType.USER, "providerId", iipOid);
        addPermission(manageInternalRole, OperationType.READ, EntityType.USER, "providerId", iipOid);
        addPermission(manageInternalRole, OperationType.UPDATE, EntityType.USER, "providerId", iipOid);
        addPermission(manageInternalRole, OperationType.DELETE, EntityType.USER, "providerId", iipOid);

        return manageInternalRole;
    }

    public static void main(String[] args) throws Exception {
        TestCreateStuff me = new TestCreateStuff();
        me.doIt();
    }
}
