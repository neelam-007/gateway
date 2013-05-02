package com.l7tech.server.security.keystore;

import com.l7tech.common.io.*;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.objectmodel.*;
import com.l7tech.security.cert.BouncyCastleCertUtils;
import com.l7tech.security.cert.ParamsKeyGenerator;
import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.event.system.Stopped;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.NotFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for SsgKeyStore implementations that are based on a JDK KeyStore instance.
 */
public abstract class JdkKeyStoreBackedSsgKeyStore implements SsgKeyStore {

    private static final Logger logger = Logger.getLogger(JdkKeyStoreBackedSsgKeyStore.class.getName());

    private static final boolean FORCE_CASE_INSENSITIVE_ALIAS_MATCH = ConfigFactory.getBooleanProperty( "com.l7tech.server.security.keystore.jdkbacked.checkForCaseInsensitiveAliasMatch", true );

    private static BlockingQueue<Runnable> mutationQueue = new LinkedBlockingQueue<Runnable>();
    private static ExecutorService mutationExecutor = new ThreadPoolExecutor(1, 1, 5 * 60, TimeUnit.SECONDS, mutationQueue);
    private static final AtomicBoolean startedRef = new AtomicBoolean(false);

    protected final KeyAccessFilter keyAccessFilter;
    private final SsgKeyMetadataManager metadataManager;

    protected JdkKeyStoreBackedSsgKeyStore(@NotNull KeyAccessFilter keyAccessFilter, @NotNull SsgKeyMetadataManager metadataManager) {
        this.keyAccessFilter = keyAccessFilter;
        this.metadataManager = metadataManager;
    }

    public static final class StartupListener implements ApplicationListener {
        @Override
        public void onApplicationEvent(ApplicationEvent event) {
            if ( event instanceof Started) {
                logger.info("Switching to executor for keystore mutation.");
                startedRef.set(true);
            } else if ( event instanceof Stopped) {
                logger.info("Shutting down keystore mutation executor.");
                startedRef.set(false);
                mutationExecutor.shutdown();
            }
        }
    }

    /**
     * Get the KeyStore instance that we will be working with.
     * @return a KeyStore instance ready to use.   Never null.
     * @throws java.security.KeyStoreException if there is a problem obtaining a KeyStore instance.
     */
    protected abstract KeyStore keyStore() throws KeyStoreException;

    /** @return the format string for this keystore, to store in the format column in the DB, ie "hsm" or "sdb". */
    protected abstract String getFormat();

    @Override
    public String getId() {
        return String.valueOf(getOid());
    }

    @Override
    public List<String> getAliases() throws KeyStoreException {
        KeyStore keystore = keyStore();

        List<String> ret = new ArrayList<String>();
        Enumeration<String> en = keystore.aliases();
        while (en.hasMoreElements()) {
            String s = en.nextElement();
            if (keystore.isKeyEntry(s)) {
                ret.add(s);
            }
        }
        return ret;
    }

