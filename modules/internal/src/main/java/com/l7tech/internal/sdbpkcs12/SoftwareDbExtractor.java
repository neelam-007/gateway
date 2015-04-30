package com.l7tech.internal.sdbpkcs12;

import com.l7tech.gateway.config.manager.NodeConfigurationManager;
import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.util.*;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.*;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class that knows how to extract a Software DB keystore from a Gateway database.
 */
public class SoftwareDbExtractor {
    private static final Logger logger = Logger.getLogger( SoftwareDbExtractor.class.getName() );

    static final String PASSPHRASE_MAP = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!@#$%^&*()-_=+[]{};:'\\|\"/?.,<>`~";
    static final String CLUSTER_WIDE_IDENTIFIER = "%ClusterWidePBE%";
    static final String SHARED_KEY_CIPHER = "PBEWithSHA1AndDESede";

    final File configDir;

    final byte[] masterPassphrase;
    final MasterPasswordManager decryptor;
    final NodeConfig nodeConfig;
    final DatabaseConfig dbconfig;
    final DBActions dbactions;
    final SecretKey sharedKeyDecryptionKey;
    final byte[] sharedKeyBytes;
    final char[] softwareKeystorePassphrase;

    public SoftwareDbExtractor( File configDirectory ) throws Exception {
        this.configDir = configDirectory;
        this.masterPassphrase = loadMasterPassphrase( configDir );

        File ompFile = new File(configDir, "omp.dat");
        File nodePropsFile = new File(configDir, "node.properties");

        nodeConfig = NodeConfigurationManager.loadNodeConfig("default", nodePropsFile, true);
        dbconfig = nodeConfig.getDatabase( DatabaseType.NODE_ALL, NodeConfig.ClusterType.STANDALONE, NodeConfig.ClusterType.REPL_MASTER );
        if ( dbconfig == null ) {
            throw new CausedIOException("Database configuration not found.");
        }

        logger.info("Using database host '" + dbconfig.getHost() + "'.");
        logger.info("Using database port '" + dbconfig.getPort() + "'.");
        logger.info("Using database name '" + dbconfig.getName() + "'.");
        logger.info("Using database user '" + dbconfig.getNodeUsername() + "'.");

        this.decryptor = new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile));
        dbconfig.setNodePassword( new String(decryptor.decryptPasswordIfEncrypted(dbconfig.getNodePassword())) );

        String clusterPass = nodeConfig.getClusterPassphrase();
        char[] clusterPassphrase = decryptor.decryptPasswordIfEncrypted( clusterPass );

        this.dbactions = new DBActions();


        SecretKeyFactory skf = SecretKeyFactory.getInstance( SHARED_KEY_CIPHER );
        this.sharedKeyDecryptionKey = skf.generateSecret( new PBEKeySpec( clusterPassphrase ) );

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = dbactions.getConnection( dbconfig, false );
            statement = connection.prepareStatement( "select b64edval from shared_keys where encodingid=?" );
            statement.setString( 1, CLUSTER_WIDE_IDENTIFIER );
            rs = statement.executeQuery();
            if ( !rs.next() )
                throw new RuntimeException( "Database does not contain a cluster shared key (has Gateway been run at least once?)" );

            String b64 = rs.getString( 1 );
            this.sharedKeyBytes = decryptSharedKey( b64, sharedKeyDecryptionKey );
            logger.info( "Successfully decrypted cluster shared key from database" );

        } finally {
            ResourceUtils.closeQuietly( rs );
            ResourceUtils.closeQuietly( statement );
            ResourceUtils.closeQuietly( connection );
        }

        this.softwareKeystorePassphrase = toPassphrase( sharedKeyBytes );
    }

    public KeyStore extractKeystore( char[] outputPassphrase ) throws Exception {
        byte[] bytes = loadKeystoreBytes();
        if ( bytes.length < 1 )
            throw new RuntimeException( "Software DB keystore is empty (zero databytes)" );

        KeyStore in = KeyStore.getInstance( "PKCS12" );
        in.load( new ByteArrayInputStream( bytes ), softwareKeystorePassphrase );

        KeyStore out = KeyStore.getInstance( "PKCS12" );
        out.load( null, outputPassphrase );

        copyKeyEntries( in, softwareKeystorePassphrase, out, outputPassphrase );

        return out;
    }

    public void storeKeystore( KeyStore keyStore, char[] inputPassphrase ) throws Exception {

        KeyStore out = KeyStore.getInstance( "PKCS12" );
        out.load( null, softwareKeystorePassphrase );

        copyKeyEntries( keyStore, inputPassphrase, out, softwareKeystorePassphrase );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        out.store( baos, softwareKeystorePassphrase );

        byte[] keystoreBytes = baos.toByteArray();

        saveKeystoreBytes( keystoreBytes );
    }

    private byte[] loadKeystoreBytes() throws Exception {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        Blob blob = null;
        InputStream blobStream = null;
        try {
            connection = dbactions.getConnection( dbconfig, false );
            statement = connection.prepareStatement( "select databytes from keystore_file where format=?" );
            statement.setString( 1, "sdb.pkcs12" );
            rs = statement.executeQuery();
            if ( !rs.next() )
                throw new RuntimeException( "Database does not contain a Software DB keystore (no sdb.pkcs12 row)" );

            blob = rs.getBlob( 1 );
            if ( blob == null )
                throw new RuntimeException( "Database does not contain a Software DB keystore (no databytes)" );

            blobStream = blob.getBinaryStream();
            return IOUtils.slurpStream( blobStream );

        } finally {
            ResourceUtils.closeQuietly( blobStream );
            if ( blob != null )
                blob.free();
            ResourceUtils.closeQuietly( rs );
            ResourceUtils.closeQuietly( statement );
            ResourceUtils.closeQuietly( connection );
        }
    }

    private void saveKeystoreBytes( byte[] keystoreBytes ) throws Exception {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = dbactions.getConnection( dbconfig, false );
            statement = connection.prepareStatement( "update keystore_file set databytes=? where format=?" );
            statement.setBinaryStream( 1, new ByteArrayInputStream( keystoreBytes ) );
            statement.setString( 2, "sdb.pkcs12" );

            int rowCount = statement.executeUpdate();
            if ( rowCount < 1 )
                throw new RuntimeException( "Zero rows updated (no Software DB sdb.pkcs12 in database? Has Gateway been run at least once?)");

        } finally {
            ResourceUtils.closeQuietly( statement );
            ResourceUtils.closeQuietly( connection );
        }
    }

    private static byte[] loadMasterPassphrase( File configDir ) throws Exception {
        File ompFile = new File( configDir, "omp.dat" );
        byte[] ompBytes = IOUtils.slurpFile( ompFile );
        return ObfuscatedFileMasterPasswordFinder.unobfuscate( new String( ompBytes, Charsets.UTF8 ) );
    }

    private static char[] toPassphrase(byte[] b) {
        char[] ret = new char[b.length];
        int nc = PASSPHRASE_MAP.length();
        for (int i = 0; i < b.length; ++i)
            ret[i] = PASSPHRASE_MAP.charAt((128 + b[i]) % nc);
        return ret;
    }

    private static byte[] decryptSharedKey( String b64edEncKey, Key sharedKeyDecryptionKey ) throws IOException, GeneralSecurityException
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
        } catch (NumberFormatException nfe) {
            throw new IOException("Invalid shared key format: " + b64edEncKey, nfe);
        }

        Cipher cipher = Cipher.getInstance( SHARED_KEY_CIPHER );
        cipher.init(Cipher.DECRYPT_MODE, sharedKeyDecryptionKey, new PBEParameterSpec(saltbytes, iterationCount));
        return cipher.doFinal(cipherbytes);
    }

    private static void copyKeyEntries( KeyStore source, char[] sourceEntryPass, KeyStore dest, char[] destEntryPass ) throws Exception {
        Enumeration<String> aliases = source.aliases();
        while ( aliases.hasMoreElements() ) {
            String alias = aliases.nextElement();
            if ( !source.isKeyEntry( alias ) )
                continue;
            Key key = source.getKey( alias, sourceEntryPass );
            Certificate[] chain = source.getCertificateChain( alias );
            dest.setKeyEntry( alias, key, destEntryPass, chain );
        }
    }

    private static void doExtract( File configDir, File keystorePath, char[] passphrase ) throws Exception {
        if ( keystorePath.exists() )
            throw new RuntimeException( "Key store file already exists: " + keystorePath );

        SoftwareDbExtractor sdex = new SoftwareDbExtractor( configDir );

        KeyStore ks = sdex.extractKeystore( passphrase );

        try ( OutputStream os = new FileOutputStream( keystorePath ) ) {
            ks.store( os, passphrase );
        }
        logger.info( "Keystore extracted to " + keystorePath );
        System.err.println( "Keystore extracted to " + keystorePath );
    }

    private static void doStore( File configDir, File keystorePath, char[] passphrase ) throws Exception {
        KeyStore ks = KeyStore.getInstance( "PKCS12" );

        try ( InputStream fis = new FileInputStream( keystorePath ) ) {
            ks.load( fis, passphrase );
        }

        SoftwareDbExtractor sdex = new SoftwareDbExtractor( configDir );

        sdex.storeKeystore( ks, passphrase );

        logger.info( "Keystore stored to database" );
        System.err.println( "Keystore stored to database" );
    }

    public static void main( String[] args ) throws Exception {
        JdkLoggerConfigurator.configure("com.l7tech.logging", "com/l7tech/gateway/config/client/logging.properties", "configlogging.properties");
        System.setProperty( "com.l7tech.server.management.laxVersionCheck", "true" );

        if ( args.length < 1 )
            usage();

        if ( "extract".equals( args[0] ) ) {
            File configDir = new File( args[1] );
            File keystorePath = new File( args[2] );
            String passphrase = args[3];
            doExtract( configDir, keystorePath, passphrase.toCharArray() );
        } else if ( "store".equals( args[0] ) ) {
            File configDir = new File( args[1] );
            File keystorePath = new File( args[2] );
            String passphrase = args[3];
            doStore( configDir, keystorePath, passphrase.toCharArray() );
        } else {
            usage();
        }
    }

    private static void usage() {
        System.out.println( "Usage:  SoftwareDbExtractor <extract|store> <configDir> <keystorePath> <keystorePassphrase>");
        System.exit( 1 );
    }

}
