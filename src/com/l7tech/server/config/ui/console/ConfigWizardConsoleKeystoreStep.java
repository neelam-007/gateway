package com.l7tech.server.config.ui.console;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.server.config.*;
import com.l7tech.server.config.beans.KeystoreConfigBean;
import com.l7tech.server.config.commands.KeystoreConfigCommand;
import com.l7tech.server.config.exceptions.KeystoreActionsException;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 9:59:35 AM
 */
public class ConfigWizardConsoleKeystoreStep extends BaseConsoleStep implements KeystoreActionsListener {

    private static final Logger logger = Logger.getLogger(ConfigWizardConsoleKeystoreStep.class.getName());

    private static final String STEP_INFO = "This step will help you configure your SSG keystore";
    private static final String KEYSTORE_TYPE_HEADER = "-- Select Keystore Type --\n";
    private static final String NO_KEYSTORE_PROMPT = "1) I already have a keystore configured and don't want to do anything here\n";
    private static final String DO_KEYSTORE_PROMPT = "2) I want to configure the keystore for this SSG\n";
    private static final String KEYSTORE_TITLE = "Set Up the SSG Keystore";

    private Map<String, KeystoreType> ksTypeMap;
    private ResourceBundle resourceBundle;

    public ConfigWizardConsoleKeystoreStep(ConfigurationWizard parentWiz) {
        super(parentWiz);
        init();
    }

    public String getTitle() {
        return KEYSTORE_TITLE;
    }

    private void init() {
        configBean = new KeystoreConfigBean();
        configCommand = new KeystoreConfigCommand(configBean);
        ksTypeMap = new TreeMap<String,KeystoreType>();
        resourceBundle = ResourceBundle.getBundle("com.l7tech.server.config.resources.configwizard");
    }

