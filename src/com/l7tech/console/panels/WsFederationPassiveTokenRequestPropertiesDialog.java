package com.l7tech.console.panels;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;

/**
 * Property edit dialog for WsFederationPassiveTokenRequest.
 *
 * @author $Author$
 * @version $Revision$
 */
public class WsFederationPassiveTokenRequestPropertiesDialog extends JDialog {

    //- PUBLIC

    public WsFederationPassiveTokenRequest getWsFederationAssertion() {
        return wsFedAssertion;
    }

    public WsFederationPassiveTokenRequestPropertiesDialog(WsFederationPassiveTokenRequest assertion, Frame owner, boolean modal) throws HeadlessException {
        super(owner, "Configure WS-Federation Token Request", modal);
        this.wsFedAssertion = assertion;

        ipStsUrlTextField.setText(assertion.getIpStsUrl());
        realmTextField.setText(assertion.getRealm());
        timestampCheckBox.setSelected(assertion.isTimestamp());

        getContentPane().add(mainPanel);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                wsFedAssertion.setIpStsUrl(ipStsUrlTextField.getText());
                wsFedAssertion.setRealm(realmTextField.getText());
                wsFedAssertion.setTimestamp(timestampCheckBox.isSelected());
                assertionChanged = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                wsFedAssertion = null;
                dispose();
            }
        });

        DocumentListener updateListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateButtons(); }
            public void insertUpdate(DocumentEvent e) { updateButtons(); }
            public void removeUpdate(DocumentEvent e) { updateButtons(); }
        };

        ipStsUrlTextField.getDocument().addDocumentListener(updateListener);
        realmTextField.getDocument().addDocumentListener(updateListener);

        updateButtons();
    }

    public boolean isAssertionChanged() {
        return assertionChanged;
    }

    //- PRIVATE

    private WsFederationPassiveTokenRequest wsFedAssertion;
    private boolean assertionChanged = false;

    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;
    private JCheckBox timestampCheckBox;
    private JTextField realmTextField;
    private JTextField ipStsUrlTextField;

    private void updateButtons() {
        boolean ok = false;
        String url = ipStsUrlTextField.getText();
        ok = url != null && url.length() > 0;
        try {
            new URL(url);
            ok = true;
        } catch (MalformedURLException e) {
            ok = false;
        }
        okButton.setEnabled(ok);
    }
}
