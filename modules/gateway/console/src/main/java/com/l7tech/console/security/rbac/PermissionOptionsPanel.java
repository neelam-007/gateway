package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.OtherOperationName;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.objectmodel.EntityType;
import org.apache.commons.collections.ComparatorUtils;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.*;
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
    private static final Map<EntityType, OtherOperationName> ENTITY_TYPES_WITH_OTHER_OPS;
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
    private JCheckBox otherCheckBox;
    private JTextField otherTextField;
    private PermissionsConfig config;

    static {
        SINGULAR_ENTITY_TYPES = new HashSet<>();
        SINGULAR_ENTITY_TYPES.add(EntityType.SSG_KEYSTORE);
        SINGULAR_ENTITY_TYPES.add(EntityType.RESOLUTION_CONFIGURATION);
        SINGULAR_ENTITY_TYPES.add(EntityType.PASSWORD_POLICY);

        LARGE_ENTITY_TYPES = new HashSet<>();
        LARGE_ENTITY_TYPES.add(EntityType.METRICS_BIN);
        LARGE_ENTITY_TYPES.add(EntityType.AUDIT_RECORD);

        ENTITY_TYPES_WITH_OTHER_OPS = new HashMap<>();
        ENTITY_TYPES_WITH_OTHER_OPS.put(EntityType.AUDIT_RECORD, OtherOperationName.AUDIT_VIEWER_POLICY);
        ENTITY_TYPES_WITH_OTHER_OPS.put(EntityType.LOG_SINK, OtherOperationName.LOG_VIEWER);
        ENTITY_TYPES_WITH_OTHER_OPS.put(EntityType.POLICY, OtherOperationName.DEBUGGER);

        ENTITY_TYPES = new TreeMap(ComparatorUtils.nullLowComparator(EntityType.NAME_COMPARATOR));
        ENTITY_TYPES.put(null, null);
        for (final EntityType type : EntityType.values()) {
            if (type != EntityType.ANY && type.isDisplayedInGui() && type != EntityType.AUDIT_ADMIN && type != EntityType.AUDIT_MESSAGE && type != EntityType.AUDIT_SYSTEM) {
                final Set<OperationType> invalidOps = new HashSet<>();
                if (SINGULAR_ENTITY_TYPES.contains(type) || type == EntityType.ASSERTION_ACCESS || type == EntityType.CLUSTER_INFO) {
                    invalidOps.add(OperationType.CREATE);
                    invalidOps.add(OperationType.DELETE);
                } else if (type == EntityType.SERVICE_TEMPLATE || type == EntityType.SERVICE_USAGE || LARGE_ENTITY_TYPES.contains(type)) {
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
        initOperationInputs();
        initRadioButtons();
        enableDisable();
    }


    @Override
    public String getStepLabel() {
        return PERMISSION_OPTIONS;
    }

    @Override
    public boolean canAdvance() {
        final boolean atLeastOneOpSelected = createCheckBox.isSelected() || readCheckBox.isSelected() ||
                updateCheckBox.isSelected() || deleteCheckBox.isSelected() ||
                (otherCheckBox.isSelected() && !otherTextField.getText().isEmpty());
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
        if (otherCheckBox.isEnabled() && otherCheckBox.isSelected() && StringUtils.isNotBlank(otherTextField.getText())) {
            final OtherOperationName otherOp = OtherOperationName.getByName(otherTextField.getText());
            if (otherOp != null) {
                config.setOtherOpName(otherOp);
                ops.add(OperationType.OTHER);
            } else {
                logger.log(Level.WARNING, "Invalid otherOperationName: " + otherTextField.getText());
            }
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

    private void initOperationInputs() {
        final RunOnChangeListener operationsListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                notifyListeners();
            }
        });
        createCheckBox.addChangeListener(operationsListener);
        readCheckBox.addChangeListener(operationsListener);
        updateCheckBox.addChangeListener(operationsListener);
        deleteCheckBox.addChangeListener(operationsListener);
        otherCheckBox.addChangeListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                otherTextField.setEnabled(otherCheckBox.isSelected());
                notifyListeners();
            }
        }));
        otherTextField.addActionListener(operationsListener);
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
                updateOtherOps();
                notifyListeners();
            }
        }));
        otherTextField.setText(StringUtils.EMPTY);
    }

    private void updateOtherOps() {
        final Object selectedItem = typeComboBox.getSelectedItem();
        final OtherOperationName otherOp = selectedItem == null ? null : ENTITY_TYPES_WITH_OTHER_OPS.get(selectedItem);
        otherTextField.setText(otherOp == null ? StringUtils.EMPTY : otherOp.getOperationName());
    }

    private void enableDisable() {
        typeComboBox.setEnabled(specificTypeRadio.isSelected());
        final Object selectedItem = typeComboBox.getSelectedItem();
        final boolean enableRadiosAndBoxes = allTypesRadio.isSelected() || selectedItem != null;
        Set<OperationType> invalidOps = null;
        EntityType selectedType = null;
        if (selectedItem instanceof EntityType) {
            selectedType = (EntityType) selectedItem;
            invalidOps = ENTITY_TYPES.get(selectedType);
        }
        allObjectsRadio.setEnabled(enableRadiosAndBoxes);
        conditionRadio.setEnabled(allTypesRadio.isSelected() || (selectedItem != null && !SINGULAR_ENTITY_TYPES.contains(selectedItem)));
        specificObjectsRadio.setEnabled(specificTypeRadio.isSelected() && selectedType != null && !SINGULAR_ENTITY_TYPES.contains(selectedType) && !LARGE_ENTITY_TYPES.contains(selectedType) && selectedType.isAllowSpecificScope());
        createCheckBox.setEnabled(enableRadiosAndBoxes && operationEnabled(OperationType.CREATE, invalidOps));
        readCheckBox.setEnabled(enableRadiosAndBoxes && operationEnabled(OperationType.READ, invalidOps));
        updateCheckBox.setEnabled(enableRadiosAndBoxes && operationEnabled(OperationType.UPDATE, invalidOps));
        deleteCheckBox.setEnabled(enableRadiosAndBoxes && operationEnabled(OperationType.DELETE, invalidOps));
        final boolean enableOther = enableRadiosAndBoxes && selectedType != null && ENTITY_TYPES_WITH_OTHER_OPS.containsKey(selectedType);
        otherCheckBox.setEnabled(enableOther);
        otherTextField.setEnabled(enableOther && otherCheckBox.isSelected());
    }

    private boolean operationEnabled(final OperationType operation, final Set<OperationType> invalidOps) {
        return invalidOps == null || !invalidOps.contains(operation);
    }
}
