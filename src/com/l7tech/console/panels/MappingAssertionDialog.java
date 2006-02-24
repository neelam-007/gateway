package com.l7tech.console.panels;

import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.assertion.identity.MappingAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * Dialog for editing the properties of a {@link com.l7tech.policy.assertion.identity.MappingAssertion}.
 *
 * @author alex &lt;acruise@layer7-tech.com&gt;
 */
public class MappingAssertionDialog extends JDialog {
    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton okButton;

    private final MappingAssertion assertion;
    private boolean modified;

    private JTextField varNameField;
    private JComboBox tokenTypeCombo;
    private JComboBox providerCombo;
    private JTextField searchAttrField;
    private JCheckBox userCheckbox;
    private JCheckBox groupCheckbox;
    private JTextField retrieveAttrField;

    public MappingAssertionDialog(Frame owner, MappingAssertion ass, boolean modal) throws HeadlessException {
        super(owner, "Identity Mapping Assertion Properties", modal);
        this.assertion = ass;

        EntityHeader[] providers;
        ArrayList ehs = new ArrayList();
        EntityHeader selected = null;
        try {
            providers = Registry.getDefault().getIdentityAdmin().findAllIdentityProviderConfig();
            for (int i = 0; i < providers.length; i++) {
                EntityHeader eh = providers[i];
                ehs.add(eh);
                if (eh.getOid() == ass.getIdentityProviderOid()) {
                    selected = eh;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Couldn't get list of identity providers");
        }
        providerCombo.setModel(new DefaultComboBoxModel(ehs.toArray(new EntityHeader[0])));
        providerCombo.setSelectedItem(selected);

        tokenTypeCombo.setModel(new DefaultComboBoxModel(new SecurityTokenType[]{SecurityTokenType.WSS_KERBEROS_BST}));
        varNameField.setText(ass.getVariableName());
        searchAttrField.setText(ass.getSearchAttributeName());
        retrieveAttrField.setText(ass.getRetrieveAttributeName());
        userCheckbox.setSelected(ass.isValidForUsers());
        groupCheckbox.setSelected(ass.isValidForGroups());

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                modified = true;
                assertion.setVariableName(varNameField.getText());
                assertion.setSearchAttributeName(searchAttrField.getText());
                assertion.setRetrieveAttributeName(retrieveAttrField.getText());
                assertion.setIdentityProviderOid(((EntityHeader)providerCombo.getSelectedItem()).getOid());
                assertion.setTokenType((SecurityTokenType)tokenTypeCombo.getSelectedItem());
                assertion.setValidForUsers(userCheckbox.isSelected());
                assertion.setValidForGroups(groupCheckbox.isSelected());
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                modified = false;
                dispose();
            }
        });

        getContentPane().add(mainPanel);
    }

    public boolean isModified() {
        return modified;
    }

    public MappingAssertion getAssertion() {
        return assertion;
    }
}
