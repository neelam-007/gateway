package com.l7tech.console.security.rbac;

import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.JCheckBoxListModel;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class SecurityZonePropertiesDialog extends JDialog {
    private static final String CLIENT_PROP_ENTITY_TYPE = "com.l7tech.szpd.entityType";
    private static final int MAX_CHARS_FOR_NAME = 128;
    private static final int MAX_CHARS_FOR_DESCRIPTION = 255;
    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField nameField;
    private JTextField descriptionField;
    private JList<JCheckBox> entityTypesList;
    private JRadioButton allEntityTypesRadio;
    private JRadioButton specifiedEntityTypesRadio;
    private final InputValidator inputValidator = new InputValidator(this, getTitle());

    private boolean confirmed = false;

    public SecurityZonePropertiesDialog(Window owner, SecurityZone securityZone, boolean readOnly) {
        super(owner, "Security Zone Properties", DEFAULT_MODALITY_TYPE);
        setContentPane(contentPane);
        getRootPane().setDefaultButton(okButton);
        Utilities.setEscAction(this, cancelButton);

        inputValidator.constrainTextFieldToBeNonEmpty("name", nameField, null);
        inputValidator.constrainTextFieldToMaxChars("name", nameField, MAX_CHARS_FOR_NAME, null);
        inputValidator.constrainTextFieldToMaxChars("description", descriptionField, MAX_CHARS_FOR_DESCRIPTION, null);
        inputValidator.attachToButton(okButton, new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                confirmed = true;
                dispose();
            }
        });

        RunOnChangeListener enableDisableListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableAndDisable();
            }
        });
        specifiedEntityTypesRadio.addActionListener(enableDisableListener);
        allEntityTypesRadio.addActionListener(enableDisableListener);
        Utilities.enableGrayOnDisabled(entityTypesList);

        cancelButton.addActionListener(Utilities.createDisposeAction(this));
        okButton.setEnabled(!readOnly);
        setData(securityZone);
    }

    private void enableAndDisable() {
        entityTypesList.setEnabled(specifiedEntityTypesRadio.isSelected());
    }

    void setData(final SecurityZone zone) {
        nameField.setText(zone.getName());
        descriptionField.setText(zone.getDescription());

        final Set<EntityType> permittedTypes = zone.getPermittedEntityTypes();
        List<JCheckBox> entries = new ArrayList<JCheckBox>();
        entries.addAll(Functions.map(getAllZoneableEntityTypes(), new Functions.Unary<JCheckBox, EntityType>() {
            @Override
            public JCheckBox call(EntityType entityType) {
                JCheckBox cb = new JCheckBox(entityType.getName(), zone.permitsEntityType(entityType));
                cb.putClientProperty(CLIENT_PROP_ENTITY_TYPE, entityType);
                return cb;
            }
        }));
        JCheckBoxListModel model = new JCheckBoxListModel(entries);
        model.attachToJList(entityTypesList);

        boolean allTypes = permittedTypes.contains(EntityType.ANY);
        allEntityTypesRadio.setSelected(allTypes);
        specifiedEntityTypesRadio.setSelected(!allTypes);

        enableAndDisable();
    }

    static Set<EntityType> getAllZoneableEntityTypes() {
        Set<EntityType> ret = EnumSet.noneOf(EntityType.class);
        for (EntityType type : EntityType.values()) {
            if (type.isSecurityZoneable())
                ret.add(type);
        }
        return ret;
    }

    public SecurityZone getData(SecurityZone zone) {
        zone.setName(nameField.getText());
        zone.setDescription(descriptionField.getText());

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
        zone.setPermittedEntityTypes(permittedTypes);

        return zone;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
