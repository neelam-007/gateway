package com.l7tech.gateway.sca.config;

import com.l7tech.common.io.ProcResult;
import com.l7tech.common.io.ProcUtils;
import com.l7tech.config.client.ConfigurationException;
import com.l7tech.gateway.hsm.sca.ScaException;
import com.l7tech.gateway.hsm.sca.ScaManager;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * User: megery
 */
public class ScaHsmKeystoreConfigurator {
    private static final Logger logger = Logger.getLogger(ScaHsmKeystoreConfigurator.class.getName());

    public static final String INITIALIZE_HSM_COMMAND="-initializeHsm";
    public static final String RESTORE_MASTER_KEY_COMMAND="-restoreMasterKey";
    public static final String ENABLE_HSM_COMMAND="-enableHsm";
    public static final String DISABLE_HSM_COMMAND="-disableHsm";
    public static final String CHECK_HSM_STATUS_COMMAND="-checkHsmStatus";

    private static final String SYSPROP_BASE = ScaHsmKeystoreConfigurator.class.getPackage().getName() + ".";
    private static final String PROPERTY_ZEROHSM_PATH = SYSPROP_BASE + "zerohsm";
    private static final String PROPERTY_MASTER_KEY_MANAGE = SYSPROP_BASE + "masterkey_manage";
    private static final String PROPERTY_HOST_PROPERTIES= SYSPROP_BASE + "host_properties";

    private static final String PROPERTY_HOST_SCA_ENABLE = "host.sca";
    private static final String PROPERTY_HOST_SCA_COMMENTS =
            "#\n" +
            "# host.properties file for use with process controller on appliance\n" +
            "# last updated by SCA Configuration Process at {0} ({1})" +
            "#";

    private static final String PROPERTY_SCA_HSMINIT_PASSWORD = "hsm.sca.password";

    private static final String DEFAULT_ZEROHSM_PATH = "/opt/SecureSpan/Appliance/libexec/zerohsm.sh";
    private static final String DEFAULT_MASTERKEYMANAGE_PATH = "/opt/SecureSpan/Appliance/libexec/masterkey-manage.pl";
    private static final String DEFAULT_HOST_PROPERTIES_PATH = "/opt/SecureSpan/Controller/etc/host.properties";

    private static final String SSG_VAR_DIR = "/opt/SecureSpan/Gateway/node/default/var/";
    private static final String HSM_INIT_FILE = "hsm_init.properties";

    private static final int MIN_LENGTH = 6;
    private static final int MAX_LENGTH = 128;


    private File sudo;
    private File zeroHsmExec;
    private File masterkeyManage;
    private File hostPropsFile;

    private ResourceBundle resourceBundle;
    public static final String MASTERKEYMANAGE_ERRMSG_KEY_PREFIX="hsm.masterkeymanage.errormessage";
    public static final String HSMINIT_PASSWORD_KEY_PREFIX="hsm.initialize.password.msg";
    public static final String MKEY_BACKUP_PASSWORD_KEY_PREFIX="hsm.masterkeybackup.password.msg";

    private  static final String MASTER_KEY_BACKUP_FILE_NAME="ssg_mkey.bak";
    private static final String GATEWAY_CONFIG_DIR = "/opt/SecureSpan/Gateway/node/default/etc/conf";


    public ScaHsmKeystoreConfigurator() throws IOException {
        sudo = SudoUtils.findSudo();
        zeroHsmExec = FileUtils.findConfiguredFile("zerohsm.sh", PROPERTY_ZEROHSM_PATH, DEFAULT_ZEROHSM_PATH, true, false, true, false, false);
        masterkeyManage = FileUtils.findConfiguredFile("masterkey-manage.pl", PROPERTY_MASTER_KEY_MANAGE, DEFAULT_MASTERKEYMANAGE_PATH, true, false, true, false, false);
        hostPropsFile = FileUtils.findConfiguredFile("host.properties", PROPERTY_HOST_PROPERTIES, DEFAULT_HOST_PROPERTIES_PATH, true, false, true, true, false);
        resourceBundle = ResourceBundle.getBundle("com.l7tech.gateway.sca.config.hsmconfig");
    }

