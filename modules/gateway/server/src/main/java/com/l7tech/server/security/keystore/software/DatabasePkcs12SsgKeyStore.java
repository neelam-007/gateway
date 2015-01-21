package com.l7tech.server.security.keystore.software;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.security.keystore.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.PoolByteArrayOutputStream;
import org.jetbrains.annotations.NotNull;

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
    private static final long refreshTime = (long) (5 * 1000);
    private static final String DB_FORMAT = "sdb.pkcs12";

    private final Goid id;
    private final String name;
    private final KeystoreFileManager kem;
    private final char[] password;

    private KeyStore cachedKeystore = null;
    private int keystoreVersion = -1;
    private long lastLoaded = 0L;

    /**
     * Create an SsgKeyStore that uses a PKCS#12 file in a KeystoreFile in the database as its backing store.
     *
     * @param id      the GOID of this SsgKeyStore.  This will also be the GOID of the KeystoreFile instance we use
     *                 as our backing store.
     * @param name     The name of this SsgKeyStore.  Required.
     * @param kem      KeystoreFileManager.  Required.
     * @param password the password to use to encrypt the PKCS#12 data bytes.  Required.
     * @param keyAccessFilter key access filter.  Required.
     * @param metadataManager the SsgKeyMetadataManager.
     */
    public DatabasePkcs12SsgKeyStore(Goid id, String name, KeystoreFileManager kem, char[] password, KeyAccessFilter keyAccessFilter, @NotNull SsgKeyMetadataManager metadataManager) {
        super(keyAccessFilter, metadataManager);
        this.id = id;
        this.name = name;
        this.kem = kem;
        this.password = password;
        if (kem == null)
            throw new IllegalArgumentException("ClusterPropertyManager and KeystoreFileManager must be provided");
    }

    @Override
    public String getId() {
        return String.valueOf(id);
    }

    @Override
    public Goid getGoid() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public SsgKeyStoreType getType() {
        return SsgKeyStoreType.PKCS12_SOFTWARE;
    }

    @Override
    protected synchronized KeyStore keyStore() throws KeyStoreException {
        if (cachedKeystore == null || System.currentTimeMillis() - lastLoaded > refreshTime) {
            try {
                KeystoreFile keystoreFile = kem.findByPrimaryKey(getGoid());
                if ( keystoreFile == null ) {
                    // Our backing row has vanished.  Keystore will not be functional
                    throw new KeyStoreException( "No row present in database for software database keystore id " + id + " (named " + name + ")" );
                }
                int dbVersion = keystoreFile.getVersion();
                if (cachedKeystore != null && keystoreVersion == dbVersion) {
                    // No changes since last time we checked.  Just use the one we've got.
                    return cachedKeystore;
                }
                final KeyStore ks;
                if ( null == keystoreFile.getDatabytes() ) {
                    // No databytes in DB -- creating a new keystore from scratch.  Make sure it gets persisted
                    // as soon as it is created, rather than waiting until the first key to be created
                    // (which will cause lots of empty keystores to be created and thrown away until that happens)
                    final KeyStore[] newKs = { null };
                    kem.mutateKeystoreFile( getGoid(), new Functions.UnaryVoid<KeystoreFile>() {
                        @Override
                        public void call( KeystoreFile keystoreFile ) {
                            byte[] dataBytes = keystoreFile.getDatabytes();
                            try {
                                KeyStore ks = bytesToKeyStore( dataBytes );
                                if ( null == dataBytes ) {
                                    // We just created a new keystore
                                    dataBytes = keyStoreToBytes( ks );
                                    keystoreFile.setDatabytes( dataBytes );
                                } // else we are using an existing keystore, and no additional mutation is required.
                                newKs[0] = ks;
                            } catch ( KeyStoreException e ) {
                                throw new RuntimeException( e.getMessage(), e );
                            }
                        }
                    } );
                    ks = newKs[0];
                } else {
                    ks = bytesToKeyStore(keystoreFile.getDatabytes());
                }
                cachedKeystore = ks;
                keystoreVersion = keystoreFile.getVersion();
                lastLoaded = System.currentTimeMillis();
            } catch (FindException e) {
                throw new KeyStoreException("Unable to load software database keystore named " + name + ": " + ExceptionUtils.getMessage(e), e);
            } catch ( UpdateException e ) {
                throw new KeyStoreException("Unable to create initial software database keystore named " + name + ": " + ExceptionUtils.getMessage(e), e);
            }
        }
        return cachedKeystore;
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
        return password;
    }

    @NotNull
    private KeyStore bytesToKeyStore(byte[] bytes) throws KeyStoreException {
        try {
            ByteArrayInputStream inputStream = null;
            if (bytes != null && bytes.length > 0) {
                if (logger.isLoggable(Level.FINE)) logger.fine("Loading existing PKCS#12 data for keystore id " + getGoid());
                inputStream = new ByteArrayInputStream(bytes);
            } else {
                if (logger.isLoggable(Level.INFO)) logger.info("Creating new empty PKCS#12 file for keystore id " + getGoid());
            }

            KeyStore keystore = JceProvider.getInstance().getKeyStore("PKCS12");
            keystore.load(inputStream, password); // If no existing data, null inputStream causes new keystore to be created
            return keystore;

        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new KeyStoreException("Unable to load software database keystore named " + name + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    @NotNull
    private byte[] keyStoreToBytes(KeyStore keystore) throws KeyStoreException {
        PoolByteArrayOutputStream outputStream = new PoolByteArrayOutputStream();
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

    @Override
    protected <OUT> Future<OUT> mutateKeystore(final Runnable transactionCallback, final Callable<OUT> mutator) throws KeyStoreException {
        return submitMutation(AdminInfo.find(false).wrapCallable(new Callable<OUT>() {
            @Override
            public OUT call() throws Exception {
                final Object[] out = new Object[] { null };
                try {
                    synchronized (DatabasePkcs12SsgKeyStore.this) {
                        KeystoreFile updated = kem.mutateKeystoreFile(getGoid(), new Functions.UnaryVoid<KeystoreFile>() {
                            @Override
                            public void call(KeystoreFile keystoreFile) {
                                try {
                                    if (transactionCallback != null)
                                        transactionCallback.run();

                                    cachedKeystore = bytesToKeyStore(keystoreFile.getDatabytes());
                                    lastLoaded = System.currentTimeMillis();
                                    out[0] = mutator.call();
                                    keystoreFile.setDatabytes(keyStoreToBytes(cachedKeystore));
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                        keystoreVersion = updated.getVersion();
                        cachedKeystore = bytesToKeyStore(updated.getDatabytes());
                    }
                } catch (UpdateException e) {
                    // use existing KeyStoreException (if any)
                    if ( ExceptionUtils.causedBy( e, KeyStoreException.class )) {
                        throw ExceptionUtils.getCauseIfCausedBy( e, KeyStoreException.class );
                    }
                    throw new KeyStoreException(e);
                }
                //noinspection unchecked
                return (OUT)out[0];
            }
        }));
    }
}
