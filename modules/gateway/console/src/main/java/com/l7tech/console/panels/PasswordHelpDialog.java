package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PasswordHelpDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(PasswordHelpDialog.class.getName());

    private JPanel contentPane;
    private JButton buttonOK;
    private JEditorPane editorPane;
    private boolean ok;

    public PasswordHelpDialog(Frame owner) {
        super(owner);
        initialize(null);
    }

    public PasswordHelpDialog(Dialog owner, String passwordPolicyDescription) {
        super(owner);
        initialize(passwordPolicyDescription);
    }

    private void initialize( String passwordPolicyDescription) {
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
        String passwordHelpMsg = passwordPolicyDescription;
        if(passwordHelpMsg == null)
        {
            try
            {
                passwordHelpMsg  = reg.getIdentityAdmin().getPasswordPolicyDescriptionForIdentityProvider();
                if (passwordHelpMsg == null || passwordHelpMsg.trim().length() < 1)
                    passwordHelpMsg = "No password policy description available for current admin user.";
            } catch (Exception e) {
                passwordHelpMsg = "Error getting password policy description";
                logger.log(Level.WARNING, passwordHelpMsg, ExceptionUtils.getDebugException(e));
            }
        }
        editorPane.setText(passwordHelpMsg);

        this.pack();
        this.setSize(480, 275);
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
