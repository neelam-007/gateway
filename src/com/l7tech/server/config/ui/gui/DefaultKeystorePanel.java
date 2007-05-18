package com.l7tech.server.config.ui.gui;

import com.l7tech.server.config.KeyStoreConstants;

import javax.swing.*;
import java.awt.*;

/**
 * User: megery
 * Date: Aug 23, 2005
 * */
public class DefaultKeystorePanel extends KeystorePanel{
    private JPanel mainPanel;
    private JRadioButton bothKeys;
    private JRadioButton sslKeysOnly;
    private JPanel passwordPanel;

    private KeystorePasswordPanel pwPanel;


    public DefaultKeystorePanel() {
        super();
        init();
    }

    private void init() {
        ButtonGroup whichKeysGroup = new ButtonGroup();
        whichKeysGroup.add(bothKeys);
        whichKeysGroup.add(sslKeysOnly);

        bothKeys.setSelected(true);

        pwPanel = new KeystorePasswordPanel();
        pwPanel.setPasswordMsg("Must have a miniumum of " + KeyStoreConstants.PASSWORD_LENGTH + " characters");
        passwordPanel.removeAll();
        passwordPanel.setLayout(new BorderLayout());
        passwordPanel.add(pwPanel, BorderLayout.CENTER);
        passwordPanel.revalidate();
        
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        validate();
    }

    public char[] getKsPassword() {
        return pwPanel.getPassword();
    }

    public void setKsPassword(char[] password) {
        pwPanel.setPassword(password);
    }

    public boolean doBothKeys() {
        return bothKeys.isSelected();
    }

    public boolean validateInput() {
        return pwPanel.validateInput(true);
    }
}
