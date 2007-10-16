/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.mapping.*;
import com.l7tech.common.gui.util.RunOnChangeListener;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.ArrayList;

/**
 * @author alex
 */
public class UserAttributeMappingDialog extends JDialog {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.UserAttributeMappingDialog");

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
    private final ActionListener radioListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            enableRadioState();
        }
    };

    private boolean allowCustom = false;

    private void enableRadioState() {
        final boolean builtin = builtInAttributeRadioButton.isSelected();
        builtInAttributeCombo.setEnabled(builtin);
        customAttributeField.setEnabled(!builtin && allowCustom);
        customAttributeRadioButton.setEnabled(allowCustom);
    }

    public UserAttributeMappingDialog(Frame owner, IdentityMapping mapping, IdentityProviderConfig config) throws HeadlessException {
        super(owner, resources.getString("dialog.title"), true);
        this.mapping = mapping;
        this.config = config;
        init();
    }

    public UserAttributeMappingDialog(Dialog owner, IdentityMapping mapping, IdentityProviderConfig config) throws HeadlessException {
        super(owner, resources.getString("dialog.title"), true);
        this.mapping = mapping;
        this.config = config;
        init();
    }

    private void init() {
        Utilities.setEscKeyStrokeDisposes(this);

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

        builtInAttributeRadioButton.addActionListener(radioListener);
        customAttributeRadioButton.addActionListener(radioListener);

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

        enableRadioState();
        
        add(mainPanel);
    }

    public boolean isOk() {
        return ok;
    }
}
