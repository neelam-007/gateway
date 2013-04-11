package com.l7tech.console.security.rbac;

import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.JCheckBoxListModel;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.ZoneableEntity;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

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
    private final InputValidator inputValidator = new InputValidator(this, getTitle());

    private boolean confirmed = false;

    public SecurityZonePropertiesDialog(Window owner, SecurityZone securityZone, boolean readOnly) {
        super(owner, "Security Zone Properties", DEFAULT_MODALITY_TYPE);
        setContentPane(contentPane);
        getRootPane().setDefaultButton(okButton);
        Utilities.setEscAction(this, cancelButton);

        inputValidator.constrainTextFieldToBeNonEmpty("name", nameField, null);
        inputValidator.constrainTextFieldToMaxChars("name", nameField, MAX_CHARS_FOR_NAME, null);
        inputValidator.constrainTextFieldToBeNonEmpty("description", descriptionField, null);
        inputValidator.constrainTextFieldToMaxChars("description", descriptionField, MAX_CHARS_FOR_DESCRIPTION, null);
        inputValidator.attachToButton(okButton, new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                confirmed = true;
                dispose();
            }
        });

        cancelButton.addActionListener(Utilities.createDisposeAction(this));
        okButton.setEnabled(!readOnly);
        setData(securityZone);
    }

    void setData(SecurityZone zone) {
        nameField.setText(zone.getName());
        descriptionField.setText(zone.getDescription());
        final Set<EntityType> permittedTypes = zone.getPermittedEntityTypes();
        List<JCheckBox> entries = new ArrayList<JCheckBox>();
        entries.addAll(Functions.map(getAllZoneableEntityTypes(), new Functions.Unary<JCheckBox, EntityType>() {
            @Override
            public JCheckBox call(EntityType entityType) {
                JCheckBox cb = new JCheckBox(entityType.getName(), permittedTypes.contains(entityType));
                cb.putClientProperty(CLIENT_PROP_ENTITY_TYPE, entityType);
                return cb;
            }
        }));
        JCheckBoxListModel model = new JCheckBoxListModel(entries);
        model.attachToJList(entityTypesList);
    }

    static Set<EntityType> getAllZoneableEntityTypes() {
        Set<EntityType> ret = EnumSet.noneOf(EntityType.class);
        EntityType[] types = EntityType.values();
        for (EntityType type : types) {
            Class<? extends Entity> ec = type.getEntityClass();
            if (ec != null && ZoneableEntity.class.isAssignableFrom(ec))
                ret.add(type);
        }
        return ret;
    }

    public SecurityZone getData(SecurityZone zone) {
        zone.setName(nameField.getText());
        zone.setDescription(descriptionField.getText());

        Set<EntityType> permittedTypes = EnumSet.noneOf(EntityType.class);
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
        zone.setPermittedEntityTypes(permittedTypes);

        return zone;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
