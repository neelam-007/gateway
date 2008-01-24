/*
 * Copyright (C) 2005-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.policy.assertion.AuditAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;

/**
 * Dialog for editing the properties of an {@link AuditAssertion}.
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
    private Level currentServerThreshold;

    public AuditAssertionDialog(Frame owner, AuditAssertion ass, String serverThreshold, boolean readOnly) throws HeadlessException {
        super(owner, "Audit Assertion Properties", true);
        this.assertion = ass;
        currentServerThreshold = Level.parse(serverThreshold);

        String[] levels = {
            Level.INFO.getName(),
            Level.WARNING.getName(),
        };

        levelCombo.setModel(new DefaultComboBoxModel(levels));
        levelCombo.setSelectedItem(ass.getLevel());
        saveRequestCheckbox.setSelected(ass.isSaveRequest());
        saveResponseCheckbox.setSelected(ass.isSaveResponse());

        //get the current server level
        updateThresholdLabel();

        okButton.setEnabled(!readOnly);

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

        levelCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateThresholdLabel();
            }

        });

        getContentPane().add(mainPanel);
    }

    private void updateThresholdLabel() {
        String selected = (String) levelCombo.getSelectedItem();
        String defaultText = THRESH_PREFIX + currentServerThreshold.getName() + THRESH_SUFFIX;
        String text = defaultText;
        if (selected != null) {
            Level selectedLevel;
            try {
                selectedLevel = Level.parse(selected);
                if ( selectedLevel.intValue() < currentServerThreshold.intValue()) {
                    text = "<html><font color='red'>" + defaultText + "</font></html>";
                }
            } catch (IllegalArgumentException iax) {}
        }
        thresholdLabel.setText(text);
    }

    public boolean isModified() {
        return modified;
    }

    public AuditAssertion getAssertion() {
        return assertion;
    }
}