    public void doUserInterview(boolean validated) throws WizardNavigationException {
        printText("\n" + STEP_INFO + "\n");

        boolean doKeystoreConfig;
        try {
            doKeystoreConfig = askDoKeystorePrompts();
            if (doKeystoreConfig) {
                doKeystoreTypePrompts();
            }
            ((KeystoreConfigBean)configBean).setHostname(getParentWizard().getHostname());
            ((KeystoreConfigBean)configBean).setDbInformation(SharedWizardInfo.getInstance().getDbinfo());
            storeInput();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doKeystoreTypePrompts() throws IOException, WizardNavigationException {
        ksTypeMap.clear();

        OSSpecificFunctions.KeystoreInfo[] keystores = osFunctions.getAvailableKeystores();
        int x = 1;
        String activePartitionName = PartitionManager.getInstance().getActivePartition().getPartitionId();
        for (OSSpecificFunctions.KeystoreInfo keystore : keystores) {
            if (keystore.getType() == KeystoreType.SCA6000_KEYSTORE_NAME && !activePartitionName.equals(PartitionInformation.DEFAULT_PARTITION_NAME))
                continue;
            ksTypeMap.put(String.valueOf(x), keystore.getType());
            x++;
        }
        Set<Map.Entry<String,KeystoreType>> entries = ksTypeMap.entrySet();
        List<String> prompts = new ArrayList<String>();

        prompts.add(KEYSTORE_TYPE_HEADER);

        for (Map.Entry<String, KeystoreType> entry : entries) {
            prompts.add(entry.getKey() + ") " + entry.getValue() + getEolChar());
        }
        prompts.add("Please select the keystore type you wish to use: [1]");

        String input = getData(
                prompts.toArray(new String[prompts.size()]),
                "1",
                ksTypeMap.keySet().toArray(new String[ksTypeMap.keySet().size()]),
                null
        );

        KeystoreType ksType = ksTypeMap.get(input);

        if (ksType == null) {
            ksType = KeystoreType.NO_KEYSTORE;
        }

        KeystoreConfigBean keystoreBean = (KeystoreConfigBean) configBean;
        keystoreBean.setKeyStoreType(ksType);
        getParentWizard().setKeystoreType(ksType);
        keystoreBean.setKeystoreTypeName(ksType.getShortTypeName());

        switch (ksType) {
            case DEFAULT_KEYSTORE_NAME:
                askDefaultKeystoreQuestions();
                break;
            case SCA6000_KEYSTORE_NAME:
                askHSMQuestions();
                break;
            case LUNA_KEYSTORE_NAME:
                askLunaKeystoreQuestions();
                break;
        }
    }

    private void askHSMQuestions() throws IOException, WizardNavigationException {
        doHSMPrompts();
    }

    private void doHSMPrompts() throws IOException, WizardNavigationException {
        boolean askAgain = false;
        do {
            printText("\n-- Configure Hardware Security Module (HSM) --\n");

            String defaultValue = "1";
            String[] prompts = new String[] {
                "1) Initialize Keystore" + getEolChar(),
                "2) Import Existing Keystore" + getEolChar(),
                "Please make a selection: [" + defaultValue + "] ",
            };
            String input = getData(prompts, defaultValue, new String[] {"1", "2"},null);

            KeystoreConfigBean keystoreBean = (KeystoreConfigBean) configBean;
            keystoreBean.setInitializeHSM((input != null && "1".equals(input)));
            if (keystoreBean.isInitializeHSM()) {
                    askInitialiseHSMQuestions();
            } else {
                askAgain = !askRestoreHSMQuestions();
            }
        } while (askAgain);

    }

    private boolean askRestoreHSMQuestions() throws IOException, WizardNavigationException {
        boolean success;

        KeystoreConfigBean keystoreBean = (KeystoreConfigBean) configBean;
        keystoreBean.setShouldBackupMasterKey(false);
        String backupPassword = getMatchingPasswords(
                "Enter the master key backup password: ",
                null,
                KeyStoreConstants.PASSWORD_LENGTH);

        keystoreBean.setMasterKeyBackupPassword(backupPassword.toCharArray());
        KeystoreActions ka = new KeystoreActions(osFunctions);
        try {
            ka.probeUSBBackupDevice();
            doKeystorePasswordPrompts(
                    "Enter the existing HSM Password",
                    resourceBundle.getString("hsm.import.password.msg") + ": " ,
                    null);
            success = true;
        } catch (KeystoreActionsException e) {
            printText("*** Cannot proceed with importing existing keystore: " + e.getMessage() + " ***" + getEolChar() + getEolChar());
            success = false;
        }
        return success;
    }

    private void askInitialiseHSMQuestions() throws IOException, WizardNavigationException {
        boolean askAgain;
        do {
            boolean shouldBackup = getConfirmationFromUser("Back Up Master Key to USB Drive After Initialization? ", "y");

            KeystoreConfigBean keystoreBean = (KeystoreConfigBean) configBean;
            if (shouldBackup) {
                keystoreBean.setShouldBackupMasterKey(true);
                String backupPassword = getMatchingPasswords("Enter the password to protect the master key backup:", "Confirm the password to protect the master key backup:", 6);
                keystoreBean.setMasterKeyBackupPassword(backupPassword.toCharArray());
                KeystoreActions ka = new KeystoreActions(osFunctions);
                try {
                    ka.probeUSBBackupDevice();
                    askAgain = false;
                } catch (KeystoreActionsException e) {
                    printText("*** Cannot backup key: " + e.getMessage() + " ***" + getEolChar() + getEolChar());
                    askAgain = true;
                }
            } else {
                keystoreBean.setShouldBackupMasterKey(false);
                keystoreBean.setMasterKeyBackupPassword(null);
                askAgain = false;
            }
        } while (askAgain);
        doKeystorePasswordPrompts(
                "Set the HSM Password",
                resourceBundle.getString("hsm.initialize.new.password.msg") + ": ",
                ((KeystoreConfigBean)configBean).isInitializeHSM()?resourceBundle.getString("hsm.initialize.confirm.password.msg") + ": ":null);
    }

    private void askLunaKeystoreQuestions() throws IOException, WizardNavigationException {
        doLunaPrompts();
    }

    private Map getValidLunaPaths(final String installPathPrompt, final String defaultInstallPath, final String jspPathPrompt, final String defaultJspPath) throws IOException, WizardNavigationException {
       return consoleWizardUtils.getValidatedDataWithConfirm(
               new String[]{installPathPrompt, jspPathPrompt},
               new String[]{defaultInstallPath, defaultJspPath},
               -1,
               isShowNavigation(),
                new WizardInputValidator() {
                    public String[] validate(Map inputs) {
                        List<String> errorMessages = new ArrayList<String>();
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
                            return errorMessages.toArray(new String[errorMessages.size()]);
                        }
                        return null;
                    }
                },
                false
        );
    }


    private void doLunaPrompts() throws IOException, WizardNavigationException {
        printText("\n-- Luna Install Paths --\n");
        OSSpecificFunctions.KeystoreInfo lunaInfo = osFunctions.getKeystore(KeystoreType.LUNA_KEYSTORE_NAME);

        String defaultInstallPath = lunaInfo.getMetaInfo("INSTALL_DIR");
        String defaultJspPath = lunaInfo.getMetaInfo("JSP_DIR");

        String installPathPrompt = "Enter the Luna installation path: [" + defaultInstallPath +"]";
        String jspPathPrompt = "Enter the path to the luna java service provider: [" + defaultJspPath +"]";

        Map installPaths = getValidLunaPaths(installPathPrompt, defaultInstallPath, jspPathPrompt, defaultJspPath);

        KeystoreConfigBean keystoreBean = (KeystoreConfigBean) configBean;
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
        String input = getData(prompts, defaultValue, new String[] {"1", "2"}, null);

        KeystoreConfigBean keystoreBean = (KeystoreConfigBean) configBean;
        keystoreBean.setDoBothKeys( (input != null && "1".equals(input)));
        doKeystorePasswordPrompts("Keystore Password",
                                  "Enter the keystore password (must be a minimum of 6 characters): ",
                                  "Enter the keystore password again (must match the first password): ");
    }

    private void doKeystorePasswordPrompts(String header, String firstMsg, String secondMsg) throws IOException, WizardNavigationException {
        printText(getEolChar() + "-- " + header + " --" + getEolChar());
        String password = getMatchingPasswords(
                firstMsg,
                secondMsg,
                KeyStoreConstants.PASSWORD_LENGTH
        );

        KeystoreConfigBean keystoreBean = (KeystoreConfigBean) configBean;
        keystoreBean.setKsPassword(password.toCharArray());
    }

    private boolean askDoKeystorePrompts() throws IOException, WizardNavigationException {
        boolean shouldConfigure;

        File keystoreDir = new File(osFunctions.getKeystoreDir());

        String defaultValue = keystoreDir.exists()?"1":"2";
        String [] prompts = new String[] {
            NO_KEYSTORE_PROMPT,
            DO_KEYSTORE_PROMPT,
            "please make a selection: [" + defaultValue + "]",
        };

        String input = getData(prompts, defaultValue, new String[]{"1","2"}, null);

        shouldConfigure = input != null && input.trim().equals("2");

        ((KeystoreConfigBean)configBean).setDoKeystoreConfig(shouldConfigure);
//        getParentWizard().setKeystoreType(KeystoreType.NO_KEYSTORE);

        PartitionInformation pinfo = PartitionManager.getInstance().getActivePartition();
        boolean shouldDisable = true;
        if (shouldConfigure) {
            shouldDisable = false;
        } else {
            if (pinfo != null) {
                if (pinfo.isNewPartition()) {
                    getData(new String[] {
                            getEolChar(),
                            "Warning: You are configuring a new partition without a keystore. \nThis partition will not be able to start without a keystore." + getEolChar(),
                            "Press Enter To Continue" + getEolChar(),
                            getEolChar(),
                    }, "", (String[]) null, null);
                    shouldDisable = true;
                } else if (!new File(osFunctions.getKeystoreDir(), KeyStoreConstants.SSL_KEYSTORE_FILE).exists()) {
                    getData(new String[] {
                            getEolChar(),
                            "Warning: The partition you are configuring does not have a keystore. \nThis partition will not be able to start without a keystore." + getEolChar(),
                            "Press Enter To Continue" + getEolChar(),
                            getEolChar(),
                    }, "", (String[]) null, null);
                    shouldDisable = true;
                } else {
                    getKeystoreInfoFromFileForStorage();
                    shouldDisable = false;
                }
            }
        }
        if (pinfo != null)
            pinfo.setShouldDisable(shouldDisable);
        return shouldConfigure;
    }

    public boolean validateStep() {
        boolean ok;
        KeystoreConfigBean ksBean = (KeystoreConfigBean) configBean;
        if (ksBean.isDoKeystoreConfig()) {
            KeystoreActions ka = new KeystoreActions(osFunctions);
            try {
                byte[] existingRawSharedKey = ka.getSharedKey(this);
                if (existingRawSharedKey != null) {
                    ksBean.setSharedKeyBytes(existingRawSharedKey);
                }
                ok = true;
            } catch (KeystoreActionsException e) {
                ok = false;
                printText("Error while updating the cluster shared key\n" + e.getMessage());
            }
        } else {
            ok = true;
        }
        return ok;
    }

    public List<String> promptForKeystoreTypeAndPassword() {
        String[] prompt = new String[] {
            "Please provide the password for the existing keystore: ",
        };
        List<String> answers = new ArrayList<String>();
        String passwd = null;
        String type = null;
        try {
            passwd = getSecretData(prompt, "", null, null);
            List<String> typePrompts = new ArrayList<String>();
            typePrompts.add("-- Please provide the type for the existing keystore --" + getEolChar());
            typePrompts.add("1) " + KeystoreType.DEFAULT_KEYSTORE_NAME.getShortTypeName() + getEolChar());
            typePrompts.add("2) " + KeystoreType.SCA6000_KEYSTORE_NAME.getShortTypeName() + getEolChar());
            typePrompts.add("3) " + KeystoreType.LUNA_KEYSTORE_NAME.getShortTypeName() + getEolChar());
            typePrompts.add("Please make a selection: [1]");
            String which  = getData(typePrompts.toArray(new String[typePrompts.size()]), "1", new String[] {"1","2","3"},null);
            if ("1".equals(which))
                type = KeystoreType.DEFAULT_KEYSTORE_NAME.getShortTypeName();
            else if ("2".equals(which))
                type = KeystoreType.SCA6000_KEYSTORE_NAME.getShortTypeName();
            else if ("3".equals(which))
                type = KeystoreType.LUNA_KEYSTORE_NAME.getShortTypeName();
            else {
                type = null;
            }
        } catch (IOException e) {
            logger.severe(e.getMessage());
        } catch (WizardNavigationException e) {
            return null;
        }

        answers.add(passwd);
        answers.add(type);
        return answers;
    }

    public void printKeystoreInfoMessage(String msg) {
        printText(msg + getEolChar());
    }

    public void getKeystoreInfoFromFileForStorage() {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(osFunctions.getKeyStorePropertiesFile());
            Properties props = new Properties();
            props.load(fis);

            KeystoreConfigBean keystoreBean = (KeystoreConfigBean)configBean;

            String typeFromFile = props.getProperty(KeyStoreConstants.PROP_KS_TYPE);
            keystoreBean.setKeystoreTypeName(typeFromFile);
            char[] password = props.getProperty(KeyStoreConstants.PROP_CA_KS_PASS).toCharArray();
            keystoreBean.setKsPassword(password);
        } catch (FileNotFoundException e) {
            logger.warning("There was an error while reading the existing keystore information from the partition. " + ExceptionUtils.getMessage(e));
        } catch (IOException e) {
            logger.warning("There was an error while reading the existing keystore information from the partition. " + ExceptionUtils.getMessage(e));
        } finally {
            ResourceUtils.closeQuietly(fis);
        }
    }
}
