package com.l7tech.server.folder;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderedEntityManager;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.test.BugId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.security.auth.Subject;
import java.security.PrivilegedAction;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FolderAdminImplTest {
    private FolderAdminImpl folderAdmin;
    @Mock
    private FolderManager folderManager;
    @Mock
    private RbacServices rbacServices;
    @Mock
    private FolderedEntityManager folderedEntityManager;
    private Map<Class<? extends Entity>, FolderedEntityManager> entityManagerMap;
    private InternalUser user;
    private Folder rootFolder;
    private Folder toFolder;
    private Folder fromFolder;

    @Before
    public void setup() {
        entityManagerMap = new HashMap<>();
        entityManagerMap.put(Policy.class, folderedEntityManager);
        entityManagerMap.put(JdbcConnection.class, folderedEntityManager);
        folderAdmin = new FolderAdminImpl(folderManager, entityManagerMap, rbacServices);
        user = new InternalUser("test");
        rootFolder = new Folder("root", null);
        toFolder = new Folder("to", rootFolder);
        fromFolder = new Folder("from", rootFolder);
    }

    @BugId("SSG-7126")
    @Test
    public void moveEntityToFolderAllowed() throws Exception {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "test", false);
        policy.setFolder(fromFolder);
        policy.setGoid(new Goid(0, 1234L));
        when(rbacServices.isPermittedForEntity(user, toFolder, OperationType.UPDATE, null)).thenReturn(true);
        when(rbacServices.isPermittedForEntity(user, policy, OperationType.UPDATE, null)).thenReturn(true);
        when(rbacServices.isPermittedForEntity(user, fromFolder, OperationType.UPDATE, null)).thenReturn(true);

        moveEntityToFolderAsUser(toFolder, policy);

        verify(rbacServices, times(3)).isPermittedForEntity(eq(user), any(PersistentEntity.class), eq(OperationType.UPDATE), eq((String) null));
        verify(folderedEntityManager).updateFolder(policy, toFolder);
    }

    @Test
    public void moveEntityToNullRootFolder() throws Exception {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "test", false);
        policy.setFolder(fromFolder);
        policy.setGoid(new Goid(0, 1234L));
        when(folderManager.findRootFolder()).thenReturn(rootFolder);
        when(rbacServices.isPermittedForEntity(user, rootFolder, OperationType.UPDATE, null)).thenReturn(true);
        when(rbacServices.isPermittedForEntity(user, policy, OperationType.UPDATE, null)).thenReturn(true);
        when(rbacServices.isPermittedForEntity(user, fromFolder, OperationType.UPDATE, null)).thenReturn(true);

        moveEntityToFolderAsUser(null, policy);

        verify(folderManager).findRootFolder();
        verify(rbacServices, times(3)).isPermittedForEntity(eq(user), any(PersistentEntity.class), eq(OperationType.UPDATE), eq((String) null));
        verify(folderedEntityManager).updateFolder(policy, rootFolder);
    }

    @Test(expected = PermissionDeniedException.class)
    public void moveEntityToFolderNotAllowedToUpdateDestinationFolder() throws Exception {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "test", false);
        policy.setFolder(fromFolder);
        policy.setGoid(new Goid(0, 1234L));
        when(rbacServices.isPermittedForEntity(user, toFolder, OperationType.UPDATE, null)).thenReturn(false);

        try {
            moveEntityToFolderAsUser(toFolder, policy);
            fail("Expected PermissionDeniedException");
        } catch (final PermissionDeniedException e) {
            verify(rbacServices, times(1)).isPermittedForEntity(eq(user), any(PersistentEntity.class), eq(OperationType.UPDATE), eq((String) null));
            verify(folderedEntityManager, never()).updateFolder(policy, toFolder);
            throw e;
        }
    }

    @Test(expected = PermissionDeniedException.class)
    public void moveEntityToFolderNotAllowedToUpdateEntity() throws Exception {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "test", false);
        policy.setFolder(fromFolder);
        policy.setGoid(new Goid(0, 1234L));
        when(rbacServices.isPermittedForEntity(user, toFolder, OperationType.UPDATE, null)).thenReturn(true);
        when(rbacServices.isPermittedForEntity(user, policy, OperationType.UPDATE, null)).thenReturn(false);

        try {
            moveEntityToFolderAsUser(toFolder, policy);
            fail("Expected PermissionDeniedException");
        } catch (final PermissionDeniedException e) {
            verify(rbacServices, times(2)).isPermittedForEntity(eq(user), any(PersistentEntity.class), eq(OperationType.UPDATE), eq((String) null));
            verify(folderedEntityManager, never()).updateFolder(policy, toFolder);
            throw e;
        }
    }

    @Test(expected = PermissionDeniedException.class)
    public void moveEntityToFolderNotAllowedToUpdateTargetFolder() throws Exception {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "test", false);
        policy.setFolder(fromFolder);
        policy.setGoid(new Goid(0, 1234L));
        when(rbacServices.isPermittedForEntity(user, toFolder, OperationType.UPDATE, null)).thenReturn(true);
        when(rbacServices.isPermittedForEntity(user, policy, OperationType.UPDATE, null)).thenReturn(true);
        when(rbacServices.isPermittedForEntity(user, fromFolder, OperationType.UPDATE, null)).thenReturn(false);

        try {
            moveEntityToFolderAsUser(toFolder, policy);
            fail("Expected PermissionDeniedException");
        } catch (final PermissionDeniedException e) {
            verify(rbacServices, times(3)).isPermittedForEntity(eq(user), any(PersistentEntity.class), eq(OperationType.UPDATE), eq((String) null));
            verify(folderedEntityManager, never()).updateFolder(policy, toFolder);
            throw e;
        }
    }

    @Test
    public void moveEntityToFolderEntityDoesNotHaveFolder() throws Exception {
        final JdbcConnection doesNotHaveFolder = new JdbcConnection();
        doesNotHaveFolder.setGoid(new Goid(0, 1234L));
        when(rbacServices.isPermittedForEntity(user, toFolder, OperationType.UPDATE, null)).thenReturn(true);
        when(rbacServices.isPermittedForEntity(user, doesNotHaveFolder, OperationType.UPDATE, null)).thenReturn(true);

        moveEntityToFolderAsUser(toFolder, doesNotHaveFolder);

        verify(rbacServices, times(2)).isPermittedForEntity(eq(user), any(PersistentEntity.class), eq(OperationType.UPDATE), eq((String) null));
        // let the manager handle how it wants to deal with the entity
        verify(folderedEntityManager).updateFolder(doesNotHaveFolder, toFolder);
    }

    @BugId("SSM-4455")
    @Test(expected = NonEmptyFolderDeletionException.class)
    public void deleteNonEmptyFolder() throws Exception {
        final Goid folderGoid = new Goid(0, 1);
        final List<Policy> folderItems = new ArrayList<>();
        folderItems.add(new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "xml", false));
        when(folderedEntityManager.findByFolder(folderGoid)).thenReturn(folderItems);
        folderAdmin.deleteFolder(folderGoid);
    }

    @BugId("SSM-4455")
    @Test()
    public void deleteEmptyFolder() throws Exception {
        final Goid folderGoid = new Goid(0, 1);
        when(folderedEntityManager.findByFolder(folderGoid)).thenReturn(Collections.emptyList());
        folderAdmin.deleteFolder(folderGoid);
        // once per manager
        verify(folderedEntityManager, times(2)).findByFolder(folderGoid);
    }

    private void moveEntityToFolderAsUser(final Folder moveTo, final PersistentEntity toMove) {
        Subject.doAs(new Subject(true, Collections.singleton(user), Collections.emptySet(), Collections.emptySet()), new PrivilegedAction() {
            @Override
            public Object run() {
                try {
                    folderAdmin.moveEntityToFolder(moveTo, toMove);
                } catch (final UpdateException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        });
    }
}
