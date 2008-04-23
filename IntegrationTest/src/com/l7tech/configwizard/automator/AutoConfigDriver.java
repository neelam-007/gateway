package com.l7tech.configwizard.automator;

import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.SilentConfigData;
import com.l7tech.server.config.SilentConfigurator;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.db.DBInformation;
import com.l7tech.server.config.db.DBActions;
import com.l7tech.server.config.db.DBActionsListener;
import com.l7tech.server.config.ui.console.SoftwareConfigWizard;
import com.l7tech.server.config.ui.console.ConfigurationWizard;
import com.l7tech.server.partition.PartitionManager;

import java.util.Map;
import java.util.HashMap;

/**
 * User: jules
 * Date: Apr 1, 2008
 * Time: 3:32:10 PM
 */
public class AutoConfigDriver {

    private static final String DEFAULT_DB_HOSTNAME = "localhost";
    private static final String DEFAULT_DB_NAME = "ssg_integrationtest";
    private static final String DEFAULT_DB_USERNAME = "gateway";
    private static final String DEFAULT_DB_PASSWORD = "7layer";
    private static final String DEFAULT_DB_PRIVILEGED_USERNAME = "root";
    private static final String DEFAULT_DB_PRIVILEGED_PASSWORD = "";

    private static final String DEFAULT_FILENAME = "cfg_data.xml";

    private SoftwareConfigWizard configWiz;

    public static void main(String[] args) {


        System.setProperty("com.l7tech.config.silentConfigFile", DEFAULT_FILENAME);
        System.setProperty("com.l7tech.config.silentConfigFile.dontencrypt", "true");

        System.out.println("Using system properties:");
        System.out.println("Property: com.l7tech.config.silentConfigFile, Value: " + System.getProperty("com.l7tech.config.silentConfigFile"));
        System.out.println("Property: com.l7tech.config.silentConfigFile.dontencrypt, Value: " + System.getProperty("com.l7tech.config.silentConfigFile.dontencrypt"));

        new AutoConfigDriver().run();
    }

    public AutoConfigDriver() {
        configWiz = new SoftwareConfigWizard(System.in, System.out);
    }

    private void run() {
        System.out.println("\nMigrating partition template to default_ partition.");
        PartitionManager.doMigration(true);

        OSSpecificFunctions osf = OSDetector.getOSSpecificFunctions();

        try {
            updateDatabase(osf);
            applyConfiguration(osf);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private DBInformation getDefaultDBInfo() {
        return new DBInformation(
                DEFAULT_DB_HOSTNAME,
                DEFAULT_DB_NAME,
                DEFAULT_DB_USERNAME,
                DEFAULT_DB_PASSWORD,
                DEFAULT_DB_PRIVILEGED_USERNAME,
                DEFAULT_DB_PRIVILEGED_PASSWORD);
    }

    private void applyConfiguration(OSSpecificFunctions osf) throws Exception {

        System.out.println("Applying configuration.");

        SilentConfigurator silentConfig = new SilentConfigurator(osf);
        DBInformation dbInfo = getDefaultDBInfo();

        byte[] configBytes;
        SilentConfigData silentConfigData;
        //this actually loads the configuration from a file not the DB
        //DBInformation is not actually used, its just needed to keep 'loadConfigFromDb' happy
        configBytes = silentConfig.loadConfigFromDb(dbInfo);

        // upgrade the database using DBActionsigData object.");
        //the passphrased passed to 'decryptConfigSettings' is also never used
        silentConfigData = silentConfig.decryptConfigSettings("neverUsed".toCharArray(), configBytes);

        //pass the ConfigurationWizard the SilentConfigData, then apply
        configWiz.setSilentConfigData(silentConfigData);

        configWiz.applyConfiguration();
    }

    private void updateDatabase(OSSpecificFunctions osf) throws Exception {

        DBActions dba = new DBActions(osf);

        DBInformation dbInfo = getDefaultDBInfo();

        String version = ConfigurationWizard.getCurrentVersion();
        System.out.println("\nUpgrading DB to version " + version + " if necessary.");

        boolean success = dba.doExistingDb(
                dbInfo.getDbName(),
                dbInfo.getHostname(),
                dbInfo.getUsername(),
                dbInfo.getPassword(),
                dbInfo.getPrivUsername(),
                dbInfo.getPrivPassword(),
                version,
                new DBActionsListener() {
                    public void showErrorMessage(String s) {
                        System.out.println(s);
                    }

                    public void hideErrorMessage() {
                    }

                    public boolean getOverwriteConfirmationFromUser(String s) {
                        return true;
                    }

                    public void showSuccess(String s) {
                        System.out.println(s);
                    }

                    public boolean getGenericUserConfirmation(String s) {
                        System.out.println(s);
                        return true;
                    }

                    public char[] getPrivilegedPassword() {
                        return AutoConfigDriver.DEFAULT_DB_PRIVILEGED_PASSWORD.toCharArray();
                    }

                    public String getPrivilegedUsername(String s) {
                        System.out.println(s);
                        return AutoConfigDriver.DEFAULT_DB_PRIVILEGED_USERNAME;
                    }

                    public Map<String, String> getPrivelegedCredentials(String message, String usernamePrompt, String passwordPrompt, String defaultUsername) {
                        System.out.println(message);
                        HashMap<String, String> credsMap = new HashMap<String, String>();
                        credsMap.put(DBActions.USERNAME_KEY, AutoConfigDriver.DEFAULT_DB_PRIVILEGED_USERNAME);
                        credsMap.put(DBActions.PASSWORD_KEY, AutoConfigDriver.DEFAULT_DB_PRIVILEGED_PASSWORD);

                        return credsMap;
                    }
                });

        if(!success){
            System.out.println("Error: DB Upgrade failed in AutoConfigDriver.");
        }
    }
}
