package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.util.TextUtils;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Gateway server implementation of Hibernate entity manager for the entities representing Security Zones.
 */
public class SecurityZoneManagerImpl extends HibernateEntityManager<SecurityZone, EntityHeader> implements SecurityZoneManager {
    @Override
    public Class<? extends Entity> getImpClass() {
        return SecurityZone.class;
    }

    @Override
    public void createRoles(final SecurityZone zone) throws SaveException {
        final Folder rootFolder = findRootFolder();
        addReadSecurityZoneRole(zone, rootFolder);
        addManageSecurityZoneRole(zone, rootFolder);
    }

    @Override
    public void addManageSecurityZoneRole(@NotNull final SecurityZone zone) throws SaveException {
        addManageSecurityZoneRole(zone, findRootFolder());
    }

    @Override
    public void addReadSecurityZoneRole(@NotNull final SecurityZone zone) throws SaveException {
        addReadSecurityZoneRole(zone, findRootFolder());
    }

    @Override
    public void updateRoles(@NotNull final SecurityZone zone) throws UpdateException {
        try {
            roleManager.renameEntitySpecificRoles(EntityType.SECURITY_ZONE, zone, RENAME_ROLE_PATTERN);
        } catch (final FindException e) {
            throw new UpdateException("Could not find roles to update", e);
        }
    }

    @Override
    public void deleteRoles(long entityOid) throws DeleteException {
        roleManager.deleteEntitySpecificRoles(EntityType.SECURITY_ZONE, entityOid);
    }

    void setRoleManager(final RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    void setFolderManager(final FolderManager folderManager) {
        this.folderManager = folderManager;
    }

    static final String READ_ZONE_ROLE_DESCRIPTION_FORMAT = "Users assigned to the {0} role have the ability to read entities within the {1} security zone.";
    static final String MANAGE_ZONE_ROLE_DESCRIPTION_FORMAT = "Users assigned to the {0} role have the ability to create, read, update and delete entities within the {1} security zone.";
    static final Pattern RENAME_ROLE_PATTERN =
            Pattern.compile(MessageFormat.format(RbacAdmin.RENAME_REGEX_PATTERN, RbacAdmin.ZONE_ROLE_NAME_TYPE_SUFFIX));
    static final int MAX_CHAR_ZONE_NAME = 50;

    private static final Logger logger = Logger.getLogger(SecurityZoneManagerImpl.class.getName());
    private static final String ROLE_ADMIN_NAME_PATTERN = RbacAdmin.ROLE_NAME_PREFIX + " {0} " + RbacAdmin.ZONE_ROLE_NAME_TYPE_SUFFIX + RbacAdmin.ROLE_NAME_OID_SUFFIX;
    private static final String ROLE_READ_NAME_PATTERN = RbacAdmin.ROLE_NAME_PREFIX_READ + " {0} " + RbacAdmin.ZONE_ROLE_NAME_TYPE_SUFFIX + RbacAdmin.ROLE_NAME_OID_SUFFIX;
    @Inject
    private RoleManager roleManager;
    @Inject
    private FolderManager folderManager;

    private Folder findRootFolder() throws SaveException {
        final Folder rootFolder;
        try {
            rootFolder = folderManager.findRootFolder();
        } catch (final FindException e) {
            throw new SaveException("Unable to find root folder", e);
        }
        return rootFolder;
    }

    private Role createBaseSecurityZoneRole(@NotNull final String name, @NotNull final String description, final long zoneOid) {
        logger.info("Creating new Role: " + name);
        final Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        // entity specific (required for auto-update/deletion of roles)
        role.setEntityType(EntityType.SECURITY_ZONE);
        role.setEntityOid(zoneOid);
        return role;
    }

    private void addReadSecurityZoneRole(@NotNull final SecurityZone zone, @NotNull final Folder rootFolder) throws SaveException {
        final String name = MessageFormat.format(ROLE_READ_NAME_PATTERN, TextUtils.truncStringMiddle(zone.getName(), MAX_CHAR_ZONE_NAME), zone.getOid());
        final Role readRole = createBaseSecurityZoneRole(name, READ_ZONE_ROLE_DESCRIPTION_FORMAT, zone.getOid());
        addReadPermissions(zone, readRole, rootFolder);
        roleManager.save(readRole);
    }

    private void addManageSecurityZoneRole(@NotNull final SecurityZone zone, @NotNull final Folder rootFolder) throws SaveException {
        final String name = MessageFormat.format(ROLE_ADMIN_NAME_PATTERN, TextUtils.truncStringMiddle(zone.getName(), MAX_CHAR_ZONE_NAME), zone.getOid());
        final Role manageRole = createBaseSecurityZoneRole(name, MANAGE_ZONE_ROLE_DESCRIPTION_FORMAT, zone.getOid());
        addReadPermissions(zone, manageRole, rootFolder);
        manageRole.addSecurityZonePermission(OperationType.CREATE, EntityType.ANY, zone);
        manageRole.addSecurityZonePermission(OperationType.UPDATE, EntityType.ANY, zone);
        manageRole.addSecurityZonePermission(OperationType.DELETE, EntityType.ANY, zone);
        roleManager.save(manageRole);
    }

    private void addReadPermissions(final SecurityZone zone, final Role role, final Folder rootFolder) throws SaveException {
        role.addEntityPermission(OperationType.READ, EntityType.ASSERTION_ACCESS, null);
        role.addEntityPermission(OperationType.READ, EntityType.FOLDER, rootFolder.getId());
        role.addEntityPermission(OperationType.READ, EntityType.SECURITY_ZONE, zone.getId());
        role.addSecurityZonePermission(OperationType.READ, EntityType.ANY, zone);
    }
}
