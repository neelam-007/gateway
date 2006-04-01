/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.InputValidator;
import com.l7tech.console.action.Actions;
import com.l7tech.policy.assertion.OversizedTextAssertion;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog for configuring OversizedTexctAssertion.
 */
public class OversizedTextDialog extends JDialog {
    private JPanel mainPanel;

    private final OversizedTextAssertion assertion;
    private final InputValidator validator;
    private JButton okButton;
    private JButton cancelButton;
    private boolean modified;
    private boolean confirmed = false;
    private JTextField textLengthField;
    private JTextField attrLengthField;
    private JTextField nestingDepthField;
    private JTextField payloadCountField;
    private JCheckBox textLengthCheckBox;
    private JCheckBox attrLengthCheckBox;
    private JCheckBox nestingDepthCheckBox;
    private JCheckBox payloadCheckBox;
    private JCheckBox soapEnvCheckBox;

    public boolean wasConfirmed() {
        return confirmed;
    }

    public OversizedTextDialog(Frame owner, OversizedTextAssertion assertion, boolean modal) throws HeadlessException {
        super(owner, "Configure Document Structure Threat Protection", modal);
        this.assertion = assertion;
        this.validator = new InputValidator(this, "Document Structure Threats");
        doInit();
    }

    private void doInit() {
        getContentPane().add(mainPanel);

        Utilities.equalizeButtonSizes(new AbstractButton[]{okButton, cancelButton});

        validator.attachToButton(okButton, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSave();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doCancel();
            }
        });

        final ChangeListener changeListener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateEnableState();
            }
        };

        textLengthCheckBox.addChangeListener(changeListener);
        attrLengthCheckBox.addChangeListener(changeListener);
        nestingDepthCheckBox.addChangeListener(changeListener);
        soapEnvCheckBox.addChangeListener(changeListener);
        payloadCheckBox.addChangeListener(changeListener);

        Actions.setEscKeyStrokeDisposes(this);
        getRootPane().setDefaultButton(okButton);

        Utilities.enableGrayOnDisabled(textLengthField);
        Utilities.enableGrayOnDisabled(attrLengthField);
        Utilities.enableGrayOnDisabled(nestingDepthField);
        Utilities.enableGrayOnDisabled(payloadCountField);

        validator.constrainTextFieldToNumberRange("text length", textLengthField, 0, Long.MAX_VALUE);
        validator.constrainTextFieldToNumberRange("attribute length", attrLengthField, 0, Long.MAX_VALUE);
        validator.constrainTextFieldToNumberRange("maximum payload count", payloadCountField, 0, Integer.MAX_VALUE);
        validator.constrainTextFieldToNumberRange("maximum nesting depth", nestingDepthField,
                                                  OversizedTextAssertion.MIN_NESTING_LIMIT,
                                                  OversizedTextAssertion.MAX_NESTING_LIMIT);

        modelToView();
        updateEnableState();
    }

    private void modelToView() {
        textLengthField.setText(Long.toString(assertion.getMaxTextChars()));
        textLengthCheckBox.setSelected(assertion.isLimitTextChars());
        attrLengthField.setText(Long.toString(assertion.getMaxAttrChars()));
        attrLengthCheckBox.setSelected(assertion.isLimitAttrChars());
        nestingDepthField.setText(Integer.toString(assertion.getMaxNestingDepth()));
        nestingDepthCheckBox.setSelected(assertion.isLimitNestingDepth());
        soapEnvCheckBox.setSelected(assertion.isRequireValidSoapEnvelope());
        final int mp = assertion.getMaxPayloadElements();
        payloadCheckBox.setSelected(mp > 0);
        payloadCountField.setText(mp > 0 ? String.valueOf(mp) : "");
    }

    private void updateEnableState() {
        textLengthField.setEnabled(textLengthCheckBox.isSelected());
        attrLengthField.setEnabled(attrLengthCheckBox.isSelected());
        nestingDepthField.setEnabled(nestingDepthCheckBox.isSelected());
        payloadCountField.setEnabled(payloadCheckBox.isSelected());
    }

    private void doCancel() {
        confirmed = false;
        modified = false;
        setVisible(false);
    }

    private void doSave() {
        String err = null;

        // Most of the validation in this method is redundant now -- the validator should ensure doSave()
        // is never invoked while the fields it is watching are in invalid states.

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

        int payloadCount = assertion.getMaxPayloadElements();
        final boolean limitPayload = payloadCheckBox.isSelected();
        try {
            payloadCount = limitPayload ? Integer.parseInt(payloadCountField.getText()) : 0;
        } catch (NumberFormatException nfe) {
            if (limitPayload)
                err = "Maximum payload count must be a number.";
        }

        if ((limitAttrLen && attrLen < 0) ||
                (limitTextLen && textLen < 0) ||
                (limitNestDepth && nestDepth < 0) ||
                (limitPayload && payloadCount < 0))
            err = "Limits must be nonnegative.";

        if (err != null) {
            JOptionPane.showMessageDialog(this, err, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        assertion.setMaxAttrChars(attrLen);
        assertion.setMaxTextChars(textLen);
        assertion.setMaxNestingDepth(nestDepth);
        assertion.setMaxPayloadElements(payloadCount);
        assertion.setLimitTextChars(limitTextLen);
        assertion.setLimitAttrChars(limitAttrLen);
        assertion.setLimitNestingDepth(limitNestDepth);
        assertion.setRequireValidSoapEnvelope(soapEnvCheckBox.isSelected());
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
