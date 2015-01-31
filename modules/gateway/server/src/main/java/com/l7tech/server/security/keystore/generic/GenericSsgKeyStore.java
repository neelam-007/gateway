package com.l7tech.server.security.keystore.generic;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.io.NullOutputStream;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.security.keystore.JdkKeyStoreBackedSsgKeyStore;
import com.l7tech.server.security.keystore.KeyAccessFilter;
import com.l7tech.server.security.keystore.SsgKeyMetadataManager;
import com.l7tech.util.*;

import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A keystore that can work with any kind of already-configured Java KeyStore instance.
 */
public class GenericSsgKeyStore extends JdkKeyStoreBackedSsgKeyStore {
    public static final String PROP_KEYSTORE_TYPE = "com.l7tech.keystore.type";
    public static final String DEFAULT_KEYSTORE_TYPE = "PKCS11";
    public static final String PROP_LOAD_PATH = "com.l7tech.keystore.path";
    public static final String DEFAULT_LOAD_PATH = "NONE"; // possible values:  NONE, EMPTY, NULL, or a path.  (NONE or EMPTY use an empty inputstream; null uses null; path reads that file)
    public static final String PROP_STORE_PATH = "com.l7tech.keystore.savePath";
    public static final String DEFAULT_STORE_PATH = "DEFAULT"; // possible values:  DEFAULT, NONE, EMPTY, NULL, or a path.  (DEFAULT is same as load path; NONE or EMPTY use a NullOutputStream; null uses null; path reads that file)
    /**
     * possible values:  NULL, EMPTY, DEFAULT, or a (possibly-empty) password.
     *   NULL uses null;
     *   EMPTY uses empty string, in case you are unable to pass in an explicitly-empty one;
     *   DEFAULT uses the system keystore password;
     *   anything else is a literal password
     */
    public static final String PROP_KEYSTORE_PASSWORD = "com.l7tech.keystore.password";
    public static final String DEFAULT_KEYSTORE_PASSWORD = "DEFAULT";
    /**
     * possible values:  NULL, EMPTY, DEFAULT, or a (possibly-empty) password.
     *   NULL uses null;
     *   EMPTY uses empty string, in case you are unable to pass in an explicitly-empty one;
     *   DEFAULT uses the keystore password;
     *   anything else is a literal password
     */
    public static final String PROP_ENTRY_PASSWORD = "com.l7tech.keystore.entryPass";
    public static final String DEFAULT_ENTRY_PASSWORD = "DEFAULT";
    public static final String PROP_READ_ONLY = "com.l7tech.keystore.readOnly";
    public static final boolean DEFAULT_READ_ONLY = false;
    public static final String PROP_STORE_AFTER_CHANGE = "com.l7tech.keystore.storeAfterChange";
    public static final boolean DEFAULT_STORE_AFTER_CHANGE = true;
    /** Use FileUtils.saveFileSafely() and loadFileSafely() (for keystores with actual pathnames). */
    public static final String PROP_SAVE_FILE_SAFELY = "com.l7tech.keystore.saveFileSafely";
    public static final boolean DEFAULT_SAVE_FILE_SAFELY = true;
    public static final String PROP_RELOAD_AFTER_STORE = "com.l7tech.keystore.reloadAfterStore";
    public static final boolean DEFAULT_RELOAD_AFTER_STORE = false;
    private static Logger logger = Logger.getLogger(GenericSsgKeyStore.class.getName());
    private final Goid goid;
    private final SsgKeyStoreType type;
    private final String name;
    private final String keystoreType;
    private final String loadPath;
    private final String savePath;
    private final char[] keystorePassword;
    private final char[] entryPassword;
    private final boolean readOnly;
    private final boolean saveFileSafely;
    private final boolean storeAfterChange;
    private final boolean reloadAfterStore;
    private final AtomicReference<KeyStore> keystore = new AtomicReference<KeyStore>();

