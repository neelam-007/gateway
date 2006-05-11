package com.l7tech.server.config.ui.gui;

import com.l7tech.server.config.WizardInputValidator;
import com.l7tech.server.config.KeyStoreConstants;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;

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

        passwordMsg.setText("Must have a miniumum of " + KeyStoreConstants.PASSWORD_LENGTH + " characters");
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
        final String password1 = new String(ksPassword.getPassword());
        final String password2 = new String(ksPasswordAgain.getPassword());

        WizardInputValidator validatorWizard = new WizardInputValidator() {
            public String[] validate(Map inputs) {
                boolean badInput = false;
                if (StringUtils.isEmpty(password1) || password1.length() < KeyStoreConstants.PASSWORD_LENGTH) {
                    passwordMsg.setForeground(Color.RED);
                    badInput = true;
                } else if (!password1.equals(password2)) {
                    passwordAgainMsg.setForeground(Color.RED);
                    badInput = true;
                }

                return (badInput? new String[0]: null);
            }
        };

        return (validatorWizard.validate(new HashMap()) == null);
    }

    private void setLabelDefaults() {
        passwordMsg.setForeground(Color.BLACK);
        passwordAgainMsg.setForeground(Color.BLACK);
    }

}
