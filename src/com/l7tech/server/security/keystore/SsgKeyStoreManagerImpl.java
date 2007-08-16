package com.l7tech.server.security.keystore;

import com.l7tech.common.security.JceProvider;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.KeystoreUtils;
import com.l7tech.server.security.keystore.sca.ScaSsgKeyStore;
import com.l7tech.server.security.keystore.software.DatabasePkcs12SsgKeyStore;
import com.l7tech.server.security.keystore.software.TomcatSsgKeyFinder;
import com.l7tech.server.security.sharedkey.SharedKeyManager;

import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

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
    private final KeystoreUtils keystoreUtils;

    private boolean initialized = false;
    private List<SsgKeyFinder> keystores = null;

    public SsgKeyStoreManagerImpl(SharedKeyManager skm, KeystoreFileManager kem, KeystoreUtils keystoreUtils) throws KeyStoreException, FindException {
        this.keystoreFileManager = kem;
        this.keystoreUtils = keystoreUtils;
        this.softwareKeystorePasssword = toPassphrase(skm.getSharedKey());
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
                    logger.info("Ignoring keystore_file row with a format of hsm because this Gateway node does not appear to have an HSM installed");
                } else {
                    if (createdHsmFinder)
                        throw new KeyStoreException("Database contains more than one keystore_file row with a format of hsm");
                    char[] hsmPassword = keystoreUtils.getSslKeystorePasswd().toCharArray();
                    String sslAlias = keystoreUtils.getSSLAlias();
                    String caAlias = keystoreUtils.getRootAlias();
                    list.add(ScaSsgKeyStore.getInstance(id, name, sslAlias, caAlias, hsmPassword, keystoreFileManager));
                    createdHsmFinder = true;
                }
            } else if (format.equals("ss")) {
                if (haveHsm)
                    logger.info("Ignoring keystore_file row with a format of ss because this Gateway node is using an HSM instead");
                else
                    list.add(new TomcatSsgKeyFinder(id, name, keystoreUtils));
            } else if (format.startsWith("sdb.")) {
                if (haveHsm)
                    logger.info("Ignoring keystore_file row with a format of sdb because this Gateway node is using an HSM instead");
                else
                    list.add(new DatabasePkcs12SsgKeyStore(id, name, keystoreFileManager,  softwareKeystorePasssword));
            }
        }

        // TODO maybe offer software keystores even if HSM is available?
        // TODO support multiple software keystores?
        // TODO remove keystoreUtils entirely, eliminating special privileges for SSL and CA keys?
        keystores = Collections.unmodifiableList(list);
        initialized = true;
    }

    public boolean isHsmAvailable() {
        return JceProvider.PKCS11_ENGINE.equals(JceProvider.getEngineClass());
    }

    public List<SsgKeyFinder> findAll() throws FindException, KeyStoreException {
        init();
        return keystores;
    }

    public SsgKeyFinder findByPrimaryKey(long id) throws FindException, KeyStoreException {
        init();
        for (SsgKeyFinder keystore : keystores) {
            if (keystore.getOid() == id)
                return keystore;
        }
        throw new FindException("No SsgKeyFinder available on this node with id=" + id);
    }
}
