/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.util.ValidationUtils;
import com.l7tech.policy.assertion.xmlsec.AuthenticationProperties;
import com.l7tech.policy.assertion.xmlsec.SamlBrowserArtifact;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author alex
 */
public class SamlBrowserArtifactPropertiesDialog extends JDialog {
    private SamlBrowserArtifact samlBrowserArtifactAssertion;
    private AuthenticationProperties authProperties;
    private boolean assertionChanged = false;
    private boolean readOnly = false;

    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;
    private JTextField ssoEndpointUrlField;
    private JTextField artifactQueryParamField;
    private JButton authenticationButton;
    private JTextArea authenticationSummaryTextArea;

    public SamlBrowserArtifact getAssertion() {
        return samlBrowserArtifactAssertion;
    }

    public SamlBrowserArtifactPropertiesDialog(SamlBrowserArtifact assertion, final Frame owner, boolean modal, boolean readOnly)
            throws HeadlessException
    {
        super(owner, "Configure SAML Browser/Artifact", modal);
        this.samlBrowserArtifactAssertion = assertion;
        this.readOnly = readOnly;
        this.authProperties = new AuthenticationProperties(assertion.getAuthenticationProperties());
        ssoEndpointUrlField.setText(assertion.getSsoEndpointUrl());
        artifactQueryParamField.setText(assertion.getArtifactQueryParameter());

        authenticationSummaryTextArea.setDisabledTextColor(Color.BLACK);
        authenticationSummaryTextArea.setBackground(this.getBackground());

        authenticationButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                AuthenticationProperties ap = authProperties;
                SamlBrowserAuthenticationDialog authDialog = new SamlBrowserAuthenticationDialog(ap, owner, true);
                authDialog.pack();
                Utilities.centerOnScreen(authDialog);
                DialogDisplayer.display(authDialog, new Runnable() {
                    public void run() {
                        updateAuthenticationSummary();
                    }
                });
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                samlBrowserArtifactAssertion.setSsoEndpointUrl(ssoEndpointUrlField.getText());
                samlBrowserArtifactAssertion.setArtifactQueryParameter(artifactQueryParamField.getText());
                samlBrowserArtifactAssertion.setAuthenticationProperties(authProperties);

                assertionChanged = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                samlBrowserArtifactAssertion = null;
                dispose();
            }
        });

        DocumentListener updateListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateButtons(); }
            public void insertUpdate(DocumentEvent e) { updateButtons(); }
            public void removeUpdate(DocumentEvent e) { updateButtons(); }
        };

        ssoEndpointUrlField.getDocument().addDocumentListener(updateListener);
        artifactQueryParamField.getDocument().addDocumentListener(updateListener);

        updateAuthenticationSummary();
        updateButtons();
        getContentPane().add(mainPanel);
    }

    private static class FieldInfo {
        private String name;
        private String value;

        public FieldInfo(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public FieldInfo() {
        }

        public String toString() {
            return name + " = " + value;
        }
    }

    public boolean isAssertionChanged() {
        return assertionChanged;
    }

    private void updateAuthenticationSummary() {
        AuthenticationProperties ap = authProperties;
        StringBuffer summary = new StringBuffer(200);

        if(AuthenticationProperties.METHOD_BASIC.equals(ap.getMethod())) {
            summary.append("Basic Authentication.");
        }
        else if(AuthenticationProperties.METHOD_FORM.equals(ap.getMethod())) {
            boolean parseRequired = ap.isCopyFormFields()
                                 || nullOrEmpty(ap.getFormTarget())
                                 || nullOrEmpty(ap.getUsernameFieldname());

            summary.append("Form Authentication");
            if(parseRequired) summary.append(" (HTML parse required)");
            else summary.append('.');
            summary.append("\n\n");

            summary.append("Request before submit:  \t");
            summary.append(ap.isRequestForm());
            summary.append('\n');
            summary.append("Redirect after submit:  \t");
            summary.append(ap.isRedirectAfterSubmit());
            summary.append('\n');
            summary.append("Enable cookies:         \t");
            summary.append(ap.isEnableCookies());
            summary.append('\n');
            summary.append("Form target:            \t");
            summary.append(nullOrEmpty(ap.getFormTarget()) ? "<not supplied>" : format(ap.getFormTarget(),64));
            summary.append('\n');
            summary.append("Username fieldname:     \t");
            summary.append(nullOrEmpty(ap.getUsernameFieldname()) ? "<not supplied>" : format(ap.getUsernameFieldname(),64));
            summary.append('\n');
            summary.append("Password fieldname      \t");
            summary.append(nullOrEmpty(ap.getPasswordFieldname()) ? "<not supplied>" : format(ap.getPasswordFieldname(),64));
            summary.append('\n');
            summary.append("Preserve form fields:   \t");
            summary.append(ap.isCopyFormFields());
            summary.append('\n');
            summary.append("Additional form fields: \t");
            summary.append(!ap.getAdditionalFields().isEmpty());
        }

        authenticationSummaryTextArea.setText(summary.toString());
        pack();
    }

    private void updateButtons() {
        boolean ok = false;
        String ssoUrl = ssoEndpointUrlField.getText();
        String queryParam = artifactQueryParamField.getText();

        ok = !readOnly && validUrl(ssoUrl, false)
          && queryParam != null && queryParam.length() > 0;

        okButton.setEnabled(ok);
    }

    private boolean nullOrEmpty(String text) {
        boolean result = false;

        if(text==null || text.trim().length()==0) {
            result = true;
        }

        return result;
    }

    private String format(String text, int max) {
        String result = text;

        if(result.length()>max) {
            result = result.substring(0, max-3) + "...";
        }

        return result;
    }

    private boolean validUrl(String urlText, boolean allowEmpty) {
        return ValidationUtils.isValidUrl(urlText, allowEmpty);
    }
}
