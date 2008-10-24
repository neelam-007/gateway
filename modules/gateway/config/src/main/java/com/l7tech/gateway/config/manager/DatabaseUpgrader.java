package com.l7tech.gateway.config.manager;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.DefaultMasterPasswordFinder;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.CausedIOException;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.gateway.config.manager.db.DBActionsListener;
import com.l7tech.gateway.config.client.options.OptionType;

import java.io.File;
import java.io.IOException;
import java.io.Console;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
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

    private static void exitOnQuit( final String perhapsQuit ) {
        if ( "quit".equals(perhapsQuit) ) {
            System.exit(1);
        }
    }

    private static String fallbackReadLine( final Console console, final BufferedReader reader, final String defaultValue ) throws IOException {
        String line;

        if ( console != null ) {
            line = console.readLine();
        } else {
            line = reader.readLine();
        }

        exitOnQuit( line );

        if ( line == null || line.trim().isEmpty() ) {
            line = defaultValue;
        }

        return line;
    }

    private static String fallbackReadPassword( final Console console, final BufferedReader reader ) throws IOException {
        String line;

        if ( console != null ) {
            line = new String(console.readPassword());
        } else {
            line = reader.readLine();
        }

        exitOnQuit( line );

        return line;
    }

    private void run() throws IOException {
        Console console = System.console();
        BufferedReader reader = new BufferedReader( new InputStreamReader( System.in ) );
        String configurationDirPath = CONFIG_PATH;
        File confDir = new File(configurationDirPath);

        System.out.println("SecureSpan Gateway Database Upgrader.");
        System.out.println("Enter 'quit' to exit at any time.");
        System.out.println();

        DatabaseConfig config;
        File ompFile = new File(confDir, "omp.dat");
        File nodePropsFile = new File(confDir, "node.properties");
        if ( nodePropsFile.exists() && ompFile.exists() ) {
            NodeConfig nodeConfig = NodeConfigurationManager.loadNodeConfig("default", true);
            config = nodeConfig.getDatabase( DatabaseType.NODE_ALL, NodeConfig.ClusterType.STANDALONE, NodeConfig.ClusterType.REPL_MASTER );
            if ( config == null ) {
                throw new CausedIOException("Database configuration not found.");
            }

            MasterPasswordManager decryptor = new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile).findMasterPassword());
            config.setNodePassword( new String(decryptor.decryptPasswordIfEncrypted(config.getNodePassword())) );
        } else {
            config = new DatabaseConfig();

            System.out.print("Enter database host [localhost]: ");
            config.setHost( fallbackReadLine( console, reader, "localhost" ) );

            System.out.print("Enter database port [3306]: ");
            config.setPort( Integer.parseInt(fallbackReadLine( console, reader, "3306" )) );

            System.out.print("Enter database name [ssg]: ");
            config.setName( fallbackReadLine( console, reader, "ssg" ) );

            System.out.print("Enter database username [gateway]: ");
            config.setNodeUsername( fallbackReadLine( console, reader, "gateway" ) );

            System.out.print("Enter database password: ");
            config.setNodePassword( fallbackReadPassword( console, reader ) );
        }

        logger.info("Using database host '" + config.getHost() + "'.");
        logger.info("Using database port '" + config.getPort() + "'.");
        logger.info("Using database name '" + config.getName() + "'.");
        logger.info("Using database user '" + config.getNodeUsername() + "'.");

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