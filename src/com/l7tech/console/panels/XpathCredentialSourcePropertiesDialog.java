/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels;

import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author alex
 * @version $Revision$
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

    public XpathCredentialSource getXpathCredsAssertion() {
        return xpathCredsAssertion;
    }

    public XpathCredentialSourcePropertiesDialog(XpathCredentialSource assertion, Frame owner, boolean modal) throws HeadlessException {
        super(owner, "Configure XPath Credential Source", modal);
        this.xpathCredsAssertion = assertion;

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
                xpathCredsAssertion.setXpathExpression(new XpathExpression(loginXpathField.getText()));
                xpathCredsAssertion.setPasswordExpression(new XpathExpression(passwordXpathField.getText()));
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

        updateButtons();
    }

    public boolean isAssertionChanged() {
        return assertionChanged;
    }

    private void updateButtons() {
        boolean ok = false;
        ok = ok && loginXpathField.getText() != null && loginXpathField.getText().length() > 0;
        ok = ok && passwordXpathField.getText() != null && passwordXpathField.getText().length() > 0;
        try {
            new DOMXPath(loginXpathField.getText());
            new DOMXPath(passwordXpathField.getText());
            ok = true;
        } catch (JaxenException e) {
            ok = false;
        }
        okButton.setEnabled(ok);
    }
}
