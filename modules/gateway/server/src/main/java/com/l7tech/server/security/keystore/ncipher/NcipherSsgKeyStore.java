package com.l7tech.server.security.keystore.ncipher;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.security.keystore.JdkKeyStoreBackedSsgKeyStore;
import com.l7tech.server.security.keystore.KeystoreFile;
import com.l7tech.server.security.keystore.KeystoreFileManager;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.util.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
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
    private static final File KMDATA_LOCAL_DIR = new File(SyspropUtil.getString("com.l7tech.server.security.keystore.ncipher.kmdataLocalPath", "/opt/nfast/kmdata/local"));

    private final long id;
    private final String name;
    private final KeystoreFileManager kem;
    private KeyStore keystore;
    private String keystoreMetadata; // Bytes produced by calling store() on the nCipher keystore.  Seems to be a 40 byte hex string identifying the keystore instance within the security world.
    private int keystoreVersion = -1;
    private long lastLoaded = 0;

    public NcipherSsgKeyStore(long id, String name, KeystoreFileManager kem) throws KeyStoreException {
        this.id = id;
        this.name = name;
        this.kem = kem;
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
                    Pair<String, KeyStore> ks = bytesToKeyStore(bytes);
                    this.keystoreMetadata = ks.left;
                    this.keystore = ks.right;
                } else {
                    // No existing keystore data present -- will need to create a new one.
                    // Begin a write transaction to create some (or to use existing in the unlikely event another node created some in the meantime).
                    kem.updateDataBytes(keystoreFile.getOid(), new Functions.Unary<byte[], byte[]>() {
                        @Override
                        public byte[] call(byte[] bytes) {
                            try {
                                final Pair<String, KeyStore> ks;
                                if (bytes == null || bytes.length < 1) {
                                    ks = createNewKeyStore();
                                } else {
                                    ks = bytesToKeyStore(bytes);
                                }
                                keystoreMetadata = ks.left;
                                keystore = ks.right;
                                return keyStoreToBytes(keystoreMetadata, keystore);
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
                        KeystoreFile updated = kem.updateDataBytes(getOid(), new Functions.Unary<byte[], byte[]>() {
                            @Override
                            public byte[] call(byte[] bytes) {
                                if (transactionCallback != null)
                                    transactionCallback.run();
                                try {
                                    Pair<String, KeyStore> ks = bytesToKeyStore(bytes);
                                    keystoreMetadata = ks.left;
                                    keystore = ks.right;
                                    lastLoaded = System.currentTimeMillis();
                                    out[0] = mutator.call();
                                    return keyStoreToBytes(keystoreMetadata, keystore);
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

    private synchronized Pair<String, KeyStore> bytesToKeyStore(byte[] bytes) throws KeyStoreException {
        logger.info("Merging nCipher keystore data from database to local disk");
        try {
            NcipherKeyStoreData ksd = NcipherKeyStoreData.createFromBytes(bytes);
            Set<String> written = ksd.saveFilesetToLocalDisk(KMDATA_LOCAL_DIR);

            // TODO Defer commit and KeyStore.load() until commit phase of enclosing transaction
            NcipherKeyStoreData.commitWrittenFiles(KMDATA_LOCAL_DIR, written);

            KeyStore keystore = JceProvider.getInstance().getKeyStore(KEYSTORE_TYPE);
            keystore.load(new ByteArrayInputStream(ksd.keystoreMetadata.getBytes(Charsets.UTF8)), null);
            lastLoaded = System.currentTimeMillis();
            return new Pair<String, KeyStore>(ksd.keystoreMetadata, keystore);

        } catch (IOException e) {
            throw new KeyStoreException("Unable to sync nCipher keystore data from database to disk: " + ExceptionUtils.getMessage(e), e);
        } catch (CertificateException e) {
            throw new KeyStoreException("Unable to sync nCipher keystore data from database to disk: " + ExceptionUtils.getMessage(e), e);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException("Unable to sync nCipher keystore data from database to disk: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private synchronized Pair<String, KeyStore> createNewKeyStore() throws KeyStoreException {
        logger.info("Creating new keystore within nCipher security world");
        KeyStore keystore = JceProvider.getInstance().getKeyStore(KEYSTORE_TYPE);
        BufferPoolByteArrayOutputStream os = null;
        try {
            keystore.load(null, null);

            // Now initialize the keystore and record its identifier
            os = new BufferPoolByteArrayOutputStream();
            keystore.store(os, null);
            String keystoreMetadata = os.toString(Charsets.UTF8);

            return new Pair<String, KeyStore>(keystoreMetadata, keystore);
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

    private synchronized byte[] keyStoreToBytes(String keystoreMetadata, KeyStore keyStore) throws KeyStoreException {
        logger.info("Merging nCipher keystore data from local disk to database");
        BufferPoolByteArrayOutputStream os = null;
        try {
            // Now initialize the keystore and record its identifier
            os = new BufferPoolByteArrayOutputStream();
            keyStore.store(os, null);
            String savedMetadata = os.toString(Charsets.UTF8);
            if (!keystoreMetadata.equals(savedMetadata))
                throw new KeyStoreException("Saved keystore metadata does not match existing metadata of:" + keystoreMetadata);

            NcipherKeyStoreData ksd = NcipherKeyStoreData.createFromLocalDisk(keystoreMetadata, KMDATA_LOCAL_DIR);
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