    public static void main(String[] args) {
        initLogging();

        int status;
        try {
            status = new ScaHsmKeystoreConfigurator().run(args);
        } catch (Throwable e) {
            String msg = "Unable to configure the HSM: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, e);
            System.err.println(msg);
            status = 10;
        }
        System.exit(status);
    }

    private static void initLogging() {
        JdkLoggerConfigurator.configure("com.l7tech.logging", "com/l7tech/gateway/config/client/logging.properties", "configlogging.properties", false, true);
    }

    public int run(String[] args) {
        String command = args[0];

        String errMsg = null;
        int exitCode = 0;
        if (INITIALIZE_HSM_COMMAND.equalsIgnoreCase(command)) {
            try {
                initializeHsmCommand();
            } catch (ExitCodeException e) {
                errMsg = "HSM Initialization failed: " + ExceptionUtils.getMessage(e);
                exitCode = e.getExitCode();
            }
        } else if (RESTORE_MASTER_KEY_COMMAND.equalsIgnoreCase(command)) {
            try {
                restoreMasterKeyCommand();
            } catch (ExitCodeException e) {
                errMsg = "HSM Master Key restore failed: " + ExceptionUtils.getMessage(e);
                exitCode = e.getExitCode();
            }
        } else if (ENABLE_HSM_COMMAND.equalsIgnoreCase(command)) {
            try {
                enableHsm();
            } catch (ExitCodeException e) {
                errMsg = "The HSM could not be enabled: " + ExceptionUtils.getMessage(e);
                exitCode = e.getExitCode();
            }
        } else if (DISABLE_HSM_COMMAND.equalsIgnoreCase(command)) {
            try {
                disableHsm();
            } catch (ExitCodeException e) {
                errMsg = "The HSM could not be disabled: " + ExceptionUtils.getMessage(e);
                exitCode = e.getExitCode();
            }
        } else if (CHECK_HSM_STATUS_COMMAND.equalsIgnoreCase(command)) {
            try {
                if (isHsmEnabled()) exitCode = 0;
                else exitCode = 1;
            } catch (ExitCodeException e) {
                errMsg = "Could not check the enabled status of the HSM: " + ExceptionUtils.getMessage(e);
                exitCode = 2;
            }
        }

        if (errMsg != null && !"".equals(errMsg)) {
            System.out.println(errMsg);
        }
        return exitCode;
    }

    private void initializeHsmCommand() throws ExitCodeException{
        System.out.println("Initializing the HSM");
        logger.info("Initializing the HSM");
        String hsmPassword = null;

        try {
            hsmPassword = getMatchingPasswords(resourceBundle.getString(HSMINIT_PASSWORD_KEY_PREFIX + ".new"),
                    resourceBundle.getString(HSMINIT_PASSWORD_KEY_PREFIX + ".confirm"));
            invokeScaInitialize(hsmPassword);
        } catch (ConfigurationException e) {
            logger.log(Level.SEVERE, "Error while initializing the HSM.", e);
            throw new ExitCodeException("Error while initializing the HSM: " + ExceptionUtils.getMessage(e), 1);
        }
    }

    private void restoreMasterKeyCommand() throws ExitCodeException {
        System.out.println("Restoring the HSM Master Key");
        logger.info("Restoring the HSM Master Key");

        String backupPassword = null;

        backupPassword = promptForPassword(resourceBundle.getString(MKEY_BACKUP_PASSWORD_KEY_PREFIX + ".new"));
        String soPassword = promptForPassword(resourceBundle.getString(HSMINIT_PASSWORD_KEY_PREFIX));

        try {
            invokeRestoreMasterKey(backupPassword, soPassword);
        } catch (ConfigurationException ce) {
            logger.log(Level.SEVERE, "Error while restoring the Master key.", ce);
            throw new ExitCodeException("Error while restoring the Master key: " + ExceptionUtils.getMessage(ce), 1);
        }
    }

