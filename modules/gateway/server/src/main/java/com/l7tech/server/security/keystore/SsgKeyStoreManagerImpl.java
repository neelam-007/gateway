package com.l7tech.server.security.keystore;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.security.keystore.sca.ScaSsgKeyStore;
import com.l7tech.server.security.keystore.software.DatabasePkcs12SsgKeyStore;
import com.l7tech.server.security.sharedkey.SharedKeyManager;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.DefaultMasterPasswordFinder;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.ExceptionUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronization;

import java.security.KeyStoreException;
import java.util.*;
import java.util.logging.Logger;
import java.io.*;

/**
 * Manages all SsgKeyFinder (and SsgKeyStore) instances that will be available on this Gateway node.
 * Key finders will be instantiated based on the content of the keystore_file table in the DB and
 * whether or not an HSM is available on this Gateway node.
 * <p/>
 * Currently there is no way to add or remove key finders -- the table is populated during DB creation (or upgrade)
 * and rows are never added or removed (although they do change: the databytes and version columns will change as
 * individual key entries are CRUDded).
 */
public class SsgKeyStoreManagerImpl implements SsgKeyStoreManager {
    protected static final Logger logger = Logger.getLogger(SsgKeyStoreManagerImpl.class.getName());
    /** Characters for converting shared key bytes into pass phrase.  Do not change this, ever! */
    private static final String PASSPHRASE_MAP = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!@#$%^&*()-_=+[]{};:'\\|\"/?.,<>`~";
    private final char[] softwareKeystorePasssword;

    private final KeystoreFileManager keystoreFileManager;
    private final ServerConfig serverConfig;
    private char[] sslKeystorePassphrase;
    private MasterPasswordManager dbEncrypter;

    private boolean initialized = false;
    private List<SsgKeyFinder> keystores = null;

    private char[] hsmInitKeystorePassword;
    private static final String GATEWAY_CONFIG_DIR = "/opt/SecureSpan/Gateway/node/default/etc/conf";
    private static final String SSG_VAR_DIR = "/opt/SecureSpan/Gateway/node/default/var/";
    private static final String HSM_INIT_FILE = "hsm_init.properties";
    private static final String PROPERTY_SCA_HSMINIT_PASSWORD = "hsm.sca.password";


    public SsgKeyStoreManagerImpl(SharedKeyManager skm, KeystoreFileManager kem, ServerConfig serverConfig, char[] sslKeystorePassphrase, MasterPasswordManager passwordManager) throws KeyStoreException, FindException {
        if ( sslKeystorePassphrase == null || sslKeystorePassphrase.length==0 ) throw new IllegalArgumentException("sslKeystorePassphrase is required");
        if ( kem instanceof KeystoreFileManagerImpl ) throw new IllegalArgumentException("kem autoproxy failure");
        this.keystoreFileManager = kem;
        this.softwareKeystorePasssword = toPassphrase(skm.getSharedKey());
        this.serverConfig = serverConfig;
        this.sslKeystorePassphrase = sslKeystorePassphrase;
        this.dbEncrypter = passwordManager;
    }

    private char[] toPassphrase(byte[] b) {
        char[] ret = new char[b.length];
        int nc = PASSPHRASE_MAP.length();
        for (int i = 0; i < b.length; ++i)
            ret[i] = PASSPHRASE_MAP.charAt((128 + b[i]) % nc);
        return ret;
    }

    private synchronized void init() throws KeyStoreException, FindException {
        if (initialized)
            return;

        List<SsgKeyFinder> list = new ArrayList<SsgKeyFinder>();
        Collection<KeystoreFile> dbFiles = keystoreFileManager.findAll();

        if (dbFiles.isEmpty()) {
            // This isn't supposed to be possible -- DefaultKeystoreFilePopulator should have run by now
            String msg = "Database contains no entries in keystore_file -- no private key management features will work";
            logger.warning(msg);
            throw new KeyStoreException(msg);
        }

        boolean haveHsm = isHsmAvailable();
        boolean createdHsmFinder = false;
        for (KeystoreFile dbFile : dbFiles) {
            long id = Long.parseLong(dbFile.getId());
            String name = dbFile.getName();
            String format = dbFile.getFormat();
            if (format == null)
                throw new KeyStoreException("Database contains keystore_file with no format objectid=" + id);
            if (format.startsWith("hsm.")) {
                if (!haveHsm) {
                    logger.info("Ignoring keystore_file row with a format of hsm because this Gateway node does not appear to have an HSM installed or enabled");
                } else {
                    if (createdHsmFinder)
                        throw new KeyStoreException("Database contains more than one keystore_file row with a format of hsm");
                    char[] decryptedHsmPassword = getInitialKeystorePasswordAndMaybePersist(dbFile);
                    list.add(ScaSsgKeyStore.getInstance(id, name, decryptedHsmPassword, keystoreFileManager));
                    createdHsmFinder = true;
                }
            } else if (format.equals("ss")) {
                logger.fine("Ignoring keystore_file row with a format of ss because this keystore type is no longer supported");
            } else if (format.startsWith("sdb.")) {
                if (haveHsm)
                    logger.info("Ignoring keystore_file row with a format of sdb because this Gateway node is using an HSM instead");
                else
                    list.add(new DatabasePkcs12SsgKeyStore(id, name, keystoreFileManager,  softwareKeystorePasssword));
            }
        }

        // TODO maybe offer software keystores even if HSM is available?
        // TODO support multiple software keystores?
        keystores = Collections.unmodifiableList(list);
        sslKeystorePassphrase = null;
        initialized = true;
    }

    public boolean isHsmAvailable() {
        return JceProvider.PKCS11_ENGINE.equals( JceProvider.getEngineClass());
    }

