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
    private JTextField nestingDepthField;
    private JCheckBox textLengthCheckBox;
    private JCheckBox attrLengthCheckBox;
    private JCheckBox nestingDepthCheckBox;

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
        getRootPane().setDefaultButton(okButton);

        Utilities.constrainTextFieldToLongRange(textLengthField, 0, Long.MAX_VALUE);
        Utilities.constrainTextFieldToLongRange(attrLengthField, 0, Long.MAX_VALUE);
        Utilities.constrainTextFieldToIntegerRange(nestingDepthField,
                                                   OversizedTextAssertion.MIN_NESTING_LIMIT,
                                                   OversizedTextAssertion.MAX_NESTING_LIMIT);
        modelToView();
    }

    private void modelToView() {
        textLengthField.setText(Long.toString(assertion.getMaxTextChars()));
        textLengthCheckBox.setSelected(assertion.isLimitTextChars());
        attrLengthField.setText(Long.toString(assertion.getMaxAttrChars()));
        attrLengthCheckBox.setSelected(assertion.isLimitAttrChars());
        nestingDepthField.setText(Integer.toString(assertion.getMaxNestingDepth()));
        nestingDepthCheckBox.setSelected(assertion.isLimitNestingDepth());
    }

    private void doCancel() {
        confirmed = false;
        modified = false;
        setVisible(false);
    }

    private void doSave() {
        String err = null;

        long textLen = assertion.getMaxTextChars();
        final boolean limitTextLen = textLengthCheckBox.isSelected();
        try {
            textLen = Long.parseLong(textLengthField.getText());
        } catch (NumberFormatException nfe) {
            if (limitTextLen)
                err = "Maximum text characters must be a number.";
        }

        long attrLen = assertion.getMaxAttrChars();
        final boolean limitAttrLen = attrLengthCheckBox.isSelected();
        try {
            attrLen = Long.parseLong(attrLengthField.getText());
        } catch (NumberFormatException nfe) {
            if (limitAttrLen)
                err = "Maximum attribute characters must be a number.";
        }

        int nestDepth = assertion.getMaxNestingDepth();
        final boolean limitNestDepth = nestingDepthCheckBox.isSelected();
        try {
            nestDepth = Integer.parseInt(nestingDepthField.getText());
        } catch (NumberFormatException nfe) {
            if (limitNestDepth)
                err = "Maximum nesting depth must be a number.";
        }

        if (limitAttrLen && attrLen < 0) err = "Limits must be nonnegative.";
        if (limitTextLen && textLen < 0) err = "Limits must be nonnegative.";
        if (limitNestDepth && nestDepth < 0) err = "Limits must be nonnegative.";

        if (err != null) {
            JOptionPane.showMessageDialog(this, err, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        assertion.setMaxAttrChars(attrLen);
        assertion.setMaxTextChars(textLen);
        assertion.setMaxNestingDepth(nestDepth);
        assertion.setLimitTextChars(limitTextLen);
        assertion.setLimitAttrChars(limitAttrLen);
        assertion.setLimitNestingDepth(limitNestDepth);
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