    private String getMatchingPasswords(String firstPrompt, String secondPrompt) {

        boolean passwordsMatch;
        String password;

        do {
            password = promptForPassword(firstPrompt);
            String confirm = promptForPassword(secondPrompt);
            passwordsMatch  = checkPasswordMatch(password , confirm);

        } while (!passwordsMatch);
        return password;
    }

    private boolean checkPasswordMatch(String hsmPassword, String confirm) {
        if (hsmPassword.equals(confirm)) {
            return true;
        } else {
            System.out.println("The passwords do not match. Please retry.");
        }
        return false;
    }

    private void invokeScaInitialize(String hsmPassword) throws ConfigurationException {

        System.out.println("Initializing HSM ... Please wait.");
        System.out.println("This may take several minutes, during which time your system may become temporarily unresponsive.");
        try {
            MyScaManager scaManager = getScaManager();
            scaManager.startSca();
            //zero the board
            zeroHsm();
            scaManager.wipeKeydata();
            initializeHSM(hsmPassword.toCharArray());
            saveKeystorePassword(hsmPassword);
        } catch (Exception e) {
            logger.severe("Error while initializing the SCA Manager: " + e.getMessage());
            throw new ConfigurationException("Error while initializing the SCA Manager: " + e.getMessage());
        }
    }

