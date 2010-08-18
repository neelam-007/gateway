/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.external.assertions.saml2attributequery.console;

import com.l7tech.gui.util.Utilities;
import com.l7tech.external.assertions.saml2attributequery.ValidateSignatureAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Simple edit dialog for cookie credential source bean.
 */
public class ValidateSignatureAssertionPropertiesDialog extends JDialog {
    private JPanel rootPanel;
    private JTextField variableNameField;
    private JButton okButton;
    private JButton cancelButton;
    private ValidateSignatureAssertion assertion;
    private boolean confirmed = false;

    public ValidateSignatureAssertionPropertiesDialog(Frame owner, boolean modal, ValidateSignatureAssertion assertion, boolean readOnly) {
        super(owner, "Validate Digital Signature", modal);
        setContentPane(rootPanel);

        this.assertion = assertion;
        setData(assertion);

        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });

        okButton.setEnabled( !readOnly );
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!isDataValid())
                    return;
                confirmed = true;

                getData(ValidateSignatureAssertionPropertiesDialog.this.assertion);

                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        getRootPane().setDefaultButton(okButton);
        Utilities.runActionOnEscapeKey(getRootPane(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) { cancelButton.doClick(); }
        });
    }

    /**
     * @return true if view is ready to be copied into a model bean.  False if it is invalid.
     *         When this returns false an error message has already been displayed.
     */
    private boolean isDataValid() {
        boolean valid = true;

        if (variableNameField.getText() == null || variableNameField.getText().length() < 1) {
            JOptionPane.showMessageDialog(this, "Please enter a variable name.", "Error", JOptionPane.ERROR_MESSAGE);
            valid = false;
        }

        return valid;
    }

    /** @return true if Ok button was pressed. */
    public boolean isConfirmed() {
        return confirmed;
    }

    /** Configure the dialog widgets to view the data from the specified assertion bean. */
    public void setData(ValidateSignatureAssertion data) {
        variableNameField.setText(data.getVariableName());
    }

    /** Configure the specified assertion bean with the data from the current dialog widgets. */
    public void getData(ValidateSignatureAssertion data) {
        data.setVariableName(variableNameField.getText());
    }

    /** @return true if the content of the dialog widgets differs from the content of the specified bean. */
    public boolean isModified(ValidateSignatureAssertion data) {
        if (variableNameField.getText() != null ? !variableNameField.getText().equals(data.getVariableName()) : data.getVariableName() != null)
            return true;
        return false;
    }
}