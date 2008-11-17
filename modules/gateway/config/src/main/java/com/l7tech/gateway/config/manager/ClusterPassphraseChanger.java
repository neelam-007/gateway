package com.l7tech.gateway.config.manager;

import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.DefaultMasterPasswordFinder;
import com.l7tech.util.HexUtils;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.Console;
import java.io.FileNotFoundException;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/**
 * Prompts the user to change the cluster passphrase.
 * 
 * <p>This will update the encrypted shared key in the DB and also the
 * node.properties file for the new cluster passphrase.</p>
 */
public class ClusterPassphraseChanger {
    private static final Logger logger = Logger.getLogger(ClusterPassphraseChanger.class.getName());
    private static final int MIN_LENGTH = 6;
    private static final int MAX_LENGTH = 128;
    private static final String CIPHER = "PBEWithSHA1AndDESede";
    private static final String DEFAULT_CONFIG_PATH = "../node/{0}/etc/conf";
    private static final String DEFAULT_NODE = "default";

    private static final String NODE = SyspropUtil.getString("com.l7tech.config.node", DEFAULT_NODE);
    private static final String CONFIG_PATH = SyspropUtil.getString("com.l7tech.config.path", DEFAULT_CONFIG_PATH);
    
    public static void main( final String[] args ) {
        JdkLoggerConfigurator.configure("com.l7tech.logging", "com/l7tech/gateway/config/client/logging.properties", "configlogging.properties", false, true);
        try {
            new ClusterPassphraseChanger().run( MessageFormat.format( CONFIG_PATH, NODE ) );
        } catch (Throwable e) {
            String msg = "Unable to change cluster passphrase: " + ExceptionUtils.getMessage(e);
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

    private void run( final String configurationDirPath ) throws IOException, SAXException {
        File configDirectory = new File( configurationDirPath );
        File ompFile = new File( configDirectory, "omp.dat" );
        if ( !ompFile.exists() ) {
            throw new FileNotFoundException( "Node is not configured (missing obfuscated master password)." );
        }

        DatabaseConfig config;
        File nodePropsFile = new File( configDirectory, "node.properties" );
        if ( nodePropsFile.exists() ) {
            NodeConfig nodeConfig = NodeConfigurationManager.loadNodeConfig( NODE, true );
            config = nodeConfig.getDatabase( DatabaseType.NODE_ALL, NodeConfig.ClusterType.STANDALONE, NodeConfig.ClusterType.REPL_MASTER );
            if ( config == null ) {
                throw new CausedIOException("Database configuration not found.");
            }
        } else {
            throw new FileNotFoundException( "Node is not configured." );
        }

        // load and decrypt DB connnection info
        final MasterPasswordManager masterPasswordManager =
            new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile).findMasterPassword());
        config.setNodePassword( new String(masterPasswordManager.decryptPasswordIfEncrypted(config.getNodePassword())) );


        logger.info( "Using database host '" + config.getHost() + "'." );
        logger.info( "Using database port '" + config.getPort() + "'." );
        logger.info( "Using database name '" + config.getName() + "'." );
        logger.info( "Using database user '" + config.getNodeUsername() + "'." );        
        
        // Read shared key from DB
        String sharedKeyB64 = readSharedKeyB64(config);
        if ( sharedKeyB64 == null ) {
            throw new CausedIOException( "Shared key not found." );
        }

        // Get existing pass and validate against DB
        byte[] sharedKey = null;
        while ( sharedKey == null ) {
            String existingClusterPass = promptForPassword("Enter the cluster passphrase, or 'quit' to quit): ");
            SecretKey sharedKeyDecryptionKey; 
            try {
                SecretKeyFactory skf = SecretKeyFactory.getInstance(CIPHER);
                sharedKeyDecryptionKey = skf.generateSecret(new PBEKeySpec(existingClusterPass.toCharArray()));
            } catch (GeneralSecurityException e) {
                throw new RuntimeException("Unable to initialize decryption key for cluster shared key: " + ExceptionUtils.getMessage(e), e);
            }       

            try {
                sharedKey = decryptKey( sharedKeyB64, sharedKeyDecryptionKey );
                //TODO [steve] need to validate this key (decrypt a keystore?)
            } catch ( GeneralSecurityException gse ) {
                logger.log( Level.INFO, "Unable to decrypt shared key (incorrect password?)", ExceptionUtils.getDebugException(gse) );
                System.out.println( "Incorrect cluster passphrase, please try again." );
            }
        }
        
        // Get new pass
        String newClusterPass;
        boolean matched;
        do {
            newClusterPass = promptForPassword("Enter the new cluster passphrase (" + MIN_LENGTH + " - " + MAX_LENGTH + " characters, 'quit' to quit): ");
            String confirm = promptForPassword("Confirm new cluster passphrase ('quit' to quit): ");
            matched = confirm.equals( newClusterPass );
            if (!matched)
                System.out.println("The passphrases do not match.");
        } while (!matched);

        SecretKey newSharedKeyDecryptionKey; 
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(CIPHER);
            newSharedKeyDecryptionKey = skf.generateSecret(new PBEKeySpec(newClusterPass.toCharArray()));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Unable to initialize encryption key for cluster shared key: " + ExceptionUtils.getMessage(e), e);
        }       

