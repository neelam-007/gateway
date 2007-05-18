package com.l7tech.server.config.ui.gui;

import com.l7tech.server.config.KeyStoreConstants;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;

/**
 * User: megery
 * Date: May 8, 2007
 * Time: 3:34:46 PM
 */
public class KeystorePasswordPanel extends JPanel {
    private JPanel mainPanel;
    private JPasswordField ksPassword;
    private JPasswordField ksPasswordAgain;
    private JLabel passwordMsg;
    private JLabel passwordAgainMsg;
    private JLabel passwdPrompt1;
    private JLabel passwdPrompt2;

    public KeystorePasswordPanel() {
        super();
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        setLabelDefaults();
    }

    public void setPasswordPrompts(String prompt1, String prompt2) {
        if (StringUtils.isEmpty(prompt1)) {
            throw new IllegalArgumentException("Must specify at least one prompt");
        }
        passwdPrompt1.setText(prompt1);

        if (StringUtils.isNotEmpty(prompt2)) {
            passwdPrompt2.setText(prompt2);
            passwdPrompt2.setVisible(true);
            ksPasswordAgain.setVisible(true);
        }
        else {
            passwdPrompt2.setVisible(false);
            ksPasswordAgain.setVisible(false);
        }
    }
    
    public void setLabelDefaults() {
        setPasswordErrorMsgColour(Color.BLACK);
        passwordMsg.setVisible(false);

        setConfirmedPasswordErrorMsgColour(Color.BLACK);
        passwordAgainMsg.setVisible(false);
    }

    public void setPasswordMsg(String s) {
        passwordMsg.setText(s);
    }

    public char[] getPassword() {
        return ksPassword.getPassword();
    }

    public char[] getConfirmedPassword() {
        return ksPasswordAgain.getPassword();
    }

    private void setPasswordErrorMsgColour(Color colour) {
        passwordMsg.setForeground(colour);
    }

    private void setConfirmedPasswordErrorMsgColour(Color colour) {
        passwordAgainMsg.setForeground(colour);
    }

    public void showPasswordErrorMsg() {
        setPasswordErrorMsgColour(Color.RED);
        passwordMsg.setVisible(true);
    }

    public void showPasswordConfirmationErrorMsg() {
        setConfirmedPasswordErrorMsgColour(Color.RED);
        passwordAgainMsg.setVisible(true);
    }

    public boolean validateInput(final boolean validateBothInputs) {
        setLabelDefaults();
        final String password1 = new String(getPassword());
        final String password2 = validateBothInputs?new String(getConfirmedPassword()):null;

        boolean goodInput = true;
        if (StringUtils.isEmpty(password1) || password1.length() < KeyStoreConstants.PASSWORD_LENGTH) {
            showPasswordErrorMsg();
            goodInput = false;
        } else if (validateBothInputs) {
            if (!password1.equals(password2)) {
                showPasswordConfirmationErrorMsg();
                goodInput = false;
            }
        }

        return goodInput;
    }

    public void setPassword(char[] password) {
        ksPassword. setText(new String(password));
    }
}
