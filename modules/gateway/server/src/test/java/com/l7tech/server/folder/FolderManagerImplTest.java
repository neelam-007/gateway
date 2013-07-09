package com.l7tech.server.folder;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.RoleMatchingTestUtil;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.test.BugId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class FolderManagerImplTest {
    private FolderManagerImpl manager;
    private Folder folder;
    @Mock
    private RoleManager roleManager;

    @Before
    public void setup() {
        manager = new FolderManagerImpl(roleManager);
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
