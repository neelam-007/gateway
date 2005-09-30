/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.common.xml.XpathExpression;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import org.jaxen.dom.DOMXPath;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.xml.soap.SOAPConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

/**
 * @author alex
 */
public class XpathCredentialSourcePropertiesDialog extends JDialog {
    private XpathCredentialSource xpathCredsAssertion;
    private boolean assertionChanged = false;

    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;
    private JTextField loginXpathField;
    private JTextField passwordXpathField;
    private JCheckBox removeLoginCheckbox;
    private JCheckBox removePasswordCheckbox;
    private JButton namespacesButton;
    private Map namespaces; // one map for both xpath expressions

    public XpathCredentialSource getXpathCredsAssertion() {
        return xpathCredsAssertion;
    }

    public XpathCredentialSourcePropertiesDialog(XpathCredentialSource assertion, Frame owner, boolean modal) throws HeadlessException {
        super(owner, "Configure XPath Credential Source", modal);
        this.xpathCredsAssertion = assertion;
        if (assertion != null && assertion.getXpathExpression() != null) {
            namespaces = assertion.getXpathExpression().getNamespaces();
        }
        if (namespaces == null) {
            namespaces = new HashMap();
        }
        if (namespaces.isEmpty()) {
            namespaces.put(SoapUtil.SOAP_ENV_PREFIX, SOAPConstants.URI_NS_SOAP_ENVELOPE);
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
            public void actionPerformed(ActionEvent e) {
                xpathCredsAssertion = null;
                dispose();
            }
        });

        DocumentListener updateListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateButtons(); }
            public void insertUpdate(DocumentEvent e) { updateButtons(); }
            public void removeUpdate(DocumentEvent e) { updateButtons(); }
        };

        loginXpathField.getDocument().addDocumentListener(updateListener);
        passwordXpathField.getDocument().addDocumentListener(updateListener);

        namespacesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                editNamespaces();
            }
        });

        updateButtons();
    }

    private void editNamespaces() {
        NamespaceMapEditor nseditor = new NamespaceMapEditor(this, namespaces, null);
        nseditor.pack();
        Utilities.centerOnScreen(nseditor);
        nseditor.setVisible(true);
        Map newMap = nseditor.newNSMap();
        if (newMap != null) {
            namespaces = newMap;
        }
    }

    public boolean isAssertionChanged() {
        return assertionChanged;
    }

    private void updateButtons() {
        boolean ok;
        try {
            new DOMXPath(loginXpathField.getText());
            new DOMXPath(passwordXpathField.getText());
            ok = true;
        } catch (Exception e) {
            ok = false;
        }
        okButton.setEnabled(ok);
    }
}
