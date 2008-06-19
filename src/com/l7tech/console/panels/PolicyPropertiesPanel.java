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
import com.l7tech.console.util.Registry;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.service.ServiceTemplate;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.util.*;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * @author alex
 */
public class PolicyPropertiesPanel extends ValidatedPanel {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.PolicyPropertiesPanel");

    private JPanel mainPanel;
    private JTextField nameField;
    private JCheckBox soapCheckbox;
    private JComboBox typeCombo;
    private JComboBox tagCombo;
    private JLabel typeLabel;
    private JLabel tagLabel;
    // TODO include a policy panel

    private final Policy policy;
    private final boolean canUpdate;
    private Map<String, String> policyTags;
    
    private RunOnChangeListener syntaxListener = new RunOnChangeListener(new Runnable() {
        public void run() {
            checkSyntax();
        }
    });
    private boolean hasInternalServiceTags;

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

        policyTags = new LinkedHashMap<String, String>();

        ServiceAdmin svcManager = Registry.getDefault().getServiceManager();
        Set<ServiceTemplate> templates = svcManager.findAllTemplates();
        for (ServiceTemplate template : templates) {
            Map<String, String> templateTags = template.getPolicyTags();
            if (templateTags != null) {
                policyTags.putAll(templateTags);
            }
        }

        typeCombo.setModel(new DefaultComboBoxModel(types.toArray(new PolicyType[types.size()])));

        List<String> tagList = new ArrayList<String>();
        tagList.addAll(policyTags.keySet());
        tagCombo.setModel(new DefaultComboBoxModel(tagList.toArray(new String[policyTags.size()])));

        if (policy.getInternalTag() != null)
            tagCombo.setSelectedItem(policy.getInternalTag());
        
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
        tagCombo.addItemListener(syntaxListener);
        nameField.getDocument().addDocumentListener(syntaxListener);

        typeCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    enableDisable();
                }
            }
        });
        enableTagChooser();
        showHideComponents();
        add(mainPanel, BorderLayout.CENTER);
    }

    private void enableDisable() {
        enableTagChooser();
    }

    private void showHideComponents() {
        hasInternalServiceTags = !policyTags.isEmpty();
        typeCombo.setVisible(hasInternalServiceTags);
        typeLabel.setVisible(hasInternalServiceTags);

        tagCombo.setVisible(hasInternalServiceTags);
        tagLabel.setVisible(hasInternalServiceTags);
    }

    private void enableTagChooser() {
        PolicyType policyType = (PolicyType) typeCombo.getSelectedItem();
        tagCombo.setEnabled(policyType == PolicyType.INTERNAL && !policyTags.isEmpty());
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

        if (policy.getType() == PolicyType.INTERNAL) {
            String tag = (String) tagCombo.getSelectedItem();
            policy.setInternalTag(tag);
            if (policyTags.get(tag) != null) {
                if (StringUtils.isEmpty(policy.getXml())) //only update the policy the tag specific policy if there aren't any policy contents already
                    policy.setXml(policyTags.get(tag));
            }
        }
    }
}
