package com.l7tech.server.security.keystore.sca;

import com.l7tech.common.security.JceProvider;
import com.l7tech.common.security.CertificateRequest;
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
import com.l7tech.server.event.AdminInfo;
import org.jboss.util.stream.NullInputStream;

import javax.security.auth.x500.X500Principal;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.List;
import java.util.ArrayList;

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
    private final String sslAlias;
    private final String caAlias;
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
     * @param sslAlias  alias of SSL cert in this keystore (ie, "tomcat"), or null if it does not contain one
     * @param caAlias   alias of CA cert in this keystore (ie, "ssgroot"), or null if it does not contain one
     * @param password the password to use when accessing the PKCS#11 keystore
     * @param kem the KeystoreFileManager.  Required.
     * @return the ScaSsgKeyStore instance for this process
     * @throws KeyStoreException  if the global instance cannot be created
     */
    public synchronized static ScaSsgKeyStore getInstance(long id, String name, String sslAlias, String caAlias, char[] password, KeystoreFileManager kem) throws KeyStoreException {
        if (INSTANCE != null)
            return INSTANCE;
        return INSTANCE = new ScaSsgKeyStore(id, name, sslAlias, caAlias, password, kem);
    }

    private ScaSsgKeyStore(long id, String name, String sslAlias, String caAlias, char[] password, KeystoreFileManager kem) throws KeyStoreException {
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
        this.sslAlias = sslAlias;
        this.caAlias = caAlias;
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

    @Override
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
        return new char[0];  // unused by PKCS#11
    }

    @Override
    public List<String> getAliases() throws KeyStoreException {
        // Convert system to display aliases, sorting SSL and CA to the top of the list, then leaving the
        // rest in their natural order
        List<String> aliases = super.getAliases();
        boolean removedSsl = aliases.remove(sslAlias);
        boolean removedCa = aliases.remove(caAlias);
        List<String> ret = new ArrayList<String>();
        if (removedSsl) ret.add(toDisplayAlias(sslAlias));
        if (removedCa) ret.add(toDisplayAlias(caAlias));
        for (String systemAlias : aliases)
            ret.add(toDisplayAlias(systemAlias));
        return ret;
    }

    @Override
    public SsgKeyEntry getCertificateChain(String displayAlias) throws KeyStoreException {
        SsgKeyEntry ret = super.getCertificateChain(toSystemAlias(displayAlias));
        ret.setAlias(toDisplayAlias(ret.getAlias()));
        return ret;
    }

    @Override
    public synchronized Future<Boolean> replaceCertificateChain(final String displayAlias, final X509Certificate[] chain) throws InvalidKeyException, KeyStoreException {
        return super.replaceCertificateChain(toSystemAlias(displayAlias), chain);
    }

    @Override
    public synchronized CertificateRequest makeCertificateSigningRequest(String displayAlias, String dn) throws InvalidKeyException, SignatureException, KeyStoreException {
        return super.makeCertificateSigningRequest(toSystemAlias(displayAlias), dn);
    }

    @Override
    public synchronized Future<Boolean> deletePrivateKeyEntry(final String displayAlias) throws KeyStoreException {
        String systemAlias = toSystemAlias(displayAlias);
        if (isSystemAliasUndeletable(systemAlias))
            throw new KeyStoreException("The specified entry is a system private key and cannot be deleted.");
        return super.deletePrivateKeyEntry(systemAlias);
    }

    @Override
    public synchronized Future<Boolean> storePrivateKeyEntry(final SsgKeyEntry entry, final boolean overwriteExisting) throws KeyStoreException {
        entry.setAlias(toSystemAlias(entry.getAlias()));
        if (isSystemAliasReserved(entry.getAlias()))
            throw new KeyStoreException("The specified private key alias is reserved for system use and cannot be created.");
        return super.storePrivateKeyEntry(entry, overwriteExisting);
    }

    @Override
    public synchronized Future<X509Certificate> generateKeyPair(final String displayAlias, final X500Principal dn, final int keybits, final int expiryDays) throws GeneralSecurityException {
        String systemAlias = toSystemAlias(displayAlias);
        if (isSystemAliasUndeletable(systemAlias))
            throw new KeyStoreException("The specified entry is a system private key and cannot be overwritten.");
        if (isSystemAliasReserved(systemAlias))
            throw new KeyStoreException("The specified private key alias is reserved for system use and cannot be created.");
        return super.generateKeyPair(systemAlias, dn, keybits, expiryDays);
    }

    private String toDisplayAlias(String systemAlias) {
        if (systemAlias.equalsIgnoreCase(sslAlias))
            return "SSL";
        if (systemAlias.equalsIgnoreCase(caAlias))
            return "CA";
        return systemAlias;
    }

    private String toSystemAlias(String displayAlias) {
        if (sslAlias != null && displayAlias.equalsIgnoreCase("SSL"))
            return sslAlias;
        if (caAlias != null && displayAlias.equalsIgnoreCase("CA"))
            return caAlias;
        return displayAlias;
    }

    private boolean isSystemAliasUndeletable(String systemAlias) {
        return systemAlias.equalsIgnoreCase(sslAlias) || systemAlias.equalsIgnoreCase(caAlias);
    }

    private boolean isSystemAliasReserved(String systemAlias) {
        // Prevent creation of a system alias that might collide with a display alias
        return ("CA".equalsIgnoreCase(systemAlias) || "SSL".equalsIgnoreCase(systemAlias));
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
    @Override
    protected <OUT> Future<OUT> mutateKeystore(final Callable<OUT> mutator) throws KeyStoreException {
        return mutationExecutor.submit(AdminInfo.find().wrapCallable(new Callable<OUT>() {
            public OUT call() throws Exception {

                final Object[] out = new Object[] { null };
                try {
                    synchronized (ScaSsgKeyStore.this) {
                        KeystoreFile updated = kem.updateDataBytes(getOid(), new Functions.Unary<byte[], byte[]>() {
                            public byte[] call(byte[] bytes) {
                                try {
                                    keystore = bytesToKeyStore(bytes);
                                    lastLoaded = System.currentTimeMillis();
                                    out[0] = mutator.call();
                                    return keyStoreToBytes();
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
