package com.l7tech.server.security.keystore.sca;

import com.l7tech.common.security.JceProvider;
import com.l7tech.common.security.keystore.SsgKeyEntry;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.Functions;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.security.keystore.JdkKeyStoreBackedSsgKeyStore;
import com.l7tech.server.security.keystore.KeystoreFile;
import com.l7tech.server.security.keystore.KeystoreFileManager;
import static com.l7tech.server.security.keystore.SsgKeyFinder.SsgKeyStoreType.PKCS11_HARDWARE;
import com.l7tech.server.security.keystore.SsgKeyStore;
import org.jboss.util.stream.NullInputStream;

import javax.security.auth.x500.X500Principal;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;

/**
 * SsgKeyStore view of this node's local SCA 6000 board.
 */
public class ScaSsgKeyStore extends JdkKeyStoreBackedSsgKeyStore implements SsgKeyStore {
    protected static final Logger logger = Logger.getLogger(ScaSsgKeyStore.class.getName());
    private static final String DB_FORMAT = "hsm.sca.targz";
    private static final long refreshTime = 5 * 1000;

    private static ScaSsgKeyStore INSTANCE = null;

    private final long id;
    private final String name;
    private final char[] password;
    private final KeystoreFileManager kem;
    private final ScaManager scaManager;
    private KeyStore keystore;
    private int keystoreVersion = -1;
    private long lastLoaded = 0;

    /**
     * Get the global ScaSsgKeyStore instance, creating it if necessary.
     * All parameters are ignored once the global instance is created.
     *
     * @param id  the Id of this keystore, and also the object ID of the KeystoreFile row to use as backing store.  Required.
     * @param name the name to return when asked.  Required.
     * @param password the password to use when accessing the PKCS#11 keystore
     * @param kem the KeystoreFileManager.  Required.
     * @return the ScaSsgKeyStore instance for this process
     * @throws KeyStoreException  if the global instance cannot be created
     */
    public synchronized static ScaSsgKeyStore getInstance(long id, String name, char[] password, KeystoreFileManager kem) throws KeyStoreException {
        if (INSTANCE != null)
            return INSTANCE;
        return INSTANCE = new ScaSsgKeyStore(id, name, password, kem);
    }

    private ScaSsgKeyStore(long id, String name, char[] password, KeystoreFileManager kem) throws KeyStoreException {
        if (!(JceProvider.PKCS11_ENGINE.equals(JceProvider.getEngineClass())))
            throw new KeyStoreException("Can only create ScaSsgKeyStore if current JceProvider is " + JceProvider.PKCS11_ENGINE);
        if (kem == null)
            throw new IllegalArgumentException("KeystoreFileManager is required");
        try {
            this.scaManager = new ScaManager();
        } catch (ScaException e) {
            throw new KeyStoreException("Unable to create ScaSsgKeyStore: ScaManager initialization failed: " + ExceptionUtils.getMessage(e), e);
        }
        this.id = id;
        this.name = name;
        this.kem = kem;
        this.password = password;
    }

    public long getOid() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SsgKeyStoreType getType() {
        return PKCS11_HARDWARE;
    }

    protected synchronized KeyStore keyStore() throws KeyStoreException {
        if (keystore == null || System.currentTimeMillis() - lastLoaded > refreshTime) {
            try {
                KeystoreFile keystoreFile = kem.findByPrimaryKey(getOid());
                int dbVersion = keystoreFile.getVersion();
                if (keystore != null && keystoreVersion == dbVersion) {
                    // No changes since last time we checked.  Just use the one we've got.
                    return keystore;
                }

                byte[] bytes = keystoreFile.getDatabytes();
                if (bytes != null && bytes.length > 0 && !keystoreFile.getFormat().equals(DB_FORMAT))
                    throw new KeyStoreException("Database key data format unrecognized for SCA keystore named " + name +
                                                "(expected \"" + DB_FORMAT + "\"; found \"" + keystoreFile.getFormat() + "\")");
                keystore = bytesToKeyStore(bytes);
                keystoreVersion = dbVersion;
            } catch (FindException e) {
                throw new KeyStoreException("Unable to load hardware keystore data from database for keystore named " + name + ": " + ExceptionUtils.getMessage(e), e);
            }
        }
        return keystore;
    }

