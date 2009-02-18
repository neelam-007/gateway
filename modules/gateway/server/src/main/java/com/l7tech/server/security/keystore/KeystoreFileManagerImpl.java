package com.l7tech.server.security.keystore;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.util.PropertiesDecryptor;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.ResourceUtils;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Hibernate manager for read/write access to the keystore table.
 *
 * @author mike@layer7-tech.com
 */
@Transactional(propagation= Propagation.REQUIRED)
public class KeystoreFileManagerImpl
        extends HibernateEntityManager<KeystoreFile, EntityHeader>
        implements KeystoreFileManager, ApplicationContextAware
{
    protected static final Logger logger = Logger.getLogger(KeystoreFileManagerImpl.class.getName());
    private static final String SSG_VAR_DIR = "/opt/SecureSpan/Gateway/node/default/var/";
    private static final String HSM_INIT_FILE = "hsm_init.properties";
    private static final String PROPERTY_SCA_HSMINIT_PASSWORD = "hsm.sca.password";
    private MasterPasswordManager masterPasswordManager;
    private ApplicationContext appContext;

    public KeystoreFileManagerImpl(MasterPasswordManager masterPasswordManager) {
        this.masterPasswordManager = masterPasswordManager;

    }

    public Class<KeystoreFile> getImpClass() {
        return KeystoreFile.class;
    }

    public Class<KeystoreFile> getInterfaceClass() {
        return KeystoreFile.class;
    }

    public String getTableName() {
        return "keystore";
    }

    public EntityType getEntityType() {
        return EntityType.SSG_KEY_ENTRY;
    }

    public KeystoreFile updateDataBytes(final long id, final Functions.Unary<byte[], byte[]> mutator) throws UpdateException {
        try {
            return (KeystoreFile)getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    try {
                        KeystoreFile keystoreFile = (KeystoreFile)session.load(KeystoreFile.class, id, LockMode.UPGRADE);
                        byte[] bytesBefore = keystoreFile.getDatabytes();
                        byte[] bytesAfter = mutator.call(bytesBefore);
                        keystoreFile.setDatabytes(bytesAfter);
                        update(keystoreFile);
                        return keystoreFile;
                    } catch (UpdateException e) {
                        throw new HibernateException(e);
                    }
                }
            });
        } catch (Exception e) {
            throw new UpdateException(e);
        }
    }

    //reads hsm_init.properties first (if it exists), and then stores the password in the KeystoreFile
    public void initializeHsmKeystorePasswordFromFile() throws UpdateException {
        if (!isHsmAvailable()) return;

        final File ssgVarDir = new File(SSG_VAR_DIR);
        final File hsmInitFile = new File(ssgVarDir, HSM_INIT_FILE);

        try {
            //get the init file, with the password already decrypted
            Properties hsmInitProps = loadHsmInitProperties(hsmInitFile);

            //iterate the keystore files and find the sca one.
            String hsmPasswordDecrypted = hsmInitProps.getProperty(PROPERTY_SCA_HSMINIT_PASSWORD);
            if (hsmPasswordDecrypted == null) {
                logger.info("No HSM init file or password found. Using the database value.");
            } else {
                KeystoreFile hsmKeystoreFile = null;
                Collection<KeystoreFile> keystoreFiles = findAll();
                for ( KeystoreFile file : keystoreFiles ) {
                    if ( file.getFormat()!=null && file.getFormat().startsWith("hsm.") ) {
                        if ( hsmKeystoreFile != null ) {
                            throw new UpdateException("Database contains multiple entries for HSM keystore.");
                        }
                        hsmKeystoreFile = file;
                    }
                }

                if (hsmKeystoreFile == null) throw new UpdateException("Could not find an HSM keystore row in the database");
                updatePassword(hsmKeystoreFile.getOidAsLong(), hsmPasswordDecrypted.toCharArray());
                if (hsmInitFile.exists()) hsmInitFile.delete();
            }
        } catch (Exception e) {
            throw new UpdateException(e);
        }
    }

    private void updatePassword(final long id, final char[] password) throws UpdateException {
        //set the password in the properties for this KeystoreFile (encrypted with the db encrypter), so we always have it from now on
        try {
            final MasterPasswordManager dbEncrypter = (MasterPasswordManager) appContext.getBean("dbPasswordEncryption");
            getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    try {
                        KeystoreFile keystoreFile = (KeystoreFile)session.load(KeystoreFile.class, id, LockMode.UPGRADE);
                        keystoreFile.setProperty( "passphrase", dbEncrypter.encryptPassword(password));
                        update(keystoreFile);
                        return keystoreFile;
                    } catch (UpdateException e) {
                        throw new HibernateException(e);
                    }
                }
            });
        } catch (Exception e) {
            throw new UpdateException(e);
        }
    }

    /**
     * Loads and decrypts the password in the given file
     *
     * @param propsFile the file to read.
     * @return a new Properties object containing the password already decrypted. Never null but may be empty if the file didn't exist.
     */
    private Properties loadHsmInitProperties( final File propsFile) {
        Properties properties = new Properties();

        InputStream in = null;
        try {
            in = new FileInputStream( propsFile );
            properties.load( in );
        } catch (IOException e) {
            logger.info("Could not read the HSM init file. HSM keystore file will not be initiailized with a new password '" + propsFile.getAbsolutePath() + "' " + ExceptionUtils.getMessage(e));
        } finally {
            ResourceUtils.closeQuietly(in);
        }

        PropertiesDecryptor decryptor = new PropertiesDecryptor( masterPasswordManager );
        decryptor.setPasswordProperties( new String[]{ PROPERTY_SCA_HSMINIT_PASSWORD } );
        decryptor.decryptEncryptedPasswords( properties );

        return properties;
    }

    public boolean isHsmAvailable() {
        return JceProvider.PKCS11_ENGINE.equals( JceProvider.getEngineClass());
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.appContext = applicationContext;
    }
}
