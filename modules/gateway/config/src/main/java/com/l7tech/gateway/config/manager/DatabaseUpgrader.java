package com.l7tech.gateway.config.manager;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.DefaultMasterPasswordFinder;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.util.BuildInfo;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.gateway.config.manager.db.DBActionsListener;
import com.l7tech.gateway.config.client.options.OptionType;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.Properties;
import java.net.PasswordAuthentication;

/**
 * Prompts the user to confirm database upgrade.
 */
public class DatabaseUpgrader {
    private static final Logger logger = Logger.getLogger(DatabaseUpgrader.class.getName());
    private static final String CONFIG_PATH = "../node/default/etc/conf";

    public static void main(String[] args) {
        JdkLoggerConfigurator.configure("com.l7tech.logging", "com/l7tech/gateway/config/client/logging.properties", "configlogging.properties", false, true);
        try {
            new DatabaseUpgrader().run();
        } catch (Throwable e) {
            String msg = "Unable to upgrade database due to error '" + ExceptionUtils.getMessage(e) + "'.";
            logger.log(Level.WARNING, msg, e);
            System.err.println(msg);
            System.exit(1);
        }
    }

    private void exitOnQuit( final String perhapsQuit ) {
        if ( "quit".equals(perhapsQuit) ) {
            System.exit(1);
        }
    }

    private void run() throws IOException, SAXException {
        String configurationDirPath = CONFIG_PATH;
        File confDir = new File(configurationDirPath);

        System.out.println("SecureSpan Gateway Database Upgrader.");
        System.out.println("Enter 'quit' to exit at any time.");
        System.out.println();

        DatabaseConfig config;
        Properties props = new Properties();
        File ompFile = new File(confDir, "omp.dat");
        File nodePropsFile = new File(confDir, "node.properties");
        if ( nodePropsFile.exists() && ompFile.exists() ) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream( nodePropsFile );
                props.load( fis );
            } finally {
                ResourceUtils.closeQuietly(fis);
            }

            String databaseHost = props.getProperty("node.db.host");
            String databasePort = props.getProperty("node.db.port");
            String databaseName = props.getProperty("node.db.name");
            String databaseUser = props.getProperty("node.db.user");
            String databasePass = props.getProperty("node.db.pass");

            MasterPasswordManager decryptor = new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile).findMasterPassword());
            databasePass = new String(decryptor.decryptPasswordIfEncrypted(databasePass));

            config = new DatabaseConfig( databaseHost, Integer.parseInt(databasePort), databaseName, databaseUser, databasePass );
        } else {
            System.out.println("Configuration files not found, cannot perform upgrade.");
            System.exit(3);
            return;
        }

        DBActions dba = new DBActions();
        String swVersion = BuildInfo.getFormalProductVersion();
        String dbVersion = dba.checkDbVersion( config );
        if ( dbVersion == null ) {
            System.out.println("Unable to determine database version, cannot upgrade.");
            System.exit(4);
        } else if ( !dbVersion.equals(swVersion) ) {
            System.out.println("Database upgrade is required.");
            System.out.println();
            System.out.println(" Software version : " + swVersion);
            System.out.println(" Database version : " + dbVersion);
            System.out.println();

            boolean upgrade = false;
            boolean confirmed = false;
            while( !confirmed ) {
                System.out.print("Perform upgrade? [false]: ");

                String response = System.console().readLine().trim();
                exitOnQuit(response);
                if ( Pattern.matches(OptionType.BOOLEAN.getDefaultRegex(), response)) {
                    confirmed = true;
                    upgrade =
                        response.toLowerCase().startsWith("t") ||
                        response.toLowerCase().startsWith("y");
                } else if ( "".equals(response) ) {
                    break;
                } else {
                    System.out.println();
                    System.out.println("Invalid choice '"+response+"', options are true/false.");
                    System.out.println();
                }
            }

            if ( upgrade ) {
                dba.upgradeDbSchema( config, dbVersion, swVersion, "etc/sql/ssg.sql", new DBActionsListener(){
                    public void showErrorMessage(String errorMsg) {}
                    public void hideErrorMessage() {}
                    public boolean getOverwriteConfirmationFromUser(String dbName) { return false; }
                    public boolean getGenericUserConfirmation(String msg) { return false; }
                    public PasswordAuthentication getPrivelegedCredentials(String message, String usernamePrompt, String passwordPrompt, String defaultUsername) {return null;}
                    public void showSuccess(String message) {
                        System.out.print(message);
                    }
                } );
            } else {
                System.out.println("Database upgrade is required, but was declined.");
            }
        } else {
            System.out.println("Database upgrade not required.");
        }
    }
}