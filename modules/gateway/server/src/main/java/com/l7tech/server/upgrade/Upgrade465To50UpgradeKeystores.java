package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.security.keystore.*;
import com.l7tech.server.security.sharedkey.SharedKeyManager;
import com.l7tech.server.security.keystore.sca.ScaSsgKeyStore;
import com.l7tech.server.util.PropertiesDecryptor;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.EncryptionUtil;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.common.io.CertUtils;
import com.l7tech.security.prov.JceProvider;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronization;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.BadPaddingException;
import java.util.logging.Logger;
import java.util.Properties;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.PrivateKey;
import java.security.Key;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * A database upgrade task that re-encrypts the cluster shared key and imports keystore settings from disk.
 */
public class Upgrade465To50UpgradeKeystores implements UpgradeTask {

    //- PUBLIC

    @Override
    public void upgrade( final ApplicationContext applicationContext ) throws FatalUpgradeException, NonfatalUpgradeException {
        this.applicationContext = applicationContext;

        final ServerConfig serverConfig = getBean( "serverConfig", ServerConfig.class );
        final SharedKeyManager sharedKeyManager = getBean( "sharedKeyManager", SharedKeyManager.class );

        final File varDirectory = serverConfig.getLocalDirectoryProperty("varDirectory", true);
        final File upgradeDirectory = new File( varDirectory, UPGRADE_DIR );

        //
        if ( upgradeDirectory.isDirectory() ) {
            File keystorePropertiesFile = new File( upgradeDirectory, "keystore.properties" );
            if ( keystorePropertiesFile.isFile() ) {
                logger.info("Found keystore.properties file for upgrade.");

                final Collection<File> filesToDelete = new ArrayList<File>();

                // Load keystore properties
                Properties keystoreProperties = loadProperties( keystorePropertiesFile );
                filesToDelete.add( keystorePropertiesFile );

                final KeystoreInfo keystoreInfo = new KeystoreInfo(keystoreProperties);
                if ( KEYSTORE_TYPE_PKCS12.equals(keystoreInfo.keystoreType) ) {
                    upgradeDatabaseKeystore(serverConfig, sharedKeyManager, upgradeDirectory, filesToDelete, keystoreInfo);
                } else if ( KEYSTORE_TYPE_PKCS11.equals(keystoreInfo.keystoreType) ) {
                    upgradeHsmKeystore(serverConfig, sharedKeyManager, keystoreInfo);
                } else {
                    throw new FatalUpgradeException("Keystore upgrade is for unsupported keystore type '"+keystoreInfo.keystoreType+"'.");
                }

                // Delete the upgrade files from disk once the DB updates are persisted
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCompletion(int status) {
                        if ( status == TransactionSynchronization.STATUS_COMMITTED ) {
                            for ( File file : filesToDelete ) {
                                if ( file.delete() ) {
                                    logger.info( "Deleted file after keystore upgrade '"+file.getAbsolutePath()+"'." );
                                } else {
                                    logger.warning( "Unable to delete file after keystore upgrade '"+file.getAbsolutePath()+"'." );
                                }
                            }

                            String[] files = upgradeDirectory.list();
                            if ( files != null ) {
                                if ( files.length == 0 ) {
                                    if ( upgradeDirectory.delete() ) {
                                        logger.info( "Deleted empty upgrade directory after keystore upgrade." );                                        
                                    } else {
                                        logger.warning( "Unable to delete empty upgrade directory after keystore upgrade." );
                                    }
                                } else {
                                    logger.info( "Not deleting upgrade directory after keystore upgrade (files present)." );
                                }
                            } else {
                                logger.warning( "Error checking for empty upgrade directory." );
                            }
                        }
                    }
                });
            }
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(Upgrade465To50UpgradeKeystores.class.getName());
    private static final String UPGRADE_DIR = "upgrade";
    private static final String KEYSTORE_TYPE_PKCS12 = "PKCS12";
    private static final String KEYSTORE_TYPE_PKCS11 = "PKCS11";

    private ApplicationContext applicationContext;

    /**
     * Get a bean safely.
     *
     * @param name the bean to get.  Must not be null.
     * @param beanClass the class of the bean to get. Must not be null.
     * @return the requested bean.  Never null.
     * @throws FatalUpgradeException  if there is no application context or the requested bean was not found
     */
    @SuppressWarnings({"unchecked"})
    private <T> T getBean( final String name, final Class<T> beanClass ) throws FatalUpgradeException {
        if (applicationContext == null) throw new FatalUpgradeException("ApplicationContext is required");
        return applicationContext.getBean(name, beanClass);
    }

