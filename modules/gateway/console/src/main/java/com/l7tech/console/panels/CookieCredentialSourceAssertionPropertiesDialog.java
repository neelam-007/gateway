package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Simple edit dialog for cookie credential source bean.
 */
public class CookieCredentialSourceAssertionPropertiesDialog extends LegacyAssertionPropertyDialog {
    private JPanel rootPanel;
    private JTextField cookieNameField;
    private JPanel variablePrefixPanel;
    private TargetVariablePanel targetVariable;
    private JButton okButton;
    private JButton cancelButton;
    private boolean confirmed = false;

    public CookieCredentialSourceAssertionPropertiesDialog(Frame owner, boolean modal, CookieCredentialSourceAssertion assertion, boolean readOnly) {
        super(owner, assertion, modal);
        setContentPane(rootPanel);
        targetVariable = new TargetVariablePanel();
        variablePrefixPanel.setLayout(new BorderLayout());
        variablePrefixPanel.add(targetVariable, BorderLayout.CENTER);
        setData(assertion);

        Utilities.equalizeButtonSizes(new JButton[]{okButton, cancelButton});

        okButton.setEnabled(!readOnly);
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!isDataValid())
                    return;
                confirmed = true;
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
    }

    /**
     * @return true if view is ready to be copied into a model bean.  False if it is invalid.
     *         When this returns false an error message has already been displayed.
     */
    private boolean isDataValid() {
        boolean valid = true;

        if (cookieNameField.getText() == null || cookieNameField.getText().length() < 1) {
            JOptionPane.showMessageDialog(this, "Please enter a cookie name.", "Error", JOptionPane.ERROR_MESSAGE);
            valid = false;
        } else if (!targetVariable.isEntryValid()) {
            JOptionPane.showMessageDialog(this, "Please enter a valid variable prefix.", "Error", JOptionPane.ERROR_MESSAGE);
            valid = false;
        }

        return valid;
    }

    /**
     * @return true if Ok button was pressed.
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Configure the dialog widgets to view the data from the specified assertion bean.
     */
    public void setData(CookieCredentialSourceAssertion data) {
        cookieNameField.setText(data.getCookieName());
        targetVariable.setAssertion(data, getPreviousAssertion());
        targetVariable.setVariable(data.getVariablePrefix());
    }

    /**
     * Configure the specified assertion bean with the data from the current dialog widgets.
     */
    public void getData(CookieCredentialSourceAssertion data) {
        data.setCookieName(cookieNameField.getText().trim());
        data.setVariablePrefix(targetVariable.getVariable().trim());
    }

    /**
     * @return true if the content of the dialog widgets differs from the content of the specified bean.
     */
    public boolean isModified(CookieCredentialSourceAssertion data) {
        if (cookieNameField.getText() != null ? !cookieNameField.getText().equals(data.getCookieName()) : data.getCookieName() != null) {
            return true;
        }
        if (targetVariable.getVariable() != null ? !targetVariable.getVariable().equals(data.getVariablePrefix()) : data.getVariablePrefix() != null) {
            return true;
        }
        return false;
    }
}
