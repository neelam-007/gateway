package com.l7tech.console.security.rbac;

import com.l7tech.console.action.Actions;
import com.l7tech.console.panels.Wizard;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.RbacUtilities;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.folder.FolderHeader;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wizard for adding Permissions to a Role.
 */
public class AddPermissionsWizard extends Wizard {
    private static final Logger logger = Logger.getLogger(AddPermissionsWizard.class.getName());
    private static final String ADD_PERMISSIONS_TO_ROLE_WIZARD = "Add Permissions to Role Wizard";
    private final Role role;

    public static AddPermissionsWizard getInstance(@NotNull final Window parent, @NotNull final Role role) {
        final PermissionSummaryPanel last = new PermissionSummaryPanel();
        last.setNextPanel(null);
        final PermissionScopeSelectionPanel second = new PermissionScopeSelectionPanel();
        second.setNextPanel(last);
        final PermissionOptionsPanel first = new PermissionOptionsPanel();
        first.setNextPanel(second);
        return new AddPermissionsWizard(parent, role, first);
    }

    private AddPermissionsWizard(@NotNull final Window parent, @NotNull Role role, @NotNull final WizardStepPanel firstPanel) {
        super(parent, firstPanel);
        setTitle(ADD_PERMISSIONS_TO_ROLE_WIZARD);
        this.role = role;
        this.wizardInput = new PermissionConfig(role);
        getButtonHelp().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(AddPermissionsWizard.this);
            }
        });
    }

    @Override
    protected void finish(final ActionEvent evt) {
        if (wizardInput instanceof PermissionConfig) {
            final PermissionConfig config = (PermissionConfig) wizardInput;
            for (final Permission added : config.getGeneratedPermissions()) {
                if (!containsPermission(added)) {
                    role.getPermissions().add(added);
                } else {
                    logger.log(Level.INFO, "Permission already exists on the role: " + added);
                }
            }
        } else {
            logger.log(Level.WARNING, "Cannot finish wizard because received invalid settings object: " + wizardInput);
        }
        super.finish(evt);
    }

    private boolean containsPermission(@NotNull final Permission permission) {
        boolean alreadyExists = false;
        for (final Permission existing : role.getPermissions()) {
            final Permission existingCopy = existing.getAnonymousClone();
            resetOids(existingCopy);
            final Permission toCheck = permission.getAnonymousClone();
            resetOids(toCheck);
            if (existingCopy.equals(toCheck)) {
                alreadyExists = true;
                break;
            }
        }
        return alreadyExists;
    }

    private void resetOids(final Permission permission) {
        permission.setOid(Permission.DEFAULT_OID);
        permission.setScope(RbacUtilities.getAnonymousNoOidsCopyOfScope(permission.getScope()));
    }

    public class PermissionConfig {
        private final Role role;
        private EntityType type;
        private Set<OperationType> operations;
        private boolean hasScope;
        private Set<SecurityZone> selectedZones = new HashSet<>();
        private Set<FolderHeader> selectedFolders = new HashSet<>();
        private boolean folderTransitive;
        private boolean folderAncestry;
        private Set<Permission> generatedPermissions = new HashSet<>();

        public PermissionConfig(@NotNull final Role role) {
            this.role = role;
        }

        @NotNull
        public Role getRole() {
            return role;
        }

        @NotNull
        public EntityType getType() {
            return type;
        }

        public void setType(@NotNull final EntityType type) {
            this.type = type;
        }

        @NotNull
        public Set<OperationType> getOperations() {
            return operations;
        }

        public void setOperations(@NotNull final Set<OperationType> operations) {
            this.operations = operations;
        }

        public boolean isHasScope() {
            return hasScope;
        }

        public void setHasScope(boolean hasScope) {
            this.hasScope = hasScope;
        }

        @NotNull
        public Set<SecurityZone> getSelectedZones() {
            return selectedZones;
        }

        public void setSelectedZones(@NotNull final Set<SecurityZone> selectedZones) {
            this.selectedZones = selectedZones;
        }

        @NotNull
        public Set<FolderHeader> getSelectedFolders() {
            return selectedFolders;
        }

        public void setSelectedFolders(@NotNull final Set<FolderHeader> selectedFolders) {
            this.selectedFolders = selectedFolders;
        }

        public boolean isFolderTransitive() {
            return folderTransitive;
        }

        public void setFolderTransitive(boolean folderTransitive) {
            this.folderTransitive = folderTransitive;
        }

        public boolean isFolderAncestry() {
            return folderAncestry;
        }

        public void setFolderAncestry(boolean folderAncestry) {
            this.folderAncestry = folderAncestry;
        }

        @NotNull
        public Set<Permission> getGeneratedPermissions() {
            return generatedPermissions;
        }

        public void setGeneratedPermissions(@NotNull final Set<Permission> generatedPermissions) {
            this.generatedPermissions = generatedPermissions;
        }
    }
}
