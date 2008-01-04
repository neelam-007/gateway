/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.common.gui.widgets.OkCancelDialog;
import com.l7tech.common.gui.widgets.ValidatedPanel;
import com.l7tech.common.gui.util.RunOnChangeListener;
import com.l7tech.common.gui.util.DocumentSizeFilter;
import com.l7tech.common.policy.Policy;
import com.l7tech.common.policy.PolicyType;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * @author alex
 */
public class PolicyPropertiesPanel extends ValidatedPanel {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.PolicyPropertiesPanel");

    private JPanel mainPanel;
    private JTextField nameField;
    private JCheckBox soapCheckbox;
    private JComboBox typeCombo;
    // TODO include a policy panel

    private final Policy policy;
    private final boolean canUpdate;
    
    private RunOnChangeListener syntaxListener = new RunOnChangeListener(new Runnable() {
        public void run() {
            checkSyntax();
        }
    });

    public static OkCancelDialog<Policy> makeDialog(Frame owner, Policy policy, boolean canUpdate) {
        return new OkCancelDialog<Policy>(owner, resources.getString("dialog.title"), true, new PolicyPropertiesPanel(policy, canUpdate));
    }

    public PolicyPropertiesPanel(Policy policy, boolean canUpdate) {
        super("policy");
        this.policy = policy;
        this.canUpdate = canUpdate;
        init();
    }

    protected Object getModel() {
        return policy;
    }

    protected void initComponents() {
        java.util.List<PolicyType> types = new ArrayList<PolicyType>();
        for (PolicyType type : PolicyType.values()) {
            if (type.isShownInGui()) types.add(type);
        }

        typeCombo.setModel(new DefaultComboBoxModel(types.toArray(new PolicyType[types.size()])));

        // The max length of a policy name is 255. 
        ((AbstractDocument)nameField.getDocument()).setDocumentFilter(new DocumentSizeFilter(255));

        nameField.setEditable(nameField.isEditable() && canUpdate);
        soapCheckbox.setEnabled(soapCheckbox.isEnabled() && canUpdate);
        typeCombo.setEnabled(typeCombo.isEnabled() && canUpdate);

        nameField.setText(policy.getName());
        soapCheckbox.setSelected(policy.isSoap());
        typeCombo.setSelectedItem(policy.getType());

        soapCheckbox.addChangeListener(syntaxListener);
        typeCombo.addItemListener(syntaxListener);
        nameField.getDocument().addDocumentListener(syntaxListener);

        add(mainPanel, BorderLayout.CENTER);
    }

    @Override
    protected String getSyntaxError(Object model) {
        if (nameField.getText().trim().length() > 0) return null;
        PolicyType type = (PolicyType) typeCombo.getSelectedItem();
        if (type == null) return resources.getString("typeRequiredError");
        if (type.getSupertype() == PolicyType.Supertype.FRAGMENT) return resources.getString("nameRequiredError");
        return null;
    }

    public void focusFirstComponent() {
        nameField.requestFocus();
    }

    protected void doUpdateModel() {
        policy.setName(nameField.getText().trim());
        policy.setSoap(soapCheckbox.isSelected());
        policy.setType((PolicyType)typeCombo.getSelectedItem());
    }
}
