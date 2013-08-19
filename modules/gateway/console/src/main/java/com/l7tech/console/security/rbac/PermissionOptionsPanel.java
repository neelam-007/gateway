package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.objectmodel.EntityType;
import org.apache.commons.collections.ComparatorUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wizard panel which allows the user to select permission options. Permissions will be generated based on the selected options.
 */
public class PermissionOptionsPanel extends WizardStepPanel {
    private static final Logger logger = Logger.getLogger(PermissionOptionsPanel.class.getName());
    private static final String PERMISSION_OPTIONS = "Permission options";
    private static final String SELECT_A_TYPE = "(select a type)";
    private static final Map<EntityType, Set<OperationType>> ENTITY_TYPES;
    private static final Set<EntityType> SINGULAR_ENTITY_TYPES;
    private static final Set<EntityType> LARGE_ENTITY_TYPES;
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
    private JRadioButton specificTypeRadio;
    private JComboBox typeComboBox;
    private JRadioButton specificObjectsRadio;
    private PermissionsConfig config;

    static {
        SINGULAR_ENTITY_TYPES = new HashSet<>();
        SINGULAR_ENTITY_TYPES.add(EntityType.SSG_KEYSTORE);
        SINGULAR_ENTITY_TYPES.add(EntityType.RESOLUTION_CONFIGURATION);
        SINGULAR_ENTITY_TYPES.add(EntityType.PASSWORD_POLICY);

        LARGE_ENTITY_TYPES = new HashSet<>();
        LARGE_ENTITY_TYPES.add(EntityType.METRICS_BIN);

        ENTITY_TYPES = new TreeMap(ComparatorUtils.nullLowComparator(EntityType.NAME_COMPARATOR));
        ENTITY_TYPES.put(null, null);
        for (final EntityType type : EntityType.values()) {
            if (type != EntityType.ANY && type.isDisplayedInGui()) {
                final Set<OperationType> invalidOps = new HashSet<>();
                if (SINGULAR_ENTITY_TYPES.contains(type) || type == EntityType.ASSERTION_ACCESS) {
                    invalidOps.add(OperationType.CREATE);
                    invalidOps.add(OperationType.DELETE);
                } else if (type == EntityType.SERVICE_TEMPLATE || type == EntityType.METRICS_BIN || type == EntityType.SERVICE_USAGE) {
                    invalidOps.add(OperationType.CREATE);
                    invalidOps.add(OperationType.DELETE);
                    invalidOps.add(OperationType.UPDATE);
                } else if (type == EntityType.TRUSTED_ESM || type == EntityType.TRUSTED_ESM_USER) {
                    invalidOps.add(OperationType.CREATE);
                    invalidOps.add(OperationType.UPDATE);
                }
                ENTITY_TYPES.put(type, invalidOps);
            }
        }
    }

    public PermissionOptionsPanel() {
        super(null);
        setLayout(new BorderLayout());
        setShowDescriptionPanel(false);
        add(contentPanel);
        initComboBox();
        initCheckBoxes();
        initRadioButtons();
        enableDisable();
    }


    @Override
    public String getStepLabel() {
        return PERMISSION_OPTIONS;
    }

