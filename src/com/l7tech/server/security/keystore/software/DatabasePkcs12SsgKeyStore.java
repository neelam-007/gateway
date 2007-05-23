package com.l7tech.server.security.keystore.software;

import com.l7tech.cluster.ClusterPropertyManager;
import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.Functions;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.security.keystore.JdkKeyStoreBackedSsgKeyStore;
import com.l7tech.server.security.keystore.KeystoreFile;
import com.l7tech.server.security.keystore.KeystoreFileManager;
import com.l7tech.server.security.keystore.SsgKeyStore;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A KeyFinder that works with PKCS#12 files read from the database.
 */
public class DatabasePkcs12SsgKeyStore extends JdkKeyStoreBackedSsgKeyStore implements SsgKeyStore {
    protected static final Logger logger = Logger.getLogger(DatabasePkcs12SsgKeyStore.class.getName());
    private static final long refreshTime = 5 * 60 * 1000;
    private static final String DB_FORMAT = "sdb.pkcs12";

    private final long id;
    private final String name;
    private final ClusterPropertyManager cpm; // TODO extract cluster shared key from this
    private final KeystoreFileManager kem;
    private final char[] password;

    private KeyStore cachedKeystore = null;
    private long lastLoaded = 0;

    /**
     * Create an SsgKeyStore that uses a PKCS#12 file in a KeystoreFile in the database as its backing store.
     *
     * @param id       the ID of this SsgKeyStore.  This will also be the ID of the KeystoreFile instance we use
     *                 as our backing store.
     * @param name     The name of this SsgKeyStore.  Required.
     * @param cpm      ClusterPropertyManager.  Required.
     * @param kem      KeystoreFileManager.  Required.
     * @param password the password to use to encrypt the PKCS#12 data bytes.  Required.
     */
    public DatabasePkcs12SsgKeyStore(long id, String name, ClusterPropertyManager cpm, KeystoreFileManager kem, char[] password) {
        this.id = id;
        this.name = name;
        this.cpm = cpm;
        this.kem = kem;
        this.password = password;
        if (kem == null || cpm == null)
            throw new IllegalArgumentException("ClusterPropertyManager and KeystoreFileManager must be provided");
    }

    public long getId() {
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
                KeystoreFile keystoreFile = kem.findByPrimaryKey(getId());
                cachedKeystore = bytesToKeyStore(keystoreFile.getDatabytes());
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
                if (logger.isLoggable(Level.FINE)) logger.fine("Loading existing PKCS#12 data for keystore id " + getId());
                inputStream = new ByteArrayInputStream(bytes);
            } else {
                if (logger.isLoggable(Level.INFO)) logger.info("Creating new empty PKCS#12 file for keystore id " + getId());
            }

            KeyStore keystore = KeyStore.getInstance("PKCS12");
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

    protected synchronized <OUT> OUT mutateKeystore(final Functions.Nullary<OUT> mutator) throws KeyStoreException {
        final Object[] out = new Object[] { null };
        try {
            kem.updateDataBytes(getId(), new Functions.Unary<byte[], byte[]>() {
                public byte[] call(byte[] bytes) {
                    try {
                        cachedKeystore = bytesToKeyStore(bytes);
                        lastLoaded = System.currentTimeMillis();
                        out[0] = mutator.call();
                        return keyStoreToBytes(cachedKeystore);
                    } catch (KeyStoreException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (UpdateException e) {
            throw new KeyStoreException(e);
        }
        //noinspection unchecked
        return (OUT)out[0];
    }
}
