package com.l7tech.server.upgrade;

import com.l7tech.server.ServerConfig;
import com.l7tech.server.security.sharedkey.SharedKeyManager;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.util.PropertiesDecryptor;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.EncryptionUtil;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.common.io.CertUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.BadPaddingException;
import java.util.logging.Logger;
import java.util.Properties;
import java.util.List;
import java.util.concurrent.Future;
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
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * A database upgrade task that re-encrypts the cluster shared key and imports keystore settings from disk.
 */
public class Upgrade465To50UpgradeKeystores implements UpgradeTask {
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
        return (T) applicationContext.getBean(name, beanClass);
    }

    @Override
    public void upgrade( final ApplicationContext applicationContext ) throws FatalUpgradeException, NonfatalUpgradeException {
        this.applicationContext = applicationContext;

        final ServerConfig serverConfig = getBean( "serverConfig", ServerConfig.class );
        final PlatformTransactionManager transactionManager = getBean( "transactionManager", PlatformTransactionManager.class );
        final SharedKeyManager sharedKeyManager = getBean( "sharedKeyManager", SharedKeyManager.class );

        File varDirectory = serverConfig.getLocalDirectoryProperty("varDirectory", true);
        File upgradeDirectory = new File( varDirectory, UPGRADE_DIR );

        //
        if ( upgradeDirectory.isDirectory() ) {
            File keystorePropertiesFile = new File( upgradeDirectory, "keystore.properties" );
            if ( keystorePropertiesFile.isFile() ) {
                logger.info("Found keystore.properties file for upgrade.");

                // Read and validate keystore properties
                Properties keystoreProperties = loadProperties( keystorePropertiesFile );

                final String caAlias = keystoreProperties.getProperty( "rootcaalias", "ssgroot" );
                final String caPassphrase = keystoreProperties.getProperty( "rootcakspasswd", "" );
                final String sslAlias = keystoreProperties.getProperty( "sslkeyalias", "tomcat" );
                final String sslPassphrase = keystoreProperties.getProperty( "sslkspasswd", "" );

                String keystoreType = keystoreProperties.getProperty( "keystoretype", KEYSTORE_TYPE_PKCS12 );
                if ( KEYSTORE_TYPE_PKCS12.equals(keystoreType) ) {
                    File caKeyStoreFile = new File( upgradeDirectory, keystoreProperties.getProperty( "rootcakstorename", "ca.ks" ) );
                    File sslKeyStoreFile = new File( upgradeDirectory, keystoreProperties.getProperty( "sslkstorename", "ssl.ks" ) );

                    if ( !caKeyStoreFile.isFile() || !sslKeyStoreFile.isFile() )
                        throw new FatalUpgradeException( "Missing keystore file on upgrade [ca='"+caKeyStoreFile.getAbsolutePath()+"', ssl='"+sslKeyStoreFile.getAbsolutePath()+"']." );

                    InputStream caKeystoreInput = null;
                    InputStream sslKeystoreInput = null;
                    try {
                        // Load keystore files.
                        KeyStore caKeyStore = KeyStore.getInstance( KEYSTORE_TYPE_PKCS12 );
                        caKeystoreInput = new FileInputStream( caKeyStoreFile );
                        caKeyStore.load( caKeystoreInput, caPassphrase.toCharArray() );

                        KeyStore sslKeyStore = KeyStore.getInstance( KEYSTORE_TYPE_PKCS12 );
                        sslKeystoreInput = new FileInputStream( sslKeyStoreFile );
                        sslKeyStore.load( sslKeystoreInput, sslPassphrase.toCharArray() );

                        // Re-encrypt the cluster shared key for the cluster passphrase                       
                        X509Certificate sslCert = (X509Certificate) sslKeyStore.getCertificate( sslAlias );
                        Key key = sslKeyStore.getKey( sslAlias, sslPassphrase.toCharArray() );
                        String b64EncSharedKeyData = sharedKeyManager.getEncrytpedSharedKey( EncryptionUtil.computeCustomRSAPubKeyID( (RSAPublicKey) sslCert.getPublicKey() ) );
                        byte[] clearClusterSharedKey = EncryptionUtil.deB64AndRsaDecrypt(b64EncSharedKeyData, key);
                        sharedKeyManager.saveAndEncryptSharedKey( clearClusterSharedKey );

                        // Import the SSL/CA keys
                        SsgKeyStore keyStore = findFirstMutableKeystore();
                        SsgKeyEntry sslKey = new SsgKeyEntry( keyStore.getOid(), "SSL",
                                CertUtils.asX509CertificateArray(sslKeyStore.getCertificateChain(sslAlias)),
                                (PrivateKey)sslKeyStore.getKey(sslAlias, sslPassphrase.toCharArray() )  );
                        SsgKeyEntry caKey = new SsgKeyEntry( keyStore.getOid(), "CA",
                                CertUtils.asX509CertificateArray(caKeyStore.getCertificateChain(caAlias)),
                                (PrivateKey)caKeyStore.getKey(caAlias, caPassphrase.toCharArray() ) );
                        try {
                            Future<Boolean> job1 = keyStore.storePrivateKeyEntry( sslKey, false );
                            job1.get();

                            Future<Boolean> job2 = keyStore.storePrivateKeyEntry( caKey, false );
                            job2.get();
                        } catch (GeneralSecurityException e) {
                            throw new IOException("Unable to create initial default SSL key: " + ExceptionUtils.getMessage(e), e);
                        } catch (ExecutionException e) {
                            throw new IOException("Unable to create initial default SSL key: " + ExceptionUtils.getMessage(e), e);
                        } catch (InterruptedException e) {
                            throw new IOException("Unable to create initial default SSL key: " + ExceptionUtils.getMessage(e), e);
                        }

                        //TODO set cluster properties for default keys (SSL/CA)

                    } catch (KeyStoreException e) {
                       //TODO [steve] exceptions
                       throw new FatalUpgradeException(e);
                    } catch (IOException e) {
                        throw new FatalUpgradeException(e);
                    } catch (NoSuchAlgorithmException e) {
                        throw new FatalUpgradeException(e);
                    } catch (CertificateException e) {
                        throw new FatalUpgradeException(e);
                    } catch (FindException e) {
                        throw new FatalUpgradeException(e);
                    } catch (UnrecoverableKeyException e) {
                        throw new FatalUpgradeException(e);
                    } catch (IllegalBlockSizeException e) {
                        throw new FatalUpgradeException(e);
                    } catch (InvalidKeyException e) {
                        throw new FatalUpgradeException(e);
                    } catch (NoSuchPaddingException e) {
                        throw new FatalUpgradeException(e);
                    } catch (BadPaddingException e) {
                        throw new FatalUpgradeException(e);
                    } finally {
                        ResourceUtils.closeQuietly( caKeystoreInput );
                        ResourceUtils.closeQuietly( sslKeystoreInput );
                    }
                } else if ( KEYSTORE_TYPE_PKCS11.equals(keystoreType) ) {
                    final MasterPasswordManager passwordManager = getBean( "dbPasswordEncryption", MasterPasswordManager.class );
                } 
            }
        }
    }

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
}