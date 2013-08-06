package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import java.awt.*;
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
    private JRadioButton conditionRadio;
    private PermissionsConfig config;

    public PermissionOptionsPanel() {
        super(null);
        setLayout(new BorderLayout());
        setShowDescriptionPanel(false);
        add(contentPanel);
        final RunOnChangeListener checkBoxListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                notifyListeners();
            }
        });
        createCheckBox.addChangeListener(checkBoxListener);
        readCheckBox.addChangeListener(checkBoxListener);
        updateCheckBox.addChangeListener(checkBoxListener);
        deleteCheckBox.addChangeListener(checkBoxListener);
        final RunOnChangeListener radioListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                if (config != null) {
                    setScopeFlag(config);
                    notifyListeners();
                }
            }
        });
        allTypesRadio.addItemListener(radioListener);
        allObjectsRadio.addItemListener(radioListener);
    }

    @Override
    public String getStepLabel() {
        return PERMISSION_OPTIONS;
    }

    @Override
    public boolean canAdvance() {
        return createCheckBox.isSelected() || readCheckBox.isSelected() || updateCheckBox.isSelected() || deleteCheckBox.isSelected();
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public void readSettings(final Object settings) throws IllegalArgumentException {
        super.readSettings(settings);
        if (settings instanceof PermissionsConfig) {
            config = (PermissionsConfig) settings;
        }
    }

    @Override
    public void storeSettings(final Object settings) throws IllegalArgumentException {
        if (settings instanceof PermissionsConfig) {
            final PermissionsConfig config = (PermissionsConfig) settings;
            if (allTypesRadio.isSelected()) {
                config.setType(EntityType.ANY);
            } else {
                // TODO
            }
            setOpsOnConfig(config);
            setScopeFlag(config);

        } else {
            logger.log(Level.WARNING, "Cannot store settings because received invalid settings object: " + settings);
        }
    }

    private void setScopeFlag(final PermissionsConfig config) {
        if (allObjectsRadio.isSelected()) {
            config.setHasScope(false);
        } else {
            config.setHasScope(true);
        }
    }

    private void setOpsOnConfig(PermissionsConfig config) {
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
    }
}
