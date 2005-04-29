package com.l7tech.console.panels;

import com.l7tech.policy.assertion.AuditAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Dialog for editing the properties of an {@link AuditAssertion}.
 * 
 * @author alex &lt;acruise@layer7-tech.com&gt;
 */
public class AuditAssertionDialog extends JDialog {
    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton okButton;
    private JCheckBox saveResponseCheckbox;
    private JCheckBox saveRequestCheckbox;
    private JComboBox levelCombo;
    private JLabel thresholdLabel;

    private final AuditAssertion assertion;
    private boolean modified;

    private static final String THRESH_PREFIX = "(NOTE: the server is currently not recording below level ";
    private static final String THRESH_SUFFIX = ")";

    public AuditAssertionDialog(Frame owner, AuditAssertion ass, String serverThreshold) throws HeadlessException {
        super(owner, "Audit Assertion Properties", true);
        this.assertion = ass;

        levelCombo.setModel(new DefaultComboBoxModel(AuditAssertion.ALLOWED_LEVELS));
        levelCombo.setSelectedItem(ass.getLevel());
        saveRequestCheckbox.setSelected(ass.isSaveRequest());
        saveResponseCheckbox.setSelected(ass.isSaveResponse());
        thresholdLabel.setText(THRESH_PREFIX + serverThreshold + THRESH_SUFFIX);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                assertion.setLevel((String) levelCombo.getSelectedItem());
                assertion.setSaveRequest(saveRequestCheckbox.isSelected());
                assertion.setSaveResponse(saveResponseCheckbox.isSelected());
                modified = true;
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

    public AuditAssertion getAssertion() {
        return assertion;
    }
}