    /**
     * Upgrade database keystore.
     *
     * For database keystore we have to:
     *
     * - Load the CA and SSL keystores from disk
     * - Decrypt the cluster shared key with the SSL key from the disk keystore
     * - Store a copy of the cluster shared key encrytped with the cluster passphrase
     * - Import the CA and SSL private keys to the DB keystore
     * - Set cluster properties for the SSL and CA keys
     */
    private void upgradeDatabaseKeystore( final ServerConfig serverConfig,
                                          final SharedKeyManager sharedKeyManager,
                                          final File upgradeDirectory,
                                          final Collection<File> filesToDelete,
                                          final KeystoreInfo keystoreInfo ) throws FatalUpgradeException {
        File caKeyStoreFile = new File( upgradeDirectory, keystoreInfo.caKeyStoreName );
        File sslKeyStoreFile = new File( upgradeDirectory, keystoreInfo.sslKeyStoreName );

        if ( !caKeyStoreFile.isFile() || !sslKeyStoreFile.isFile() )
            throw new FatalUpgradeException( "Missing keystore file on upgrade [ca='"+caKeyStoreFile.getAbsolutePath()+"', ssl='"+sslKeyStoreFile.getAbsolutePath()+"']." );

        InputStream caKeystoreInput = null;
        InputStream sslKeystoreInput = null;
        try {
            // Load keystore files.
            KeyStore caKeyStore = KeyStore.getInstance( KEYSTORE_TYPE_PKCS12 );
            caKeystoreInput = new FileInputStream( caKeyStoreFile );
            caKeyStore.load( caKeystoreInput, keystoreInfo.caPassphrase );
            filesToDelete.add( caKeyStoreFile );

            KeyStore sslKeyStore = KeyStore.getInstance( KEYSTORE_TYPE_PKCS12 );
            sslKeystoreInput = new FileInputStream( sslKeyStoreFile );
            sslKeyStore.load( sslKeystoreInput, keystoreInfo.sslPassphrase );
            filesToDelete.add( sslKeyStoreFile );

            // Re-encrypt the cluster shared key for the cluster passphrase
            X509Certificate sslCert = (X509Certificate) sslKeyStore.getCertificate( keystoreInfo.sslAlias );
            Key key = sslKeyStore.getKey( keystoreInfo.sslAlias, keystoreInfo.sslPassphrase );
            String b64EncSharedKeyData = sharedKeyManager.getEncrytpedSharedKey( EncryptionUtil.computeCustomRSAPubKeyID( (RSAPublicKey) sslCert.getPublicKey() ) );
            byte[] clearClusterSharedKey = decryptClusterSharedKey( b64EncSharedKeyData, key );
            sharedKeyManager.saveAndEncryptSharedKey( clearClusterSharedKey );

            // Import the SSL/CA keys
            SsgKeyStore keyStore = findFirstMutableKeystore();
            SsgKeyEntry sslKey = new SsgKeyEntry( keyStore.getOid(), "SSL",
                    CertUtils.asX509CertificateArray(sslKeyStore.getCertificateChain(keystoreInfo.sslAlias)),
                    (PrivateKey)sslKeyStore.getKey(keystoreInfo.sslAlias, keystoreInfo.sslPassphrase )  );
            SsgKeyEntry caKey = new SsgKeyEntry( keyStore.getOid(), "CA",
                    CertUtils.asX509CertificateArray(caKeyStore.getCertificateChain(keystoreInfo.caAlias)),
                    (PrivateKey)caKeyStore.getKey(keystoreInfo.caAlias, keystoreInfo.caPassphrase ) );

            if ( !keyStore.storePrivateKeyEntry(null, sslKey, false ).get() ) {
                throw new FatalUpgradeException("Unable to import SSL key.");
            }

            if ( !keyStore.storePrivateKeyEntry(null, caKey, false ).get() ){
                throw new FatalUpgradeException("Unable to import CA key.");
            }

            // Set cluster properties for default keys (SSL/CA)
            setKeyClusterProperties( serverConfig, keyStore.getId(), "SSL", "CA" );
        } catch (KeyStoreException e) {
            throw new FatalUpgradeException("Error processing key data during import.", e);
        } catch (IOException e) {
            throw new FatalUpgradeException("Unable to read key data for import.", e);
        } catch (NoSuchAlgorithmException e) {
            throw new FatalUpgradeException("Unable to read key data for import.", e);
        } catch (CertificateException e) {
            throw new FatalUpgradeException("Unable to read key data for import.", e);
        } catch (UnrecoverableKeyException e) {
            throw new FatalUpgradeException("Unable to read key for import.", e);
        } catch (ObjectModelException e) {
            throw new FatalUpgradeException("Database error during keystore upgrade.", e);
        } catch (ExecutionException e) {
            throw new FatalUpgradeException("Unable to import key.", e);
        } catch (InterruptedException e) {
            throw new FatalUpgradeException("Unable to import key.", e);
        } finally {
            ResourceUtils.closeQuietly( caKeystoreInput );
            ResourceUtils.closeQuietly( sslKeystoreInput );
        }
    }

