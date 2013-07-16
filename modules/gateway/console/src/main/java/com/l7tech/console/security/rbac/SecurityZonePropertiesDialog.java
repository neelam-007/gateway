package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.BasicPropertiesPanel;
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
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
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
    private JList<JCheckBox> entityTypesList;
    private JRadioButton allEntityTypesRadio;
    private JRadioButton specifiedEntityTypesRadio;
    private OkCancelPanel okCancelPanel;
    private JButton selectAllButton;
    private JButton selectNoneButton;
    private BasicPropertiesPanel basicPropertiesPanel;
    private InputValidator inputValidator;
    private Set<String> reservedNames = new HashSet<>();
    private SecurityZone securityZone;
    private boolean readOnly;
    private OperationType operation;
    private Set<EntityType> originalSupportedEntityTypes = new HashSet<>();
    private Map<EntityType, Collection<Serializable>> entitiesToRemoveFromZone = new HashMap<>();
    private Functions.Unary<Boolean, SecurityZone> afterEditListener;

    public SecurityZonePropertiesDialog(@NotNull final Window owner, @NotNull final SecurityZone securityZone, final boolean readOnly, @NotNull final Functions.Unary<Boolean, SecurityZone> afterEditListener) {
        super(owner, readOnly ? "Security Zone Properties" : securityZone.getOid() == SecurityZone.DEFAULT_OID ? "Create Security Zone" : "Edit Security Zone", DEFAULT_MODALITY_TYPE);
        this.afterEditListener = afterEditListener;
        setContentPane(contentPane);
        getRootPane().setDefaultButton(okCancelPanel.getOkButton());
        Utilities.setEscAction(this, okCancelPanel.getCancelButton());
        this.securityZone = securityZone;
        this.readOnly = readOnly;
        operation = readOnly ? OperationType.READ : securityZone.getOid() == SecurityZone.DEFAULT_OID ? OperationType.CREATE : OperationType.UPDATE;
        if (operation == OperationType.UPDATE) {
            originalSupportedEntityTypes.addAll(securityZone.getPermittedEntityTypes().contains(EntityType.ANY) ? SecurityZoneUtil.getNonHiddenZoneableEntityTypes() : securityZone.getPermittedEntityTypes());
        }
        initComponents();
        setData();
        initValidation();
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
        okCancelPanel.getCancelButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                afterEditListener.call(null);
                dispose();
            }
        });
        if (readOnly || basicPropertiesPanel.getNameField().getText().trim().isEmpty()) {
            okCancelPanel.getOkButton().setEnabled(false);
        }
        Utilities.buttonToLink(selectAllButton);
        Utilities.buttonToLink(selectNoneButton);
        selectAllButton.addActionListener(new SelectActionListener(true));
        selectNoneButton.addActionListener(new SelectActionListener(false));
    }

    private void initValidation() {
        initReservedNames();
        inputValidator = basicPropertiesPanel.configureValidation(this, getTitle(), okCancelPanel.getOkButton(),
                new OkButtonActionListener(), getResourceInt(NAME_MAX_CHARS), getResourceInt(DESCRIPTION_MAX_CHARS));
        inputValidator.isValid();
    }

    private void loadEntitiesToRemoveFromZone(final Collection<EntityType> removedEntityTypes) throws FindException {
        this.entitiesToRemoveFromZone.clear();
        final RbacAdmin rbacAdmin = Registry.getDefault().getRbacAdmin();
        for (EntityType removedType : removedEntityTypes) {
            final Collection<EntityHeader> entitiesInZone = new ArrayList<>();
            if (removedType == EntityType.SSG_KEY_ENTRY) {
                removedType = EntityType.SSG_KEY_METADATA;
            }
            entitiesInZone.addAll(rbacAdmin.findEntitiesByTypeAndSecurityZoneOid(removedType, securityZone.getOid()));
            if (!entitiesInZone.isEmpty()) {
                final List<Serializable> ids = new ArrayList<>(entitiesInZone.size());
                for (final EntityHeader entity : entitiesInZone) {
                    ids.add(entity.getStrId());
                }
                this.entitiesToRemoveFromZone.put(removedType, ids);
            }
        }
    }

    private Map<EntityType, Integer> countEntitiesToRemoveFromZone() {
        final Map<EntityType, Integer> removeCount = new TreeMap<>(EntityType.NAME_COMPARATOR);
        for (final Map.Entry<EntityType, Collection<Serializable>> entry : entitiesToRemoveFromZone.entrySet()) {
            removeCount.put(entry.getKey(), entry.getValue().size());
        }
        return removeCount;
    }

    private void save() {
        SecurityZone copy = new SecurityZone();
        copy(securityZone, copy);
        final SecurityZone data = getData(copy);
        try {
            for (Map.Entry<EntityType, Collection<Serializable>> entry : entitiesToRemoveFromZone.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    Registry.getDefault().getRbacAdmin().setSecurityZoneForEntities(null, entry.getKey(), entry.getValue());
                }
            }
            final boolean successful = afterEditListener.call(data);
            if (successful) {
                dispose();
            }
        } catch (final UpdateException e) {
            DialogDisplayer.showMessageDialog(SecurityZonePropertiesDialog.this, "Error", "Unable to remove entities from this zone.", e);
        }
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
        basicPropertiesPanel.getNameField().setText(securityZone.getName());
        basicPropertiesPanel.getDescriptionTextArea().setText(securityZone.getDescription());

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
        zone.setName(basicPropertiesPanel.getNameField().getText().trim());
        zone.setDescription(basicPropertiesPanel.getDescriptionTextArea().getText().trim());

        final Set<EntityType> permittedTypes = getSelectedEntityTypes();
        zone.setPermittedEntityTypes(permittedTypes);

        return zone;
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

        // ensure that any 'hidden' types are selected as well
        for (final Map.Entry<EntityType, Collection<EntityType>> entry : SecurityZoneUtil.getEntityTypesWithInheritedZones().entrySet()) {
            if (permittedTypes.contains(entry.getKey())) {
                permittedTypes.addAll(entry.getValue());
            }
        }

        return permittedTypes;
    }

    private void copy(final SecurityZone src, final SecurityZone dest) {
        dest.setOid(src.getOid());
        dest.setVersion(src.getVersion());
        dest.setName(src.getName());
        dest.setDescription(src.getDescription());
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

    private class OkButtonActionListener implements ActionListener {
        public void actionPerformed(final ActionEvent e) {
            // don't display name validation errors until after user clicks ok
            if (reservedNames.contains(basicPropertiesPanel.getNameField().getText().trim().toLowerCase())) {
                final String error = MessageFormat.format(RESOURCES.getString(ERROR_UNIQUE_NAME), operation.getName().toLowerCase());
                DialogDisplayer.showMessageDialog(SecurityZonePropertiesDialog.this, error,
                        operation.getName() + " Security Zone", JOptionPane.ERROR_MESSAGE, null);
            } else {
                final Set<EntityType> selected = getSelectedEntityTypes();
                if (operation == OperationType.UPDATE && !selected.contains(EntityType.ANY)) {
                    final Collection<EntityType> removedEntityTypes = CollectionUtils.subtract(originalSupportedEntityTypes, selected);
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
                                        save();
                                    }
                                }
                            });
                        } else {
                            save();
                        }
                    } catch (final FindException ex) {
                        DialogDisplayer.showMessageDialog(SecurityZonePropertiesDialog.this, "Error", "Unable to retrieve entities which must be removed from this zone.", ex);
                    }
                } else {
                    save();
                }
            }
        }
    }
}