    @Override
    public SsgKeyEntry getCertificateChain(String alias) throws ObjectNotFoundException, KeyStoreException {
        KeyStore keystore = keyStore();

        Certificate[] chain = keystore.getCertificateChain(alias);
        if (chain == null && FORCE_CASE_INSENSITIVE_ALIAS_MATCH) {
            // Check for an entry that differs only by alias case, to work-around how some keystores have case-sensitive aliases while others are case-insensitive (Bug #9991)
            Enumeration<String> aliasEn = keystore.aliases();
            while (aliasEn.hasMoreElements()) {
                String haveAlias = aliasEn.nextElement();
                if (haveAlias.equalsIgnoreCase(alias)) {
                    alias = haveAlias;
                    chain = keystore.getCertificateChain(alias);
                    break;
                }
            }
        }

        if (chain == null)
            throw new ObjectNotFoundException("Keystore " + getName() + " does not contain any certificate chain entry with alias " + alias);
        if (chain.length < 1)
            throw new KeyStoreException("Keystore " + getName() + " contains an empty certificate chain entry for alias " + alias);

        // Make sure all are correct type
        X509Certificate[] x509chain = new X509Certificate[chain.length];
        for (int i = 0; i < chain.length; ++i) {
            if (chain[i] == null)
                throw new KeyStoreException("Keystore " + getName() + " entry with alias " + alias + " contains a null entry in its cert chain"); // shouldn't happen
            if (chain[i] instanceof X509Certificate)
                x509chain[i] = (X509Certificate)chain[i];
            else
                throw new KeyStoreException("Keystore " + getName() + " entry with alias " + alias + " contains a non-X.509 Certificate of type " + chain[i].getClass());
        }

        // Get the private key, if we can access it
        PrivateKey privateKey = null;
        try {
            Key key = keystore.getKey(alias, getEntryPassword());
            if (key instanceof PrivateKey)
                privateKey = (PrivateKey) key;

        } catch (NoSuchAlgorithmException e) {
            getLogger().log(Level.WARNING, "Unsupported key type in cert entry in " + "Keystore " + getName() + " with alias " + alias + ": " + ExceptionUtils.getMessage(e), e);
            // Fallthrough and do without
        } catch (UnrecoverableKeyException e) {
            getLogger().log(Level.WARNING, "Unrecoverable key in cert entry in " + "Keystore " + getName() + " with alias " + alias + ": " + ExceptionUtils.getMessage(e), e);
            // Fallthrough and do without
        }

        SsgKeyEntry ssgKeyEntry = new SsgKeyEntry(getOid(), alias, x509chain, privateKey);

        try {
            SsgKeyMetadata meta = metadataManager.findMetadata(getOid(), alias);
            if (meta != null)
                ssgKeyEntry.attachMetadata(meta);
        } catch (FindException e) {
            getLogger().log(Level.WARNING, "Unable to retrieve key entry metadata for " + "Keystore " + getName() + " with alias " + alias + ": " + ExceptionUtils.getMessage(e), e);
            // Fallthrough and do without
        }

        if (keyAccessFilter.isRestrictedAccessKeyEntry(ssgKeyEntry))
            ssgKeyEntry.setRestrictedAccess();
        return ssgKeyEntry;
    }

    /**
     * Get a Logger that can be used to log warnings about things like cert chain entries that are being
     * omitted from a list due to some problem, or private key entries that cannot be used.
     *
     * @return a Logger instance.  Never null.
     */
    protected abstract Logger getLogger();

    private void storePrivateKeyEntryImpl(SsgKeyEntry entry, boolean overwriteExisting) throws KeyStoreException {
        final String alias = entry.getAlias();
        if (alias == null || alias.trim().length() < 1)
            throw new KeyStoreException("Unable to store private key entry with null or empty alias");

        final X509Certificate[] chain = entry.getCertificateChain();
        if (chain == null || chain.length < 1)
            throw new KeyStoreException("Unable to store private key entry with null or empty certificate chain");

        final PrivateKey key;
        try {
            key = entry.getPrivateKey();
            if (key == null)
                throw new KeyStoreException("Unable to store private key entry with null private key");
        } catch (UnrecoverableKeyException e) {
            throw new KeyStoreException("Unable to store private key entry with unrecoverable private key");
        }

        KeyStore keystore = keyStore();
        if (!overwriteExisting && keystore.containsAlias(alias))
            throw new KeyStoreException("Keystore already contains an entry with the alias '" + alias + '\'');

        keystore.setKeyEntry(alias, key, getEntryPassword(), chain);
    }

    /**
     * Get a password that will be used to protect each key entry set, and
     * to decrypt each key entry read.
     *
     * @return a non-null (but possibly empty) character array
     */
    protected abstract char[] getEntryPassword();

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public boolean isKeyExportSupported() {
        return true;
    }

    @Override
    public SsgKeyStore getKeyStore() {
        return this;
    }

