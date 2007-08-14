package com.l7tech.server.config.ui.gui;

import com.l7tech.server.config.KeyStoreConstants;
import com.l7tech.server.config.KeystoreActions;
import com.l7tech.server.config.beans.KeystoreConfigBean;
import com.l7tech.server.config.exceptions.KeystoreActionsException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

/**
 * User: megery
 * Date: May 7, 2007
 * Time: 3:44:28 PM
 */
public class Sca6000KeystorePanel extends KeystorePanel {
    private JPanel mainPanel;
    private JPanel passwordPanel;
    private JRadioButton initializeKeystore;
    private JRadioButton importExistingKeystore;
    private JCheckBox shouldBackupMasterKey;

    KeystorePasswordPanel pwPanel;
    private ResourceBundle resourceBundle;


    public Sca6000KeystorePanel() {
        super();
        init();
    }

    private void init() {
        resourceBundle = ResourceBundle.getBundle("com.l7tech.server.config.resources.configwizard");
        ButtonGroup bg = new ButtonGroup();

        bg.add(initializeKeystore);
        bg.add(importExistingKeystore);

        initializeKeystore.setSelected(true);

        initializeKeystore.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setBackupMasterKeyEnabled();
                setPasswordFieldsVisible();
            }
        });

        importExistingKeystore.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setBackupMasterKeyEnabled();
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

    private void setBackupMasterKeyEnabled() {
        shouldBackupMasterKey.setEnabled(initializeKeystore.isSelected());
    }

    private void setPasswordFieldsVisible() {
        if (initializeKeystore.isSelected()) {
            pwPanel.setPasswordPrompts(
                    resourceBundle.getString("hsm.initialize.new.password.msg"),
                    resourceBundle.getString("hsm.initialize.confirm.password.msg"));
        } else {
            pwPanel.setPasswordPrompts(
                    resourceBundle.getString("hsm.import.password.msg"),
                    null);
        }
    }

    public char[] getPassword() {
        return pwPanel.getPassword();
    }

    public boolean isShouldBackupMasterKey() {
        return shouldBackupMasterKey.isSelected();
    }

    public boolean validateInput(KeystoreConfigBean ksBean) {
        boolean ok = false;
        KeystoreActions ka = new KeystoreActions(ksBean.getOsFunctions());
        if (pwPanel.validateInput(initializeKeystore.isSelected())) {
            if (initializeKeystore.isSelected()) {
                if(isShouldBackupMasterKey()) {
                    //prompt for masterkeybackup password
                    JPasswordField pFld = new JPasswordField();
                    String passwordMsg = "Enter a password to protect the master key backup";
                    int action = JOptionPane.showConfirmDialog(this, new Object[]{passwordMsg, pFld},"Enter Password", JOptionPane.OK_CANCEL_OPTION);
                    if (action == JOptionPane.OK_OPTION) {
                        ksBean.setMasterKeyBackupPassword(pFld.getPassword());
                        //prompt for the GDDC
                        try {
                            ka.probeUSBBackupDevice();
                            ok = true;
                        } catch (KeystoreActionsException e) {
                            JOptionPane.showMessageDialog(this.getTopLevelAncestor(), "Cannot backup key: " + e.getMessage());
                        }
                    }
                }
            } else {
                JPasswordField pFld = new JPasswordField();
                String passwordMsg = "Enter the master key backup password";
                int action = JOptionPane.showConfirmDialog(this, new Object[]{passwordMsg, pFld},"Enter Password", JOptionPane.OK_CANCEL_OPTION);
                if (action == JOptionPane.OK_OPTION) {
                    ksBean.setMasterKeyBackupPassword(pFld.getPassword());
                    //prompt for the GDDC
                    try {
                       ka.probeUSBBackupDevice();
                        ok = true;
                    } catch (KeystoreActionsException e) {
                        JOptionPane.showMessageDialog(this.getTopLevelAncestor(), "Cannot proceed with importing existing keystore: " + e.getMessage());
                    }
                }
            }
        } else {
            return false;
        }
        return ok;
    }

    public boolean isInitializeHSM() {
        return initializeKeystore.isSelected();
    }

    public void setKsPassword(char[] ksPassword) {
        pwPanel.setPassword(ksPassword);
    }
}
