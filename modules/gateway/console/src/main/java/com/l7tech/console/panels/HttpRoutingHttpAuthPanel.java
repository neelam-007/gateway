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
public class HttpRoutingHttpAuthPanel extends JPanel {
    private static final ResourceBundle resources =
        ResourceBundle.getBundle("com.l7tech.console.resources.HttpRoutingAssertionDialog");

    private JPanel mainPanel;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField ntlmDomainField;
    private JTextField ntlmHostField;

    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JLabel ntlmDomainLabel;
    private JLabel ntlmHostLabel;
    private JCheckBox showPasswordCheckBox;

    private final HttpRoutingAssertion assertion;

    public HttpRoutingHttpAuthPanel(HttpRoutingAssertion assertion) {
        super();
        this.assertion = assertion;

        usernameField.setText(assertion.getLogin());
        passwordField.setText(assertion.getPassword());
        ntlmDomainField.setText(assertion.getRealm());
        ntlmHostField.setText(assertion.getNtlmHost());
        Utilities.configureShowPasswordButton(showPasswordCheckBox, passwordField);

        usernameLabel.setLabelFor(usernameField);
        passwordLabel.setLabelFor(passwordField);
        ntlmDomainLabel.setLabelFor(ntlmDomainField);
        ntlmHostLabel.setLabelFor(ntlmHostField);

        add(mainPanel);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        usernameLabel.setEnabled(enabled);
        passwordLabel.setEnabled(enabled);
        ntlmDomainLabel.setEnabled(enabled);

        usernameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        ntlmDomainField.setEnabled(enabled);
    }

    public void updateModel() {
        assertion.setLogin(nullIfEmpty(usernameField.getText()));
        final char[] pass = passwordField.getPassword();
        assertion.setPassword(pass == null || pass.length == 0 ? null : new String(pass));
        assertion.setRealm(nullIfEmpty(ntlmDomainField.getText()));
        assertion.setNtlmHost(nullIfEmpty(ntlmHostField.getText()));
    }

    String getError() {
        String login = nullIfEmpty(usernameField.getText());
        final char[] p = passwordField.getPassword();
        String pass = p == null || p.length == 0 ? null : new String(p);
        String domain = nullIfEmpty(ntlmDomainField.getText());

        if (login == null && (pass != null || domain != null)) {
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
