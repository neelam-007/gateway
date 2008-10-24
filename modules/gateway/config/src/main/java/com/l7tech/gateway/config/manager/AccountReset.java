package com.l7tech.gateway.config.manager;

import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.DefaultMasterPasswordFinder;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.TimeUnit;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.gateway.config.manager.db.DBActions;

import java.io.IOException;
import java.io.File;
import java.io.Console;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Utility to allow creation of administrative user account.
 */
public class AccountReset {
    private static final Logger logger = Logger.getLogger(AccountReset.class.getName());
    private static final String CONFIG_PATH = "../node/default/etc/conf";
    private static final int PASSWORD_EXPIRY_DAYS = 90;

    public static void main(String[] args) {
        JdkLoggerConfigurator.configure("com.l7tech.logging", "com/l7tech/gateway/config/client/logging.properties", "configlogging.properties", false, true);
        try {
            new AccountReset().run();
        } catch (Throwable e) {
            String msg = "Unable to create admin account due to error '" + ExceptionUtils.getMessage(e) + "'.";
            logger.log(Level.WARNING, msg, e);
            System.err.println(msg);
            System.exit(1);
        }
    }

    public static void resetAccount( final DatabaseConfig config, final String accountname, final String accountPassword ) throws IOException {
        DBActions dba = new DBActions();
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = dba.getConnection( config, false );
            statement = connection.prepareStatement(
                    "INSERT INTO internal_user (objectid,version,name,login,password,first_name,last_name,email,description,expiration,password_expiry) " +
                    "VALUES (3,1,?,?,?,'','','','',-1,?) " +
                    "ON DUPLICATE KEY UPDATE login=values(login), name=values(name), version=version+1, password=values(password), expiration=-1, password_expiry=values(password_expiry), change_password=0;"
            );
            statement.setString(1, accountname);
            statement.setString(2, accountname);
            statement.setString(3, HexUtils.encodePasswd(accountname, accountPassword, "L7SSGDigestRealm"));
            statement.setLong(4, TimeUnit.DAYS.toMillis(PASSWORD_EXPIRY_DAYS) + System.currentTimeMillis());
            statement.executeUpdate();
        } catch ( SQLException se ) {
            throw new CausedIOException( se.getMessage(), se );
        } finally {
            ResourceUtils.closeQuietly(statement);
            ResourceUtils.closeQuietly(connection);
        }

    }

    private static void exitOnQuit( final String perhapsQuit ) {
        if ( "quit".equals(perhapsQuit) ) {
            System.exit(1);
        }
    }

    private static String fallbackReadLine( final Console console, final BufferedReader reader ) throws IOException {
        String line;

        if ( console != null ) {
            line = console.readLine();
        } else {
            line = reader.readLine();
        }

        exitOnQuit( line );

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

        System.out.println("SecureSpan Gateway Administration Account Utility.");
        System.out.println("Enter 'quit' to exit at any time.");
        System.out.println();

        DatabaseConfig config;
        File ompFile = new File(confDir, "omp.dat");
        File nodePropsFile = new File(confDir, "node.properties");
        if ( nodePropsFile.exists() && ompFile.exists() ) {
            NodeConfig nodeConfig = NodeConfigurationManager.loadNodeConfig("default", nodePropsFile, true);
            config = nodeConfig.getDatabase( DatabaseType.NODE_ALL, NodeConfig.ClusterType.STANDALONE, NodeConfig.ClusterType.REPL_MASTER );
            if ( config == null ) {
                throw new CausedIOException("Database configuration not found.");
            }

            logger.info("Using database host '" + config.getHost() + "'.");
            logger.info("Using database port '" + config.getPort() + "'.");
            logger.info("Using database name '" + config.getName() + "'.");
            logger.info("Using database user '" + config.getNodeUsername() + "'.");

            MasterPasswordManager decryptor = new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile).findMasterPassword());
            config.setNodePassword( new String(decryptor.decryptPasswordIfEncrypted(config.getNodePassword())) );
        } else {
            System.out.println("Configuration files not found, cannot reset administration account.");
            System.exit(3);
            return;
        }

        System.out.print("Enter the administrative username: ");
        String name = fallbackReadLine( console, reader );

        System.out.print("Enter the administrative password: ");
        String pass = fallbackReadPassword( console, reader );

        resetAccount( config, name, pass );
        System.out.println( "Account reset successfully." );
    }
}