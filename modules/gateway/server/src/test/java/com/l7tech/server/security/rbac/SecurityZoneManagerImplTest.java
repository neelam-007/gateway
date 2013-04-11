package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.folder.FolderManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Set;

import static com.l7tech.server.security.rbac.SecurityZoneManagerImpl.RENAME_ROLE_PATTERN;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SecurityZoneManagerImplTest {
    private SecurityZoneManagerImpl manager;
    @Mock
    private RoleManager roleManager;
    @Mock
    private FolderManager folderManager;
    private SecurityZone zone;
    private Folder rootFolder;

    @Before
    public void setup() throws Exception {
        manager = new SecurityZoneManagerImpl();
        manager.setRoleManager(roleManager);
        manager.setFolderManager(folderManager);
        zone = new SecurityZone();
        zone.setName("Test");
        zone.setOid(1234L);
        rootFolder = new Folder("RootFolder", null);
        when(folderManager.findRootFolder()).thenReturn(rootFolder);
    }

    @Test
    public void createRoles() throws Exception {
        manager.createRoles(zone);
        verify(roleManager).save(argThat(isReadZoneRole("Test")));
        verify(roleManager).save(argThat(isManageZoneRole("Test")));
    }

    @Test(expected = SaveException.class)
    public void createRolesCannotFindRootFolder() throws Exception {
        when(folderManager.findRootFolder()).thenThrow(new FindException("mocking exception"));
        manager.createRoles(zone);
    }

    @Test(expected = SaveException.class)
    public void createRolesCannotSaveRoles() throws Exception {
        when(roleManager.save(any(Role.class))).thenThrow(new SaveException("mocking exception"));
        manager.createRoles(zone);
    }

    @Test
    public void addReadSecurityZoneRole() throws Exception {
        manager.addReadSecurityZoneRole(zone);
        verify(roleManager).save(argThat(isReadZoneRole("Test")));
    }

    @Test(expected = SaveException.class)
    public void addReadSecurityZoneRoleCannotFindRootFolder() throws Exception {
        when(folderManager.findRootFolder()).thenThrow(new FindException("mocking exception"));
        manager.addReadSecurityZoneRole(zone);
    }

    @Test(expected = SaveException.class)
    public void addReadSecurityZoneRoleCannotSaveRole() throws Exception {
        when(roleManager.save(any(Role.class))).thenThrow(new SaveException("mocking exception"));
        manager.addReadSecurityZoneRole(zone);
    }

    @Test
    public void addManageSecurityZoneRole() throws Exception {
        manager.addManageSecurityZoneRole(zone);
        verify(roleManager).save(argThat((isManageZoneRole("Test"))));
    }

    @Test(expected = SaveException.class)
    public void addManageSecurityZoneRoleCannotFindRootFolder() throws Exception {
        when(folderManager.findRootFolder()).thenThrow(new FindException("mocking exception"));
        manager.addManageSecurityZoneRole(zone);
    }

    @Test(expected = SaveException.class)
    public void addManageSecurityZoneRoleCannotSaveRole() throws Exception {
        when(roleManager.save(any(Role.class))).thenThrow(new SaveException("mocking exception"));
        manager.addManageSecurityZoneRole(zone);
    }

    @Test
    public void updateRoles() throws Exception {
        manager.updateRoles(zone);
        verify(roleManager).renameEntitySpecificRoles(EntityType.SECURITY_ZONE, zone, RENAME_ROLE_PATTERN);
    }

    @Test(expected = UpdateException.class)
    public void updateRolesCannotFindRolesToUpdate() throws Exception {
        doThrow(new FindException("mocking exception")).when(roleManager).renameEntitySpecificRoles(EntityType.SECURITY_ZONE, zone, RENAME_ROLE_PATTERN);
        manager.updateRoles(zone);
    }

    @Test
    public void deleteRoles() throws Exception {
        manager.deleteRoles(zone.getOid());
        verify(roleManager).deleteEntitySpecificRoles(EntityType.SECURITY_ZONE, zone.getOid());
    }

    @Test(expected = DeleteException.class)
    public void deleteRolesDeleteException() throws Exception {
        doThrow(new DeleteException("mocking exception")).when(roleManager).deleteEntitySpecificRoles(EntityType.SECURITY_ZONE, zone.getOid());
        manager.deleteRoles(zone.getOid());
    }

    private ZoneRoleMatcher isReadZoneRole(final String zoneName) {
        return new ZoneRoleMatcher(true);
    }

    private ZoneRoleMatcher isManageZoneRole(final String zoneName) {
        return new ZoneRoleMatcher(false);
    }

    private class ZoneRoleMatcher extends ArgumentMatcher<Role> {
        private boolean readOnly;

        private ZoneRoleMatcher(final boolean readOnly) {
            this.readOnly = readOnly;
        }

        @Override
        public boolean matches(final Object o) {
            boolean match = false;
            String roleType = readOnly ? "View" : "Manage";
            if (o != null) {
                final Role role = (Role) o;
                final String zoneName = zone.getName();
                if (role.getName().equals(roleType + " " + zoneName + " Zone (#1,234)")
                        && role.getEntityType() == EntityType.SECURITY_ZONE
                        && role.getEntityOid() == zone.getOid()
                        // common permissions
                        && hasReadZoneEntityPermission(role)
                        && hasSecurityZonePermission(role, OperationType.READ, zoneName)
                        && hasRootFolderPermission(role, OperationType.READ)
                        && hasPermission(role, OperationType.READ, EntityType.ASSERTION_ACCESS)
                        && hasPermission(role, OperationType.CREATE, EntityType.ASSERTION_ACCESS)) {
                    if (readOnly && role.getDescription().equals(SecurityZoneManagerImpl.READ_ZONE_ROLE_DESCRIPTION_FORMAT)) {
                        match = true;
                    } else if (!readOnly && role.getDescription().equals(SecurityZoneManagerImpl.MANAGE_ZONE_ROLE_DESCRIPTION_FORMAT)
                            && hasSecurityZonePermission(role, OperationType.CREATE, zoneName)
                            && hasSecurityZonePermission(role, OperationType.UPDATE, zoneName)
                            && hasSecurityZonePermission(role, OperationType.DELETE, zoneName)) {
                        match = true;
                    }
                }
            }
            return match;
        }

    }

    private boolean hasPermission(final Role role, final OperationType expectedOperation, final EntityType expectedEntityType) {
        final Set<Permission> permissions = role.getPermissions();
        for (final Permission permission : permissions) {
            if (expectedOperation == permission.getOperation() && expectedEntityType == permission.getEntityType()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSecurityZonePermission(final Role role, final OperationType expectedOperation, final String zoneName) {
        final Set<Permission> permissions = role.getPermissions();
        for (final Permission permission : permissions) {
            final Set<ScopePredicate> predicates = permission.getScope();
            if (expectedOperation == permission.getOperation() && EntityType.ANY == permission.getEntityType() && predicates.size() == 1) {
                final ScopePredicate predicate = predicates.iterator().next();
                if (predicate instanceof SecurityZonePredicate) {
                    final SecurityZonePredicate zonePredicate = (SecurityZonePredicate) predicate;
                    if (zonePredicate.getRequiredZone().getName().equals(zoneName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasRootFolderPermission(final Role role, final OperationType expectedOperation) {
        final Set<Permission> permissions = role.getPermissions();
        for (final Permission permission : permissions) {
            final Set<ScopePredicate> predicates = permission.getScope();
            if (expectedOperation == permission.getOperation() && EntityType.FOLDER == permission.getEntityType() && predicates.size() == 1) {
                final ScopePredicate predicate = predicates.iterator().next();
                if (predicate instanceof FolderPredicate) {
                    final FolderPredicate folderPredicate = (FolderPredicate) predicate;
                    if (folderPredicate.getFolder().equals(rootFolder)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasReadZoneEntityPermission(final Role role) {
        final Set<Permission> permissions = role.getPermissions();
        for (final Permission permission : permissions) {
            final Set<ScopePredicate> predicates = permission.getScope();
            if (OperationType.READ == permission.getOperation() && EntityType.SECURITY_ZONE == permission.getEntityType() && predicates.size() == 1) {
                final ScopePredicate predicate = predicates.iterator().next();
                if (predicate instanceof ObjectIdentityPredicate) {
                    final ObjectIdentityPredicate objectPredicate = (ObjectIdentityPredicate) predicate;
                    if (objectPredicate.getTargetEntityId().equals(zone.getId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
