/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.idattr.console;

import com.l7tech.common.gui.util.RunOnChangeListener;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.mapping.*;
import com.l7tech.policy.variable.VariableMetadata;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * @author alex
 */
public class UserAttributeMappingDialog extends JDialog {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.idattr.console.resources.UserAttributeMappingDialog");

    private JRadioButton builtInAttributeRadioButton;
    private JRadioButton customAttributeRadioButton;
    private JComboBox builtInAttributeCombo;
    private JTextField variableNameField;
    private JButton cancelButton;
    private JButton okButton;
    private JTextField customAttributeField;

    private JPanel mainPanel;
    private JLabel providerNameLabel;
    private JLabel variablePrefixLabel;
    private JCheckBox multivaluedCheckBox;

    private boolean ok = false;

    private final IdentityMapping mapping;
    private final IdentityProviderConfig config;
    private final RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
        public void run() {
            enableDisable();
        }
    });

    private boolean allowCustom = false;
    private final String prefix;

    private void enableDisable() {
        final boolean builtin = builtInAttributeRadioButton.isSelected();
        builtInAttributeCombo.setEnabled(builtin);
        customAttributeField.setEnabled(!builtin && allowCustom);
        customAttributeRadioButton.setEnabled(allowCustom);

        String vn = variableNameField.getText();
        boolean ok = true;
        if (vn == null || vn.trim().length() == 0 || !VariableMetadata.isNameValid(vn)) {
            ok = false;
        } else if (customAttributeRadioButton.isSelected()) {
            final String cf = customAttributeField.getText();
            if (cf == null || cf.trim().length() == 0) ok = false;
        }
        okButton.setEnabled(ok);
    }

    public UserAttributeMappingDialog(Frame owner, IdentityMapping mapping, IdentityProviderConfig config, String prefix) throws HeadlessException {
        super(owner, resources.getString("dialog.title"), true);
        this.mapping = mapping;
        this.config = config;
        this.prefix = prefix;
        init();
    }

    public UserAttributeMappingDialog(Dialog owner, IdentityMapping mapping, IdentityProviderConfig config, String prefix) throws HeadlessException {
        super(owner, resources.getString("dialog.title"), true);
        this.mapping = mapping;
        this.config = config;
        this.prefix = prefix;
        init();
    }

    private void init() {
        Utilities.setEscKeyStrokeDisposes(this);

        variablePrefixLabel.setText(prefix + " . ");
        
        providerNameLabel.setBorder(LineBorder.createGrayLineBorder());
        providerNameLabel.setText(config.getName());

        IdentityProviderType type = config.type();
        if (type == null) throw new IllegalArgumentException("IdentityProviderType is null");
        final AttributeHeader header = mapping.getAttributeConfig().getHeader();

        // TODO display only user attributes if applicable
        AttributeHeader[] builtinAtts;
        if (type == IdentityProviderType.INTERNAL) {
            builtinAtts = InternalAttributeMapping.getBuiltinAttributes();
            allowCustom = false;
        } else if (type == IdentityProviderType.FEDERATED) {
            builtinAtts = FederatedAttributeMapping.getBuiltinAttributes();
            allowCustom = false;
        } else if (type == IdentityProviderType.LDAP) {
            builtinAtts = LdapAttributeMapping.getBuiltinAttributes();
            allowCustom = true;
        } else {
            throw new IllegalArgumentException("Can't handle IDP of type " + type.description());
        }

        java.util.List<AttributeHeader> displayHeaders = new ArrayList<AttributeHeader>();
        for (AttributeHeader att : builtinAtts) {
            switch(att.getUsersOrGroups()) {
                case USERS:
                    if (mapping.isValidForUsers()) displayHeaders.add(att);
                    break;
                case BOTH:
                    if (mapping.isValidForGroups() || mapping.isValidForUsers()) displayHeaders.add(att);
                    break;
                case GROUPS:
                    if (mapping.isValidForGroups()) displayHeaders.add(att);
                    break;
            }
        }

        builtInAttributeCombo.setModel(new DefaultComboBoxModel(displayHeaders.toArray(new AttributeHeader[0])));

        if (header.isBuiltin()) {
            builtInAttributeRadioButton.setSelected(true);
            builtInAttributeCombo.setSelectedItem(header);
            variableNameField.setText(mapping.getAttributeConfig().getVariableName());
        } else if (mapping.getAttributeConfig().getVariableName() == null) {
            // New object, created from scratch--copy the variable name from the first built-in option
            builtInAttributeRadioButton.setSelected(true);
            variableNameField.setText(((AttributeHeader)builtInAttributeCombo.getSelectedItem()).getVariableName());
        } else {
            customAttributeRadioButton.setSelected(true);
            customAttributeField.setText(mapping.getCustomAttributeName());
            variableNameField.setText(mapping.getAttributeConfig().getVariableName());
        }

        multivaluedCheckBox.setSelected(mapping.isMultivalued());

        builtInAttributeRadioButton.addActionListener(changeListener);
        customAttributeRadioButton.addActionListener(changeListener);
        customAttributeRadioButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { customAttributeField.requestFocus(); }
        });
        customAttributeField.getDocument().addDocumentListener(changeListener);
        variableNameField.getDocument().addDocumentListener(changeListener);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final AttributeConfig config = mapping.getAttributeConfig();

                if (builtInAttributeCombo.isEnabled()) {
                    AttributeHeader builtinHeader = (AttributeHeader) builtInAttributeCombo.getSelectedItem();
                    if (builtinHeader != null) config.setHeader(builtinHeader);
                } else if (customAttributeRadioButton.isSelected()) {
                    mapping.setCustomAttributeName(customAttributeField.getText());
                }

                mapping.setMultivalued(multivaluedCheckBox.isSelected());
                // This must happen after {@link AttributeConfig#setHeader}
                config.setVariableName(variableNameField.getText());
                ok = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        builtInAttributeCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                variableNameField.setText(((AttributeHeader) builtInAttributeCombo.getSelectedItem()).getVariableName());
            }
        });

        customAttributeField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            public void run() {
                if (customAttributeRadioButton.isSelected()) {
                    variableNameField.setText(customAttributeField.getText());
                }
            }
        }));

        enableDisable();
        
        add(mainPanel);
    }

    public boolean isOk() {
        return ok;
    }
}
