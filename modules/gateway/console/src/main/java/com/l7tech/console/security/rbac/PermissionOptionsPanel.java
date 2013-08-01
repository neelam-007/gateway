package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.ScopePredicate;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wizard panel which allows the user to select permission options. Permissions will be generated based on the selected options.
 */
public class PermissionOptionsPanel extends WizardStepPanel {
    private static final Logger logger = Logger.getLogger(PermissionOptionsPanel.class.getName());
    private static final String PERMISSION_OPTIONS = "Permission options";
    private JPanel contentPanel;
    private JPanel applyToPanel;
    private JPanel restrictScopePanel;
    private JPanel permittedOperationsPanel;
    private JRadioButton allTypesRadio;
    private JRadioButton allObjectsRadio;
    private JCheckBox createCheckBox;
    private JCheckBox readCheckBox;
    private JCheckBox updateCheckBox;
    private JCheckBox deleteCheckBox;

    public PermissionOptionsPanel() {
        super(new PermissionSummaryPanel());
        setLayout(new BorderLayout());
        setShowDescriptionPanel(false);
        add(contentPanel);
        final RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                notifyListeners();
            }
        });
        createCheckBox.addChangeListener(changeListener);
        readCheckBox.addChangeListener(changeListener);
        updateCheckBox.addChangeListener(changeListener);
        deleteCheckBox.addChangeListener(changeListener);
    }

    @Override
    public String getStepLabel() {
        return PERMISSION_OPTIONS;
    }

    @Override
    public boolean canAdvance() {
        return atLeastOneOpSelected();
    }

    @Override
    public boolean canFinish() {
        return atLeastOneOpSelected();
    }

    @Override
    public void storeSettings(final Object settings) throws IllegalArgumentException {
        if (settings instanceof AddPermissionsWizard.PermissionConfig) {
            final AddPermissionsWizard.PermissionConfig config = (AddPermissionsWizard.PermissionConfig) settings;
            if (allTypesRadio.isSelected()) {
                config.setType(EntityType.ANY);
            } else {
                // TODO
            }

            if (allObjectsRadio.isSelected()) {
                // scope is not restricted
                config.setScope(Collections.<ScopePredicate>emptySet());
            } else {
                // TODO
            }

            final Set<OperationType> ops = new HashSet<>();
            if (createCheckBox.isSelected()) {
                ops.add(OperationType.CREATE);
            }
            if (readCheckBox.isSelected()) {
                ops.add(OperationType.READ);
            }
            if (updateCheckBox.isSelected()) {
                ops.add(OperationType.UPDATE);
            }
            if (deleteCheckBox.isSelected()) {
                ops.add(OperationType.DELETE);
            }
            config.setOperations(ops);

            final Set<Permission> permissions = new HashSet<>();
            for (final OperationType operationType : config.getOperations()) {
                final Permission permission = new Permission(config.getRole(), operationType, config.getType());
                permission.setScope(config.getScope());
                permissions.add(permission);
            }
            config.setAddedPermissions(permissions);
        } else {
            logger.log(Level.WARNING, "Cannot store settings because received invalid settings object: " + settings);
        }
    }

    private boolean atLeastOneOpSelected() {
        return createCheckBox.isSelected() || readCheckBox.isSelected() || updateCheckBox.isSelected() || deleteCheckBox.isSelected();
    }
}