    private synchronized KeyStore bytesToKeyStore(byte[] bytes) throws KeyStoreException {
        try {
            if (bytes != null && bytes.length > 0) {
                logger.info("Copying updated keystore info from database to local keydata directory for SCA keystore named " + name);
                scaManager.saveKeydata(bytes);
            }

            lastLoaded = System.currentTimeMillis();
            keystore = KeyStore.getInstance("PKCS11");
            keystore.load(new NullInputStream(), password);
            return keystore;
        } catch (ScaException e) {
            final String msg = "Unable to update local hardware keystore from database for keystore named " + name + ": " + ExceptionUtils.getMessage(e);
            logger.log(Level.SEVERE, "Possible local keystore damage: " + msg, e); // TODO make keystore save safely
            throw new KeyStoreException(msg, e);
        } catch (Exception e) {
            final String msg = "Unable to initialize keystore named " + name + ": " + ExceptionUtils.getMessage(e);
            throw new KeyStoreException(msg, e);
        }
    }

    private synchronized byte[] keyStoreToBytes() throws KeyStoreException {
        try {
            logger.info("Copying updated keystore info from local keydata directory to database for SCA keystore named " + name);
            return scaManager.loadKeydata();
        } catch (ScaException e) {
            final String msg = "Unable to read local hardware keystore data for keystore named " + name + ": " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, e);
            throw new KeyStoreException(e);
        }
    }

    protected String getFormat() {
        return DB_FORMAT;
    }

    protected Logger getLogger() {
        return logger;
    }

    protected char[] getEntryPassword() {
        return new char[0];  // unused by PKCS#11
    }

    public synchronized Future<Boolean> deletePrivateKeyEntry(final String keyAlias) throws KeyStoreException {
        if (isSystemAlias(keyAlias))
            throw new KeyStoreException("The specified entry is a system private key and cannot be deleted.");
        return super.deletePrivateKeyEntry(keyAlias);
    }

    public synchronized Future<Boolean> storePrivateKeyEntry(final SsgKeyEntry entry, final boolean overwriteExisting) throws KeyStoreException {
        if (isReservedAlias(entry.getAlias()))
            throw new KeyStoreException("The specified private key alias is reserved for system use and cannot be created.");
        return super.storePrivateKeyEntry(entry, overwriteExisting);
    }

    public synchronized Future<X509Certificate> generateKeyPair(final String alias, final X500Principal dn, final int keybits, final int expiryDays) throws GeneralSecurityException {
        if (isSystemAlias(alias))
            throw new KeyStoreException("The specified entry is a system private key and cannot be overwritten.");
        if (isReservedAlias(alias))
            throw new KeyStoreException("The specified private key alias is reserved for system use and cannot be created.");
        return super.generateKeyPair(alias, dn, keybits, expiryDays);
    }

    private boolean isSystemAlias(String keyAlias) {
        // TODO find some other way to enforce this besides this awful hack
        return "tomcat".equalsIgnoreCase(keyAlias) || "ssgroot".equalsIgnoreCase(keyAlias);
    }

    private boolean isReservedAlias(String keyAlias) {
        // TODO find some other way to enforce this besides this awful hack
        return ("CA".equalsIgnoreCase(keyAlias) || "SSL".equalsIgnoreCase(keyAlias));
    }

    /**
     * Load the keystore from the database, mutate it, and save it back, all atomically.
     *
     * @param mutator  a Runnable that will mutate the current {@link #keystore}, which will be guaranteed
     *                 to be up-to-date and non-null when the runnable is invoked.
     * @throws KeyStoreException if the runnable throws a RuntimeException or if any other problem occurs during
     *                           the process
     * @return the value returned by the mutator
     */
    protected synchronized <OUT> Future<OUT> mutateKeystore(final Functions.Nullary<OUT> mutator) throws KeyStoreException {
        return mutationExecutor.submit(new Callable<OUT>() {
            public OUT call() throws Exception {

                final Object[] out = new Object[] { null };
                try {
                    KeystoreFile updated = kem.updateDataBytes(getOid(), new Functions.Unary<byte[], byte[]>() {
                        public byte[] call(byte[] bytes) {
                            try {
                                keystore = bytesToKeyStore(bytes);
                                lastLoaded = System.currentTimeMillis();
                                out[0] = mutator.call();
                                return keyStoreToBytes();
                            } catch (KeyStoreException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                    keystoreVersion = updated.getVersion();
                } catch (UpdateException e) {
                    throw new KeyStoreException(e);
                }
                //noinspection unchecked
                return (OUT)out[0];
            }
        });
    }
}
