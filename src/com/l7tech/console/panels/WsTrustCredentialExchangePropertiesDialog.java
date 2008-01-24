/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.common.xml.WsTrustRequestType;

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
public class WsTrustCredentialExchangePropertiesDialog extends JDialog {
    private WsTrustCredentialExchange wsTrustAssertion;
    private boolean assertionChanged = false;
    private boolean readOnly = false;

    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;
    private JComboBox requestTypeCombo;
    private JTextField appliesToField;
    private JTextField tokenServiceUrlField;
    private JTextField issuerField;

    public WsTrustCredentialExchange getWsTrustAssertion() {
        return wsTrustAssertion;
    }

    public WsTrustCredentialExchangePropertiesDialog(WsTrustCredentialExchange assertion, Frame owner, boolean modal, boolean readOnly) throws HeadlessException {
        super(owner, "Configure WS-Trust Credential Exchange", modal);
        this.wsTrustAssertion = assertion;
        this.readOnly = readOnly;
        requestTypeCombo.setModel(new DefaultComboBoxModel(new WsTrustRequestType[] {WsTrustRequestType.ISSUE, WsTrustRequestType.VALIDATE}));

        WsTrustRequestType type = assertion.getRequestType();
        if (type == null) {
            requestTypeCombo.setSelectedIndex(0);
        } else {
            requestTypeCombo.setSelectedItem(type);
        }
        tokenServiceUrlField.setText(assertion.getTokenServiceUrl());
        appliesToField.setText(assertion.getAppliesTo());
        issuerField.setText(assertion.getIssuer());

        getContentPane().add(mainPanel);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                wsTrustAssertion.setAppliesTo(appliesToField.getText());
                wsTrustAssertion.setIssuer(issuerField.getText());
                wsTrustAssertion.setTokenServiceUrl(tokenServiceUrlField.getText());
                wsTrustAssertion.setRequestType((WsTrustRequestType)requestTypeCombo.getSelectedItem());
                assertionChanged = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                wsTrustAssertion = null;
                dispose();
            }
        });

        DocumentListener updateListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateButtons(); }
            public void insertUpdate(DocumentEvent e) { updateButtons(); }
            public void removeUpdate(DocumentEvent e) { updateButtons(); }
        };

        tokenServiceUrlField.getDocument().addDocumentListener(updateListener);
        appliesToField.getDocument().addDocumentListener(updateListener);
        issuerField.getDocument().addDocumentListener(updateListener);

        updateButtons();
    }

    public boolean isAssertionChanged() {
        return assertionChanged;
    }

    private void updateButtons() {
        boolean ok = false;
        String url = tokenServiceUrlField.getText();
        ok = url != null && url.length() > 0;
        try {
            new URL(url);
            ok = !readOnly;
        } catch (MalformedURLException e) {
            ok = false;
        }
        okButton.setEnabled(ok);
    }
}
