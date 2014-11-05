package com.l7tech.gateway.config.manager;

import com.l7tech.common.password.PasswordHasher;
import com.l7tech.common.password.Sha512CryptPasswordHasher;
import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.util.*;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility to allow creation of administrative user account. If the account already exists, then various fields
 * including the password will not be modifed and the existing values will be favoured.
 */
public class AccountReset {
    private static final Logger logger = Logger.getLogger(AccountReset.class.getName());
    private static final String CONFIG_PATH = "../node/default/etc/conf";
    private static final int PASSWORD_EXPIRY_DAYS = 90;
    private static final PasswordHasher passwordHasher = new Sha512CryptPasswordHasher();
    private static boolean updateExistingAccount = false;

    public static void main(String[] args) {
        JdkLoggerConfigurator.configure("com.l7tech.logging", "com/l7tech/gateway/config/client/logging.properties", "configlogging.properties");
        updateExistingAccount = (args!=null && args.length>0 && (args[0].equalsIgnoreCase("-passwordReset")));
        try {
            new AccountReset().run();
        } catch (Throwable e) {
            String msg = (updateExistingAccount) ? "Unable to update admin account due to error '" + ExceptionUtils.getMessage(e) + "'.":
                                                    "Unable to create admin account due to error '" + ExceptionUtils.getMessage(e) + "'.";
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
                    "INSERT INTO internal_user (goid,version,name,login,password,first_name,last_name,email,description,expiration,password_expiry) " +
                            "VALUES (?,1,?,?,?,'','','','',-1,?) " +
                            "ON DUPLICATE KEY UPDATE login=values(login), name=values(name), version=version+1, password=values(password), expiration=-1, password_expiry=values(password_expiry), change_password=0;"
            );
            statement.setBytes(1,(new Goid(0,3).getBytes()));
            statement.setString(2, accountname);
            statement.setString(3, accountname);
            statement.setString(4, passwordHasher.hashPassword(accountPassword.getBytes(Charsets.UTF8)));
            statement.setLong(5, TimeUnit.DAYS.toMillis(PASSWORD_EXPIRY_DAYS) + System.currentTimeMillis());
            statement.executeUpdate();
        } catch ( SQLException se ) {
            throw new CausedIOException( se.getMessage(), se );
        } finally {
            ResourceUtils.closeQuietly(statement);
            ResourceUtils.closeQuietly(connection);
        }
        System.out.println( "Account reset successfully." );
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

        if ( line == null ) line = "";

        exitOnQuit( line );

        return line;
    }

    private static String fallbackReadPassword( final Console console, final BufferedReader reader ) throws IOException {
        String line;

        if ( console != null ) {
            line = TextUtils.string(console.readPassword());
        } else {
            line = reader.readLine();
            if ( line == null ) line = "";
        }

        exitOnQuit( line );

        return line;
    }

    private void run() throws IOException {
        Console console = System.console();
        BufferedReader reader = new BufferedReader( new InputStreamReader( System.in ) );
        String configurationDirPath = CONFIG_PATH;
        File confDir = new File(configurationDirPath);

        System.out.println("Gateway Administration Account Utility.");
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

            MasterPasswordManager decryptor = new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile));
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

        if (name==null || pass==null || name.isEmpty() || pass.isEmpty()){
            System.out.println("The administrative username and password cannot be empty");
            return;
        }

        if (updateExistingAccount) {
            updateAccount(config, name, pass);
        }else {
            resetAccount(config, name, pass);
        }
    }

    private void updateAccount(DatabaseConfig config, String name, String pass)  throws IOException {
        DBActions dba = new DBActions();
        Connection connection = null;
        PreparedStatement statement1=null;
        PreparedStatement statement2=null;
        try {
            connection = dba.getConnection( config, false );
            statement1 = connection.prepareStatement("select login FROM internal_user WHERE login=?");
            statement1.setString(1, name);
            ResultSet resultSet = statement1.executeQuery();
            if (!resultSet.next()){
                System.out.println(String.format("The administrative username, '%s', does not exist.", name));
                return;
            }

            statement2 = connection.prepareStatement("UPDATE internal_user SET password=? , password_expiry=? WHERE login=?");
            statement2.setString(1, passwordHasher.hashPassword(pass.getBytes(Charsets.UTF8)));
            statement2.setLong(2, TimeUnit.DAYS.toMillis(PASSWORD_EXPIRY_DAYS) + System.currentTimeMillis());
            statement2.setString(3, name);
            statement2.executeUpdate();

        } catch ( SQLException se ) {
            throw new CausedIOException( se.getMessage(), se );
        } finally {
            ResourceUtils.closeQuietly(statement1);
            ResourceUtils.closeQuietly(statement2);
            ResourceUtils.closeQuietly(connection);
        }
        System.out.println(String.format( "The administrative user, '%s',  password reset successfully." , name));
    }

}