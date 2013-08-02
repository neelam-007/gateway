package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.ScopePredicate;
import com.l7tech.gateway.common.security.rbac.SecurityZonePredicate;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wizard panel which displays a summary of the permissions that will be added.
 */
public class PermissionSummaryPanel extends WizardStepPanel {
    private static final Logger logger = Logger.getLogger(PermissionSummaryPanel.class.getName());
    private static final String SUMMARY = "Summary";
    private JPanel contentPanel;
    private JPanel optionsPanel;
    private RolePermissionsPanel permissionsPanel;
    private JLabel applyToLabel;
    private JLabel restrictScopeLabel;
    private JLabel permittedOperationsLabel;

    public PermissionSummaryPanel() {
        super(null);
        setLayout(new BorderLayout());
        setShowDescriptionPanel(false);
        add(contentPanel);
    }

    @Override
    public String getStepLabel() {
        return SUMMARY;
    }

    @Override
    public void readSettings(final Object settings) throws IllegalArgumentException {
        if (settings instanceof AddPermissionsWizard.PermissionConfig) {
            final AddPermissionsWizard.PermissionConfig config = (AddPermissionsWizard.PermissionConfig) settings;
            if (config.getType() == EntityType.ANY) {
                applyToLabel.setText("All object types");
            } else {
                // TODO
            }

            final Set<String> ops = new HashSet<>(config.getOperations().size());
            for (final OperationType operationType : config.getOperations()) {
                ops.add(operationType.getName().toLowerCase());
            }
            permittedOperationsLabel.setText(StringUtils.join(ops, ", "));

            // start fresh
            config.getGeneratedPermissions().clear();
            if (config.isHasScope()) {
                restrictScopeLabel.setText("All objects of the specified type");
                for (final SecurityZone zone : config.getSelectedZones()) {
                    for (final OperationType op : config.getOperations()) {
                        final Permission zonePermission = new Permission(config.getRole(), op, config.getType());
                        final SecurityZonePredicate predicate = new SecurityZonePredicate(zonePermission, zone);
                        zonePermission.getScope().add(predicate);
                        config.getGeneratedPermissions().add(zonePermission);
                    }
                }
            } else {
                restrictScopeLabel.setText("Objects matching a set of conditions");
                for (final OperationType operationType : config.getOperations()) {
                    final Permission unrestricted = new Permission(config.getRole(), operationType, config.getType());
                    unrestricted.setScope(Collections.<ScopePredicate>emptySet());
                    config.getGeneratedPermissions().add(unrestricted);
                }
            }

            permissionsPanel.configure(config.getGeneratedPermissions());
        } else {
            logger.log(Level.WARNING, "Cannot read settings because received invalid settings object: " + settings);
        }
    }
}
