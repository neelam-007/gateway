/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.util.SoapConstants;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import org.jaxen.dom.DOMXPath;

import javax.swing.*;
import javax.xml.soap.SOAPConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

/**
 * @author alex
 */
public class XpathCredentialSourcePropertiesDialog extends LegacyAssertionPropertyDialog {
    private XpathCredentialSource xpathCredsAssertion;
    private boolean assertionChanged = false;
    private boolean readOnly = false;

    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;
    private JTextField loginXpathField;
    private JTextField passwordXpathField;
    private JCheckBox removeLoginCheckbox;
    private JCheckBox removePasswordCheckbox;
    private JButton namespacesButton;
    private Map<String,String> namespaces; // one map for both xpath expressions

    public XpathCredentialSource getXpathCredsAssertion() {
        return xpathCredsAssertion;
    }

    public XpathCredentialSourcePropertiesDialog(XpathCredentialSource assertion, Frame owner, boolean modal, boolean readOnly) throws HeadlessException {
        super(owner, assertion, modal);
        this.xpathCredsAssertion = assertion;
        this.readOnly = readOnly;

        if (assertion.getXpathExpression() != null) {
            namespaces = assertion.getXpathExpression().getNamespaces();
        }
        if (namespaces == null) {
            namespaces = new HashMap<String,String>();
        }

        // ensure standard namespaces are available
        if(!namespaces.containsKey(SoapConstants.SOAP_ENV_PREFIX)) {
            namespaces.put(SoapConstants.SOAP_ENV_PREFIX, SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE);
        }
        if(!namespaces.containsKey(SoapConstants.SOAP_1_2_ENV_PREFIX)) {
            namespaces.put(SoapConstants.SOAP_1_2_ENV_PREFIX, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE);
        }

        XpathExpression loginExpr = assertion.getXpathExpression();
        XpathExpression passExpr = assertion.getPasswordExpression();

        if (loginExpr != null && loginExpr.getExpression() != null)
            loginXpathField.setText(loginExpr.getExpression());

        if (passExpr != null && passExpr.getExpression() != null)
            passwordXpathField.setText(passExpr.getExpression());

        removeLoginCheckbox.setSelected(assertion.isRemoveLoginElement());
        removePasswordCheckbox.setSelected(assertion.isRemovePasswordElement());

        getContentPane().add(mainPanel);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                xpathCredsAssertion.setXpathExpression(new XpathExpression(loginXpathField.getText(), namespaces));
                xpathCredsAssertion.setPasswordExpression(new XpathExpression(passwordXpathField.getText(), namespaces));
                xpathCredsAssertion.setRemoveLoginElement(removeLoginCheckbox.isSelected());
                xpathCredsAssertion.setRemovePasswordElement(removePasswordCheckbox.isSelected());
                assertionChanged = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                xpathCredsAssertion = null;
                dispose();
            }
        });

        RunOnChangeListener updateListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() { updateButtons(); }
        });

        loginXpathField.getDocument().addDocumentListener(updateListener);
        passwordXpathField.getDocument().addDocumentListener(updateListener);

        namespacesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editNamespaces();
            }
        });

        updateButtons();
    }

    private void editNamespaces() {
        final NamespaceMapEditor nseditor = new NamespaceMapEditor(this, namespaces, null);
        nseditor.pack();
        Utilities.centerOnScreen(nseditor);
        DialogDisplayer.display(nseditor, new Runnable() {
            @Override
            public void run() {
                Map<String,String> newMap = nseditor.newNSMap();
                if (newMap != null) {
                    namespaces = newMap;
                }
            }
        });
    }

    public boolean isAssertionChanged() {
        return assertionChanged;
    }

    private void updateButtons() {
        boolean ok;
        try {
            new DOMXPath(loginXpathField.getText());
            new DOMXPath(passwordXpathField.getText());
            ok = !readOnly;
        } catch (Exception e) {
            ok = false;
        }
        okButton.setEnabled(ok);
    }
}
