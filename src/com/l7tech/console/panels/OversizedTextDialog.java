/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.action.Actions;
import com.l7tech.policy.assertion.OversizedTextAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog for configuring OversizedTexctAssertion.
 */
public class OversizedTextDialog extends JDialog {
    private JPanel mainPanel;

    private final OversizedTextAssertion assertion;
    private JButton okButton;
    private JButton cancelButton;
    private boolean modified;
    private boolean confirmed = false;
    private JTextField textLengthField;
    private JTextField attrLengthField;

    public boolean wasConfirmed() {
        return confirmed;
    }

    public OversizedTextDialog(Frame owner, OversizedTextAssertion assertion, boolean modal) throws HeadlessException {
        super(owner, "Configure Oversized Element Protection", modal);
        this.assertion = assertion;
        doInit();
    }

    private void doInit() {
        getContentPane().add(mainPanel);

        Utilities.equalizeButtonSizes(new AbstractButton[]{okButton, cancelButton});

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSave();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doCancel();
            }
        });

        Actions.setEscKeyStrokeDisposes(this);

        Utilities.constrainTextFieldToLongRange(textLengthField, 0, Long.MAX_VALUE);
        Utilities.constrainTextFieldToLongRange(attrLengthField, 0, Long.MAX_VALUE);
        modelToView();
    }

    private void modelToView() {
        textLengthField.setText(Long.toString(assertion.getMaxTextChars()));
        attrLengthField.setText(Long.toString(assertion.getMaxAttrChars()));
    }

    private void doCancel() {
        confirmed = false;
        modified = false;
        setVisible(false);
    }

    private void doSave() {
        String err = null;

        long textLen = assertion.getMaxTextChars();
        try {
            textLen = Long.parseLong(textLengthField.getText());
        } catch (NumberFormatException nfe) {
            err = "Maximum text characters must be a number.";
        }

        long attrLen = assertion.getMaxAttrChars();
        try {
            attrLen = Long.parseLong(attrLengthField.getText());
        } catch (NumberFormatException nfe) {
            err = "Maximum attribute characters must be a number.";
        }

        if (attrLen < 0 || textLen < 0) err = "Limit must be nonnegative.";

        if (err != null) {
            JOptionPane.showMessageDialog(this, err, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        assertion.setMaxAttrChars(attrLen);
        assertion.setMaxTextChars(textLen);
        confirmed = true;
        modified = true;
        setVisible(false);
    }

    public boolean isModified() {
        return modified;
    }

    public OversizedTextAssertion getAssertion() {
        return assertion;
    }
}
