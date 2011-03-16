package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderPasswordPolicy;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;

public class PasswordHelpDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JEditorPane editorPane;
    private boolean ok;

    public PasswordHelpDialog(Frame owner) {
        super(owner);
        initialize();
    }

    public PasswordHelpDialog(Dialog owner) {
        super(owner);
        initialize();
    }

    private void initialize() {
        setContentPane(contentPane);
        setModal(true);
        setTitle("Password Rules");
        getRootPane().setDefaultButton(buttonOK);
        ok = false;

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        Registry reg = Registry.getDefault();
        String passwordHelpMsg;
        try
        {
           passwordHelpMsg  = reg.getIdentityAdmin().getPasswordPolicyForIdentityProvider(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID).getDescription();
        } catch (FindException e) {
            passwordHelpMsg = null;
        }
        editorPane.setText(passwordHelpMsg);

        this.pack();
        this.setSize(400, 275);
        this.setResizable(true);
    }

    public boolean isOk() {
        return ok;
    }

    private void onOK() {
        ok = true;
        dispose();
    }
}