    public GenericSsgKeyStore(Goid goid, SsgKeyStoreType type, String name, char[] systemKeystorePassword, KeyAccessFilter keyAccessFilter, SsgKeyMetadataManager metadataManager) throws KeyStoreException {
        super(keyAccessFilter, metadataManager);
        this.goid = goid;
        this.type = type;
        this.name = name;
        this.keystorePassword = getPassword(PROP_KEYSTORE_PASSWORD, DEFAULT_KEYSTORE_PASSWORD, systemKeystorePassword);
        this.entryPassword = getPassword(PROP_ENTRY_PASSWORD, DEFAULT_ENTRY_PASSWORD, keystorePassword);
        this.keystoreType = ConfigFactory.getProperty( PROP_KEYSTORE_TYPE, DEFAULT_KEYSTORE_TYPE );
        this.loadPath = ConfigFactory.getProperty( PROP_LOAD_PATH, DEFAULT_LOAD_PATH );
        String storePath = ConfigFactory.getProperty( PROP_STORE_PATH, DEFAULT_STORE_PATH );
        if ("DEFAULT".equalsIgnoreCase(storePath))
            storePath = loadPath;
        this.savePath = storePath;
        this.readOnly = ConfigFactory.getBooleanProperty( PROP_READ_ONLY, DEFAULT_READ_ONLY );
        this.saveFileSafely = ConfigFactory.getBooleanProperty( PROP_SAVE_FILE_SAFELY, DEFAULT_SAVE_FILE_SAFELY );
        this.storeAfterChange = ConfigFactory.getBooleanProperty( PROP_STORE_AFTER_CHANGE, DEFAULT_STORE_AFTER_CHANGE );
        this.reloadAfterStore = ConfigFactory.getBooleanProperty( PROP_RELOAD_AFTER_STORE, DEFAULT_RELOAD_AFTER_STORE );
        logger.info("Using generic keystore type: " + keystoreType + " with path " + loadPath);
    }