    @Override
    public synchronized Future<Boolean> storePrivateKeyEntry(Runnable transactionCallback, final SsgKeyEntry entry, final boolean overwriteExisting) throws KeyStoreException {
        if (entry == null) throw new NullPointerException("entry must not be null");
        if (entry.getAlias() == null) throw new NullPointerException("entry's alias must not be null");
        if (entry.getAlias().length() < 1) throw new IllegalArgumentException("entry's alias must not be empty");
        final PrivateKey privateKey;
        try {
            privateKey = entry.getPrivateKey();
            if (privateKey == null) throw new NullPointerException("entry's private key must be present");
        } catch (UnrecoverableKeyException e) {
            throw new IllegalArgumentException("entry's private key must be present", e);
        }

        return mutateKeystore(transactionCallback, new Callable<Boolean>() {
            @Override
            public Boolean call() throws KeyStoreException {
                storePrivateKeyEntryImpl(entry, overwriteExisting);
                try {
                    metadataManager.updateMetadataForKey(entry.getKeystoreId(), entry.getAlias(), entry.getKeyMetadata());
                } catch (final ObjectModelException e) {
                    throw new KeyStoreException("Unable to update metadata for key " + entry.getAlias(), e);
                }
                return Boolean.TRUE;
            }
        });
    }

    @Override
    public synchronized Future<Boolean> deletePrivateKeyEntry(Runnable transactionCallback, final String keyAlias) throws KeyStoreException {
        return mutateKeystore(transactionCallback, new Callable<Boolean>() {
            @Override
            public Boolean call() throws KeyStoreException {
                keyStore().deleteEntry(keyAlias);
                try {
                    metadataManager.deleteMetadataForKey(getOid(), keyAlias);
                } catch (final DeleteException e) {
                    throw new KeyStoreException("Unable to delete metadata for key " + keyAlias, e);
                }
                return Boolean.TRUE;
            }
        });
    }

    @Override
    public synchronized Future<X509Certificate> generateKeyPair(Runnable transactionCallback, final String alias, final KeyGenParams keyGenParams, final CertGenParams certGenParams, @Nullable final SsgKeyMetadata metadata)
            throws GeneralSecurityException
    {
        return mutateKeystore(transactionCallback, new Callable<X509Certificate>() {
            @Override
            public X509Certificate call() throws GeneralSecurityException, DuplicateAliasException, CertificateGeneratorException {
                KeyStore keystore = keyStore();
                if (keystore.containsAlias(alias))
                    throw new DuplicateAliasException("Keystore already contains alias " + alias);

                // Requires that current crypto engine already by the correct one for this keystore type
                KeyPair keyPair = new ParamsKeyGenerator(keyGenParams).generateKeyPair();
                X509Certificate cert = BouncyCastleCertUtils.generateSelfSignedCertificate(certGenParams, keyPair);

                keystore.setKeyEntry(alias, keyPair.getPrivate(), getEntryPassword(), new Certificate[] { cert });
                try {
                    metadataManager.updateMetadataForKey(getOid(), alias, metadata);
                } catch (final ObjectModelException e) {
                    throw new KeyStoreException("Unable to save metadata for key " + alias, e);
                }
                return cert;
            }
        });
    }

