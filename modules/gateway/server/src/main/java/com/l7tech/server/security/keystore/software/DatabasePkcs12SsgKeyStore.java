package com.l7tech.server.security.keystore.software;

import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.security.keystore.JdkKeyStoreBackedSsgKeyStore;
import com.l7tech.server.security.keystore.KeystoreFile;
import com.l7tech.server.security.keystore.KeystoreFileManager;
import com.l7tech.server.security.keystore.SsgKeyStore;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A KeyFinder that works with PKCS#12 files read from the database.
 */
public class DatabasePkcs12SsgKeyStore extends JdkKeyStoreBackedSsgKeyStore implements SsgKeyStore {
    protected static final Logger logger = Logger.getLogger(DatabasePkcs12SsgKeyStore.class.getName());
    private static final long refreshTime = 5 * 1000;
    private static final String DB_FORMAT = "sdb.pkcs12";

    private final long id;
    private final String name;
    private final KeystoreFileManager kem;
    private final char[] password;

    private KeyStore cachedKeystore = null;
    private int keystoreVersion = -1;
    private long lastLoaded = 0;

    /**
     * Create an SsgKeyStore that uses a PKCS#12 file in a KeystoreFile in the database as its backing store.
     *
     * @param oid      the OID of this SsgKeyStore.  This will also be the OID of the KeystoreFile instance we use
     *                 as our backing store.
     * @param name     The name of this SsgKeyStore.  Required.
     * @param kem      KeystoreFileManager.  Required.
     * @param password the password to use to encrypt the PKCS#12 data bytes.  Required.
     */
    public DatabasePkcs12SsgKeyStore(long oid, String name, KeystoreFileManager kem, char[] password) {
        this.id = oid;
        this.name = name;
        this.kem = kem;
        this.password = password;
        if (kem == null)
            throw new IllegalArgumentException("ClusterPropertyManager and KeystoreFileManager must be provided");
    }

    public String getId() {
        return String.valueOf(id);
    }

    public long getOid() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SsgKeyStoreType getType() {
        return SsgKeyStoreType.PKCS12_SOFTWARE;
    }

    protected synchronized KeyStore keyStore() throws KeyStoreException {
        if (cachedKeystore == null || System.currentTimeMillis() - lastLoaded > refreshTime) {
            try {
                KeystoreFile keystoreFile = kem.findByPrimaryKey(getOid());
                int dbVersion = keystoreFile.getVersion();
                if (cachedKeystore != null && keystoreVersion == dbVersion) {
                    // No changes since last time we checked.  Just use the one we've got.
                    return cachedKeystore;
                }
                cachedKeystore = bytesToKeyStore(keystoreFile.getDatabytes());
                keystoreVersion = keystoreFile.getVersion();
                lastLoaded = System.currentTimeMillis();
            } catch (FindException e) {
                throw new KeyStoreException("Unable to load software database keystore named " + name + ": " + ExceptionUtils.getMessage(e), e);
            }
        }
        return cachedKeystore;
    }

    protected String getFormat() {
        return DB_FORMAT;
    }

    protected Logger getLogger() {
        return logger;
    }

    protected char[] getEntryPassword() {
        return password;
    }

    private KeyStore bytesToKeyStore(byte[] bytes) throws KeyStoreException {
        try {
            ByteArrayInputStream inputStream = null;
            if (bytes != null && bytes.length > 0) {
                if (logger.isLoggable(Level.FINE)) logger.fine("Loading existing PKCS#12 data for keystore id " + getOid());
                inputStream = new ByteArrayInputStream(bytes);
            } else {
                if (logger.isLoggable(Level.INFO)) logger.info("Creating new empty PKCS#12 file for keystore id " + getOid());
            }

            KeyStore keystore = KeyStore.getInstance("PKCS12", new BouncyCastleProvider());
            keystore.load(inputStream, password); // If no existing data, null inputStream causes new keystore to be created
            return keystore;
        } catch (KeyStoreException e) {
            throw new KeyStoreException("Unable to load software database keystore named " + name + ": " + ExceptionUtils.getMessage(e), e);
        } catch (IOException e) {
            throw new KeyStoreException("Unable to load software database keystore named " + name + ": " + ExceptionUtils.getMessage(e), e);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException("Unable to load software database keystore named " + name + ": " + ExceptionUtils.getMessage(e), e);
        } catch (CertificateException e) {
            throw new KeyStoreException("Unable to load software database keystore named " + name + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    private byte[] keyStoreToBytes(KeyStore keystore) throws KeyStoreException {
        BufferPoolByteArrayOutputStream outputStream = new BufferPoolByteArrayOutputStream();
        try {
            keystore.store(outputStream, password);
            return outputStream.toByteArray();
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException("Unable to save software database keystore named " + name + ": " + ExceptionUtils.getMessage(e), e);
        } catch (IOException e) {
            throw new KeyStoreException("Unable to save software database keystore named " + name + ": " + ExceptionUtils.getMessage(e), e);
        } catch (CertificateException e) {
            throw new KeyStoreException("Unable to save software database keystore named " + name + ": " + ExceptionUtils.getMessage(e), e);
        } finally {
            outputStream.close();
        }
    }

    protected <OUT> Future<OUT> mutateKeystore(final Callable<OUT> mutator) throws KeyStoreException {
        return mutationExecutor.submit(AdminInfo.find().wrapCallable(new Callable<OUT>() {
            public OUT call() throws Exception {
                final Object[] out = new Object[] { null };
                try {
                    synchronized (DatabasePkcs12SsgKeyStore.this) {
                        KeystoreFile updated = kem.updateDataBytes(getOid(), new Functions.Unary<byte[], byte[]>() {
                            public byte[] call(byte[] bytes) {
                                try {
                                    cachedKeystore = bytesToKeyStore(bytes);
                                    lastLoaded = System.currentTimeMillis();
                                    out[0] = mutator.call();
                                    return keyStoreToBytes(cachedKeystore);
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
}
