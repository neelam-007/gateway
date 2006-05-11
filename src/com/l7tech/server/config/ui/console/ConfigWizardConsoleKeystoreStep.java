package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.KeyStoreConstants;
import com.l7tech.server.config.WizardInputValidator;
import com.l7tech.server.config.commands.KeystoreConfigCommand;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.beans.KeystoreConfigBean;

import java.io.*;
import java.util.*;

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
        printText("\n" + STEP_INFO + "\n");

        boolean doKeystoreConfig = false;
        try {
            doKeystoreConfig = askDoKeystorePrompts();
            if (doKeystoreConfig) {
                doKeystoreTypePrompts();
            }
            keystoreBean.setHostname(getParentWizard().getHostname());
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

        String input = getData(prompts, "1");
        String ksType = (String) ksTypeMap.get(input);
        if (ksType == null) {
            ksType = KeyStoreConstants.NO_KEYSTORE;
        }

        keystoreBean.setKeyStoreType(ksType);
        getParentWizard().setKeystoreType(ksType);

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
        doLunaPrompts();
    }

    private Map getValidLunaPaths(final String installPathPrompt, final String defaultInstallPath, final String jspPathPrompt, final String defaultJspPath) throws IOException, WizardNavigationException {
       return consoleWizardUtils.getValidatedDataWithConfirm(
               new String[]{installPathPrompt, jspPathPrompt},
               new String[]{defaultInstallPath, defaultJspPath},
               -1,
                new WizardInputValidator() {
                    public String[] validate(Map inputs) {
                        List errorMessages = new ArrayList();
                        String installPath = (String) inputs.get(installPathPrompt);
                        String jspPath = (String) inputs.get(jspPathPrompt);

                        boolean lunaInstallDirExists = new File(installPath).exists();
                        boolean lunaJspInstallDirExists = new File(jspPath).exists();

                        if (!lunaInstallDirExists) {
                            //makes sure that the validator returns false;
                            errorMessages.add("**** The specified Luna installation directory does not exist. ****\n");
                        }

                        if (!lunaJspInstallDirExists) {
                            errorMessages.add("**** The specified Luna Java Service Provider directory does not exist ****\n");
                        }
                        if (errorMessages.size() > 0) {
                            return (String[]) errorMessages.toArray(new String[errorMessages.size()]);
                        }
                        return null;
                    }
                }
        );
    }


    private void doLunaPrompts() throws IOException, WizardNavigationException {
        printText("\n-- Luna Install Paths --\n");
        String defaultInstallPath = osFunctions.getLunaInstallDir();
        String defaultJspPath = osFunctions.getLunaJSPDir();

        String installPathPrompt = "Enter the Luna installation path: [" + defaultInstallPath +"]";
        String jspPathPrompt = "Enter the path to the luna java service provider: [" + defaultJspPath +"]";

        Map installPaths = getValidLunaPaths(installPathPrompt, defaultInstallPath, jspPathPrompt, defaultJspPath);

        keystoreBean.setLunaInstallationPath((String) installPaths.get(installPathPrompt));
        keystoreBean.setLunaJspPath((String) installPaths.get(jspPathPrompt));
    }

    private void askDefaultKeystoreQuestions() throws IOException, WizardNavigationException {
        String defaultValue = "1";
        String[] prompts = new String[] {
            "-- Create keys for this SSG --\n",
            "1) Create both CA and SSL keys\n",
            "2) Create SSL keys only\n",
            "Please make a selection: [" + defaultValue + "] ",
        };
        String input = getData(prompts, defaultValue);
        keystoreBean.setDoBothKeys( (input != null && "1".equals(input)));
        doKeystorePasswordPrompts();
    }

    private void doKeystorePasswordPrompts() throws IOException, WizardNavigationException {
        printText("\n-- Keystore Password --\n");
        String password = getMatchingPasswords(
                "Enter the keystore password (must be a minimum of 6 characters): ",
                "Enter the keystore password again (must match the first password): ",
                KeyStoreConstants.PASSWORD_LENGTH
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

        String input = getData(prompts, defaultValue);

        shouldConfigure = input != null && input.trim().equals("2");
        keystoreBean.doKeystoreConfig(shouldConfigure);
        getParentWizard().setKeystoreType(KeyStoreConstants.NO_KEYSTORE);
        return shouldConfigure;
    }

    boolean validateStep() {
        return true;
    }

}
