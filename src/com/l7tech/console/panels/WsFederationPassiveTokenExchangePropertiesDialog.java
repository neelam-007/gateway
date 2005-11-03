package com.l7tech.console.panels;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

import com.l7tech.common.gui.util.RunOnChangeListener;
import com.l7tech.common.util.ValidationUtils;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenExchange;

/**
 * Property edit dialog for WsFederationPassiveTokenExchange.
 *
 * @author $Author$
 * @version $Revision$
 */
public class WsFederationPassiveTokenExchangePropertiesDialog extends JDialog {

    //- PUBLIC

    public WsFederationPassiveTokenExchange getWsFederationAssertion() {
        return wsFedAssertion;
    }

    public WsFederationPassiveTokenExchangePropertiesDialog(WsFederationPassiveTokenExchange assertion, Frame owner, boolean modal) throws HeadlessException {
        super(owner, "Configure WS-Federation Token Exchange", modal);
        this.wsFedAssertion = assertion;

        ipStsUrlTextField.setText(assertion.getIpStsUrl());
        contextTextField.setText(assertion.getContext());
        authenticationCheckBox.setSelected(wsFedAssertion.isAuthenticate());
        replyUrlTextField.setText(wsFedAssertion.getReplyUrl());

        getContentPane().add(mainPanel);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                wsFedAssertion.setIpStsUrl(ipStsUrlTextField.getText());
                wsFedAssertion.setContext(contextTextField.getText());
                wsFedAssertion.setAuthenticate(authenticationCheckBox.isSelected());
                wsFedAssertion.setReplyUrl(replyUrlTextField.getText());
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

        RunOnChangeListener rocl = new RunOnChangeListener(new Runnable(){
            public void run() {
                updateButtons();
            }
        });

        authenticationCheckBox.addChangeListener(rocl);
        ipStsUrlTextField.getDocument().addDocumentListener(rocl);
        contextTextField.getDocument().addDocumentListener(rocl);
        replyUrlTextField.getDocument().addDocumentListener(rocl);

        updateButtons();
    }

    public boolean isAssertionChanged() {
        return assertionChanged;
    }

    //- PRIVATE

    private WsFederationPassiveTokenExchange wsFedAssertion;
    private boolean assertionChanged = false;

    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;
    private JTextField contextTextField;
    private JTextField ipStsUrlTextField;
    private JCheckBox authenticationCheckBox;
    private JTextField replyUrlTextField;

    private void updateButtons() {
        boolean ok = false;
        String url = ipStsUrlTextField.getText();
        String rurl = replyUrlTextField.getText();
        boolean auth = authenticationCheckBox.isSelected();

        ok = validUrl(url, auth) && validUrl(rurl, !auth);

        okButton.setEnabled(ok);
    }


    private boolean validUrl(String url, boolean allowEmpty) {
        return ValidationUtils.isValidUrl(url, allowEmpty);
    }
}
