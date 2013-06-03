package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.OkCancelPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.JCheckBoxListModel;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SecurityZonePropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(SecurityZonePropertiesDialog.class.getName());
    private static final ResourceBundle RESOURCES = ResourceBundle.getBundle(SecurityZonePropertiesDialog.class.getName());
    private static final String CLIENT_PROP_ENTITY_TYPE = "com.l7tech.szpd.entityType";
    private static final String NAME_MAX_CHARS = "name.max.chars";
    private static final String DESCRIPTION_MAX_CHARS = "description.max.chars";
    private static final String ERROR_UNIQUE_NAME = "error.unique.name";
    private JPanel contentPane;
    private JTextField nameField;
    private JTextArea descriptionTextArea;
    private JList<JCheckBox> entityTypesList;
    private JRadioButton allEntityTypesRadio;
    private JRadioButton specifiedEntityTypesRadio;
    private OkCancelPanel okCancelPanel;
    private JButton selectAllButton;
    private JButton selectNoneButton;
    private JLabel errorLabel;
    private InputValidator inputValidator;
    private Set<String> reservedNames = new HashSet<>();
    private SecurityZone securityZone;
    private boolean readOnly;
    private boolean confirmed = false;
    private OperationType operation;
    private Set<EntityType> originalSupportedEntityTypes = new HashSet<>();
    private Map<EntityType, Collection<Long>> entitiesToRemoveFromZone = new HashMap<>();

    public SecurityZonePropertiesDialog(@NotNull final Window owner, @NotNull final SecurityZone securityZone, final boolean readOnly) {
        super(owner, readOnly ? "Security Zone Properties" : securityZone.getOid() == SecurityZone.DEFAULT_OID ? "Create Security Zone" : "Edit Security Zone", DEFAULT_MODALITY_TYPE);
        setContentPane(contentPane);
        getRootPane().setDefaultButton(okCancelPanel.getOkButton());
        Utilities.setEscAction(this, okCancelPanel.getCancelButton());
        this.securityZone = securityZone;
        this.readOnly = readOnly;
        operation = readOnly ? OperationType.READ : securityZone.getOid() == SecurityZone.DEFAULT_OID ? OperationType.CREATE : OperationType.UPDATE;
        if (operation == OperationType.UPDATE) {
            originalSupportedEntityTypes.addAll(securityZone.getPermittedEntityTypes());
        }
        initValidation();
        initComponents();
        setData();
    }

    private void initComponents() {
        final RunOnChangeListener enableDisableListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableAndDisable();
            }
        });
        specifiedEntityTypesRadio.addActionListener(enableDisableListener);
        allEntityTypesRadio.addActionListener(enableDisableListener);
        Utilities.enableGrayOnDisabled(entityTypesList);

        if (OperationType.CREATE == operation) {
            okCancelPanel.setOkButtonText(OperationType.CREATE.getName());
        } else if (OperationType.UPDATE == operation) {
            okCancelPanel.setOkButtonText(OperationType.UPDATE.getName());
        }
        okCancelPanel.getCancelButton().addActionListener(Utilities.createDisposeAction(this));
        if (readOnly || nameField.getText().trim().isEmpty()) {
            okCancelPanel.getOkButton().setEnabled(false);
        }
        Utilities.buttonToLink(selectAllButton);
        Utilities.buttonToLink(selectNoneButton);
        selectAllButton.addActionListener(new SelectActionListener(true));
        selectNoneButton.addActionListener(new SelectActionListener(false));
        errorLabel.setVisible(false);
    }

    private void initValidation() {
        initReservedNames();
        inputValidator = new InputValidator(this, getTitle()) {
            @Override
            public boolean isValid() {
                // dynamic error message - shows first error if there are errors
                final boolean valid = super.isValid();
                if (!valid) {
                    final String[] errors = getAllValidationErrors();
                    errorLabel.setText(errors.length > 0 ? errors[0] : StringUtils.EMPTY);
                    errorLabel.setVisible(true);
                } else {
                    errorLabel.setText(StringUtils.EMPTY);
                    errorLabel.setVisible(false);
                }
                return valid;
            }
        };
        inputValidator.disableButtonWhenInvalid(okCancelPanel.getOkButton());
        inputValidator.constrainTextFieldToBeNonEmpty("name", nameField, null);
        final Integer maxNameChars = getResourceInt(NAME_MAX_CHARS);
        if (maxNameChars != null) {
            inputValidator.constrainTextFieldToMaxChars("name", nameField, maxNameChars, null);
        }
        final Integer maxDescChars = getResourceInt(DESCRIPTION_MAX_CHARS);
        if (maxDescChars != null) {
            inputValidator.constrainTextFieldToMaxChars("description", descriptionTextArea, maxDescChars, null);
        }
        inputValidator.attachToButton(okCancelPanel.getOkButton(), new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                // don't display name validation errors until after user clicks ok
                if (reservedNames.contains(nameField.getText().trim().toLowerCase())) {
                    final String error = MessageFormat.format(RESOURCES.getString(ERROR_UNIQUE_NAME), operation.getName().toLowerCase());
                    DialogDisplayer.showMessageDialog(SecurityZonePropertiesDialog.this, error,
                            operation.getName() + " Security Zone", JOptionPane.ERROR_MESSAGE, null);
                } else {
                    if (operation == OperationType.UPDATE) {
                        final Collection<EntityType> removedEntityTypes = CollectionUtils.subtract(originalSupportedEntityTypes, getSelectedEntityTypes());
                        try {
                            loadEntitiesToRemoveFromZone(removedEntityTypes);
                            if (!entitiesToRemoveFromZone.isEmpty()) {
                                final Map<EntityType, Integer> removeCount = countEntitiesToRemoveFromZone();
                                final EntityTypeRemovalDialog confirmRemove = new EntityTypeRemovalDialog(SecurityZonePropertiesDialog.this, removeCount);
                                confirmRemove.pack();
                                DialogDisplayer.display(confirmRemove, new Runnable() {
                                    @Override
                                    public void run() {
                                        if (confirmRemove.isConfirmed()) {
                                            confirm();
                                        }
                                    }
                                });
                            } else {
                                confirm();
                            }
                        } catch (final FindException ex) {
                            DialogDisplayer.showMessageDialog(SecurityZonePropertiesDialog.this, "Error", "Unable to retrieve entities which must be removed from this zone.", ex);
                        }
                    } else {
                        confirm();
                    }
                }
            }
        });
    }

    private void loadEntitiesToRemoveFromZone(final Collection<EntityType> removedEntityTypes) throws FindException {
        final RbacAdmin rbacAdmin = Registry.getDefault().getRbacAdmin();
        for (EntityType removedType : removedEntityTypes) {
            final Collection<Entity> entitiesInZone = new ArrayList<>();
            if (removedType == EntityType.SSG_KEY_ENTRY) {
                removedType = EntityType.SSG_KEY_METADATA;
            }
            // TODO create a different admin method which only returns relevant data (name and oids)
            entitiesInZone.addAll(rbacAdmin.findEntitiesByTypeAndSecurityZoneOid(removedType, securityZone.getOid()));
            if (!entitiesInZone.isEmpty()) {
                final List<Long> oids = new ArrayList<>(entitiesInZone.size());
                for (final Entity entity : entitiesInZone) {
                    oids.add(Long.valueOf(entity.getId()));
                }
                this.entitiesToRemoveFromZone.put(removedType, oids);
            }
        }
    }

    private Map<EntityType, Integer> countEntitiesToRemoveFromZone() {
        final Map<EntityType, Integer> removeCount = new TreeMap<>(EntityType.NAME_COMPARATOR);
        for (final Map.Entry<EntityType, Collection<Long>> entry : entitiesToRemoveFromZone.entrySet()) {
            removeCount.put(entry.getKey(), entry.getValue().size());
        }
        return removeCount;
    }

    private void confirm() {
        confirmed = true;
        dispose();
    }

    /**
     * Converts all name to lower case so that matching can be done as case insensitive.
     */
    private void initReservedNames() {
        for (final SecurityZone zone : SecurityZoneUtil.getSecurityZones()) {
            reservedNames.add(zone.getName().toLowerCase());
        }
        if (securityZone.getOid() != SecurityZone.DEFAULT_OID) {
            reservedNames.remove(securityZone.getName().toLowerCase());
        }
    }

    private void enableAndDisable() {
        entityTypesList.setEnabled(specifiedEntityTypesRadio.isSelected());
        for (int i = 0; i < entityTypesList.getModel().getSize(); i++) {
            entityTypesList.getModel().getElementAt(i).setEnabled(specifiedEntityTypesRadio.isSelected());
        }
        selectAllButton.setEnabled(specifiedEntityTypesRadio.isSelected());
        selectNoneButton.setEnabled(specifiedEntityTypesRadio.isSelected());
    }

    void setData() {
        nameField.setText(securityZone.getName());
        descriptionTextArea.setText(securityZone.getDescription());

        final Set<EntityType> permittedTypes = securityZone.getPermittedEntityTypes();
        final List<JCheckBox> entries = new ArrayList<JCheckBox>();
        final Set<EntityType> entityTypes = SecurityZoneUtil.getNonHiddenZoneableEntityTypes();
        entries.addAll(Functions.map(entityTypes, new Functions.Unary<JCheckBox, EntityType>() {
            @Override
            public JCheckBox call(EntityType entityType) {
                JCheckBox cb = new JCheckBox(entityType.getName(), securityZone.permitsEntityType(entityType));
                cb.putClientProperty(CLIENT_PROP_ENTITY_TYPE, entityType);
                return cb;
            }
        }));
        final JCheckBoxListModel model = new JCheckBoxListModel(entries);
        model.attachToJList(entityTypesList);

        boolean allTypes = permittedTypes.contains(EntityType.ANY);
        allEntityTypesRadio.setSelected(allTypes);
        specifiedEntityTypesRadio.setSelected(!allTypes);

        enableAndDisable();
    }

    public SecurityZone getData(@NotNull final SecurityZone zone) {
        zone.setName(nameField.getText().trim());
        zone.setDescription(descriptionTextArea.getText().trim());

        final Set<EntityType> permittedTypes = getSelectedEntityTypes();
        zone.setPermittedEntityTypes(permittedTypes);

        return zone;
    }

    public Map<EntityType, Collection<Long>> getEntitiesToRemoveFromZone() {
        return entitiesToRemoveFromZone;
    }

    private Set<EntityType> getSelectedEntityTypes() {
        Set<EntityType> permittedTypes;
        if (allEntityTypesRadio.isSelected()) {
            permittedTypes = EnumSet.of(EntityType.ANY);
        } else {
            permittedTypes = EnumSet.noneOf(EntityType.class);
            final ListModel<JCheckBox> typesModel = entityTypesList.getModel();
            int typeSize = typesModel.getSize();
            for (int i = 0; i < typeSize; ++i) {
                JCheckBox cb = typesModel.getElementAt(i);
                if (cb.isSelected()) {
                    EntityType entityType = (EntityType) cb.getClientProperty(CLIENT_PROP_ENTITY_TYPE);
                    if (entityType != null) {
                        permittedTypes.add(entityType);
                    }
                }
            }
        }
        return permittedTypes;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private Integer getResourceInt(final String property) {
        Integer ret = null;
        final String val = RESOURCES.getString(property);
        try {
            ret = Integer.valueOf(val);
        } catch (final NumberFormatException e) {
            logger.log(Level.WARNING, "Property " + property + " is an invalid number: " + val, ExceptionUtils.getDebugException(e));
        }
        return ret;
    }

    private class SelectActionListener implements ActionListener {
        // true for select all, false for select none
        private final boolean selectAll;

        private SelectActionListener(final boolean selectAll) {
            this.selectAll = selectAll;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final JCheckBoxListModel model = (JCheckBoxListModel) entityTypesList.getModel();
            model.visitEntries(new Functions.Binary<Boolean, Integer, JCheckBox>() {
                @Override
                public Boolean call(final Integer integer, final JCheckBox jCheckBox) {
                    return selectAll;
                }
            });
        }
    }
}
