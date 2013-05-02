package com.l7tech.server.ems.setup;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.KeyGenParams;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.ems.listener.ListenerConfigurationUpdatedEvent;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationEvent;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 */
public class SslSetupManagerImpl implements SslSetupManager {

    //- PUBLIC

    public SslSetupManagerImpl( final ServerConfig serverConfig,
                                final ClusterPropertyManager clusterPropertyManager,
                                final SsgKeyStoreManager keyStoreManager ) {
        this.serverConfig = serverConfig;
        this.clusterPropertyManager = clusterPropertyManager;
        this.keyStoreManager = keyStoreManager;
    }

    @Override
    public String saveSsl( final PrivateKey key, final X509Certificate[] certificateChain ) throws SetupException {
        try {
            // save key
            String alias = findUnusedAlias();
            SsgKeyStore sks = findFirstMutableKeystore();
            SsgKeyEntry entry = new SsgKeyEntry( sks.getOid(), alias, certificateChain, key );
            sks.storePrivateKeyEntry(null, entry, false );
            return alias;
        } catch ( KeyStoreException kse ) {
            throw new SetupException( "Error during keystore configuration.", kse );
        } catch ( IOException ioe ) {
            throw new SetupException( "Error during keystore configuration.", ioe );
        }
    }

    @Override
    public String generateSsl(final String hostname, final RsaKeySize rsaKeySize) throws SetupException {
        try {
            // generate key and save
            String alias = findUnusedAlias();
            SsgKeyStore sks = findFirstMutableKeystore();
            generateKeyPair( hostname, sks, alias, rsaKeySize );
            return alias;
        } catch ( IOException ioe ) {
            throw new SetupException( "Error during keystore configuration.", ioe );
        }
    }

    @Override
    public void setSslAlias( final String alias ) throws SetupException {
        try {
            SsgKeyStore sks = findFirstMutableKeystore();
            configureAsDefaultSslCert(sks, alias);
        } catch ( IOException ioe ) {
            throw new SetupException( "Error during keystore configuration.", ioe );
        }
    }

    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof ListenerConfigurationUpdatedEvent ) {
            cleanupAlias( ((ListenerConfigurationUpdatedEvent) event).getAlias() );
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SslSetupManagerImpl.class.getName());

    private static final String BASE_ALIAS = "ssl";

    private final ServerConfig serverConfig;
    private final ClusterPropertyManager clusterPropertyManager;
    private final SsgKeyStoreManager keyStoreManager;


    private void generateKeyPair(final String hostname, final SsgKeyStore sks, final String alias,
                                 final RsaKeySize rsaKeySize) throws IOException {
        X500Principal dn = new X500Principal("cn=" + hostname);
        try {
            Future<X509Certificate> job = sks.generateKeyPair(null, alias, new KeyGenParams(rsaKeySize.getKeySize()), new CertGenParams(dn, 365 * 10, false, null), null);
            job.get();
        } catch ( GeneralSecurityException e) {
            throw new IOException("Unable to create initial default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch ( ExecutionException e) {
            throw new IOException("Unable to create initial default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch (InterruptedException e) {
            throw new IOException("Unable to create initial default SSL key: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private SsgKeyEntry configureAsDefaultSslCert( final SsgKeyStore sks, final String alias) throws IOException {
        try {
            SsgKeyEntry entry = sks.getCertificateChain(alias);
            String name = serverConfig.getClusterPropertyName( ServerConfigParams.PARAM_KEYSTORE_DEFAULT_SSL_KEY);
            if (name == null)
                throw new IOException("Unable to configure default SSL key: no cluster property defined for ServerConfig property " + ServerConfigParams.PARAM_KEYSTORE_DEFAULT_SSL_KEY);
            String value = entry.getKeystoreId() + ":" + alias;
            clusterPropertyManager.putProperty(name, value);
            return entry;
        } catch (KeyStoreException e) {
            throw new IOException("Unable to find default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch ( FindException e) {
            throw new IOException("Unable to configure default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch ( UpdateException e) {
            throw new IOException("Unable to configure default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch ( SaveException e) {
            throw new IOException("Unable to configure default SSL key: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private SsgKeyStore findFirstMutableKeystore() throws IOException {
        SsgKeyStore sks = null;
        try {
            List<SsgKeyFinder> got = keyStoreManager.findAll();
            for (SsgKeyFinder ssgKeyFinder : got) {
                if (ssgKeyFinder.isMutable()) {
                    sks = ssgKeyFinder.getKeyStore();
                    break;
                }
            }
            if (sks == null)
                throw new IOException("No mutable keystores found in which to store default SSL key");
        } catch (FindException e) {
            throw new IOException("Unable to find keystore in which to store default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch (KeyStoreException e) {
            throw new IOException("Unable to find keystore in which to store default SSL key: " + ExceptionUtils.getMessage(e), e);
        }
        return sks;
    }

    private String findUnusedAlias() throws IOException {
        String alias = BASE_ALIAS;
        int count = 1;
        while (aliasAlreadyUsed(alias)) {
            alias = BASE_ALIAS + (count++);
        }
        return alias;
    }

    private boolean aliasAlreadyUsed( final String alias ) throws IOException {
        try {
            keyStoreManager.lookupKeyByKeyAlias(alias, -1);
            return true;
        } catch ( ObjectNotFoundException e) {
            return false;
        } catch (FindException e) {
            throw new IOException("Unable to check if alias \"" + alias + "\" is already used: " + ExceptionUtils.getMessage(e), e);
        } catch (KeyStoreException e) {
            throw new IOException("Unable to check if alias \"" + alias + "\" is already used: " + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * Delete all aliases and keys except for the given value and the currently
     * configured default.
     */
    private void cleanupAlias( final String alias ) {
        String configAlias = serverConfig.getProperty("keyStoreDefaultSslKey");
        if ( configAlias != null ) {
            int index = configAlias.indexOf(':');
            if ( index > -1 ) {
                configAlias = configAlias.substring( index+1 );
            }
        }

        try {
            String currentAlias = BASE_ALIAS;
            for ( int i=1; i<100; i++ ) {
                if ( !currentAlias.equalsIgnoreCase(alias) &&
                     (configAlias == null || !currentAlias.equalsIgnoreCase(configAlias)) ) {
                    try {
                        SsgKeyEntry entry = keyStoreManager.lookupKeyByKeyAlias(currentAlias, -1);
                        long keystoreId = entry.getKeystoreId();
                        SsgKeyFinder finder = keyStoreManager.findByPrimaryKey( keystoreId );
                        if ( finder.isMutable() ) {
                            if ( finder.getKeyStore().deletePrivateKeyEntry(null, currentAlias ).get() ) {
                                logger.config("Deleted old private key entry for alias '"+currentAlias+"'.");
                            } else {
                                logger.config("Deletion of old private key entry for alias '"+currentAlias+"' failed.");
                            }
                        } else {
                            logger.warning("Cannot delete alias in read-only store '"+currentAlias+"'.");
                        }
                    } catch (ObjectNotFoundException e) {
                        // ok, the alias is not in use check next
                    }
                }

                currentAlias = BASE_ALIAS + (i);
            }
        } catch (FindException e) {
            logger.log( Level.WARNING, "Error deleting old private keys.", e );
        } catch (KeyStoreException e) {
            logger.log( Level.WARNING, "Error deleting old private keys.", e );
        } catch (ExecutionException e) {
            logger.log( Level.WARNING, "Error deleting old private keys.", e );
        } catch (InterruptedException e) {
            logger.log( Level.WARNING, "Interrupted when deleting old private keys.", ExceptionUtils.getDebugException(e) );
        }
    }
}
