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

import static com.l7tech.server.security.rbac.SecurityZoneManagerImpl.MAX_CHAR_ZONE_NAME;
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
        zone.setGoid(new Goid(0,1234L));
        rootFolder = new Folder("RootFolder", null);
        rootFolder.setGoid(new Goid(0,1111L));
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

    @Test
    public void addReadSecurityZoneRoleLongName() throws Exception {
        zone.setName(createStringOverMaxChars());
        manager.addReadSecurityZoneRole(zone);
        verify(roleManager).save(argThat(isReadZoneRole("xxxxxxxxxxxxxxxxxxxxxx...xxxxxxxxxxxxxxxxxxxxxx")));
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

    @Test
    public void addManageSecurityZoneRoleLongName() throws Exception {
        zone.setName(createStringOverMaxChars());
        manager.addManageSecurityZoneRole(zone);
        verify(roleManager).save(argThat((isManageZoneRole("xxxxxxxxxxxxxxxxxxxxxx...xxxxxxxxxxxxxxxxxxxxxx"))));
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
        manager.deleteRoles(zone.getGoid());
        verify(roleManager).deleteEntitySpecificRoles(EntityType.SECURITY_ZONE, zone.getGoid());
    }

    @Test(expected = DeleteException.class)
    public void deleteRolesDeleteException() throws Exception {
        doThrow(new DeleteException("mocking exception")).when(roleManager).deleteEntitySpecificRoles(EntityType.SECURITY_ZONE, zone.getGoid());
        manager.deleteRoles(zone.getGoid());
    }

    private ZoneRoleMatcher isReadZoneRole(final String zoneName) {
        return new ZoneRoleMatcher(zoneName, true);
    }

    private ZoneRoleMatcher isManageZoneRole(final String zoneName) {
        return new ZoneRoleMatcher(zoneName, false);
    }

    private class ZoneRoleMatcher extends ArgumentMatcher<Role> {
        private String expectedZoneName;
        private boolean readOnly;

        private ZoneRoleMatcher(final String expectedZoneName, final boolean readOnly) {
            this.expectedZoneName = expectedZoneName;
            this.readOnly = readOnly;
        }

        @Override
        public boolean matches(final Object o) {
            boolean match = false;
            String roleType = readOnly ? "View" : "Manage";
            if (o != null) {
                final Role role = (Role) o;
                if (role.getName().equals(roleType + " " + expectedZoneName + " Zone (#"+new Goid(0,1234L).toHexString()+")")
                        && role.getEntityType() == EntityType.SECURITY_ZONE
                        && role.getEntityGoid().equals(zone.getGoid())
                        // common permissions
                        && hasEntitySpecificPermission(role, OperationType.READ, EntityType.SECURITY_ZONE, zone.getId())
                        && hasSecurityZonePermission(role, OperationType.READ)
                        && hasEntitySpecificPermission(role, OperationType.READ, EntityType.FOLDER, rootFolder.getId())) {
                    if (readOnly && role.getDescription().equals(SecurityZoneManagerImpl.READ_ZONE_ROLE_DESCRIPTION_FORMAT)) {
                        match = true;
                    } else if (!readOnly && role.getDescription().equals(SecurityZoneManagerImpl.MANAGE_ZONE_ROLE_DESCRIPTION_FORMAT)
                            && hasSecurityZonePermission(role, OperationType.CREATE)
                            && hasSecurityZonePermission(role, OperationType.UPDATE)
                            && hasSecurityZonePermission(role, OperationType.DELETE)) {
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

    private boolean hasEntitySpecificPermission(final Role role, final OperationType expectedOperation, final EntityType expectedEntityType, final String expectedEntityId) {
        final Set<Permission> permissions = role.getPermissions();
        for (final Permission permission : permissions) {
            final Set<ScopePredicate> predicates = permission.getScope();
            if (expectedOperation == permission.getOperation() && expectedEntityType == permission.getEntityType() && predicates.size() == 1) {
                final ScopePredicate predicate = predicates.iterator().next();
                if (predicate instanceof ObjectIdentityPredicate) {
                    final ObjectIdentityPredicate idPredicate = (ObjectIdentityPredicate) predicate;
                    if (idPredicate.getTargetEntityId().equals(expectedEntityId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasSecurityZonePermission(final Role role, final OperationType expectedOperation) {
        final Set<Permission> permissions = role.getPermissions();
        for (final Permission permission : permissions) {
            final Set<ScopePredicate> predicates = permission.getScope();
            if (expectedOperation == permission.getOperation() && EntityType.ANY == permission.getEntityType() && predicates.size() == 1) {
                final ScopePredicate predicate = predicates.iterator().next();
                if (predicate instanceof SecurityZonePredicate) {
                    final SecurityZonePredicate zonePredicate = (SecurityZonePredicate) predicate;
                    if (zonePredicate.getRequiredZone().equals(zone)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String createStringOverMaxChars() {
        String overMax = "";
        for (int i = 0; i < MAX_CHAR_ZONE_NAME + 1; i++) {
            overMax = overMax + "x";
        }
        return overMax;
    }

}
