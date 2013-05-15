package com.l7tech.console.security.rbac;

import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.JCheckBoxListModel;
import com.l7tech.console.panels.OkCancelPanel;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
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
    private JTextField nameField;
    private JTextField descriptionField;
    private JList<JCheckBox> entityTypesList;
    private JRadioButton allEntityTypesRadio;
    private JRadioButton specifiedEntityTypesRadio;
    private OkCancelPanel okCancelPanel;
    private JButton selectAllButton;
    private JButton selectNoneButton;
    private final InputValidator inputValidator = new InputValidator(this, getTitle());

    private boolean confirmed = false;

    public SecurityZonePropertiesDialog(Window owner, SecurityZone securityZone, boolean readOnly) {
        super(owner, "Security Zone Properties", DEFAULT_MODALITY_TYPE);
        setContentPane(contentPane);
        getRootPane().setDefaultButton(okCancelPanel.getOkButton());
        Utilities.setEscAction(this, okCancelPanel.getCancelButton());

        inputValidator.constrainTextFieldToBeNonEmpty("name", nameField, null);
        inputValidator.constrainTextFieldToMaxChars("name", nameField, MAX_CHARS_FOR_NAME, null);
        inputValidator.constrainTextFieldToMaxChars("description", descriptionField, MAX_CHARS_FOR_DESCRIPTION, null);
        inputValidator.attachToButton(okCancelPanel.getOkButton(), new ActionListener() {
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

        okCancelPanel.getCancelButton().addActionListener(Utilities.createDisposeAction(this));
        okCancelPanel.getOkButton().setEnabled(!readOnly);
        buttonToLink(selectAllButton);
        buttonToLink(selectNoneButton);
        selectAllButton.addActionListener(new SelectActionListener(true));
        selectNoneButton.addActionListener(new SelectActionListener(false));
        setData(securityZone);
    }

    private void enableAndDisable() {
        entityTypesList.setEnabled(specifiedEntityTypesRadio.isSelected());
        selectAllButton.setEnabled(specifiedEntityTypesRadio.isSelected());
        selectNoneButton.setEnabled(specifiedEntityTypesRadio.isSelected());
    }

    void setData(final SecurityZone zone) {
        nameField.setText(zone.getName());
        descriptionField.setText(zone.getDescription());

        final Set<EntityType> permittedTypes = zone.getPermittedEntityTypes();
        List<JCheckBox> entries = new ArrayList<JCheckBox>();
        final Set<EntityType> allZoneableEntityTypes = SecurityZoneUtil.getAllZoneableEntityTypes();
        entries.addAll(Functions.map(allZoneableEntityTypes, new Functions.Unary<JCheckBox, EntityType>() {
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

    private void buttonToLink(final JButton button) {
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setForeground(Color.BLUE);
        button.setMargin(new Insets(0, 0, 0, 0));
    }

    private class SelectActionListener implements ActionListener {
        // true for select all, false for select none
        private final boolean selectAll;
        private SelectActionListener(final boolean selectAll) {
            this.selectAll = selectAll;
        }
        @Override
        public void actionPerformed(final ActionEvent e) {
            final JCheckBoxListModel model = (JCheckBoxListModel)entityTypesList.getModel();
            model.visitEntries(new Functions.Binary<Boolean, Integer, JCheckBox>() {
                @Override
                public Boolean call(final Integer integer, final JCheckBox jCheckBox) {
                    return selectAll;
                }
            });
        }
    }
}