    private static KeyStore loadKeyStore(String keystoreType, boolean loadFileSafely, String loadPath, final char[] keystorePassword) throws KeyStoreException {
        final KeyStore keystore = JceProvider.getInstance().getKeyStore(keystoreType);

        try {
            load(loadPath, loadFileSafely, new Functions.UnaryVoid<java.io.InputStream>() {
                @Override
                public void call(InputStream inputStream) {
                    try {
                        keystore.load(inputStream, keystorePassword);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    } catch (CertificateException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (IOException e) {
            throw new KeyStoreException(e);
        } catch (RuntimeException e) {
            throw new KeyStoreException(e);
        }

        return keystore;
    }

    private static void load(String pathOrOther, boolean loadFileSafely, final Functions.UnaryVoid<InputStream> streamUser) throws IOException {
        if ("NONE".equalsIgnoreCase(pathOrOther) || "EMPTY".equalsIgnoreCase(pathOrOther)) {
            streamUser.call(new EmptyInputStream());
        } else if ("NULL".equalsIgnoreCase(pathOrOther)) {
            streamUser.call(null);
        } else if (loadFileSafely) {
            streamUser.call(FileUtils.loadFileSafely(pathOrOther));
        } else {
            InputStream stream = null;
            try {
                stream = new FileInputStream(pathOrOther);
                streamUser.call(stream);
            } finally {
                ResourceUtils.closeQuietly(stream);
            }
        }
    }

    private static void storeKeyStore(final KeyStore keystore, boolean saveFileSafely, String savePath, final char[] keystorePassword) throws KeyStoreException {
        try {
            save(savePath, saveFileSafely, new Functions.UnaryVoid<java.io.OutputStream>() {
                @Override
                public void call(OutputStream outputStream) {
                    try {
                        keystore.store(outputStream, keystorePassword);
                    } catch (KeyStoreException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    } catch (CertificateException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (RuntimeException e) {
            throw new KeyStoreException("Unable to save keystore named: " + ExceptionUtils.getMessage(e), e);
        } catch (IOException e) {
            throw new KeyStoreException("Unable to save keystore named: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private static void save(String pathOrOther, boolean saveFileSafely, final Functions.UnaryVoid<OutputStream> streamUser) throws IOException {
        if ("NONE".equalsIgnoreCase(pathOrOther) || "EMPTY".equalsIgnoreCase(pathOrOther)) {
            streamUser.call(new NullOutputStream());
        } else if ("NULL".equalsIgnoreCase(pathOrOther)) {
            streamUser.call(null);
        } else if (saveFileSafely) {
            FileUtils.saveFileSafely(pathOrOther, new FileUtils.Saver() {
                @Override
                public void doSave(FileOutputStream fos) throws IOException {
                    streamUser.call(fos);
                }
            });
        } else {
            OutputStream stream = null;
            try {
                stream = new FileOutputStream(pathOrOther);
                streamUser.call(stream);
            } finally {
                ResourceUtils.closeQuietly(stream);
            }
        }
    }

    /**
     * Get a password from a system property.
     *
     * @param syspropName  name of system property.  Required.
     * @param syspropDefaultValue   value to use if system property is not set.
     * @param explicitDefaultValue  value to use if system property is set to the word "DEFAULT".
     * @return  the value.  May be null.
     */
    private static char[] getPassword(String syspropName, String syspropDefaultValue, char[] explicitDefaultValue) {
        String val = ConfigFactory.getProperty( syspropName, syspropDefaultValue );
        if ("NULL".equals(val)) {
            return null;
        } else if ("DEFAULT".equals(val)) {
            return explicitDefaultValue;
        } else if ("EMPTY".equals(val)) {
            return new char[0];
        } else {
            return val.toCharArray();
        }
    }

    @Override
    protected KeyStore keyStore() throws KeyStoreException {
        KeyStore ks = keystore.get();

        if (ks == null) {
            synchronized (keystore) {
                if (keystore.get() == null) {
                    ks = loadKeyStore(keystoreType, saveFileSafely, loadPath, keystorePassword);
                    keystore.set(ks);
                }
            }
        }

        return ks;
    }

    @Override
    protected String getFormat() {
        return "generic";
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    @Override
    public List<String> getAliases() throws KeyStoreException {
        // A generic keystore may contain various objects that don't follow the correct convention to be
        // usable as Java KeyStore key entries, so we need to filter them out.
        List<String> got = super.getAliases();
        List<String> ret = new ArrayList<String>();
        final KeyStore ks = keyStore();
        for (String alias : got) {
            try {
                if (null == ks.getKey(alias, getEntryPassword())) {
                    logger.log(Level.FINE, "Ignoring entry in generic keystore with no Key, alias = " + alias);
                    continue;
                }
            } catch (NoSuchAlgorithmException e) {
                // Ignore this key
                logger.log(Level.INFO, "Ignoring key entry in generic keystore with unsupported algorithm, alias = " + alias + "; error = " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            } catch (UnrecoverableKeyException e) {
                // Ignore this key
                logger.log(Level.INFO, "Ignoring unrecoverable key in generic keystore, alias = " + alias + "; error = " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }

            try {
                if (null == ks.getCertificateChain(alias)) {
                    logger.log(Level.FINE, "Ignoring entry in generic keystore with no certificate chain, alias = " + alias);
                    continue;
                }
            } catch (KeyStoreException e) {
                // Ignore this entry
                logger.log(Level.WARNING, "Ignoring key entry in generic keystore with bad certificate chain, alias = " + alias + "; error = " + ExceptionUtils.getMessage(e), e);
            }

            // All good
            ret.add(alias);

        }
        return ret;
    }

    @Override
    protected char[] getEntryPassword() {
        return entryPassword;
    }

    @Override
    protected <OUT> Future<OUT> mutateKeystore(final boolean useCurrentThread, Runnable transactionCallback, Callable<OUT> mutator) throws KeyStoreException {
        synchronized (keystore) {
            if (readOnly)
                throw new KeyStoreException("This keystore is read-only.");

            // Generic doesn't attempt to replicate through the DB, so just run in foreground and return result
            if (transactionCallback != null)
                transactionCallback.run();

            // Force task to occur now, in foreground
            OUT result;
            try {
                result = mutator.call();
            } catch (Exception e) {
                return new NotFuture<OUT>(e);
            }

            if (storeAfterChange) {
                KeyStore keystore = keyStore();
                storeKeyStore(keystore, saveFileSafely, savePath, keystorePassword);

                if (reloadAfterStore) {
                    // Force keystore to be reloaded
                    this.keystore.set(null);
                    keyStore();
                }
            }

            return new NotFuture<OUT>(result);
        }
    }

    @Override
    public Goid getGoid() {
        return goid;
    }

    @Override
    public SsgKeyStoreType getType() {
        return type;
    }

    @Override
    public String getName() {
        return name + " " + keystoreType;
    }

    @Override
    public boolean isKeyExportSupported() {
        return false;
    }
}
