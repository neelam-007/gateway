package com.l7tech.server.security.keystore.ncipher;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.security.keystore.*;
import com.l7tech.util.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A key store that assumes all cluster nodes possess access to nCipher devices programmed into the same security world.
 * Key material is replicated via the keystore_file table in the database.
 */
public class NcipherSsgKeyStore extends JdkKeyStoreBackedSsgKeyStore implements SsgKeyStore {
    private static final Logger logger = Logger.getLogger(NcipherSsgKeyStore.class.getName());
    private static final String DB_FORMAT = "hsm.NcipherKeyStoreData";
    private static final String KEYSTORE_TYPE = "nCipher.sworld";
    private static final long refreshTime = 5 * 1000;
    private static final File KMDATA_LOCAL_DIR = new File( ConfigFactory.getProperty( "com.l7tech.server.security.keystore.ncipher.kmdataLocalPath", "/opt/nfast/kmdata/local" ) );
    static final String KF_PROP_INITIAL_KEYSTORE_ID = "initialKeystoreId";
    static final String KF_PROP_IGNORE_KEYSTORE_IDS = "ignoreKeystoreIds";

    private final long id;
    private final String name;
    private final KeystoreFileManager kem;
    private KeyStore keystore;
    private NcipherKeyStoreData keystoreData;
    private int keystoreVersion = -1;
    private long lastLoaded = 0;
    private boolean checkForDeletedFilesOnNextStoreToDisk = false;

    public NcipherSsgKeyStore(long id, String name, KeystoreFileManager kem, KeyAccessFilter keyAccessFilter, SsgKeyMetadataFinder ssgKeyMetadataFinder) throws KeyStoreException {
        super(keyAccessFilter, ssgKeyMetadataFinder);
        this.id = id;
        this.name = name;
        this.kem = kem;
    }

    @Override
    public Future<Boolean> deletePrivateKeyEntry(Runnable transactionCallback, final String keyAlias) throws KeyStoreException {
        return mutateKeystore(transactionCallback, new Callable<Boolean>() {
            @Override
            public Boolean call() throws KeyStoreException {
                KeyStore ks = keyStore();
                if (ks.containsAlias(keyAlias)) {
                    ks.deleteEntry(keyAlias);
                    checkForDeletedFilesOnNextStoreToDisk = true;
                    return Boolean.TRUE;
                } else {
                    return Boolean.FALSE;
                }
            }
        });
    }

    private Set<String> listAllRelevantKeyFiles(String keystoreId) throws IOException, KeyStoreException {
        return NcipherKeyStoreData.createFromLocalDisk(keystoreId, NcipherSsgKeyStore.KMDATA_LOCAL_DIR).fileset.keySet();
    }

    private static Set<String> findDeletedFiles(Set<String> filesBeforeDelete, Set<String> filesAfterDelete) {
        Set<String> deletedFiles = new LinkedHashSet<String>(filesBeforeDelete);
        deletedFiles.removeAll(filesAfterDelete);
        return deletedFiles;
    }

