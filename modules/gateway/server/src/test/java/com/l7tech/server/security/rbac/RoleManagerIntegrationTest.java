package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.RoleEntityHeader;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.EntityManagerTest;
import org.hibernate.Hibernate;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.*;

public class RoleManagerIntegrationTest extends EntityManagerTest {
    private static final Goid ADMIN_ROLE_GOID = new Goid("0000000000000000ffffffffffffff9c");
    private static final Goid INTERNAL_PROVIDER_GOID = new Goid("0000000000000000fffffffffffffffe");
    private static final String ADMIN_USER_GOID = "00000000000000000000000000000003";
    private static final Goid ROOT_FOLDER_GOID = new Goid("0000000000000000ffffffffffffec76");
    private RoleManager roleManager;

    @Before
    public void setup() {
        roleManager = applicationContext.getBean("roleManager", RoleManager.class);
    }

    @Test
    public void findAllLazyLoadsPermissions() throws Exception {
        final Collection<Role> all = roleManager.findAll();
        assertFalse(Hibernate.isInitialized(all.iterator().next().getPermissions()));
    }

    @Test
    public void findByPrimaryKey() throws Exception {
        final Role administrator = roleManager.findByPrimaryKey(ADMIN_ROLE_GOID);
        assertTrue(Hibernate.isInitialized(administrator.getPermissions()));
    }

    @Test
    public void getAssignedRoles() throws Exception {
        final UserBean admin = new UserBean(INTERNAL_PROVIDER_GOID, "admin");
        admin.setUniqueIdentifier(ADMIN_USER_GOID);
        final Collection<Role> roles = roleManager.getAssignedRoles(admin);
        assertTrue(Hibernate.isInitialized(roles.iterator().next().getPermissions()));
    }

    @Test
    public void findByTag() throws Exception {
        final Role administrator = roleManager.findByTag(Role.Tag.ADMIN);
        assertTrue(Hibernate.isInitialized(administrator.getPermissions()));
    }

    @Test
    public void findEntitySpecificRoles() throws Exception {
        final Role folderRole = new Role();
        folderRole.setName("role for root folder");
        folderRole.setEntityGoid(ROOT_FOLDER_GOID);
        folderRole.setEntityType(EntityType.FOLDER);
        roleManager.save(folderRole);
        session.flush();
        final Collection<Role> roles = roleManager.findEntitySpecificRoles(EntityType.FOLDER, ROOT_FOLDER_GOID);
        assertEquals(1, roles.size());
        assertTrue(Hibernate.isInitialized(roles.iterator().next().getPermissions()));
    }

    @Test
    public void findAllHeaders() throws Exception {
        final Collection<EntityHeader> headers = roleManager.findAllHeaders();
        assertTrue(headers.iterator().next() instanceof RoleEntityHeader);
    }

    @Test
    public void findByUniqueName() throws Exception {
        final Role administrator = roleManager.findByUniqueName("Administrator");
        assertTrue(Hibernate.isInitialized(administrator.getPermissions()));
    }

    @Test
    public void findByHeader() throws Exception {
        final Role administrator = roleManager.findByHeader(new EntityHeader(ADMIN_ROLE_GOID, EntityType.RBAC_ROLE, "Administrator", null));
        assertTrue(Hibernate.isInitialized(administrator.getPermissions()));
    }

    @Test
    public void getCachedEntity() throws Exception {
        final Role administrator = roleManager.getCachedEntity(ADMIN_ROLE_GOID, 0);
        assertTrue(Hibernate.isInitialized(administrator.getPermissions()));
    }
}
