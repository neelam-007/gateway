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
public class AuditAssertionDialog extends LegacyAssertionPropertyDialog {
    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton okButton;
    private JComboBox levelCombo;
    private JLabel thresholdLabel;
    private JRadioButton saveRequestAlwaysButton;
    private JRadioButton saveRequestNeverButton;
    private JRadioButton saveRequestDefaultButton;
    private JRadioButton saveResponseAlwaysButton;
    private JRadioButton saveResponseNeverButton;
    private JRadioButton saveResponseDefaultButton;

    private final AuditAssertion assertion;
    private boolean modified;

    private static final String THRESH_PREFIX = "(NOTE: the server is currently not recording below level ";
    private static final String THRESH_SUFFIX = ")";
    private Level currentServerThreshold;

    public AuditAssertionDialog(Frame owner, AuditAssertion ass, String serverThreshold, boolean readOnly) throws HeadlessException {
        super(owner, ass, true);
        this.assertion = ass;
        currentServerThreshold = Level.parse(serverThreshold);

        String[] levels = {
            Level.INFO.getName(),
            Level.WARNING.getName(),
        };

        levelCombo.setModel(new DefaultComboBoxModel(levels));
        levelCombo.setSelectedItem(ass.getLevel());
        
        configSaveButtons(saveRequestAlwaysButton, saveRequestNeverButton, saveRequestDefaultButton, ass.isChangeSaveRequest(), ass.isSaveRequest());
        configSaveButtons(saveResponseAlwaysButton, saveResponseNeverButton, saveResponseDefaultButton, ass.isChangeSaveResponse(), ass.isSaveResponse());

        //get the current server level
        updateThresholdLabel();

        okButton.setEnabled(!readOnly);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                assertion.setLevel((String) levelCombo.getSelectedItem());
                assertion.setChangeSaveRequest(!saveRequestDefaultButton.isSelected());
                assertion.setSaveRequest(saveRequestAlwaysButton.isSelected());
                assertion.setChangeSaveResponse(!saveResponseDefaultButton.isSelected());
                assertion.setSaveResponse(saveResponseAlwaysButton.isSelected());
                modified = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                modified = false;
                dispose();
            }
        });

        levelCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateThresholdLabel();
            }

        });

        getContentPane().add(mainPanel);
    }
    
    private void configSaveButtons(AbstractButton alwaysButton, AbstractButton neverButton, AbstractButton defaultButton, boolean changeDefault, boolean newValue) {
        defaultButton.setSelected(!changeDefault);
        alwaysButton.setSelected(changeDefault && newValue);
        neverButton.setSelected(changeDefault && !newValue);
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
