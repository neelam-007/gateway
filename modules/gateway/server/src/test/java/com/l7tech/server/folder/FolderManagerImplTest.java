package com.l7tech.server.folder;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.RoleMatchingTestUtil;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.test.BugId;
import com.l7tech.util.MockConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Properties;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FolderManagerImplTest {
    private FolderManagerImpl manager;
    private Folder folder;
    @Mock
    private RoleManager roleManager;
    private Properties properties;

    @Before
    public void setup() {
        properties = new Properties();
        manager = new FolderManagerImpl(roleManager, new MockConfig(properties));
        folder = new Folder("testFolder", null);
    }

    @BugId("SSM-4256")
    @Test
    public void addManageFolderRoleCanReadEncapsulatedAssertions() throws Exception {
        manager.addManageFolderRole(folder);
        verify(roleManager).save(argThat(canReadEncapsulatedAssertions()));
    }

    @BugId("SSM-4258")
    @Test
    public void addReadonlyFolderRoleCanReadEncapsulatedAssertions() throws Exception {
        manager.addReadonlyFolderRole(folder);
        verify(roleManager).save(argThat(canReadEncapsulatedAssertions()));
    }

    @BugId("SSG-7101")
    @Test
    public void addReadonlyFolderRoleCanReadAllAssertions() throws Exception {
        manager.addReadonlyFolderRole(folder);
        verify(roleManager).save(argThat(RoleMatchingTestUtil.canReadAllAssertions()));
    }

    @BugId("SSG-7101")
    @Test
    public void addManageFolderRoleCanReadAllAssertions() throws Exception {
        manager.addManageFolderRole(folder);
        verify(roleManager).save(argThat(RoleMatchingTestUtil.canReadAllAssertions()));
    }

    @Test
    public void createRoles() throws Exception {
        manager.createRoles(folder);
        verify(roleManager, times(2)).save(any(Role.class));
    }

    @Test
    public void createRolesSkipped() throws Exception {
        properties.setProperty(FolderManagerImpl.AUTO_CREATE_MANAGE_ROLE_PROPERTY, "false");
        properties.setProperty(FolderManagerImpl.AUTO_CREATE_VIEW_ROLE_PROPERTY, "false");
        manager.createRoles(folder);
        verify(roleManager, never()).save(any(Role.class));
    }

    @Test
    public void createRolesManageSkipped() throws Exception {
        properties.setProperty(FolderManagerImpl.AUTO_CREATE_MANAGE_ROLE_PROPERTY, "false");
        manager.createRoles(folder);
        verify(roleManager, times(1)).save(any(Role.class));
    }

    @Test
    public void createRolesViewSkipped() throws Exception {
        properties.setProperty(FolderManagerImpl.AUTO_CREATE_VIEW_ROLE_PROPERTY, "false");
        manager.createRoles(folder);
        verify(roleManager, times(1)).save(any(Role.class));
    }

    private RoleWithReadEncapsulatedAssertionPermission canReadEncapsulatedAssertions() {
        return new RoleWithReadEncapsulatedAssertionPermission();
    }

    private class RoleWithReadEncapsulatedAssertionPermission extends ArgumentMatcher<Role> {
        @Override
        public boolean matches(Object o) {
            boolean canReadEncapsulatedAssertions = false;
            final Role role = (Role) o;
            for (final Permission permission : role.getPermissions()) {
                if (permission.getEntityType() == EntityType.ENCAPSULATED_ASSERTION && permission.getOperation() == OperationType.READ) {
                    canReadEncapsulatedAssertions = true;
                    break;
                }
            }
            return canReadEncapsulatedAssertions;
        }
    }
}
