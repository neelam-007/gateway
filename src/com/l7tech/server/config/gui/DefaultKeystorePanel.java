package com.l7tech.server.config.gui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

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

//    private final static String DEFAULT_PASSWORD_MSG = "Must be a minimum of 6 characters";
//    private final static String DEFAULT_PASSWORDAGAIN_MSG = "Must match the first password";
    private JPanel passwordPanel;

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

    private char[] getKsPasswordAgain() {
        return ksPasswordAgain.getPassword();
    }

    public boolean doBothKeys() {
        return bothKeys.isSelected();
    }

    public boolean validateInput() {
        boolean isValid = true;
        setLabelDefaults();

        if (ksPassword.getPassword().length < 6) {
            passwordMsg.setForeground(Color.RED);
            isValid = false;
        }

        if(!(new String(ksPassword.getPassword()).equals(new String(ksPasswordAgain.getPassword())))) {
            passwordAgainMsg.setForeground(Color.RED);
            isValid = false;
        }

        return isValid;
    }

    private void setLabelDefaults() {
//        passwordMsg.setText(DEFAULT_PASSWORD_MSG);
        passwordMsg.setForeground(Color.BLACK);

//        passwordAgainMsg.setText(DEFAULT_PASSWORDAGAIN_MSG);
        passwordAgainMsg.setForeground(Color.BLACK);
    }

}
