package com.l7tech.server.config.ui.gui;

import com.l7tech.server.config.KeyStoreConstants;
import com.l7tech.server.config.KeystoreActions;
import com.l7tech.server.config.beans.KeystoreConfigBean;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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


    public Sca6000KeystorePanel() {
        super();
        init();
    }

    private void init() {
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
            pwPanel.setPasswordPrompts("Select the HSM password", "Confirm the HSM password");
        } else {
            pwPanel.setPasswordPrompts("Enter the HSM password", null);
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
            if(isShouldBackupMasterKey()) {
                //prompt for masterkeybackup password
                JPasswordField pFld = new JPasswordField();
                String passwordMsg = "Enter a password to protect the master key backup";
                int action = JOptionPane.showConfirmDialog(this, new Object[]{passwordMsg, pFld},"Enter Password", JOptionPane.OK_CANCEL_OPTION);
                if (action >= 0) {
                    ksBean.setMasterKeyBackupPassword(pFld.getPassword());
                    //prompt for the GDDC
                    try {
                        ka.probeUSBBackupDevice();
                        ok = true;
                    } catch (KeystoreActions.KeystoreActionsException e) {
                        JOptionPane.showMessageDialog(this.getTopLevelAncestor(), "Cannot backup key: " + e.getMessage());
                    }
                }
            } else {
                ok = true;
            }
        } else {
            ok = false;
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
