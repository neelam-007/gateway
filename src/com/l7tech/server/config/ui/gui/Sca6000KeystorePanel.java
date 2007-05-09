package com.l7tech.server.config.ui.gui;

import com.l7tech.server.config.KeyStoreConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * User: megery
 * Date: May 7, 2007
 * Time: 3:44:28 PM
 */
public class Sca6000KeystorePanel extends KeystorePanel{
    private JPanel mainPanel;
    private JPanel passwordPanel;
    private JRadioButton initializeKeystore;
    private JRadioButton restoreKeystoreBackup;

    KeystorePasswordPanel pwPanel;


    public Sca6000KeystorePanel() {
        super();
        init();
    }

    private void init() {
        ButtonGroup bg = new ButtonGroup();

        bg.add(initializeKeystore);
        bg.add(restoreKeystoreBackup);

        initializeKeystore.setSelected(true);

        initializeKeystore.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setPasswordFieldsVisible();
            }
        });

        restoreKeystoreBackup.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setPasswordFieldsVisible();
            }
        });

        pwPanel = new KeystorePasswordPanel();
        pwPanel.setPasswordMsg("Must have a miniumum of " + KeyStoreConstants.PASSWORD_LENGTH + " characters");
        pwPanel.setLabelDefaults();

        passwordPanel.removeAll();
        passwordPanel.setLayout(new BorderLayout());
        setPasswordFieldsVisible();
        passwordPanel.add(pwPanel, BorderLayout.CENTER);
        passwordPanel.revalidate();


        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    private void setPasswordFieldsVisible() {
        if (initializeKeystore.isSelected()) {
            pwPanel.setPasswordPrompts("Select the HSM password", "Confirm the HSM password");
        } else {
            pwPanel.setPasswordPrompts("Enter the HSM password", null);
        }
    }

    public char[] getPassword() {
        return pwPanel.getPassword();
    }

    public boolean validateInput() {
        return pwPanel.validateInput(initializeKeystore.isSelected());
    }

    public boolean isInitializeHSM() {
        return initializeKeystore.isSelected();
    }
}