    /**
     * Upgrade an HSM keystore.
     *
     * For an HSM, we have to:
     *
     * - Locate the HSM keystore file in the DB
     * - Open the HSM keystore with the existing passphrase (from keystore.properties)
     * - Decrypt the cluster shared key with the SSL key from the HSM keystore
     * - Store a copy of the cluster shared key encrytped with the cluster passphrase
     * - Update the HSM keystore file DB entry with the keystore passphrase
     * - Set cluster properties for the SSL and CA keys
     */
    private void upgradeHsmKeystore( final ServerConfig serverConfig,
                                     final SharedKeyManager sharedKeyManager,
                                     final KeystoreInfo keystoreInfo) throws FatalUpgradeException {
        if ( !isHsmAvailable() ) {
            throw new FatalUpgradeException("Keystore upgrade is for HSM, but HSM is not available. Please enable the HSM and retry.");
        }

        final KeystoreFileManager keystoreFileManager = getBean("keystoreFileManager", KeystoreFileManager.class);
        try {
            KeystoreFile hsmKeystoreFile = null;
            Collection<KeystoreFile> keystoreFiles = keystoreFileManager.findAll();
            for ( KeystoreFile file : keystoreFiles ) {
                if ( file.getFormat()!=null && file.getFormat().startsWith("hsm.sca.") ) {
                    if ( hsmKeystoreFile != null ) {
                        throw new FatalUpgradeException("Database contains multiple entries for HSM keystore.");
                    }
                    hsmKeystoreFile = file;
                }
            }

            if ( hsmKeystoreFile == null ) {
                throw new FatalUpgradeException("Keystore upgrade is for HSM, but there is no HSM keystore database entry.");
            }

            ScaSsgKeyStore scaKeyStore = ScaSsgKeyStore.getInstance(hsmKeystoreFile.getOid(), hsmKeystoreFile.getName(), keystoreInfo.sslPassphrase, keystoreFileManager, new KeyAccessFilter() {
                @Override
                public boolean isRestrictedAccessKeyEntry(SsgKeyEntry keyEntry) {
                    return false;
                }
            }, new SsgKeyMetadataFinder() {
                    @Override
                    public SsgKeyMetadata findMetadata(long keystoreOid, @NotNull String alias) throws FindException {
                        return null;
                    }
                });

            // Re-encrypt the cluster shared key for the cluster passphrase
            SsgKeyEntry entry = scaKeyStore.getCertificateChain(keystoreInfo.sslAlias);
            String b64EncSharedKeyData = sharedKeyManager.getEncrytpedSharedKey( EncryptionUtil.computeCustomRSAPubKeyID( (RSAPublicKey) entry.getPublic() ) );
            byte[] clearClusterSharedKey = decryptClusterSharedKey( b64EncSharedKeyData, entry.getPrivateKey() );
            sharedKeyManager.saveAndEncryptSharedKey( clearClusterSharedKey );

            // Create password encryption bean only after the re-encrytped cluster shared key is available
            final MasterPasswordManager passwordManager = getBean( "dbPasswordEncryption", MasterPasswordManager.class );
            hsmKeystoreFile.setProperty( "passphrase", passwordManager.encryptPassword(keystoreInfo.sslPassphrase) );
            keystoreFileManager.update(hsmKeystoreFile);
            
            // Set cluster properties for default keys using existing aliases
            setKeyClusterProperties( serverConfig, hsmKeystoreFile.getId(), keystoreInfo.sslAlias, keystoreInfo.caAlias );
        } catch (KeyStoreException e) {
            throw new FatalUpgradeException( "Error accessing HSM keystore.", e);
        } catch (ObjectModelException e) {
            throw new FatalUpgradeException("Database error during keystore upgrade.", e);
        } catch (UnrecoverableKeyException e) {
            throw new FatalUpgradeException( "Error accessing HSM keystore.", e);
        }
    }

    /**
     * Decrypt the given cluster shared key.
     */
    private byte[] decryptClusterSharedKey( final String b64EncSharedKeyData, final Key key ) throws FatalUpgradeException {
        try {
            return EncryptionUtil.deB64AndRsaDecrypt(b64EncSharedKeyData, key);
        } catch (IOException e) {
            throw new FatalUpgradeException("Error decrypting cluster shared key.", e);
        } catch (InvalidKeyException e) {
            throw new FatalUpgradeException("Error decrypting cluster shared key.", e);
        } catch (NoSuchAlgorithmException e) {
            throw new FatalUpgradeException("Error decrypting cluster shared key.", e);
        } catch (NoSuchPaddingException e) {
            throw new FatalUpgradeException("Error decrypting cluster shared key.", e);
        } catch (IllegalBlockSizeException e) {
            throw new FatalUpgradeException("Error decrypting cluster shared key.", e);
        } catch (BadPaddingException e) {
            throw new FatalUpgradeException("Error decrypting cluster shared key.", e);
        }
    }