        // Save shared key to DB
        try {
            saveSharedKeyB64( config, encryptKey( sharedKey, newSharedKeyDecryptionKey ) );
        } catch ( GeneralSecurityException gse ) {
            throw new CausedIOException( "Unable to encrypt shared key", gse );
        }
        
        // Update properties file with new passphrase
        if ( nodePropsFile.exists() ) {
            NodeConfigurationManager.configureGatewayNode( NODE, null, null, newClusterPass, null, null );
        } 
        
        System.out.println("Cluster passphrase changed successfully.");
    }
    
    private String promptForPassword( final String prompt ) throws IOException {
        String password = null;

        Console console = System.console();
        Pattern pattern = Pattern.compile("^.{" + MIN_LENGTH + "," + MAX_LENGTH + "}$", Pattern.DOTALL);
        while( password == null  ) {
            System.out.println( prompt );
            password = new String(console.readPassword());

            exitOnQuit( password );

            if ( !pattern.matcher(password).matches() ) {
                System.out.println( "Cluster passphrase should be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters long.\n" );
                password = null;
            }
        }

        return password;
    }   
    
    private String encryptKey( final byte[] toEncrypt, final SecretKey sharedKeyDecryptionKey ) 
            throws GeneralSecurityException
    {
        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, sharedKeyDecryptionKey);
        byte[] cipherBytes = cipher.doFinal(toEncrypt);
        PBEParameterSpec pbeSpec = cipher.getParameters().getParameterSpec(PBEParameterSpec.class);
        byte[] salt = pbeSpec.getSalt();
        int itc = pbeSpec.getIterationCount();
        return "$PBE1$" + HexUtils.encodeBase64(salt, true) + "$" + itc + "$" + HexUtils.encodeBase64(cipherBytes, true) + "$";
    }

    private byte[] decryptKey( final String b64edEncKey, final SecretKey sharedKeyDecryptionKey ) 
            throws IOException, GeneralSecurityException
    {
        Pattern pattern = Pattern.compile("^\\$PBE1\\$([^$]+)\\$(\\d+)\\$([^$]+)\\$$");
        Matcher matcher = pattern.matcher(b64edEncKey);
        if (!matcher.matches())
            throw new IOException("Invalid shared key format: " + b64edEncKey);
        String b64edSalt = matcher.group(1);
        String strIterationCount = matcher.group(2);
        String b64edCiphertext = matcher.group(3);

        byte[] saltbytes;
        int iterationCount;
        byte[] cipherbytes;
        try {
            saltbytes = HexUtils.decodeBase64(b64edSalt);
            iterationCount = Integer.parseInt(strIterationCount);
            cipherbytes = HexUtils.decodeBase64(b64edCiphertext);
        } catch ( IOException e ) {
            throw new IOException("Invalid shared key format: " + b64edEncKey, e);
        } catch ( NumberFormatException nfe ) {
            throw new IOException("Invalid shared key format: " + b64edEncKey, nfe);
        }

        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, sharedKeyDecryptionKey, new PBEParameterSpec(saltbytes, iterationCount));
        return cipher.doFinal(cipherbytes);
    }    
    
    private String readSharedKeyB64( final DatabaseConfig config ) throws CausedIOException {
        String sharedKeyB64 = null;
        
        DBActions dbactions = new DBActions();
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dbactions.getConnection(config, false);
            statement = connection.createStatement();
            resultSet = statement.executeQuery("select b64edval from shared_keys where shared_keys.encodingid = '%ClusterWidePBE%'");
            if ( resultSet.next() ) {
                sharedKeyB64 = resultSet.getString(1);
            }
        } catch (SQLException sqle) {
            throw new CausedIOException("Could not read shared key.", sqle);
        } finally {
            ResourceUtils.closeQuietly(resultSet);
            ResourceUtils.closeQuietly(statement);
            ResourceUtils.closeQuietly(connection);
        }

        return sharedKeyB64;
    }   
        
    private void saveSharedKeyB64( final DatabaseConfig config, final String sharedKeyB64 ) throws CausedIOException {
        // Read shared key from DB
        DBActions dbactions = new DBActions();
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = dbactions.getConnection(config, false);
            statement = connection.prepareStatement("update shared_keys set b64edval = ? where shared_keys.encodingid = '%ClusterWidePBE%'");
            statement.setString( 1, sharedKeyB64 );
            int result = statement.executeUpdate();
            if ( result != 1 ) {
                throw new CausedIOException( "Could not save shared key." );                        
            }
        } catch (SQLException sqle) {
            throw new CausedIOException( "Could not save shared key.", sqle);
        } finally {
            ResourceUtils.closeQuietly(statement);
            ResourceUtils.closeQuietly(connection);
        }
    }     
}
