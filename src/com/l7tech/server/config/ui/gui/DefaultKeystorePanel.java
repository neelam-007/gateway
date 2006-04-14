package com.l7tech.server.config.ui.gui;

import com.l7tech.server.config.PasswordValidator;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 23, 2005
 * Time: 1:11:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultKeystorePanel extends KeystorePanel{
    private JPanel mainPanel;
    private JRadioButton bothKeys;
    private JRadioButton sslKeysOnly;
    private JPasswordField ksPassword;
    private JPasswordField ksPasswordAgain;
    private JLabel passwordMsg;
    private JLabel passwordAgainMsg;


    public DefaultKeystorePanel() {
        super();
        init();
    }

    private void init() {
        ButtonGroup whichKeysGroup = new ButtonGroup();
        whichKeysGroup.add(bothKeys);
        whichKeysGroup.add(sslKeysOnly);

        bothKeys.setSelected(true);
        setLabelDefaults();

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        validate();
    }

    public char[] getKsPassword() {
        return ksPassword.getPassword();
    }

    public boolean doBothKeys() {
        return bothKeys.isSelected();
    }

    public boolean validateInput() {
        setLabelDefaults();
        PasswordValidator validator = new PasswordValidator() {
            public String[] validate(String password1, String password2) {
                boolean isValid = true;
                if (password1.length() < 6) {
                    passwordMsg.setForeground(Color.RED);
                    isValid = false;
                }

                if(!(new String(ksPassword.getPassword()).equals(new String(ksPasswordAgain.getPassword())))) {
                    passwordAgainMsg.setForeground(Color.RED);
                    isValid = false;
                }
                if (!isValid) return new String[1];
                return null;
            }
        };
        String[] validationErrors = validator.validate(String.valueOf(ksPassword.getPassword()), String.valueOf(ksPasswordAgain.getPassword()));
        return validationErrors == null;
    }

    private void setLabelDefaults() {
        passwordMsg.setForeground(Color.BLACK);
        passwordAgainMsg.setForeground(Color.BLACK);
    }

}
