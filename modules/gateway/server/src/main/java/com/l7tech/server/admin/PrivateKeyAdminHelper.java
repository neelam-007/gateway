package com.l7tech.server.admin;

import com.l7tech.common.io.AliasNotFoundException;
import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.KeyGenParams;
import com.l7tech.gateway.common.security.MultipleAliasesException;
import com.l7tech.gateway.common.security.SpecialKeyType;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.gateway.common.spring.remoting.RemoteUtils;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.*;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.event.EntityChangeSet;
import com.l7tech.server.event.admin.Created;
import com.l7tech.server.event.admin.Deleted;
import com.l7tech.server.event.admin.KeyExportedEvent;
import com.l7tech.server.event.admin.Updated;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.util.CallableRunnable;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.PoolByteArrayOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Helper class for private key administrative tasks.
 */
public class PrivateKeyAdminHelper {

    //- PUBLIC

    /**
     * Provider for parsing PKCS#12 files when someone calls {@link #doImportKeyFromKeyStoreFile}.  Values:
     *   "default" to use the system current most-preferred implementation of KeyStore.PKCS12;  "BC" to use
     *   Bouncy Castle's implementation (note that Bouncy Castle need not be registered as a Security provider
     *   for this to work); or else the name of any registered Security provider that offers KeyStore.PKCS12.
     */
    public static final String PROP_PKCS12_PARSING_PROVIDER = "com.l7tech.keyStore.pkcs12.parsing.provider";