    @Override
    public synchronized Future<Boolean> replaceCertificateChain(Runnable transactionCallback, final String alias, final X509Certificate[] chain) throws InvalidKeyException, KeyStoreException {
        if (chain == null || chain.length < 1 || chain[0] == null)
            throw new IllegalArgumentException("Cert chain must contain at least one cert.");
        return mutateKeystore(transactionCallback, new Callable<Boolean>() {
            @Override
            public Boolean call() throws GeneralSecurityException, AliasNotFoundException {
                KeyStore keystore = keyStore();
                if (!keystore.isKeyEntry(alias))
                    throw new AliasNotFoundException("Keystore does not contain a key with alias " + alias);
                Key key = keystore.getKey(alias, getEntryPassword());
                Certificate[] oldChain = keystore.getCertificateChain(alias);
                if (oldChain == null || oldChain.length < 1)
                    throw new KeyStoreException("Existing certificate chain for alias " + alias + " is empty");
                if (!(oldChain[0] instanceof X509Certificate))
                    throw new KeyStoreException("Existing certificate for alias " + alias + " is not an X.509 certificate");
                X509Certificate oldCert = (X509Certificate)oldChain[0];
                PublicKey oldPublicKey = oldCert.getPublicKey();
                PublicKey newPublicKey = chain[0].getPublicKey();

                if (!CertUtils.arePublicKeysEqual(oldPublicKey, newPublicKey))
                    throw new KeyStoreException("New certificate does not certify the public key for this private key.");

                keystore.setKeyEntry(alias, key, getEntryPassword(), chain);

                return Boolean.TRUE;
            }
        });
    }

    @Override
    public synchronized CertificateRequest makeCertificateSigningRequest(String alias, CertGenParams certGenParams) throws InvalidKeyException, SignatureException, KeyStoreException {
        try {
            if (certGenParams.getSubjectDn() == null)
                throw new KeyStoreException("A subjectDn is required to create a CSR");
            KeyStore keystore = keyStore();
            Key key = keystore.getKey(alias, getEntryPassword());
            PrivateKey privateKey = (PrivateKey)key;
            Certificate[] chain = keystore.getCertificateChain(alias);
            if (chain == null || chain.length < 1)
                throw new KeyStoreException("Existing certificate chain for alias " + alias + " is empty");
            if (!(chain[0] instanceof X509Certificate))
                throw new KeyStoreException("Existing certificate for alias " + alias + " is not an X.509 certificate");
            X509Certificate cert = (X509Certificate)chain[0];
            KeyPair keyPair = new KeyPair(cert.getPublicKey(), privateKey);
            return BouncyCastleCertUtils.makeCertificateRequest(certGenParams, keyPair);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidKeyException("Keystore contains no key with alias " + alias, e);
        } catch (UnrecoverableKeyException e) {
            throw new KeyStoreException(e);
        } catch (NoSuchProviderException e) {
            throw new KeyStoreException(e);
        }
    }

    @Override
    public void updateKeyMetadata(final long keystoreOid, @NotNull final String alias, @Nullable SsgKeyMetadata metadata) throws UpdateException, FindException, SaveException {
        metadataManager.updateMetadataForKey(keystoreOid, alias, metadata);
    }

    /**
     * Get the future that arises from processing of the given mutator.
     *
     * <p>Note that if the gateway is not yet started the processing will
     * occur in the current thread.</p>
     *
     * @param mutator The mutation action
     * @return The future.
     * @throws KeyStoreException if an error occurs.
     */
    protected <OUT> Future<OUT> submitMutation( final Callable<OUT> mutator ) throws KeyStoreException {
        if ( !startedRef.get() ) {
            logger.info("Using current thread for keystore mutation (server not started).");
            try {
                return new NotFuture<OUT>(mutator.call());
            } catch ( Exception e ) {
                return new NotFuture<OUT>(e);
            }
        } else {
            return mutationExecutor.submit( mutator );
        }
    }

    /**
     * Load the keystore from the database, mutate it, and save it back, all atomically.  The actual transaction
     * will not necessarily occur before this call returns -- it may be scheduled for later execution.  The returned
     * Future will show Done when the mutation eventually completes.
     *
     * @param transactionCallback Optional callback to invoke inside the transaction, or null.
     *                            Can be used for more detailed auditing.
     * @param mutator  a Runnable that will mutate the current keystore, which will be guaranteed
     *                 to be up-to-date and non-null when the runnable is invoked.
     * @throws java.security.KeyStoreException if the runnable throws a RuntimeException or if any other problem occurs during
     *                           the process
     * @return the value returned by the mutator
     */
    protected abstract <OUT> Future<OUT> mutateKeystore(Runnable transactionCallback, Callable<OUT> mutator) throws KeyStoreException;

}