    @Override
    public boolean canAdvance() {
        final boolean atLeastOneOpSelected = createCheckBox.isSelected() || readCheckBox.isSelected() || updateCheckBox.isSelected() || deleteCheckBox.isSelected();
        final boolean typeOk = allTypesRadio.isSelected() || (specificTypeRadio.isSelected() && typeComboBox.getSelectedItem() != null);
        final boolean scopeOk = (allObjectsRadio.isSelected() && allObjectsRadio.isEnabled()) ||
                (conditionRadio.isSelected() && conditionRadio.isEnabled()) ||
                (specificObjectsRadio.isSelected() && specificObjectsRadio.isEnabled());
        return atLeastOneOpSelected && typeOk && scopeOk;
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
            final EntityType selectedType;
            if (allTypesRadio.isSelected()) {
                selectedType = EntityType.ANY;
            } else {
                final Object selected = typeComboBox.getSelectedItem();
                if (selected instanceof EntityType) {
                    selectedType = (EntityType) selected;
                } else {
                    throw new IllegalStateException("Cannot store settings because no EntityType is selected.");
                }
            }
            if (config.getType() != selectedType) {
                // type was changed
                config.setType(selectedType);
                config.getSelectedEntities().clear();
            }
            setOpsOnConfig(config);
            setScope(config);

        } else {
            logger.log(Level.WARNING, "Cannot store settings because received invalid settings object: " + settings);
        }
    }

    private void setScope(final PermissionsConfig config) {
        if (allObjectsRadio.isSelected()) {
            config.setScopeType(null);
        } else if (conditionRadio.isSelected()) {
            config.setScopeType(PermissionsConfig.ScopeType.CONDITIONAL);
        } else if (specificObjectsRadio.isSelected()) {
            config.setScopeType(PermissionsConfig.ScopeType.SPECIFIC_OBJECTS);
        }
    }

    private void setOpsOnConfig(final PermissionsConfig config) {
        final Set<OperationType> ops = new HashSet<>();
        if (createCheckBox.isEnabled() && createCheckBox.isSelected()) {
            ops.add(OperationType.CREATE);
        }
        if (readCheckBox.isEnabled() && readCheckBox.isSelected()) {
            ops.add(OperationType.READ);
        }
        if (updateCheckBox.isEnabled() && updateCheckBox.isSelected()) {
            ops.add(OperationType.UPDATE);
        }
        if (deleteCheckBox.isEnabled() && deleteCheckBox.isSelected()) {
            ops.add(OperationType.DELETE);
        }
        config.setOperations(ops);
    }

    private void initRadioButtons() {
        final RunOnChangeListener radioListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                if (config != null) {
                    setScope(config);
                    enableDisable();
                    notifyListeners();
                }
            }
        });
        allTypesRadio.addItemListener(radioListener);
        specificTypeRadio.addItemListener(radioListener);
        allObjectsRadio.addItemListener(radioListener);
        conditionRadio.addItemListener(radioListener);
        specificObjectsRadio.addItemListener(radioListener);
    }

    private void initCheckBoxes() {
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
    }

    private void initComboBox() {
        typeComboBox.setModel(new DefaultComboBoxModel(ENTITY_TYPES.keySet().toArray()));
        typeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
                if (value instanceof EntityType) {
                    final EntityType type = (EntityType) value;
                    value = SINGULAR_ENTITY_TYPES.contains(type) ? type.getName() : type.getPluralName();
                } else {
                    value = SELECT_A_TYPE;
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        typeComboBox.addItemListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableDisable();
                notifyListeners();
            }
        }));
    }

    private void enableDisable() {
        typeComboBox.setEnabled(specificTypeRadio.isSelected());
        final Object selectedItem = typeComboBox.getSelectedItem();
        final boolean enableRadiosAndBoxes = allTypesRadio.isSelected() || selectedItem != null;
        Set<OperationType> invalidOps = null;
        if (selectedItem instanceof EntityType) {
            final EntityType selected = (EntityType) selectedItem;
            invalidOps = ENTITY_TYPES.get(selected);
        }
        allObjectsRadio.setEnabled(enableRadiosAndBoxes);
        conditionRadio.setEnabled(allTypesRadio.isSelected() || (selectedItem != null && !SINGULAR_ENTITY_TYPES.contains(selectedItem)));
        specificObjectsRadio.setEnabled(specificTypeRadio.isSelected() && selectedItem != null && !SINGULAR_ENTITY_TYPES.contains(selectedItem) && !LARGE_ENTITY_TYPES.contains(selectedItem));
        createCheckBox.setEnabled(enableRadiosAndBoxes && operationEnabled(OperationType.CREATE, invalidOps));
        readCheckBox.setEnabled(enableRadiosAndBoxes && operationEnabled(OperationType.READ, invalidOps));
        updateCheckBox.setEnabled(enableRadiosAndBoxes && operationEnabled(OperationType.UPDATE, invalidOps));
        deleteCheckBox.setEnabled(enableRadiosAndBoxes && operationEnabled(OperationType.DELETE, invalidOps));
    }

    private boolean operationEnabled(final OperationType operation, final Set<OperationType> invalidOps) {
        return invalidOps == null || !invalidOps.contains(operation);
    }
}