    /**
     * Set default key cluster properties with the given information
     */
    private void setKeyClusterProperties( final ServerConfig serverConfig,
                                          final String keyStoreId,
                                          final String sslAlias,
                                          final String caAlias) throws FatalUpgradeException, ObjectModelException {
        ClusterPropertyManager clusterPropertyManager = getBean("clusterPropertyManager", ClusterPropertyManager.class);
        String sslPropertyName = serverConfig.getClusterPropertyName( ServerConfigParams.PARAM_KEYSTORE_DEFAULT_SSL_KEY);
        if (sslPropertyName != null) {
            String value = keyStoreId + ":" + sslAlias;
            clusterPropertyManager.putProperty(sslPropertyName, value);
        } else {
            logger.warning("Could not configure default SSL key.");
        }
        String caPropertyName = serverConfig.getClusterPropertyName( ServerConfigParams.PARAM_KEYSTORE_DEFAULT_CA_KEY);
        if (caPropertyName != null) {
            String value = keyStoreId + ":" + caAlias;
            clusterPropertyManager.putProperty(caPropertyName, value);
        } else {
            logger.warning("Could not configure default CA key.");
        }

    }

    /**
     * Check if the HSM is enabled
     */
    private boolean isHsmAvailable() {
        return JceProvider.PKCS11_ENGINE.equals( JceProvider.getEngineClass() );
    }

    /**
     * Get the first mutable keystore, if there's an HSM this will return it
     */
    private SsgKeyStore findFirstMutableKeystore() throws FatalUpgradeException {
        SsgKeyStore sks = null;
        try {
            SsgKeyStoreManager keyStoreManager = getBean("ssgKeyStoreManager", SsgKeyStoreManager.class);
            List<SsgKeyFinder> got = keyStoreManager.findAll();
            for (SsgKeyFinder ssgKeyFinder : got) {
                if (ssgKeyFinder.isMutable()) {
                    sks = ssgKeyFinder.getKeyStore();
                    break;
                }
            }
            if (sks == null)
                throw new FatalUpgradeException("No mutable keystores found in which to import key");
        } catch (FindException e) {
            throw new FatalUpgradeException("Unable to find keystore in which to import key: " + ExceptionUtils.getMessage(e), e);
        } catch (KeyStoreException e) {
            throw new FatalUpgradeException("Unable to find keystore in which to import key: " + ExceptionUtils.getMessage(e), e);
        }
        return sks;
    }

    /**
     * Loads and decrypts passwords in the given file 
     */
    private Properties loadProperties( final File keystorePropertiesFile ) throws FatalUpgradeException, NonfatalUpgradeException {
        Properties properties = new Properties();

        InputStream in = null;
        try {
            in = new FileInputStream( keystorePropertiesFile );
            properties.load( in );
        } catch ( IOException ioe ) {
            throw new FatalUpgradeException( "Error loading keystore properties file for upgrade.", ioe );
        } finally {
            ResourceUtils.closeQuietly(in);
        }

        MasterPasswordManager passwordManager = getBean( "masterPasswordManager", MasterPasswordManager.class );
        PropertiesDecryptor decryptor = new PropertiesDecryptor( passwordManager );
        decryptor.setPasswordProperties( new String[]{ "rootcakspasswd", "sslkspasswd" } );
        decryptor.decryptEncryptedPasswords( properties );

        return properties;
    }

    /**
     * Keystore info data object
     */
    private static final class KeystoreInfo {
        private final String caKeyStoreName;
        private final String caAlias;
        private final char[] caPassphrase;
        private final String sslKeyStoreName;
        private final String sslAlias;
        private final char[] sslPassphrase;
        private final String keystoreType;

        KeystoreInfo( final Properties keystoreProperties ) {
            caKeyStoreName = keystoreProperties.getProperty( "rootcakstorename", "ca.ks" );
            caAlias = keystoreProperties.getProperty( "rootcaalias", "ssgroot" );
            caPassphrase = keystoreProperties.getProperty( "rootcakspasswd", "" ).toCharArray();
            sslKeyStoreName = keystoreProperties.getProperty( "sslkstorename", "ssl.ks" );
            sslAlias = keystoreProperties.getProperty( "sslkeyalias", "tomcat" );
            sslPassphrase = keystoreProperties.getProperty( "sslkspasswd", "" ).toCharArray();
            keystoreType = keystoreProperties.getProperty( "keystoretype", KEYSTORE_TYPE_PKCS12 );
        }
    }
}