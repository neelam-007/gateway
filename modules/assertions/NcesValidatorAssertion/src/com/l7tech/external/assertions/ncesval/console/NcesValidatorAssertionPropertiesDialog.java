/**
 * Copyright (C) 2007-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.ncesval.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.external.assertions.ncesval.NcesValidatorAssertion;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.common.gui.util.RunOnChangeListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/** @author alex */
public class NcesValidatorAssertionPropertiesDialog extends AssertionPropertiesEditorSupport<NcesValidatorAssertion> {
    private volatile boolean ok = false;

    private JCheckBox samlCheckbox;
    private JTextField otherMessageVariableTextfield;
    private JButton okButton;
    private JButton cancelButton;
    private JRadioButton requestRadioButton;
    private JRadioButton responseRadioButton;
    private JRadioButton otherRadioButton;
    private JPanel mainPanel;

    private final RunOnChangeListener enableDisableListener = new RunOnChangeListener(new Runnable() {
        public void run() {
            enableDisable();
        }
    });

    public NcesValidatorAssertionPropertiesDialog(Dialog owner) {
        super(owner, "NCES Validator Properties", true);
        initialize();
    }

    public NcesValidatorAssertionPropertiesDialog(Frame owner) {
        super(owner, "NCES Validator Properties", true);
        initialize();
    }

    private void initialize() {
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok = false;
                dispose();
            }
        });

        requestRadioButton.addActionListener(enableDisableListener);
        responseRadioButton.addActionListener(enableDisableListener);
        otherRadioButton.addActionListener(enableDisableListener);
        otherMessageVariableTextfield.getDocument().addDocumentListener(enableDisableListener);

        enableDisable();

        add(mainPanel);
    }

    private void enableDisable() {
        otherMessageVariableTextfield.setEnabled(otherRadioButton.isSelected());
        okButton.setEnabled(!otherRadioButton.isSelected() || otherMessageVariableTextfield.getText().length() > 0);
    }

    public boolean isConfirmed() {
        return ok;
    }

    public void setData(NcesValidatorAssertion assertion) {
        switch(assertion.getTarget()) {
            case REQUEST:
                requestRadioButton.setSelected(true);
                break;
            case RESPONSE:
                responseRadioButton.setSelected(true);
                break;
            case OTHER:
                otherRadioButton.setSelected(true);
                otherMessageVariableTextfield.setText(assertion.getOtherMessageVariableName());
                break;
            default:
                throw new IllegalStateException();
        }
        samlCheckbox.setSelected(assertion.isSamlRequired());
    }

    public NcesValidatorAssertion getData(NcesValidatorAssertion assertion) {
        final TargetMessageType target;
        final String messageName;
        if (requestRadioButton.isSelected()) {
            target = TargetMessageType.REQUEST;
            messageName = null;
        } else if (responseRadioButton.isSelected()) {
            target = TargetMessageType.RESPONSE;
            messageName = null;
        } else if (otherRadioButton.isSelected()) {
            target = TargetMessageType.OTHER;
            messageName = otherMessageVariableTextfield.getText();
        } else {
            throw new IllegalStateException();
        }

        assertion.setTarget(target);
        assertion.setOtherMessageVariableName(messageName);
        assertion.setSamlRequired(samlCheckbox.isSelected());

        return assertion;
    }
}
