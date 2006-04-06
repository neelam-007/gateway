package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.commands.KeystoreConfigCommand;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.beans.KeystoreConfigBean;

import java.io.*;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 9:59:35 AM
 */
public class ConfigWizardConsoleKeystoreStep extends BaseConsoleStep{

    KeystoreConfigBean keystoreBean;

    public ConfigWizardConsoleKeystoreStep(ConfigurationWizard parentWiz, OSSpecificFunctions osf) {
        super(parentWiz, osf);
        configBean = new KeystoreConfigBean(osFunctions);
        keystoreBean = (KeystoreConfigBean) configBean;
        command = new KeystoreConfigCommand(configBean);
    }

    void doUserInterview(boolean validated) throws WizardNavigationException {
        out.println("Press <Enter> to continue");
        out.flush();

        try {
            handleInput(reader.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

     public String getTitle() {
        return "SSG Keystore Setup";
    }

    protected boolean validateStep() {
        return true;
    }
}
