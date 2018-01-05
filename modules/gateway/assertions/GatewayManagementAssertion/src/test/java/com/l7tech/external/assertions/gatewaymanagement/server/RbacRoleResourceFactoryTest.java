package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.*;
import com.l7tech.gateway.common.security.rbac.FolderPredicate;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.SecurityZonePredicate;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.test.BugId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author mzhang, 12/20/2017
 */
@RunWith(MockitoJUnitRunner.class)
public class RbacRoleResourceFactoryTest {
    private RbacRoleResourceFactory factory;
    @Mock
    private RbacServices rbacServices;
    @Mock
    private SecurityFilter securityFilter;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private RoleManager roleManager;
    @Mock
    private FolderManager folderManager;
    @Mock
    private SecurityZoneManager securityZoneManager;
    @Mock
    private EntityFinder entityFinder;
    @Mock
    private IdentityProviderFactory identityProviderFactory;

    private final Goid goid = new Goid(0, -1);
    private final String goid_hex = goid.toHexString();

    @Before
    public void setup() {
        factory = new RbacRoleResourceFactory(rbacServices, securityFilter, transactionManager, roleManager,
                folderManager, securityZoneManager, entityFinder, identityProviderFactory
        );
    }

    @BugId("DE271778")
    @Test(expected = ResourceFactory.InvalidResourceException.class)
    public void FolderPredicateStrictTest() throws Exception {
        final RbacRoleMO rbacRoleMO = createFolderRbacRoleMO();
        factory.fromResource(rbacRoleMO, true);
    }

    @BugId("DE271778")
    @Test
    public void FolderPredicateNonStrictTest() throws Exception {
        final RbacRoleMO rbacRoleMO = createFolderRbacRoleMO();
        Role role = factory.fromResource(rbacRoleMO, false);
        assertNotNull(role);
        assertEquals(goid,role.getPermissions().iterator().next().getGoid());
        assertTrue(role.getPermissions().iterator().next().getScope().iterator().next() instanceof FolderPredicate);
    }

    @BugId("DE271778")
    @Test(expected = ResourceFactory.InvalidResourceException.class)
    public void SecurityZonePredicateStrictTest() throws Exception {
        final RbacRoleMO rbacRoleMO = createSecurityZoneRbacRoleMO();
        factory.fromResource(rbacRoleMO, true);
    }

    @BugId("DE271778")
    @Test
    public void SecurityZonePredicateNonStrictTest() throws Exception {
        final RbacRoleMO rbacRoleMO = createSecurityZoneRbacRoleMO();
        Role role = factory.fromResource(rbacRoleMO, false);
        assertNotNull(role);
        assertEquals(goid,role.getPermissions().iterator().next().getGoid());
        assertTrue(role.getPermissions().iterator().next().getScope().iterator().next() instanceof SecurityZonePredicate);
    }

    private RbacRoleMO createFolderRbacRoleMO(){

        final RbacRoleMO rbacRoleMO = ManagedObjectFactory.createRbacRoleMO();
        final RbacRolePermissionMO rbacRolePermissionMO = ManagedObjectFactory.createRbacRolePermissionMO();
        final RbacRolePredicateMO rbacRolePredicateMO = ManagedObjectFactory.createRbacRolePredicateMO();

        rbacRolePredicateMO.setType(RbacRolePredicateMO.Type.FolderPredicate);
        Map<String, String> properties = new HashMap<>();
        properties.put("folderId", goid_hex);
        rbacRolePredicateMO.setProperties(properties);

        List<RbacRolePredicateMO> predicateList = new ArrayList<>();
        predicateList.add(rbacRolePredicateMO);
        rbacRolePermissionMO.setScope(predicateList);
        rbacRolePermissionMO.setEntityType("RBAC_ROLE");
        rbacRolePermissionMO.setOperation(RbacRolePermissionMO.OperationType.CREATE);

        List<RbacRolePermissionMO> permissionsList = new ArrayList<>();
        permissionsList.add(rbacRolePermissionMO);
        rbacRoleMO.setPermissions(permissionsList);

        return rbacRoleMO;
    }

    private RbacRoleMO createSecurityZoneRbacRoleMO(){

        final RbacRoleMO rbacRoleMO = ManagedObjectFactory.createRbacRoleMO();
        final RbacRolePermissionMO rbacRolePermissionMO = ManagedObjectFactory.createRbacRolePermissionMO();
        final RbacRolePredicateMO rbacRolePredicateMO = ManagedObjectFactory.createRbacRolePredicateMO();

        rbacRolePredicateMO.setType(RbacRolePredicateMO.Type.SecurityZonePredicate);
        Map<String, String> properties = new HashMap<>();
        properties.put("securityZoneId", goid_hex);
        rbacRolePredicateMO.setProperties(properties);

        List<RbacRolePredicateMO> predicateList = new ArrayList<>();
        predicateList.add(rbacRolePredicateMO);
        rbacRolePermissionMO.setScope(predicateList);
        rbacRolePermissionMO.setEntityType("RBAC_ROLE");
        rbacRolePermissionMO.setOperation(RbacRolePermissionMO.OperationType.CREATE);

        List<RbacRolePermissionMO> permissionsList = new ArrayList<>();
        permissionsList.add(rbacRolePermissionMO);
        rbacRoleMO.setPermissions(permissionsList);

        return rbacRoleMO;
    }
}
