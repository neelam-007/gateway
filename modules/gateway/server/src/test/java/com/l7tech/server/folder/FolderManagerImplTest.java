package com.l7tech.server.folder;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.objectmodel.EntityCreator;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
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

import java.util.*;

import static org.junit.Assert.*;
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

    @Test
    public void testFindFolderByPath() throws Exception {
        //Set up dummy folder structure
        final List<Folder> folders = pathTestSetup();
        manager = spy(new FolderManagerImpl(roleManager, new MockConfig(properties)));
        doReturn(folders.get(0)).when(manager).findRootFolder();
        doReturn(Arrays.asList(folders.get(1), folders.get(2))).when(manager).findByFolder(folders.get(0).getGoid());
        doReturn(Arrays.asList(folders.get(3), folders.get(4))).when(manager).findByFolder(folders.get(1).getGoid());
        final Folder answer = manager.findByPath("/folder1/folder1_a");

        //Verify that the folder has the same Id as folder1_a
        assertTrue("Id of answer is folder1_a", answer.getId().equals(folders.get(3).getId()));
    }

    @Test
    public void testFindFolderByPathDoesNotExist() throws Exception {
        //Set up dummy folder structure
        final List<Folder> folders = pathTestSetup();
        manager = spy(new FolderManagerImpl(roleManager, new MockConfig(properties)));
        doReturn(folders.get(0)).when(manager).findRootFolder();
        doReturn(Arrays.asList(folders.get(1), folders.get(2))).when(manager).findByFolder(folders.get(0).getGoid());
        doReturn(Arrays.asList(folders.get(3), folders.get(4))).when(manager).findByFolder(folders.get(1).getGoid());

        try {
            manager.findByPath("/folder1/NoExisting");
            fail("Find Exception should have been thrown");
        } catch (FindException e) {
            assertTrue("Find Exception was thrown because No existing folder exists", e.getMessage().equals("There is no such folder path: /folder1/NoExisting"));
        }
    }

    @Test
    public void testFindEmptyPath() throws Exception {
        assertNull(manager.findByPath(""));
    }

    @Test
    public void testBuildByPath() throws Exception {
        final List<Folder> folders = pathTestSetup();
        manager = spy(new FolderManagerImpl(roleManager, new MockConfig(properties)));
        doReturn(folders.get(0)).when(manager).findRootFolder();
        doReturn(Arrays.asList(folders.get(1), folders.get(2))).when(manager).findByFolder(folders.get(0).getGoid());
        doReturn(Arrays.asList(folders.get(3), folders.get(4))).when(manager).findByFolder(folders.get(1).getGoid());
        doReturn(null).when(manager).save(any(Folder.class));

        final Folder answer = manager.buildByPath("/folder1/folder1_new");

        assertTrue("folder1_new is created",answer.getName().equals("folder1_new"));
        assertTrue("folder1_new's parent is folder1", answer.getFolder().getName().equals("folder1"));
    }

    @Test
    public void testBuildEmptyPath() throws Exception {
        try {
            manager.buildByPath("bad");
            fail("IllegalArgumentException should've been thrown.");
        } catch (IllegalArgumentException e) {
            assertTrue("IllegalArgumentException was thrown", e.getMessage().equals("The folder path is not an absolute path."));
        }
    }

    private List<Folder> pathTestSetup() {
        //Set up dummy folder structure
        final Folder rootFolder = createRootFolder(); //0
        final Folder folder1 = EntityCreator.createFolderWithRandomGoid("folder1", rootFolder); //1
        final Folder folder2 = EntityCreator.createFolderWithRandomGoid("folder2", rootFolder); //2
        final Folder folder1_a = EntityCreator.createFolderWithRandomGoid("folder1_a", folder1); //3
        final Folder folder1_b = EntityCreator.createFolderWithRandomGoid("folder1_b", folder1); //4
        final Folder folder2_a = EntityCreator.createFolderWithRandomGoid("folder2_a", folder2); //5

        return Arrays.asList(rootFolder, folder1, folder2, folder1_a, folder1_b, folder2_a);
    }

    private Folder createRootFolder() {
        final Folder rootFolder = new Folder("Root", new Folder());
        rootFolder.setGoid(Folder.ROOT_FOLDER_ID);
        return rootFolder;
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
