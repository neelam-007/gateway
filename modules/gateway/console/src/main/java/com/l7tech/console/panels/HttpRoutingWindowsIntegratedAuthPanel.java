/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.HttpRoutingAssertion;

import javax.swing.*;
import java.util.ResourceBundle;

/**
 * @author alex
 */
public class HttpRoutingWindowsIntegratedAuthPanel extends JPanel {
    private static final ResourceBundle resources =
        ResourceBundle.getBundle("com.l7tech.console.resources.HttpRoutingAssertionDialog");

    private JPanel mainPanel;

    private JTextField krbAccountNameField;
    private JPasswordField krbPasswordField;

    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JRadioButton useConfiguredCredentialsRadioButton;
    private JRadioButton useDelegatedCredentialsRadioButton;
    private JRadioButton useGatewayKeytabRadioButton;
    private JCheckBox showPasswordCheckBox;

    private final HttpRoutingAssertion assertion;

    public HttpRoutingWindowsIntegratedAuthPanel(HttpRoutingAssertion assertion) {
        super();
        this.assertion = assertion;

        krbAccountNameField.setText(assertion.getKrbConfiguredAccount());
        krbPasswordField.setText(assertion.getKrbConfiguredPassword());
        Utilities.configureShowPasswordButton(showPasswordCheckBox, krbPasswordField);

        usernameLabel.setLabelFor(krbAccountNameField);
        passwordLabel.setLabelFor(krbPasswordField);

        if (assertion.getKrbConfiguredAccount() != null) {
            useDelegatedCredentialsRadioButton.setSelected(false);
            useGatewayKeytabRadioButton.setSelected(false);
            useConfiguredCredentialsRadioButton.setSelected(true);

        } else if (assertion.isKrbUseGatewayKeytab()) {
            useDelegatedCredentialsRadioButton.setSelected(false);
            useGatewayKeytabRadioButton.setSelected(true);
            useConfiguredCredentialsRadioButton.setSelected(false);

        } else {
            useDelegatedCredentialsRadioButton.setSelected(true);
            useGatewayKeytabRadioButton.setSelected(false);
            useConfiguredCredentialsRadioButton.setSelected(false);
        }

        ButtonGroup krbDelegationGroup = new ButtonGroup();
        krbDelegationGroup.add(this.useConfiguredCredentialsRadioButton);
        krbDelegationGroup.add(this.useDelegatedCredentialsRadioButton);
        krbDelegationGroup.add(this.useGatewayKeytabRadioButton);

        add(mainPanel);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        usernameLabel.setEnabled(enabled);
        passwordLabel.setEnabled(enabled);

        krbAccountNameField.setEnabled(enabled);
        krbPasswordField.setEnabled(enabled);

        useConfiguredCredentialsRadioButton.setEnabled(enabled);
        useDelegatedCredentialsRadioButton.setEnabled(enabled);
        useGatewayKeytabRadioButton.setEnabled(enabled);
    }

    public void updateModel() {

        if (useConfiguredCredentialsRadioButton.isSelected()) {
            assertion.setKrbDelegatedAuthentication(false);
            assertion.setKrbUseGatewayKeytab(false);
            assertion.setKrbConfiguredAccount(nullIfEmpty(krbAccountNameField.getText()));
            final char[] pass = krbPasswordField.getPassword();
            assertion.setKrbConfiguredPassword(pass == null || pass.length == 0 ? null : new String(pass));

        } else {
            assertion.setKrbUseGatewayKeytab(useGatewayKeytabRadioButton.isSelected());
            assertion.setKrbDelegatedAuthentication(useDelegatedCredentialsRadioButton.isSelected());
            assertion.setKrbConfiguredAccount(null);
            assertion.setKrbConfiguredPassword(null);
        }
    }

    String getError() {
        String login = nullIfEmpty(krbAccountNameField.getText());
        final char[] p = krbPasswordField.getPassword();
        String pass = p == null || p.length == 0 ? null : new String(p);

        // need to change these messages
        if (login == null && pass != null) {
            return resources.getString("httpAuth.usernameRequiredError");
        }

        if (login != null && pass == null) {
            return resources.getString("httpAuth.passwordRequiredError");
        }

        return null;
    }

    private String nullIfEmpty(String s) {
        if (s == null || s.length() == 0) return null;
        return s;
    }


}