    @Override
    protected String getFormat() {
        return DB_FORMAT;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected char[] getEntryPassword() {
        // Should not require a per-entry password
        return new char[0];
    }

    protected synchronized KeyStore keyStore() throws KeyStoreException {
        if (keystore == null || System.currentTimeMillis() - lastLoaded > refreshTime) {
            try {
                KeystoreFile keystoreFile = kem.findByPrimaryKey(getOid());
                if (keystoreFile == null)
                    throw new KeyStoreException("No keystore_file found with objectid " + getOid());
                int dbVersion = keystoreFile.getVersion();
                if (keystore != null && keystoreVersion == dbVersion) {
                    // No changes since last time we checked.  Just use the one we've got.
                    return keystore;
                }

                byte[] bytes = keystoreFile.getDatabytes();
                if (!keystoreFile.getFormat().equals(DB_FORMAT))
                    throw new KeyStoreException("Database key data format unrecognized for nCipher keystore named " + name +
                            "(expected \"" + DB_FORMAT + "\"; found \"" + keystoreFile.getFormat() + "\")");

                if (bytes != null && bytes.length >= 1) {
                    // Load existing keystore data.
                    Pair<NcipherKeyStoreData, KeyStore> ks = bytesToKeyStore(bytes);
                    this.keystore = ks.right;
                } else {
                    // No existing keystore data present -- will need to create a new one.
                    // Begin a write transaction to create some (or to use existing in the unlikely event another node created some in the meantime).
                    kem.mutateKeystoreFile(keystoreFile.getOid(), new Functions.UnaryVoid<KeystoreFile>() {
                        @Override
                        public void call(KeystoreFile keystoreFile) {
                            try {
                                byte[] bytes = keystoreFile.getDatabytes();
                                final Pair<NcipherKeyStoreData, KeyStore> ks;
                                if (bytes == null || bytes.length < 1) {
                                    ks = createNewKeyStore();
                                } else {
                                    ks = bytesToKeyStore(bytes);
                                }

                                keystoreData = ks.left;
                                keystore = ks.right;
                                keystoreFile.setProperty(KF_PROP_INITIAL_KEYSTORE_ID, keystoreData.keystoreMetadata);
                                keystoreFile.setDatabytes(keyStoreToBytes(keystoreData, keystore));
                            } catch (KeyStoreException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                }
            } catch (FindException e) {
                throw new KeyStoreException("Unable to load hardware keystore data from database for keystore named " + name + ": " + ExceptionUtils.getMessage(e), e);
            } catch (UpdateException e) {
                throw new KeyStoreException("Unable to initialize hardware keystore data in database for keystore named " + name + ": " + ExceptionUtils.getMessage(e), e);
            }
        }
        return keystore;
    }

    @Override
    protected <OUT> Future<OUT> mutateKeystore(final Runnable transactionCallback, final Callable<OUT> mutator) throws KeyStoreException {
        return submitMutation(AdminInfo.find(false).wrapCallable(new Callable<OUT>() {
            @Override
            public OUT call() throws Exception {

                final Object[] out = new Object[] { null };
                try {
                    synchronized (NcipherSsgKeyStore.this) {
                        KeystoreFile updated = kem.mutateKeystoreFile(getOid(), new Functions.UnaryVoid<KeystoreFile>() {
                            @Override
                            public void call(KeystoreFile keystoreFile) {
                                if (transactionCallback != null)
                                    transactionCallback.run();
                                try {
                                    Pair<NcipherKeyStoreData, KeyStore> ks = bytesToKeyStore(keystoreFile.getDatabytes());
                                    keystoreData = ks.left;
                                    keystore = ks.right;
                                    lastLoaded = System.currentTimeMillis();
                                    out[0] = mutator.call();
                                    keystoreFile.setDatabytes(keyStoreToBytes(keystoreData, keystore));
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                        keystoreVersion = updated.getVersion();
                    }
                } catch (UpdateException e) {
                    throw new KeyStoreException(e);
                }
                //noinspection unchecked
                return (OUT)out[0];
            }
        }));
    }

    private synchronized Pair<NcipherKeyStoreData, KeyStore> bytesToKeyStore(byte[] bytes) throws KeyStoreException {
        logger.info("Merging nCipher keystore data from database to local disk");
        try {
            NcipherKeyStoreData ksd = NcipherKeyStoreData.createFromBytes(bytes);
            Set<String> written = ksd.saveFilesetToLocalDisk(KMDATA_LOCAL_DIR);

            // TODO Defer commit and KeyStore.load() until commit phase of enclosing transaction
            ksd.commitWrittenFiles(KMDATA_LOCAL_DIR, written);

            KeyStore keystore = JceProvider.getInstance().getKeyStore(KEYSTORE_TYPE);
            keystore.load(new ByteArrayInputStream(ksd.keystoreMetadata.getBytes(Charsets.UTF8)), null);
            lastLoaded = System.currentTimeMillis();
            return new Pair<NcipherKeyStoreData, KeyStore>(ksd, keystore);

        } catch (IOException e) {
            throw new KeyStoreException("Unable to sync nCipher keystore data from database to disk: " + ExceptionUtils.getMessage(e), e);
        } catch (CertificateException e) {
            throw new KeyStoreException("Unable to sync nCipher keystore data from database to disk: " + ExceptionUtils.getMessage(e), e);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException("Unable to sync nCipher keystore data from database to disk: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private synchronized Pair<NcipherKeyStoreData, KeyStore> createNewKeyStore() throws KeyStoreException {
        // Check for files on disk from a preexisting key_jcecsp_* keystore, before creating a new empty one
        Pair<NcipherKeyStoreData, KeyStore> ret = createNewKeyStoreFromExistingLocalFiles();
        return ret != null ? ret : createNewKeyStoreEmpty();
    }

    private Pair<NcipherKeyStoreData, KeyStore> createNewKeyStoreFromExistingLocalFiles() throws KeyStoreException {
        List<String> identifiersToTry = new ArrayList<String>();
        identifiersToTry = checkForConfiguredInitialKeystoreIdentifiers(identifiersToTry);
        return tryLoadLocalIdentifiers(identifiersToTry);
    }

    private static Pair<NcipherKeyStoreData, KeyStore> tryLoadLocalIdentifiers(List<String> identifiersToTry) throws KeyStoreException {
        Pair<NcipherKeyStoreData, KeyStore> ret = null;
        String lastMessage = null;
        Throwable lastException = null;

        // See if at least one ID represents a lodable keystore with at least one existing key entry
        for (String keystoreId : identifiersToTry) {
            try {
                logger.info("Attempting to load from nCipher security world a preexisting keystore with ID " + keystoreId);
                KeyStore ks = loadKeystoreByKeystoreId(keystoreId);
                if (ks != null) {
                    // found one
                    NcipherKeyStoreData ksd = NcipherKeyStoreData.createFromLocalDisk(keystoreId, KMDATA_LOCAL_DIR);
                    ret = new Pair<NcipherKeyStoreData, KeyStore>(ksd, ks);
                    break;
                }
            } catch (Exception e) {
                lastMessage = "Unable to load preexisting nCipher keystore with ID " + keystoreId + ": " + ExceptionUtils.getMessage(e);
                logger.log(Level.WARNING, lastMessage, e);
                lastException = e;
            }
        }

        if (lastException != null && lastMessage != null)
            throw new KeyStoreException(lastMessage, lastException);

        return ret;
    }

    private List<String> checkForConfiguredInitialKeystoreIdentifiers(List<String> identifiersToTry) throws KeyStoreException {
        try {
            // Check if we have a property telling us what ID to use
            KeystoreFile keystoreFile = kem.findByPrimaryKey(getOid());
            String keystoreId = keystoreFile.getProperty(KF_PROP_INITIAL_KEYSTORE_ID);
            if (keystoreId != null && keystoreId.trim().length() > 0)
                identifiersToTry.add(keystoreId);
        } catch (FindException e) {
            throw new KeyStoreException("Unable to look up initial keystore ID: " + ExceptionUtils.getMessage(e), e);
        }

        if (identifiersToTry.isEmpty()) {
            Set<String> toIgnore = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
            try {
                KeystoreFile keystoreFile = kem.findByPrimaryKey(getOid());
                String ignoreIds = keystoreFile.getProperty(KF_PROP_IGNORE_KEYSTORE_IDS);
                if (ignoreIds != null && ignoreIds.trim().length() > 0) {
                    String[] ids = ignoreIds.split("\\s*,\\s*");
                    toIgnore.addAll(Arrays.asList(ids));
                }
            } catch (FindException e) {
                throw new KeyStoreException("Unable to look up keystore IDs to ignore: " + ExceptionUtils.getMessage(e), e);
            }

            identifiersToTry = NcipherKeyStoreData.readKeystoreIdentifiersFromLocalDisk(KMDATA_LOCAL_DIR, toIgnore);
        }

        return identifiersToTry;
    }

    private static KeyStore loadKeystoreByKeystoreId(String keystoreId) throws KeyStoreException {
        KeyStore ret = null;
        KeyStore keystore = JceProvider.getInstance().getKeyStore(KEYSTORE_TYPE);
        PoolByteArrayOutputStream os = null;
        try {
            // See if this ID represents a loadable keystore with at least one existing key entry
            keystore.load(new ByteArrayInputStream(keystoreId.getBytes(Charsets.UTF8)), null);
            Enumeration<String> aliases = keystore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (keystore.isKeyEntry(alias)) {
                    ret = keystore;
                    break;
                }
            }
        } catch (IOException e) {
            throw new KeyStoreException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException(e);
        } catch (CertificateException e) {
            throw new KeyStoreException(e);
        } finally {
            ResourceUtils.closeQuietly(os);
        }
        return ret;
    }

    private static Pair<NcipherKeyStoreData, KeyStore> createNewKeyStoreEmpty() throws KeyStoreException {
        logger.info("Creating new keystore within nCipher security world");
        KeyStore keystore = JceProvider.getInstance().getKeyStore(KEYSTORE_TYPE);
        PoolByteArrayOutputStream os = null;
        try {
            keystore.load(null, null);

            // Now initialize the keystore and record its identifier
            os = new PoolByteArrayOutputStream();
            keystore.store(os, null);
            String keystoreMetadata = os.toString(Charsets.UTF8);

            NcipherKeyStoreData ksd = NcipherKeyStoreData.createFromLocalDisk(keystoreMetadata, KMDATA_LOCAL_DIR);
            return new Pair<NcipherKeyStoreData, KeyStore>(ksd, keystore);
        } catch (IOException e) {
            throw new KeyStoreException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException(e);
        } catch (CertificateException e) {
            throw new KeyStoreException(e);
        } finally {
            ResourceUtils.closeQuietly(os);
        }
    }

    private synchronized byte[] keyStoreToBytes(NcipherKeyStoreData ksd, KeyStore keyStore) throws KeyStoreException {
        logger.info("Merging nCipher keystore data from local disk to database");
        PoolByteArrayOutputStream os = null;
        try {
            final String keystoreId = ksd.keystoreMetadata;
            boolean recordDeletes = checkForDeletedFilesOnNextStoreToDisk;
            checkForDeletedFilesOnNextStoreToDisk = false;

            Set<String> filesBeforeStore = recordDeletes ? listAllRelevantKeyFiles(keystoreId) : Collections.<String>emptySet();

            os = new PoolByteArrayOutputStream();
            keyStore.store(os, null);
            String savedMetadata = os.toString(Charsets.UTF8);
            if (!keystoreId.equals(savedMetadata))
                throw new KeyStoreException("Saved keystore metadata does not match existing metadata of:" + keystoreId);

            Set<String> filesAfterStore = recordDeletes ? listAllRelevantKeyFiles(keystoreId) : Collections.<String>emptySet();
            Set<String> deletedFiles = findDeletedFiles(filesBeforeStore, filesAfterStore);
            ksd.addDeletedFiles(deletedFiles);
            for (String deletedFile : deletedFiles) {
                logger.info("Marking nCipher key file as to be deleted cluster-wide: " + deletedFile);
            }

            ksd.loadFilesetFromLocalDisk(KMDATA_LOCAL_DIR);
            return ksd.toBytes();

        } catch (IOException e) {
            throw new KeyStoreException(e);
        } catch (CertificateException e) {
            throw new KeyStoreException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException(e);
        } finally {
            ResourceUtils.closeQuietly(os);
        }
    }

    @Override
    public long getOid() {
        return id;
    }

    @Override
    public SsgKeyStoreType getType() {
        return SsgKeyStoreType.NCIPHER_HARDWARE;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isKeyExportSupported() {
        return false;
    }
}
