package com.l7tech.server.security.keystore;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.security.keystore.sca.ScaSsgKeyStore;
import com.l7tech.server.security.keystore.software.DatabasePkcs12SsgKeyStore;
import com.l7tech.server.security.sharedkey.SharedKeyManager;
import org.springframework.transaction.annotation.Transactional;

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
    private final ServerConfig serverConfig;
    private char[] sslKeystorePassphrase;

    private boolean initialized = false;
    private List<SsgKeyFinder> keystores = null;

    public SsgKeyStoreManagerImpl(SharedKeyManager skm, KeystoreFileManager kem, ServerConfig serverConfig, char[] sslKeystorePassphrase) throws KeyStoreException, FindException {
        if ( sslKeystorePassphrase == null || sslKeystorePassphrase.length==0 ) throw new IllegalArgumentException("sslKeystorePassphrase is required");
        if ( kem instanceof KeystoreFileManagerImpl ) throw new IllegalArgumentException("kem autoproxy failure");
        this.keystoreFileManager = kem;
        this.softwareKeystorePasssword = toPassphrase(skm.getSharedKey());
        this.serverConfig = serverConfig;
        this.sslKeystorePassphrase = sslKeystorePassphrase;
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
                    list.add(ScaSsgKeyStore.getInstance(id, name, sslKeystorePassphrase, keystoreFileManager));
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
}
