package com.l7tech.console.security.rbac;

import com.l7tech.console.action.Actions;
import com.l7tech.console.panels.Wizard;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.RbacUtilities;
import com.l7tech.gateway.common.security.rbac.Role;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
        this.wizardInput = new PermissionsConfig(role);
        getButtonHelp().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(AddPermissionsWizard.this);
            }
        });
    }

    @Override
    protected void finish(final ActionEvent evt) {
        if (wizardInput instanceof PermissionsConfig) {
            final PermissionsConfig config = (PermissionsConfig) wizardInput;
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
        permission.setGoid(Permission.DEFAULT_GOID);
        permission.setScope(RbacUtilities.getAnonymousNoOidsCopyOfScope(permission.getScope()));
    }
}
