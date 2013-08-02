package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.test.BugId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.FatalBeanException;
import org.springframework.context.ApplicationContext;

import java.util.*;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class Upgrade70To71UpgradeRolesTest {
    private Upgrade70To71UpdateRoles upgrade;
    @Mock
    private ApplicationContext context;
    @Mock
    private PolicyManager policyManager;
    @Mock
    private ServiceManager serviceManager;
    @Mock
    private RoleManager roleManager;
    @Mock
    private FolderManager folderManager;
    private List<PolicyHeader> policies;
    private List<ServiceHeader> services;
    private List<FolderHeader> folders;
    private List<Role> policyRoles;
    private List<Role> serviceRoles;
    private List<Role> folderRoles;

    @Before
    public void setup() throws Exception {
        upgrade = new Upgrade70To71UpdateRoles();
        policies = new ArrayList<PolicyHeader>();
        services = new ArrayList<ServiceHeader>();
        folders = new ArrayList<FolderHeader>();
        policyRoles = new ArrayList<Role>();
        serviceRoles = new ArrayList<Role>();
        folderRoles = new ArrayList<Role>();
        when(context.getBean("policyManager", PolicyManager.class)).thenReturn(policyManager);
        when(context.getBean("serviceManager", ServiceManager.class)).thenReturn(serviceManager);
        when(context.getBean("roleManager", RoleManager.class)).thenReturn(roleManager);
        when(context.getBean("folderManager", FolderManager.class)).thenReturn(folderManager);
        when(policyManager.findAllHeaders()).thenReturn(policies);
        when(serviceManager.findAllHeaders(false)).thenReturn(services);
        when(folderManager.findAllHeaders()).thenReturn(folders);
        when(roleManager.findEntitySpecificRoles(eq(EntityType.POLICY), any(Goid.class))).thenReturn(policyRoles);
        when(roleManager.findEntitySpecificRoles(eq(EntityType.SERVICE), any(Goid.class))).thenReturn(serviceRoles);
        when(roleManager.findEntitySpecificRoles(eq(EntityType.FOLDER), any(Goid.class))).thenReturn(folderRoles);
    }

    @Test
    public void upgradePolicyRoles() throws Exception {
        policies.add(createPolicyHeader(new Goid(0,1L)));
        policyRoles.add(createRole("Manage Policy"));
        upgrade.upgrade(context);
        verify(roleManager).update(argThat(hasRoleWithNameAndPermission("Manage Policy", OperationType.READ, EntityType.ENCAPSULATED_ASSERTION)));
    }

    @Test
    public void upgradeServiceRoles() throws Exception {
        services.add(createServiceHeader(new Goid(0,1L)));
        serviceRoles.add(createRole("Manage Service"));
        upgrade.upgrade(context);
        verify(roleManager).update(argThat(hasRoleWithNameAndPermission("Manage Service", OperationType.READ, EntityType.ENCAPSULATED_ASSERTION)));
    }

    @BugId("SSM-4256")
    @Test
    public void upgradeFolderRoles() throws Exception {
        folders.add(createFolderHeader(new Goid(0,1L)));
        folderRoles.add(createRole("Manage Folder"));
        folderRoles.add(createRole("View Folder"));
        upgrade.upgrade(context);
        verify(roleManager).update(argThat(hasRoleWithNameAndPermission("Manage Folder", OperationType.READ, EntityType.ENCAPSULATED_ASSERTION)));
        verify(roleManager).update(argThat(hasRoleWithNameAndPermission("View Folder", OperationType.READ, EntityType.ENCAPSULATED_ASSERTION)));
    }

    @Test
    public void noPolicies() throws Exception {
        policies.clear();
        upgrade.upgrade(context);
        verify(roleManager, never()).update(any(Role.class));
    }

    @Test
    public void ignoreNonPolicyFragments() throws Exception {
        policies.add(createPolicyHeader(new Goid(0,1L), PolicyType.PRIVATE_SERVICE));
        policyRoles.add(createRole("Manage Policy"));
        upgrade.upgrade(context);
        verify(roleManager, never()).update(any(Role.class));
    }

    @Test
    public void doNotCreateMissingPolicyRoles() throws Exception {
        policies.add(createPolicyHeader(new Goid(0,1L)));
        policyRoles.clear();
        upgrade.upgrade(context);
        verify(roleManager, never()).update(any(Role.class));
        verify(policyManager, never()).createRoles(any(Policy.class));
    }

    @Test
    public void unexpectedNumberOfPolicyRoles() throws Exception {
        policies.add(createPolicyHeader(new Goid(0,1L)));
        policyRoles.add(createRole("Manage Policy"));
        policyRoles.add(createRole("Some Extra Role"));
        upgrade.upgrade(context);
        verify(roleManager, never()).update(any(Role.class));
    }

    @Test
    public void policyRoleAlreadyHasPermission() throws Exception {
        policies.add(createPolicyHeader(new Goid(0,1L)));
        policyRoles.add(createRole("Manage Policy", Collections.singletonMap(OperationType.READ, EntityType.ENCAPSULATED_ASSERTION)));
        upgrade.upgrade(context);
        verify(roleManager, never()).update(any(Role.class));
    }

    @Test(expected = NonfatalUpgradeException.class)
    public void policyException() throws Exception {
        when(policyManager.findAllHeaders()).thenThrow(new FindException("mocking exception"));
        upgrade.upgrade(context);
    }

    @Test
    public void noServices() throws Exception {
        services.clear();
        upgrade.upgrade(context);
        verify(roleManager, never()).update(any(Role.class));
    }

    @Test
    public void doNotCreateMissingServiceRoles() throws Exception {
        services.add(createServiceHeader(new Goid(0,1L)));
        serviceRoles.clear();
        upgrade.upgrade(context);
        verify(roleManager, never()).update(any(Role.class));
        verify(serviceManager, never()).createRoles(any(PublishedService.class));
    }

    @Test
    public void unexpectedNumberOfServiceRoles() throws Exception {
        services.add(createServiceHeader(new Goid(0,1L)));
        serviceRoles.add(createRole("Manage Service"));
        serviceRoles.add(createRole("Some Extra Role"));
        upgrade.upgrade(context);
        verify(roleManager, never()).update(any(Role.class));
    }

    @Test
    public void serviceRoleAlreadyHasPermission() throws Exception {
        services.add(createServiceHeader(new Goid(0,1L)));
        serviceRoles.add(createRole("Manage Service", Collections.singletonMap(OperationType.READ, EntityType.ENCAPSULATED_ASSERTION)));
        upgrade.upgrade(context);
        verify(roleManager, never()).update(any(Role.class));
    }

    @Test(expected = NonfatalUpgradeException.class)
    public void serviceException() throws Exception {
        when(serviceManager.findAllHeaders(false)).thenThrow(new FindException("mocking exception"));
        upgrade.upgrade(context);
    }

    @Test
    public void noFolders() throws Exception {
        folders.clear();
        upgrade.upgrade(context);
        verify(roleManager, never()).update(any(Role.class));
    }

    @Test
    public void doNotCreateMissingFolderRoles() throws Exception {
        folders.add(createFolderHeader(new Goid(0,1L)));
        folderRoles.clear();
        upgrade.upgrade(context);
        verify(roleManager, never()).update(any(Role.class));
        verify(folderManager, never()).createRoles(any(Folder.class));
    }

    @Test
    public void tooLittleFolderRoles() throws Exception {
        folders.add(createFolderHeader(new Goid(0,1L)));
        // missing View Folder role
        folderRoles.add(createRole("Manage Folder"));
        upgrade.upgrade(context);
        verify(roleManager, never()).update(any(Role.class));
        verify(folderManager, never()).createRoles(any(Folder.class));
    }

    @Test
    public void tooManyFolderRoles() throws Exception {
        folders.add(createFolderHeader(new Goid(0,1L)));
        folderRoles.add(createRole("Manage Folder"));
        folderRoles.add(createRole("View Folder"));
        folderRoles.add(createRole("Some Extra Role"));
        upgrade.upgrade(context);
        verify(roleManager, never()).update(any(Role.class));
        verify(folderManager, never()).createRoles(any(Folder.class));
    }

    @Test
    public void folderRoleAlreadyHasPermission() throws Exception {
        folders.add(createFolderHeader(new Goid(0,1L)));
        folderRoles.add(createRole("Manage Folder", Collections.singletonMap(OperationType.READ, EntityType.ENCAPSULATED_ASSERTION)));
        folderRoles.add(createRole("View Folder"));
        upgrade.upgrade(context);
        verify(roleManager, never()).update(argThat(hasRoleWithNameAndPermission("Manage Folder", OperationType.READ, EntityType.ENCAPSULATED_ASSERTION)));
        verify(roleManager).update(argThat(hasRoleWithNameAndPermission("View Folder", OperationType.READ, EntityType.ENCAPSULATED_ASSERTION)));
    }

    @Test
    public void folderRolesAlreadyHavePermission() throws Exception {
        folders.add(createFolderHeader(new Goid(0,1L)));
        folderRoles.add(createRole("Manage Folder", Collections.singletonMap(OperationType.READ, EntityType.ENCAPSULATED_ASSERTION)));
        folderRoles.add(createRole("View Folder", Collections.singletonMap(OperationType.READ, EntityType.ENCAPSULATED_ASSERTION)));
        upgrade.upgrade(context);
        verify(roleManager, never()).update(any(Role.class));
    }

    @Test(expected = NonfatalUpgradeException.class)
    public void folderException() throws Exception {
        when(folderManager.findAllHeaders()).thenThrow(new FindException("mocking exception"));
        upgrade.upgrade(context);
    }

    @Test(expected = NonfatalUpgradeException.class)
    public void roleException() throws Exception {
        policies.add(createPolicyHeader(new Goid(0,1L)));
        policyRoles.add(createRole("Manage Policy"));
        when(roleManager.findEntitySpecificRoles(any(EntityType.class), any(Goid.class))).thenThrow(new FindException("mocking exception"));
        upgrade.upgrade(context);
    }

    @Test(expected = FatalUpgradeException.class)
    public void beanException() throws Exception {
        when(context.getBean(anyString(), any(Class.class))).thenThrow(new FatalBeanException("mocking exception"));
        upgrade.upgrade(context);
    }

    private class RoleMatcher extends ArgumentMatcher<Role> {
        private final String name;
        private final OperationType operationType;
        private final EntityType entityType;

        private RoleMatcher(final String name, final OperationType operationType, final EntityType entityType) {
            this.name = name;
            this.operationType = operationType;
            this.entityType = entityType;
        }

        @Override
        public boolean matches(Object o) {
            final Role toMatch = (Role) o;
            return (toMatch.getName().equals(name) && hasPermission(toMatch));
        }

        private boolean hasPermission(final Role toMatch) {
            final Set<Permission> perms = toMatch.getPermissions();
            boolean hasPermission = false;
            for (final Permission perm : perms) {
                if (perm.getOperation() == operationType && perm.getEntityType() == entityType) {
                    hasPermission = true;
                    break;
                }
            }
            return hasPermission;
        }
    }

    private RoleMatcher hasRoleWithNameAndPermission(final String name, final OperationType operationType, final EntityType entityType) {
        return new RoleMatcher(name, operationType, entityType);
    }

    private PolicyHeader createPolicyHeader(final Goid oid, final PolicyType policyType) {
        final Policy policy = new Policy(policyType, "testPolicy", "xml", false);
        policy.setGoid(oid);
        return new PolicyHeader(policy);
    }

    private PolicyHeader createPolicyHeader(final Goid oid) {
        return createPolicyHeader(oid, PolicyType.INCLUDE_FRAGMENT);
    }

    private ServiceHeader createServiceHeader(final Goid oid) {
        final PublishedService service = new PublishedService();
        service.setGoid(oid);
        return new ServiceHeader(service);
    }

    private FolderHeader createFolderHeader(final Goid oid) {
        final Folder folder = new Folder("testFolder", null);
        folder.setGoid(oid);
        return new FolderHeader(folder);
    }

    private Role createRole(final String name) {
        return createRole(name, Collections.<OperationType, EntityType>emptyMap());
    }

    private Role createRole(final String name, final Map<OperationType, EntityType> permissions) {
        final Role role = new Role();
        role.setName(name);
        for (final Map.Entry<OperationType, EntityType> entry : permissions.entrySet()) {
            role.getPermissions().add(new Permission(role, entry.getKey(), entry.getValue()));
        }
        return role;
    }
}
