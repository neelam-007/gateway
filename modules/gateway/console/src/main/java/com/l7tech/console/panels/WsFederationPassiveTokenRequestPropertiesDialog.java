package com.l7tech.console.panels;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Property edit dialog for WsFederationPassiveTokenRequest.
 *
 * @author $Author$
 * @version $Revision$
 */
public class WsFederationPassiveTokenRequestPropertiesDialog extends LegacyAssertionPropertyDialog {
    private static final Logger logger = Logger.getLogger(WsFederationPassiveTokenRequestPropertiesDialog.class.getName());


    //- PUBLIC

    public WsFederationPassiveTokenRequest getWsFederationAssertion() {
        return wsFedAssertion;
    }

    public WsFederationPassiveTokenRequestPropertiesDialog(WsFederationPassiveTokenRequest assertion, boolean request, Frame owner, boolean modal, boolean readOnly) throws HeadlessException {
        super(owner, assertion, modal);
        this.wsFedAssertion = assertion;
        this.readOnly = readOnly;

        ipStsUrlTextField.setText(assertion.getIpStsUrl());
        realmTextField.setText(assertion.getRealm());
        timestampCheckBox.setSelected(assertion.isTimestamp());
        replyUrlTextField.setText(assertion.getReplyUrl());
        authenticationCheckBox.setSelected(assertion.isAuthenticate());
        contextUrlTextField.setText(assertion.getContext());

        getContentPane().add(mainPanel);

        actionComboBox.setModel(new DefaultComboBoxModel(new String[]{"Token Request", "Token Exchange"}));
        actionComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls();
                updateButtons();
            }
        });
        if (!request) {
            actionComboBox.setSelectedIndex(1);
        }

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                wsFedAssertion.setIpStsUrl(ipStsUrlTextField.getText());
                wsFedAssertion.setRealm(realmTextField.getText());
                wsFedAssertion.setTimestamp(timestampCheckBox.isSelected());
                wsFedAssertion.setReplyUrl(replyUrlTextField.getText());
                wsFedAssertion.setAuthenticate(authenticationCheckBox.isSelected());
                wsFedAssertion.setContext(contextUrlTextField.getText());
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
        realmTextField.getDocument().addDocumentListener(rocl);
        replyUrlTextField.getDocument().addDocumentListener(rocl);

        updateControls();
        updateButtons();
    }

    public boolean isTokenRequest() {
        return actionComboBox.getSelectedIndex()==0;
    }

    public boolean isAssertionChanged() {
        return assertionChanged;
    }

    //- PRIVATE

    private static final String REALM_SCHEME = "urn";

    private WsFederationPassiveTokenRequest wsFedAssertion;
    private boolean assertionChanged = false;
    private boolean readOnly = false;

    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;
    private JComboBox actionComboBox;
    private JCheckBox timestampCheckBox;
    private JTextField realmTextField;
    private JTextField ipStsUrlTextField;
    private JCheckBox authenticationCheckBox;
    private JTextField replyUrlTextField;
    private JTextField contextUrlTextField;

    private void updateButtons() {
        boolean ok = false;
        String url = ipStsUrlTextField.getText();
        String rurl = replyUrlTextField.getText();
        boolean auth = authenticationCheckBox.isSelected();
        String realm = realmTextField.getText();

        ok = !readOnly && validUrl(url, !isTokenRequest() && auth) && validUrl(rurl, !auth) && (!isTokenRequest() || validUrn(realm));

        okButton.setEnabled(ok);
    }

    private void updateControls() {
        if (isTokenRequest()) {
            // Configure for token request
            realmTextField.setEnabled(true);
            timestampCheckBox.setEnabled(true);
        }
        else {
            // Configure for token exchange
            realmTextField.setEnabled(false);
            timestampCheckBox.setEnabled(false);
        }
    }

    private boolean validUrl(String url, boolean allowEmpty) {
        return ValidationUtils.isValidUrl(url, allowEmpty);
    }

    private boolean validUrn(String urnText) {
        boolean valid = false;

        if(urnText==null || urnText.trim().length()==0) {
            valid = true; // allow empty
        }
        else {
            try {
                URI urn = new URI(urnText);
                if(REALM_SCHEME.equalsIgnoreCase(urn.getScheme())) {
                    valid = true;
                }
            }
            catch(URISyntaxException use) {
                logger.log(Level.FINE, ExceptionUtils.getMessage(use), ExceptionUtils.getDebugException(use));
            }
        }

        return valid;
    }
}
