/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.SamlBrowserArtifact;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author alex
 */
public class SamlBrowserArtifactPropertiesDialog extends JDialog {
    private SamlBrowserArtifact samlBrowserArtifactAssertion;
    private boolean assertionChanged = false;

    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;
    private JTextField ssoEndpointUrlField;
    private JTextField artifactQueryParamField;

    public SamlBrowserArtifact getAssertion() {
        return samlBrowserArtifactAssertion;
    }

    public SamlBrowserArtifactPropertiesDialog(SamlBrowserArtifact assertion, Frame owner, boolean modal) throws HeadlessException {
        super(owner, "Configure SAML Browser/Artifact", modal);
        this.samlBrowserArtifactAssertion = assertion;
        ssoEndpointUrlField.setText(assertion.getSsoEndpointUrl());
        artifactQueryParamField.setText(assertion.getArtifactQueryParameter());

        getContentPane().add(mainPanel);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                samlBrowserArtifactAssertion.setSsoEndpointUrl(ssoEndpointUrlField.getText());
                samlBrowserArtifactAssertion.setArtifactQueryParameter(artifactQueryParamField.getText());
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

        updateButtons();
    }

    public boolean isAssertionChanged() {
        return assertionChanged;
    }

    private void updateButtons() {
        boolean ok = false;
        String url = ssoEndpointUrlField.getText();
        ok = url != null && url.length() > 0;
        if (ok) try {
            new URL(url);
        } catch (MalformedURLException e) {
            ok = false;
        }
        String queryParam = artifactQueryParamField.getText();
        ok = ok && queryParam != null && queryParam.length() > 0;

        okButton.setEnabled(ok);
    }
}
