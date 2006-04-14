package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.KeyStoreConstants;
import com.l7tech.server.config.PasswordValidator;
import com.l7tech.server.config.commands.KeystoreConfigCommand;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.beans.KeystoreConfigBean;

import java.io.*;
import java.util.*;

import org.apache.commons.lang.StringUtils;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 9:59:35 AM
 */
public class ConfigWizardConsoleKeystoreStep extends BaseConsoleStep{

    private static final String STEP_INFO = "This step will help you configure your SSG keystore";
    private static final String KEYSTORE_TYPE_HEADER = "-- Select Keystore Type --\n";
    private static final String NO_KEYSTORE_PROMPT = "1) I already have a keystore configured and don't want to do anything here\n";
    private static final String DO_KEYSTORE_PROMPT = "2) I want to configure the keystore for this SSG\n";
    private static final String KEYSTORE_TITLE = "SSG Keystore Setup";

    private KeystoreConfigBean keystoreBean;

    private Map ksTypeMap;

    public ConfigWizardConsoleKeystoreStep(ConfigurationWizard parentWiz, OSSpecificFunctions osf) {
        super(parentWiz, osf);
        init();
    }

    public String getTitle() {
        return KEYSTORE_TITLE;
    }

    private void init() {
        configBean = new KeystoreConfigBean(osFunctions);
        keystoreBean = (KeystoreConfigBean) configBean;
        configCommand = new KeystoreConfigCommand(configBean);
        ksTypeMap = new TreeMap();
        ksTypeMap.put("1", KeyStoreConstants.DEFAULT_KEYSTORE_NAME);
        ksTypeMap.put("2", KeyStoreConstants.LUNA_KEYSTORE_NAME);
    }

    void doUserInterview(boolean validated) throws WizardNavigationException {
        printText(STEP_INFO + "\n");

        boolean doKeystoreConfig = false;
        try {
            doKeystoreConfig = askDoKeystorePrompts();
            if (doKeystoreConfig) {
                doKeystoreTypePrompts();
            }
            storeInput();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doKeystoreTypePrompts() throws IOException, WizardNavigationException {
        Set entries = ksTypeMap.entrySet();
        String[] prompts = new String[2 + entries.size()];

        prompts[0] = KEYSTORE_TYPE_HEADER;
        int index = 1;
        for (Iterator iterator = entries.iterator(); iterator.hasNext();) {
        Map.Entry entry = (Map.Entry) iterator.next();
            prompts[index++] = entry.getKey() + ") " + entry.getValue() + "\n";
        }
        prompts[prompts.length -1] = "Please select the keystore type you wish to use: [1]";

        String input = getData(prompts, "1", true);
        String ksType = (String) ksTypeMap.get(input);
        if (ksType == null) {
            ksType = KeyStoreConstants.NO_KEYSTORE;
        }

        keystoreBean.setKeyStoreType(ksType);
        getParent().setKeystoreType(ksType);

        if (ksType.equalsIgnoreCase(KeyStoreConstants.DEFAULT_KEYSTORE_NAME)) {
            askDefaultKeystoreQuestions();
        } else if (ksType.equalsIgnoreCase(KeyStoreConstants.LUNA_KEYSTORE_NAME)) {
            askLunaKeystoreQuestions();
        } else {
            printText("**** Invalid selection, please try again ****\n");
            doKeystoreTypePrompts();
        }
    }

    private void askLunaKeystoreQuestions() throws IOException, WizardNavigationException {
        String defaultValue = osFunctions.getLunaInstallDir();
        String[] prompts = new String[] {
            "Enter the Luna installation path: [" + defaultValue +"]",
        };
        keystoreBean.setLunaInstallationPath(getData(prompts, defaultValue, true));

        defaultValue = osFunctions.getLunaJSPDir();
        prompts = new String[] {
                "Enter the path to the luna java service provider: [" + defaultValue +"]",
        };

        keystoreBean.setLunaJspPath(getData(prompts, defaultValue, true));
    }

    private void askDefaultKeystoreQuestions() throws IOException, WizardNavigationException {
        String defaultValue = "1";
        String[] prompts = new String[] {
            "-- Create keys for this SSG --\n",
            "1) Create both CA and SSL keys\n",
            "2) Create SSL keys only\n",
            "Please make a selection: [" + defaultValue + "] ",
        };
        String input = getData(prompts, defaultValue, true);
        keystoreBean.setDoBothKeys( (input != null && "1".equals(input)));
        doKeystorePasswordPrompts();
    }

    private void doKeystorePasswordPrompts() throws IOException, WizardNavigationException {
        printText("-- Keystore Password --\n");

        String password = getMatchingPasswords(
            "Enter the keystore password (must be a minimum of 6 characters): ",
            "Enter the keystore password again (must match the first password): ",
            new PasswordValidator() {
                public String[] validate(String password1, String password2) {
                    String theError = null;
                    if (password1 == null || password1.equals("")) {
                        theError = "**** The password cannot be empty ****\n";
                    } else if (password1.length() < 6) {
                        theError = "**** The password must be at least 6 characters long. Please try again ****\n";
                    } else if (!StringUtils.equals(password1, password2)) {
                        theError = "**** The passwords do not match ****\n";
                    }
                    else {
                        //the the passwords match
                    }

                    if (theError != null) {
                        return new String[]  {theError};
                    }

                    return null;
                }
            }
        );

        keystoreBean.setKsPassword(password.toCharArray());
    }

    private boolean askDoKeystorePrompts() throws IOException, WizardNavigationException {
        boolean shouldConfigure = false;
        String defaultValue = "1";
        String [] prompts = new String[] {
            NO_KEYSTORE_PROMPT,
            DO_KEYSTORE_PROMPT,
            "please make a selection: [" + defaultValue + "]",
        };

        String input = getData(prompts, defaultValue, true);

        shouldConfigure = input != null && input.trim().equals("2");
        keystoreBean.doKeystoreConfig(shouldConfigure);
        getParent().setKeystoreType(KeyStoreConstants.NO_KEYSTORE);
        return shouldConfigure;
    }

    protected boolean validateStep() {
        return true;
    }
}