    public PrivateKeyAdminHelper( final DefaultKey defaultKey,
                                  final SsgKeyStoreManager ssgKeyStoreManager,
                                  final ApplicationEventPublisher applicationEventPublisher ) {
        this.defaultKey = defaultKey;
        this.ssgKeyStoreManager = ssgKeyStoreManager;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Find a private key using the given alias / keystore oid.
     *
     * @param keyAlias The alias (optional, null for the default ssl key)
     * @param preferredKeystoreOid The preferred keystore oid (-1 for any)
     * @return The key or null
     * @throws FindException If an error occurs
     * @throws KeyStoreException  If an error occurs
     */
    public SsgKeyEntry doFindKeyEntry( final String keyAlias,
                                       final long preferredKeystoreOid ) throws FindException, KeyStoreException {
        try {
            return keyAlias == null ? defaultKey.getSslInfo() : ssgKeyStoreManager.lookupKeyByKeyAlias(keyAlias, preferredKeystoreOid);
        } catch (IOException e) {
            // No default SSL key
            return null;
        } catch (ObjectNotFoundException e) {
            return null;
        }

    }

    /**
     * Check if this thread is processing an admin request that arrived over an SSL connector that appears
     * to be using the specified private key as its SSL server cert.
     * <p/>
     * This method will return false if one of the following is true:
     * <ul>
     * <li> this thread has no active servlet request
     * <li> no active connector can be found for this thread's active servlet request
     * <li> the active connector explicitly specifies a key alias that does not match the specified keyAlias
     * <li> the active connector explicitly specifies a keystore OID that does not match the specified store's OID
     * <li> the specified store does not contain the specified keyAlias
     * <li> the specified key entry's certificate does not exactly match the active connector's SSL server cert
     * <li> there is a database problem checking any of the above information
     * </ul>
     * <p/>
     * Otherwise, this method returns true.
     *
     * @param store  the keystore in which to find the alias.  Required.
     * @param keyAlias  the alias to find.  Required.
     * @return true if the specified key appears to be in use by the current admin connection.
     * @throws KeyStoreException if there is a problem reading a keystore
     */
    public boolean isKeyActive( final SsgKeyFinder store,
                                final String keyAlias ) throws KeyStoreException {
        HttpServletRequest req = RemoteUtils.getHttpServletRequest();
        if (null == req)
            return false;
        SsgConnector connector = HttpTransportModule.getConnector( req );
        if (null == connector)
            return false;
        if (connector.getKeyAlias() != null && !keyAlias.equalsIgnoreCase(connector.getKeyAlias()))
            return false;
        Long portStoreOid = connector.getKeystoreOid();
        if (portStoreOid != null && portStoreOid != store.getOid())
            return false;

        final SsgKeyEntry entry;
        try {
            entry = store.getCertificateChain(keyAlias);
        } catch (ObjectNotFoundException e) {
            return false;
        }

        try {
            SsgKeyEntry portEntry = doFindKeyEntry( connector.getKeyAlias(), portStoreOid != null ? portStoreOid : -1L );
            return CertUtils.certsAreEqual(portEntry.getCertificate(), entry.getCertificate());
        } catch (FindException e) {
            return false;
        }
    }

    /**
     * Update the certificate chain for the given private key.
     *
     * @param keystoreId The keystore oid.
     * @param alias The key alias.
     * @param chain The chain to use
     * @throws UpdateException If an error occurs
     */
    public void doUpdateCertificateChain( final long keystoreId,
                                          final String alias,
                                          final X509Certificate[] chain ) throws UpdateException {
        SsgKeyFinder keyFinder;
        try {
            keyFinder = ssgKeyStoreManager.findByPrimaryKey(keystoreId);
        } catch (KeyStoreException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            throw new UpdateException("Error getting keystore: " + ExceptionUtils.getMessage(e), e);
        } catch (FindException e) {
            throw new UpdateException("Error getting keystore: " + ExceptionUtils.getMessage(e), e);
        }

        SsgKeyStore keystore = keyFinder.getKeyStore();
        if (keystore == null)
            throw new UpdateException("error: keystore ID " + keystoreId + " is read-only");

        try {
            Future<Boolean> future = keystore.replaceCertificateChain(auditAfterUpdate(keystore, alias, "certificateChain", "replaced"), alias, chain);
            // Force it to be synchronous (Bug #3852)
            future.get();
        } catch (Exception e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            throw new UpdateException("Error setting new cert: " + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * Update metadata for a private key.  The private key must exist in the keystore in order to have its
     * metadata updated.
     *
     * @param keystoreId The keystore oid.
     * @param alias The key alias.
     * @param keyMetadata The new key metadata, or null to clear any existing metadata.
     * @throws UpdateException If an error occurs
     */
    public void doUpdateKeyMetadata(long keystoreId, @NotNull String alias, @Nullable SsgKeyMetadata keyMetadata) throws UpdateException {
        SsgKeyFinder keyFinder;
        try {
            keyFinder = ssgKeyStoreManager.findByPrimaryKey(keystoreId);
        } catch (KeyStoreException | FindException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            throw new UpdateException("Error getting keystore: " + ExceptionUtils.getMessage(e), e);
        }
        try {
            // Ensure key currently exists
            keyFinder.getCertificateChain(alias);
            // Update metadata
            final SsgKeyStore keyStore = keyFinder.getKeyStore();
            if (keyStore != null) {
                keyStore.updateKeyMetadata(keystoreId, alias, keyMetadata);
            }
        } catch (final Exception e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            throw new UpdateException("Error setting new key metadata: " + ExceptionUtils.getMessage(e), e);
        }

    }

    /**
     * Generate a new key pair.
     *
     * @param keystoreId The keystore id for the new key
     * @param alias The alias for the new key.
     * @param ssgKeyMetadata The metadata to record for the new key, or null.
     * @param dn The subject DN to use.
     * @param params The key generation parameters.
     * @param expiryDays The number of days before certificate expiry
     * @param makeCaCert True for a CA certificate
     * @param sigAlg The signature algorithm to use (e.g. SHA1withRSA)
     * @return A future for the generated certificate
     * @throws FindException If an error occurs
     * @throws GeneralSecurityException If an error occurs
     */
    public Future<X509Certificate> doGenerateKeyPair( final long keystoreId,
                                                      final String alias,
                                                      final @Nullable SsgKeyMetadata ssgKeyMetadata,
                                                      final X500Principal dn,
                                                      final KeyGenParams params,
                                                      final int expiryDays,
                                                      final boolean makeCaCert,
                                                      final String sigAlg ) throws FindException, GeneralSecurityException {
        SsgKeyStore keystore = checkBeforeGenerate(keystoreId, alias, dn, expiryDays);
        return keystore.generateKeyPair( afterCreate(keystore, alias, "generated"),
                alias, params, new CertGenParams( dn, expiryDays, makeCaCert, sigAlg ), ssgKeyMetadata );
    }

    /**
     * Import a private key from a keystore file.
     *
     * @param keystoreId The target keystore oid
     * @param alias The target alias
     * @param ssgKeyMetadata The metadata to record for the new key, or null.
     * @param keyStoreBytes The keystore data
     * @param keyStoreType  the type of key store, eg "PKCS12" or "JKS"
     * @param keyStorePass The keystore password, or null to pass null as the second argument to KeyStore.load()
     * @param entryPass  The per-entry password protecting the private key entry, or null to pass null as the second argument to KeyStore.getKey()
     * @param entryAlias The alias of the key in the keystore
     * @return The newly imported key
     */
    public SsgKeyEntry doImportKeyFromKeyStoreFile( final long keystoreId,
                                              final String alias,
                                              final @Nullable SsgKeyMetadata ssgKeyMetadata,
                                              @Nullable final byte[] keyStoreBytes, 
                                              String keyStoreType, 
                                              @Nullable final char[] keyStorePass, 
                                              final char[] entryPass, 
                                              String entryAlias )
            throws KeyStoreException, NoSuchProviderException, IOException, NoSuchAlgorithmException, CertificateException,
                AliasNotFoundException, MultipleAliasesException, UnrecoverableKeyException, SaveException, InterruptedException, ExecutionException, ObjectNotFoundException
    {
        KeyStore inks = createKeyStoreForParsingKeyStoreFile(keyStoreType);
        inks.load(new ByteArrayInputStream(keyStoreBytes), keyStorePass);

        if (entryAlias == null) {
            List<String> aliases = new ArrayList<String>( Collections.list( inks.aliases() ));
            if (aliases.isEmpty())
                throw new AliasNotFoundException("KeyStore file contains no private key entries");
            if (aliases.size() > 1) {
                // Retain private keys and filter out those certificates.
                for (Iterator<String> itr = aliases.iterator(); itr.hasNext();) {
                    if (! inks.isKeyEntry(itr.next())) {
                        itr.remove();
                    }
                }
                throw new MultipleAliasesException(aliases.toArray(new String[aliases.size()]));
            }
            entryAlias = aliases.iterator().next();
        }

        Certificate[] chain = inks.getCertificateChain(entryAlias);
        Key key = inks.getKey(entryAlias, entryPass);
        if (chain == null || key == null)
            throw new AliasNotFoundException("alias not found in KeyStore file: " + entryAlias);

        X509Certificate[] x509chain = CertUtils.asX509CertificateArray( chain );
        if (!(key instanceof PrivateKey))
            throw new KeyStoreException("Key entry is not a PrivateKey: " + key.getClass());

        SsgKeyStore keystore = getKeyStore(keystoreId);
        SsgKeyEntry entry = new SsgKeyEntry(keystore.getOid(), alias, x509chain, (PrivateKey)key);
        if (ssgKeyMetadata != null) {
            entry.attachMetadata(ssgKeyMetadata);
        }
        Future<Boolean> future = keystore.storePrivateKeyEntry(afterCreate(keystore, alias, "imported"), entry, false);
        if (!future.get())
            throw new KeyStoreException("Import operation returned false"); // can't happen

        return keystore.getCertificateChain(alias);
    }

    /**
     * Export the identified key as PKCS12 data.
     *
     * @param keystoreId The keystore oid
     * @param alias The alias of the key to export
     * @param p12alias The alias to use in the PKCS12 data
     * @param p12passphrase The passphrase ot use in the PKCS12 data
     * @return The PKCS12 keystore data
     */
    public byte[] doExportKeyAsPkcs12( final long keystoreId,
                                       final String alias,
                                       String p12alias,
                                       final char[] p12passphrase ) throws FindException, KeyStoreException, UnrecoverableKeyException {
        SsgKeyFinder ks = ssgKeyStoreManager.findByPrimaryKey(keystoreId);
        if (!ks.isKeyExportSupported())
            throw new UnrecoverableKeyException("Key export not available");

        SsgKeyEntry entry = ssgKeyStoreManager.lookupKeyByKeyAlias(alias, keystoreId);
        if (!entry.isPrivateKeyAvailable())
            throw new UnrecoverableKeyException("Private Key for alias " + alias + " cannot be exported.");

        if (p12alias == null)
            p12alias = alias;

        PrivateKey privateKey = entry.getPrivateKey();
        Certificate[] certChain = entry.getCertificateChain();

        PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream();
        KeyStore keystore = KeyStore.getInstance("PKCS12", new BouncyCastleProvider());
        try {
            keystore.load(null, p12passphrase);
            keystore.setKeyEntry(p12alias, privateKey, p12passphrase, certChain);
            keystore.store(baos, p12passphrase);
            applicationEventPublisher.publishEvent(new KeyExportedEvent(this, entry.getKeystoreId(), entry.getAlias(), getSubjectDN(entry)));
            return baos.toByteArray();
        } catch (IOException e) {
            throw new KeyStoreException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException(e);
        } catch (CertificateException e) {
            throw new KeyStoreException(e);
        } finally {
            baos.close();
        }
    }

    /**
     * Get the cluster property name for the given special key type.
     *
     * @param keyType The key type (required)
     * @return The cluster property name
     * @throws IllegalArgumentException if the key type is unknown
     */
    public static String getClusterPropertyForSpecialKeyType( final SpecialKeyType keyType ) {
        final String clusterPropertyName;

        switch (keyType) {
            case SSL:
                clusterPropertyName = "keyStore.defaultSsl.alias";
                break;

            case CA:
                clusterPropertyName = "keyStore.defaultCa.alias";
                break;

            case AUDIT_VIEWER:
                clusterPropertyName = "keyStore.auditViewer.alias";
                break;

            case AUDIT_SIGNING:
                clusterPropertyName = "keyStore.auditSigning.alias";
                break;

            default:
                throw new IllegalArgumentException("No such keyType: " + keyType);
        }

        return clusterPropertyName;
    }

    //- PRIVATE

    private final DefaultKey defaultKey;
    private final SsgKeyStoreManager ssgKeyStoreManager;
    private final ApplicationEventPublisher applicationEventPublisher;

    private String getSubjectDN(SsgKeyEntry entry) {
        X509Certificate cert = entry.getCertificate();
        if (cert == null) return null;
        return cert.getSubjectDN().getName();
    }

    private KeyStore createKeyStoreForParsingKeyStoreFile(String keyStoreType) throws KeyStoreException, NoSuchProviderException {
        if ("PKCS12".equals(keyStoreType)) {
            String p = ConfigFactory.getProperty( PROP_PKCS12_PARSING_PROVIDER, "BC" );
            if (null == p || p.length() < 1 || p.equalsIgnoreCase("default"))
                return KeyStore.getInstance("PKCS12");
            if ("BC".equalsIgnoreCase(p))
                return KeyStore.getInstance("PKCS12", new BouncyCastleProvider());
            return KeyStore.getInstance("PKCS12", p);
        } else if ("JKS".equals(keyStoreType)) {
            return KeyStore.getInstance("JKS");
        } else {
            throw new KeyStoreException("KeyStore file type not supported: " + keyStoreType);
        }
    }

    private SsgKeyStore getKeyStore(long keystoreId) throws SaveException {
        SsgKeyFinder keyFinder;
        try {
            keyFinder = ssgKeyStoreManager.findByPrimaryKey(keystoreId);
        } catch (KeyStoreException e) {
            throw new SaveException("error getting keystore: " + ExceptionUtils.getMessage( e ), e);
        } catch (FindException e) {
            throw new SaveException("error getting keystore: " + ExceptionUtils.getMessage(e), e);
        }

        SsgKeyStore keystore = keyFinder.getKeyStore();
        if (keystore == null)
            throw new SaveException("error: keystore ID " + keystoreId + " is read-only");
        return keystore;
    }

    private SsgKeyStore checkBeforeGenerate(long keystoreId, String alias, X500Principal dn, int expiryDays) throws FindException, KeyStoreException {
        if (alias == null) throw new NullPointerException("alias is null");
        if (alias.length() < 1) throw new IllegalArgumentException("alias is empty");
        if (dn == null) throw new NullPointerException("dn is null");

        // Ensure that Sun certificate parser will like this dn
        new X500Principal(dn.getName(X500Principal.CANONICAL)).getEncoded();

        SsgKeyFinder keyFinder;
        try {
            keyFinder = ssgKeyStoreManager.findByPrimaryKey(keystoreId);
        } catch (ObjectNotFoundException e) {
            throw new FindException("No keystore found with id " + keystoreId);
        }
        SsgKeyStore keystore = null;
        if (keyFinder != null && keyFinder.isMutable())
            keystore = keyFinder.getKeyStore();
        if (keystore == null)
            throw new FindException("Keystore with id " + keystoreId + " is not mutable");
        if (expiryDays < 1)
            throw new IllegalArgumentException("expiryDays must be positive");
        return keystore;
    }

    private Runnable afterCreate(final SsgKeyStore keystore, final String alias, final String note) {
        return new CallableRunnable<Object>( AdminInfo.find( true ).wrapCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                applicationEventPublisher.publishEvent(new Created<SsgKeyEntry>(SsgKeyEntry.createDummyEntityForAuditing(keystore.getOid(), alias), note));
                return null;
            }
        }));
    }

    private Runnable auditAfterUpdate(SsgKeyStore keystore, String alias, String property, String note) {
        EntityChangeSet changeset = new EntityChangeSet(new String[] {property}, new Object[] {new Object()}, new Object[] {new Object()});
        return publisher(new Updated<SsgKeyEntry>(SsgKeyEntry.createDummyEntityForAuditing(keystore.getOid(), alias), changeset, note));
    }

    private Runnable auditAfterDelete(SsgKeyStore keystore, String alias) {
        return publisher( new Deleted<SsgKeyEntry>( SsgKeyEntry.createDummyEntityForAuditing( keystore.getOid(), alias ) ) );
    }

    private Runnable publisher(final ApplicationEvent event) {
        return new CallableRunnable<Object>( AdminInfo.find( true ).wrapCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                applicationEventPublisher.publishEvent(event);
                return null;
            }
        }));
    }

    public void doDeletePrivateKeyEntry( final SsgKeyStore store, final String keyAlias ) throws DeleteException, KeyStoreException {
        try {
            Future<Boolean> result = store.deletePrivateKeyEntry(auditAfterDelete(store, keyAlias), keyAlias);
            // Force it to be synchronous (Bug #3852)
            result.get();
        } catch (ExecutionException e) {
            throw new DeleteException("Unable to delete key: " + ExceptionUtils.getMessage(e), e);
        } catch (InterruptedException e) {
            throw new DeleteException("Unable to find keystore: " + ExceptionUtils.getMessage(e), e);
        }
    }
}
