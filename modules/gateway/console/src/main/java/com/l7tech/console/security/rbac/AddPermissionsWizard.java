package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.Wizard;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.ScopePredicate;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collections;
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

    public AddPermissionsWizard(@NotNull final Window parent, @NotNull Role role) {
        super(parent, new PermissionOptionsPanel());
        setTitle(ADD_PERMISSIONS_TO_ROLE_WIZARD);
        this.role = role;
        this.wizardInput = new PermissionConfig(role, EntityType.ANY, Collections.<ScopePredicate>emptySet(), Collections.<OperationType>emptySet());
    }

    @Override
    protected void finish(final ActionEvent evt) {
        if (wizardInput instanceof PermissionConfig) {
            final PermissionConfig config = (PermissionConfig) wizardInput;
            if (config.getAddedPermissions() == null) {
                getSelectedWizardPanel().storeSettings(config);
            }
            role.getPermissions().addAll(config.getAddedPermissions());
        } else {
            logger.log(Level.WARNING, "Cannot finish wizard because received invalid settings object: " + wizardInput);
        }
        super.finish(evt);
    }

    public class PermissionConfig {
        private final Role role;
        private EntityType type;
        private Set<ScopePredicate> scope;
        private Set<OperationType> operations;
        private Set<Permission> addedPermissions;

        public PermissionConfig(@NotNull final Role role,
                                @NotNull final EntityType type,
                                @NotNull final Set<ScopePredicate> scope,
                                @NotNull final Set<OperationType> operations) {
            this.role = role;
            this.type = type;
            this.scope = scope;
            this.operations = operations;
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
        public Set<ScopePredicate> getScope() {
            return scope;
        }

        public void setScope(@NotNull final Set<ScopePredicate> scope) {
            this.scope = scope;
        }

        @NotNull
        public Set<OperationType> getOperations() {
            return operations;
        }

        public void setOperations(@NotNull final Set<OperationType> operations) {
            this.operations = operations;
        }

        public Set<Permission> getAddedPermissions() {
            return addedPermissions;
        }

        public void setAddedPermissions(@NotNull final Set<Permission> addedPermissions) {
            this.addedPermissions = addedPermissions;
        }
    }
}