    public List<SsgKeyFinder> findAll() throws FindException, KeyStoreException {
        init();
        return keystores;
    }

    public SsgKeyFinder findByPrimaryKey(long id) throws FindException, KeyStoreException, ObjectNotFoundException {
        init();
        for (SsgKeyFinder keystore : keystores) {
            if (keystore.getOid() == id)
                return keystore;
        }
        throw new ObjectNotFoundException("No SsgKeyFinder available on this node with id=" + id);
    }

    @Transactional(readOnly = true)
    public SsgKeyEntry lookupKeyByKeyAlias(String keyAlias, long preferredKeystoreId) throws ObjectNotFoundException, FindException, KeyStoreException {
        boolean mustSearchAll = preferredKeystoreId == -1;

        // First look in the preferred keystore
        SsgKeyFinder alreadySearched = null;
        if (preferredKeystoreId != -1) {

            try {
                alreadySearched = findByPrimaryKey(preferredKeystoreId);
                if (alreadySearched != null)
                    return alreadySearched.getCertificateChain(keyAlias);
                /* FALLTHROUGH and scan all keystores */
            } catch (ObjectNotFoundException e) {
                /* FALLTHROUGH and scan all keystores */
            } catch (FindException e) {
                /* FALLTHROUGH and scan all keystores */
            } catch (KeyStoreException e) {
                /* FALLTHROUGH and scan the other keystores */
            }
        }

        boolean scanOthers = mustSearchAll || serverConfig.getBooleanPropertyCached(ServerConfig.PARAM_KEYSTORE_SEARCH_FOR_ALIAS, true, 2 * 60 * 1000);

        // Scan the other keystores
        List<SsgKeyFinder> finders = scanOthers ? findAll() : Collections.<SsgKeyFinder>emptyList();
        for (SsgKeyFinder finder : finders) {
            if (finder == alreadySearched)
                continue;
            try {
                return finder.getCertificateChain(keyAlias);
            } catch (KeyStoreException e) {
                /* FALLTHROUGH and check the next keystore */
            }
        }

        String whichks = scanOthers ? "any keystore" : "keystore ID " + preferredKeystoreId;
        throw new ObjectNotFoundException("No key with alias " + keyAlias + " found in " + whichks);
    }

    private char[] getInitialKeystorePasswordAndMaybePersist(KeystoreFile dbFile) throws KeyStoreException {

        //reads hsm_init.properties first (if it exists), and then stores the password in the KeystoreFile
        //if no hsm_init.properties exists, use the existing value from the KeystoreFile
        char[] hsmInitKeystorePassword = null;

        final File ssgVarDir = new File(SSG_VAR_DIR);
        final File hsmInitFile = new File(ssgVarDir, HSM_INIT_FILE);

        final Properties hsmInitProps = loadProperties(hsmInitFile);

        if (hsmInitProps == null) {
            logger.info("No HSM init file was found. Using the keystore password from the database '" + hsmInitFile.getAbsolutePath() + "'");
            //get it from the db KeystoreFile
            String dbEncPassword = dbFile.getProperty("passphrase");
            hsmInitKeystorePassword = dbEncrypter.decryptPasswordIfEncrypted(dbEncPassword);
        } else {
            String hsmPasswordEncrypted = hsmInitProps.getProperty(PROPERTY_SCA_HSMINIT_PASSWORD);
            if (hsmPasswordEncrypted == null) {
                logger.warning("Found " + hsmInitFile.getAbsolutePath() + " but did not find a password. Using the database value.");
                //get it from the db KeystoreFile
                String dbEncPassword = dbFile.getProperty("passphrase");
                hsmInitKeystorePassword = dbEncrypter.decryptPasswordIfEncrypted(dbEncPassword);
            } else {
                File configDirectory = new File(GATEWAY_CONFIG_DIR);
                File ompFile = new File(configDirectory, "omp.dat");

                //decrypt the password that was stored using the omp
                final MasterPasswordManager masterPasswordManager =
                            new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile).findMasterPassword());

                hsmInitKeystorePassword = masterPasswordManager.decryptPasswordIfEncrypted(hsmPasswordEncrypted);

                //set the password in the properties for this KeystoreFile (encrypted with the db encrypter), so we always have it from now on
                dbFile.setProperty( "passphrase", dbEncrypter.encryptPassword(hsmInitKeystorePassword));

                // Delete the upgrade files from disk once the DB updates are persisted
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCompletion(int status) {
                        if ( status == TransactionSynchronization.STATUS_COMMITTED ) {
                            if (hsmInitFile.exists()) {
                                if ( hsmInitFile.delete() ) {
                                    logger.info( "Deleted HSM init file after persisting the password to the database '"+hsmInitFile.getAbsolutePath()+"'." );
                                } else {
                                    logger.warning( "Unable to delete the HSM init file after persisting the password to the database '"+hsmInitFile.getAbsolutePath()+"'." );
                                }
                            }
                        }
                    }
                });
            }
        }

        return hsmInitKeystorePassword;
    }

    /**
     * load the properties from the specified file.
     * @param propFile the properties file to load
     * @return a properties object populated with the values from the specified file or null if there was an error
     * (eg. file not found or an error reading the properties file)
     */
    private Properties loadProperties(File propFile) {
        Properties props = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(propFile);
            props = new Properties();
            props.load(fis);
        } catch (FileNotFoundException e) {
            logger.info("Didn't find " + propFile.getAbsolutePath() + ").");
            props = null;
        } catch (IOException e) {
            logger.severe("Error while reading " + propFile.getAbsolutePath() + ")." + ExceptionUtils.getMessage(e));
            props = null;
        } finally {
            ResourceUtils.closeQuietly(fis);
        }
        return props;
    }
}
