package com.l7tech.server.config.ui.console;

import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.server.config.*;
import com.l7tech.server.config.beans.KeystoreConfigBean;
import com.l7tech.server.config.commands.KeystoreConfigCommand;
import com.l7tech.server.config.exceptions.KeystoreActionsException;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;

import javax.crypto.BadPaddingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 9:59:35 AM
 */
public class ConfigWizardConsoleKeystoreStep extends BaseConsoleStep<KeystoreConfigBean, KeystoreConfigCommand> implements KeystoreActionsListener {

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
            configBean.setHostname(getParentWizard().getHostname());
            configBean.setDbInformation(SharedWizardInfo.getInstance().getDbinfo());
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

        KeystoreConfigBean keystoreBean = configBean;
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

            KeystoreConfigBean keystoreBean = configBean;
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

        KeystoreConfigBean keystoreBean = configBean;
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

            KeystoreConfigBean keystoreBean = configBean;
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
                ConfigurationType configType = SharedWizardInfo.getInstance().getConfigType();
                if (configType == ConfigurationType.CONFIG_CLUSTER) {
                    printText(getEolChar() + "You are configuring the first node in a cluster to use an HSM" + getEolChar() +
                              "but are not backing up the master key." + getEolChar() + getEolChar());
                    printText("Subsequent nodes in the cluster will need the master key in order" + getEolChar()  +
                              "to join the cluster." + getEolChar() + getEolChar());

                    boolean foundAFob;
                    do {
                        getData(
                                new String[] {
                                        "Please insert the USB Backup device and press Enter to continue: ",
                                },
                                "",
                                (String[]) null,
                                null
                        );
                        printText(getEolChar());
                        KeystoreActions ka = new KeystoreActions(osFunctions);
                        try {
                            ka.probeUSBBackupDevice();
                            foundAFob = true;
                        } catch (KeystoreActionsException e) {
                            printText("*** Cannot backup key: " + e.getMessage() + " ***" + getEolChar() + getEolChar());
                            foundAFob = false;
                        }
                    } while (!foundAFob);

                    String backupPassword = getMatchingPasswords("Enter the password to protect the master key backup:", "Confirm the password to protect the master key backup:", 6);
                    keystoreBean.setMasterKeyBackupPassword(backupPassword.toCharArray());
                    keystoreBean.setShouldBackupMasterKey(true);
                } else {
                    keystoreBean.setMasterKeyBackupPassword(null);
                    keystoreBean.setShouldBackupMasterKey(false);
                }
                askAgain = false;
            }
        } while (askAgain);
        doKeystorePasswordPrompts(
                "Set the HSM Password",
                resourceBundle.getString("hsm.initialize.new.password.msg") + ": ",
                configBean.isInitializeHSM()?resourceBundle.getString("hsm.initialize.confirm.password.msg") + ": ":null);
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

        KeystoreConfigBean keystoreBean = configBean;
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
        configBean.setDoBothKeys( (input != null && "1".equals(input)));

        defaultValue = "1";
        prompts = new String[] {
                "\n1) Generate new SSL key\n",
                "2) Import an external SSL key from a PKCS#12 file\n",
                "Please make a selection: [" + defaultValue + "] ",
        };
        input = getData(prompts, defaultValue, new String[] {"1", "2"}, null);
        if ("2".equals(input)) {
            doImportExternalSslKey();
        }

        doKeystorePasswordPrompts("Gateway Keystore Password",
                                  "Enter the keystore password (must be a minimum of 6 characters): ",
                                  "Enter the keystore password again (must match the first password): ");
    }

    private void doImportExternalSslKey() throws IOException, WizardNavigationException {
        KeyStore.PrivateKeyEntry keyEntry;
        do {
            File ksfile = promptForExternalSslKeystoreFile();
            String passphrase = consoleWizardUtils.getSecretData(
                    new String[] { "Enter passphrase for this PKCS#12 file: " },
                    "changeme",
                    true,
                    null,
                    "");

            keyEntry = doLoadImportedPrivateKey(ksfile, passphrase);
        } while (keyEntry == null);

        configBean.setImportedSslKey(keyEntry);
    }

    private KeyStore.PrivateKeyEntry doLoadImportedPrivateKey(File ksfile, String passphrase) throws IOException, WizardNavigationException {
        String errorDetail;
        try {
            return CertUtils.loadPrivateKey(
                    new CertUtils.FileInputStreamFactory(ksfile),
                    "PKCS12",
                    passphrase.toCharArray(),
                    makeConsoleAliasPicker(),
                    passphrase.toCharArray());
        } catch (IOException e) {
            if (ExceptionUtils.causedBy(e, BadPaddingException.class)) {
                errorDetail = "Incorrect passphrase or damaged file";
            } else
                errorDetail = ExceptionUtils.getMessage(e);
        } catch (KeyStoreException e) {
            errorDetail = ExceptionUtils.getMessage(e);
        } catch (NoSuchAlgorithmException e) {
            errorDetail = ExceptionUtils.getMessage(e);
        } catch (CertificateException e) {
            errorDetail = ExceptionUtils.getMessage(e);
        } catch (CertUtils.AliasNotFoundException e) {
            // Is this actually a wrapped console navigatio request from our alias picker?
            WizardNavigationException wne = ExceptionUtils.getCauseIfCausedBy(e, WizardNavigationException.class);
            if (wne != null) throw wne;

            // Is this actually a wrapped console I/O error from our alias picker?
            IOException ioe = ExceptionUtils.getCauseIfCausedBy(e, IOException.class);
            if (ioe != null) throw ioe;

            errorDetail = ExceptionUtils.getMessage(e);
        } catch (UnrecoverableKeyException e) {
            errorDetail = "Incorrect passphrase";
        }

        if (errorDetail != null)
            consoleWizardUtils.printText("Unable to import SSL key: " + errorDetail + "\n");
        return null;
    }

    private File promptForExternalSslKeystoreFile() throws IOException, WizardNavigationException {
        File ksfile;
        boolean done = false;
        do {
            String kspath = consoleWizardUtils.getData(
                    new String[] { "Enter path to PKCS#12 file to import: " },
                    "/tmp/import.p12",
                    true,
                    (Pattern) null,
                    "Not a valid pathname");
            ksfile = new File(kspath);
            if (!ksfile.exists())
                consoleWizardUtils.printText("File does not exist\n");
            else if (!ksfile.isFile())
                consoleWizardUtils.printText("File is not a plain file\n");
            else if (!ksfile.canRead())
                consoleWizardUtils.printText("File is not readable by this configuration wizard process\n");
            else
                done = true;
        } while (!done);
        return ksfile;
    }

    private CertUtils.AliasPicker makeConsoleAliasPicker() {
        return new CertUtils.AliasPicker() {
            public String selectAlias(String[] options) throws CertUtils.AliasNotFoundException {
                try {
                    List<String> prompts = new ArrayList<String>();
                    List<String> allowed = new ArrayList<String>();
                    prompts.add("\nChoose alias of key to import:\n");
                    String defaultValue = "1";
                    for (int i = 0; i < options.length; i++) {
                        String option = options[i];
                        String num = Integer.toString(i + 1);
                        prompts.add(num + ") " + option + "\n");
                        allowed.add(num);
                    }
                    prompts.add("Please make a selection: [" + defaultValue + "] ");
                    String input = getData(prompts.toArray(new String[prompts.size()]),
                            defaultValue,
                            allowed.toArray(new String[allowed.size()]),
                            null);
                    int gotnum = Integer.parseInt(input.trim());
                    return options[gotnum - 1];
                } catch (IOException e) {
                    throw new CertUtils.AliasNotFoundException(e);
                } catch (WizardNavigationException e) {
                    throw new CertUtils.AliasNotFoundException(e);
                }
            }
        };
    }

    private void doKeystorePasswordPrompts(String header, String firstMsg, String secondMsg) throws IOException, WizardNavigationException {
        printText(getEolChar() + "-- " + header + " --" + getEolChar());
        String password = getMatchingPasswords(
                firstMsg,
                secondMsg,
                KeyStoreConstants.PASSWORD_LENGTH
        );

        KeystoreConfigBean keystoreBean = configBean;
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

        configBean.setDoKeystoreConfig(shouldConfigure);

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
        KeystoreConfigBean ksBean = configBean;
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

            KeystoreConfigBean keystoreBean = configBean;

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