    //saves the obfuscated keystore password to the appropriate location for the gateway to find on startup.
    private void saveKeystorePassword(String hsmPassword) throws ConfigurationException {
        File configDirectory = new File(GATEWAY_CONFIG_DIR);
        File ompFile = new File(configDirectory, "omp.dat");

        final MasterPasswordManager masterPasswordManager =
                new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile).findMasterPassword());
        String hsmPasswordEncrypted = masterPasswordManager.encryptPassword(("gateway:" + hsmPassword).toCharArray());

        final File ssgVarDir = new File(SSG_VAR_DIR);
        final File hsmInitFile = new File(ssgVarDir, HSM_INIT_FILE);

        final Properties hsmInitProps = new Properties();
        hsmInitProps.setProperty(PROPERTY_SCA_HSMINIT_PASSWORD, hsmPasswordEncrypted);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(hsmInitFile);
            hsmInitProps.store(fos,"");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Couldn't write " + SSG_VAR_DIR, e);
            throw new ConfigurationException("Couldn't write " + SSG_VAR_DIR);
        } finally {
            ResourceUtils.closeQuietly(fos);
        }
    }

    private MyScaManager getScaManager() throws ScaException {
        return new MyScaManager();
    }

    private void zeroHsm() throws IOException, ScaException {
        try {
            String[] args = ProcUtils.args(zeroHsmExec.getAbsolutePath());
            logger.finest(MessageFormat.format("Executing {0} {1}", sudo.getAbsolutePath(), zeroHsmExec.getAbsolutePath()));
            ProcResult result = ProcUtils.exec(null, sudo, args, null, true);
            if (result.getExitStatus() == 0) {
                System.out.println("Successfully zeroed the HSM");
                logger.info("Successfully zeroed the HSM");
            } else {
                logger.severe("There was an error trying to zero the HSM: Output=" + String.valueOf(result.getOutput()));
                throw new ScaException("There was an error trying to zero the HSM: Output=" + new String(result.getOutput()));
            }
        } catch (IOException e) {
            logger.severe("There was an error trying to zero the HSM: " + ExceptionUtils.getMessage(e));
            throw e;
        }
    }

    private void initializeHSM(char[] keystorePassword) throws KeystoreActionsException {
        logger.finest(MessageFormat.format("Executing {0} {1} {2} {3}", sudo.getAbsolutePath(), masterkeyManage.getAbsolutePath(), "init", "<keystorePassword>"));
        String[] args = ProcUtils.args(masterkeyManage.getAbsolutePath(), "init", String.valueOf(keystorePassword));
        ProcResult result = null;
        try {
            result = ProcUtils.exec(null, sudo, args, null, true);
            if (result.getExitStatus() != 0) {
                logHSMManagementProblemAndThrow(result, masterkeyManage.getAbsolutePath(), "There was an error while trying to initialize the HSM");
            } else {
                System.out.println("Successfully initialized the HSM.");
                logger.info("Successfully initialized the HSM.");
            }
        } catch (IOException e) {
            logger.severe("Failed to execute the command:" + e.getMessage());
            throw new KeystoreActionsException("There was an error while trying to initialize the HSM: " + e.getMessage());
        }
    }

    private void logHSMManagementProblemAndThrow(ProcResult result, String initializeScript, String messageInThrow) throws KeystoreActionsException {
        String errorMessage = resourceBundle.getString(MASTERKEYMANAGE_ERRMSG_KEY_PREFIX + "." + String.valueOf(result.getExitStatus()));
        if (StringUtils.isEmpty(errorMessage)) {
            errorMessage = "An unexpected error occured. HSM Configuration is not complete.";
        }

        logger.severe(MessageFormat.format("{0} exited with a non zero return code: {1} ({2})",
                initializeScript,
                result.getExitStatus(),
                errorMessage
        ));
        logger.severe(MessageFormat.format("The output of {0} was :\n{1}", initializeScript, new String(result.getOutput())));
        throw new KeystoreActionsException(messageInThrow + ": " + errorMessage);
    }


    public void invokeRestoreMasterKey(String masterKeyBackupPassword, String soPassword) throws ConfigurationException {
        System.out.println("Restoring the HSM Master Key");
        logger.info("Restoring the HSM Master Key");
        try {
            MyScaManager scaManager = getScaManager();

            scaManager.startSca();
            //zero the board
            zeroHsm();

            //delete keydata dir
            logger.info("Cleaning up existing keydata directory.");
            scaManager.wipeKeydata();
            logger.info("Successfully cleaned up existing keydata directory.");

            //TODO: [megery] see if this needed under the new regime
            //replace keydata dir
//            logger.info("Building new keydata directory.");
//            scaManager.saveKeydata(databytes);
//            logger.info("Successfully built new keydata directory.");

            //restore the master key
            restoreHsmMasterkey(masterKeyBackupPassword.toCharArray(), soPassword.toCharArray());

        } catch (Exception e) {
            logger.severe("Error while restoring the HSM master key: " + ExceptionUtils.getMessage(e));
            throw new ConfigurationException("Error while restoring the HSM master key: " + ExceptionUtils.getMessage(e));
        }
    }

    private void restoreHsmMasterkey(final char[] backupPassword, char[] soPassword) throws KeystoreActionsException {
        System.out.println("Attempting to restore the master key.");
        logger.info("Attempting to restore the master key.");
        String[] args = ProcUtils.args(masterkeyManage.getAbsolutePath(), "restore", String.valueOf(soPassword), MASTER_KEY_BACKUP_FILE_NAME, String.valueOf(backupPassword));
        logger.finest(MessageFormat.format("Executing {0} {1} {2} {3} {4} {5}",sudo.getAbsolutePath(), masterkeyManage.getAbsolutePath(), "restore", "<keystorePassword>", MASTER_KEY_BACKUP_FILE_NAME, "<backupPassword>"));
        ProcResult result = null;
        try {
            result = ProcUtils.exec(null, sudo, args, null, true);
            if (result.getExitStatus() != 0) {
                logHSMManagementProblemAndThrow(result, masterkeyManage.getAbsolutePath() ,"There was an error trying to restore the HSM master key. Please ensure that the USB key is attached and that the password is correct.");
            } else {
                System.out.println("Successfully restored the HSM master key");
                logger.info("Successfully restored the HSM master key");
            }
        } catch (IOException e) {
            logger.severe("Failed to execute the command:" + e.getMessage());
            throw new KeystoreActionsException("There was an error trying to restore the HSM master key: " + e.getMessage());
        }
    }

    public void enableHsm() throws ExitCodeException {
        System.out.println("Enabling the HSM");
        logger.info("Enabling the HSM");

        Properties hostProperties;
        try {
            hostProperties = readHostProperties();
        } catch ( ConfigurationException ce ) {
            throw new ExitCodeException(MessageFormat.format("Could not read host configuration: {0}.", ExceptionUtils.getMessage(ce)), 1);
        }

        hostProperties.setProperty(PROPERTY_HOST_SCA_ENABLE, Boolean.toString(true));

        try {
            saveHostProperties(hostProperties, "Enabled SCA");
        } catch (ConfigurationException ce) {
            throw new ExitCodeException(MessageFormat.format("Could not write the host configuration: {0}.", ExceptionUtils.getMessage(ce)), 2);
        }
    }

    private void saveHostProperties(Properties hostProperties, String scaEnabledMsg) throws ConfigurationException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(hostPropsFile);
            hostProperties.store(fos,
                    MessageFormat.format(PROPERTY_HOST_SCA_COMMENTS,
                        Calendar.getInstance().getTime(),
                        "SCA Enabled = " + scaEnabledMsg));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Couldn't write host.properties", e);
            throw new ConfigurationException("Couldn't write host.properties");
        } finally {
            ResourceUtils.closeQuietly(fos);
        }
    }

    private Properties readHostProperties() throws ConfigurationException {
        if (!hostPropsFile.exists()) throw new ConfigurationException(hostPropsFile.getAbsolutePath() + " not found");
        final Properties hostProps = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(hostPropsFile);
            hostProps.load(fis);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Couldn't write host.properties", e);
            throw new ConfigurationException("Couldn't load " + hostPropsFile.getAbsolutePath(), e);
        } finally {
            ResourceUtils.closeQuietly(fis);
        }
        return hostProps;
    }

    private void disableHsm() throws ExitCodeException {
        System.out.println("Disabling the HSM");
        logger.info("Disabling the HSM");

        Properties hostProperties;
        try {
            hostProperties = readHostProperties();
        } catch ( ConfigurationException ce ) {
            throw new ExitCodeException(MessageFormat.format("Could not read host configuration: {0}.", ExceptionUtils.getMessage(ce)), 1);
        }

        hostProperties.setProperty(PROPERTY_HOST_SCA_ENABLE, Boolean.toString(false));

        try {
            saveHostProperties(hostProperties, "Disabled SCA");
        } catch (ConfigurationException ce) {
            throw new ExitCodeException(MessageFormat.format("Could not write the host configuration: {0}.", ExceptionUtils.getMessage(ce)), 2);
        }
    }

    private boolean isHsmEnabled() throws ExitCodeException {
        System.out.println("Checking the if the HSM is enabled.");
        logger.info ("Checking the if the HSM is enabled.");
        Properties hostProperties;
        try {
            hostProperties = readHostProperties();
        } catch ( ConfigurationException ce ) {
            throw new ExitCodeException(MessageFormat.format("Could not read host configuration: {0}.", ExceptionUtils.getMessage(ce)), 1);
        }

        String propVal = hostProperties.getProperty(PROPERTY_HOST_SCA_ENABLE);
        return ( propVal == null ? false : Boolean.valueOf(propVal));
    }

    private int checkHsmInitialized() {
        System.out.println("Check HSM Initialization Status");
        return 1;
    }

     private String promptForPassword( final String prompt ) {
        String password = null;

        Console console = System.console();
        Pattern pattern = Pattern.compile("^.{" + MIN_LENGTH + "," + MAX_LENGTH + "}$", Pattern.DOTALL);
        while( password == null  ) {
            System.out.print( prompt );
            password = new String(console.readPassword());
            if ( !pattern.matcher(password).matches() ) {
                System.out.println( "The password should be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters long.\n" );
                password = null;
            }
        }

        return password;
    }

    private class MyScaManager extends ScaManager {
        public MyScaManager() throws ScaException {
            super();
        }

        public void startSca() throws ScaException, KeystoreActionsException {
            try {
                doStartSca();
            } catch (ScaException e) {
                String message = "Could not start the SCA kiod. Please ensure that the SCA drivers are loaded (/etc/init.d/sca start).";
                throw new KeystoreActionsException(message, e);
            }

        }

        public void stopSca() throws ScaException {
            doStopSca();
        }
    }

    private static final class ExitCodeException extends Exception {
        final int exitCode;

        private ExitCodeException(String message, int exitCode) {
            super(message);
            this.exitCode = exitCode;
        }

        public int getExitCode() {
            return exitCode;
        }
    }
}
