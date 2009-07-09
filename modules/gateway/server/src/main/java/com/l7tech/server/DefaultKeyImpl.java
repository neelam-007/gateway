package com.l7tech.server;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.KeyGenParams;
import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.net.ssl.KeyManager;
import javax.security.auth.x500.X500Principal;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Knows about the location and passwords for the SSL and CA keystores, server certificates, etc.
 */
public class DefaultKeyImpl implements DefaultKey, PropertyChangeListener {
    private static final Logger logger = Logger.getLogger(DefaultKeyImpl.class.getName());

    private static final Pattern KEYSTORE_ID_AND_ALIAS_PATTERN = Pattern.compile("^(-?\\d+):(.*)$");
    private static final SsgKeyEntry NULL_ENTRY = new SsgKeyEntry(Long.MIN_VALUE, null, null, null);

    private final ServerConfig serverConfig;
    private final ClusterPropertyManager clusterPropertyManager;
    private final SsgKeyStoreManager keyStoreManager;
    private final PlatformTransactionManager transactionManager;
    private final AtomicReference<SsgKeyEntry> cachedSslInfo = new AtomicReference<SsgKeyEntry>();
    private final AtomicReference<SsgKeyEntry> cachedCaInfo = new AtomicReference<SsgKeyEntry>();
    private static final String SC_PROP_SSL_KEY = ServerConfig.PARAM_KEYSTORE_DEFAULT_SSL_KEY;

    public DefaultKeyImpl( final ServerConfig serverConfig,
                           final ClusterPropertyManager clusterPropertyManager,
                           final SsgKeyStoreManager keyStoreManager,
                           final PlatformTransactionManager transactionManager) {
        this.serverConfig = serverConfig;
        this.clusterPropertyManager = clusterPropertyManager;
        this.keyStoreManager = keyStoreManager;
        this.transactionManager = transactionManager;
    }

    public SsgKeyEntry getSslInfo() throws IOException {
        try {
            return getCachedEntry(cachedSslInfo, SC_PROP_SSL_KEY, true);
        } catch (ObjectNotFoundException e) {
            // Get the lock, then try again
            synchronized (this) {
                try {
                    // If someone else created it in the meantime, use theirs
                    return getCachedEntry(cachedSslInfo, SC_PROP_SSL_KEY, true);
                } catch (ObjectNotFoundException e1) {
                    // Create a new one
                    TransactionTemplate template = new TransactionTemplate(transactionManager);
                    template.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
                    final IOException[] holder = new IOException[1];
                    SsgKeyEntry entry = (SsgKeyEntry) template.execute(new TransactionCallback(){
                        public Object doInTransaction(TransactionStatus transactionStatus) {
                            try {
                                return generateSelfSignedSslCert();
                            } catch (IOException ioe) {
                                holder[0] = ioe;
                                return null;
                            }
                        }
                    });

                    if ( holder[0] != null ) {
                        throw holder[0];
                    }

                    return entry;
                }
            }
        }
    }

    // There's currently no default SSL key designated.  Generate a new self-signed one to get us started.
    private SsgKeyEntry generateSelfSignedSslCert() throws IOException {
        logger.log(Level.INFO, "No default SSL private key is designated, or the designated SSL private key is not found; " +
                "creating new self-signed SSL private key and certificate chain");
        final boolean wasSystem = AuditContextUtils.isSystem();
        AuditContextUtils.setSystem(true);
        try {
            String alias = findUnusedAlias(SC_PROP_SSL_KEY, "SSL");
            SsgKeyStore sks = findFirstMutableKeystore();
            generateKeyPair(sks, alias);
            return configureAsDefaultSslCert(sks, alias);
        } finally {
            AuditContextUtils.setSystem(wasSystem);
        }
    }

    private SsgKeyEntry configureAsDefaultSslCert(SsgKeyStore sks, String alias) throws IOException {
        try {
            SsgKeyEntry entry = sks.getCertificateChain(alias);
            String name = serverConfig.getClusterPropertyName(SC_PROP_SSL_KEY);
            if (name == null)
                throw new IOException("Unable to configure default SSL key: no cluster property defined for ServerConfig property " + SC_PROP_SSL_KEY);
            String value = entry.getKeystoreId() + ":" + alias;
            clusterPropertyManager.putProperty(name, value);
            invalidateCachedCerts();
            return entry;
        } catch (ObjectNotFoundException e) {
            throw new IOException("Unable to find default SSL key: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        } catch (KeyStoreException e) {
            throw new IOException("Unable to find default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch (FindException e) {
            throw new IOException("Unable to configure default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch (UpdateException e) {
            throw new IOException("Unable to configure default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch (SaveException e) {
            throw new IOException("Unable to configure default SSL key: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void generateKeyPair(SsgKeyStore sks, String alias) throws IOException {
        X500Principal dn = findDefaultSslKeyDn();
        try {
            Future<X509Certificate> job = sks.generateKeyPair(null, alias, new KeyGenParams(), new CertGenParams(dn, 365 * 10, false, null));
            job.get();
        } catch (GeneralSecurityException e) {
            throw new IOException("Unable to create initial default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch (ExecutionException e) {
            throw new IOException("Unable to create initial default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch (InterruptedException e) {
            throw new IOException("Unable to create initial default SSL key: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private X500Principal findDefaultSslKeyDn() {
        String hostname = serverConfig.getProperty( "clusterHost" );
        if ( hostname == null || hostname.trim().length() == 0) {
            hostname = serverConfig.getHostname();
        }
        return new X500Principal("cn=" + hostname);
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
                throw new IOException("No mutable keystores found in which to create initial default SSL key");
        } catch (FindException e) {
            throw new IOException("Unable to find keystore in which to create initial default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch (KeyStoreException e) {
            throw new IOException("Unable to find keystore in which to create initial default SSL key: " + ExceptionUtils.getMessage(e), e);
        }
        return sks;
    }

    private String findUnusedAlias(String propertyName, String defaultBaseAlias) throws IOException {
        Pair<Long, String> keyaddr = getKeyStoreOidAndAlias(propertyName);
        String baseAlias = keyaddr.right;
        if (baseAlias == null || baseAlias.trim().length() < 1)
            baseAlias = defaultBaseAlias;
        String alias = baseAlias;
        int count = 1;
        while (aliasAlreadyUsed(alias)) {
            alias = baseAlias + (count++);
        }
        return alias;
    }

    private boolean aliasAlreadyUsed(String alias) throws IOException {
        try {
            keyStoreManager.lookupKeyByKeyAlias(alias, -1);
            return true;
        } catch (ObjectNotFoundException e) {
            return false;
        } catch (FindException e) {
            throw new IOException("Unable to check if alias \"" + alias + "\" is already used: " + ExceptionUtils.getMessage(e), e);
        } catch (KeyStoreException e) {
            throw new IOException("Unable to check if alias \"" + alias + "\" is already used: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public SsgKeyEntry getCaInfo() {
        try {
            return getCachedEntry(cachedCaInfo, ServerConfig.PARAM_KEYSTORE_DEFAULT_CA_KEY, false);
        } catch (IOException e) {
            throw new RuntimeException("Unable to look up default CA key: " + ExceptionUtils.getMessage(e), e);
        } catch (ObjectNotFoundException e) {
            throw new RuntimeException("Unable to look up default CA key: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public KeyManager[] getSslKeyManagers() {
        try {
            SsgKeyEntry info = getSslInfo();
            X509Certificate[] chain = info.getCertificateChain();
            PrivateKey key = info.getPrivate();
            String alias = info.getAlias();
            return new KeyManager[] { new SingleCertX509KeyManager(chain, key, alias) };
        } catch (IOException e) {
            throw new RuntimeException("No default SSL key available: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public SsgKeyEntry lookupKeyByKeyAlias(String keyAlias, long preferredKeystoreId) throws FindException, KeyStoreException, IOException {
        return keyAlias == null ? getSslInfo() : keyStoreManager.lookupKeyByKeyAlias(keyAlias, preferredKeystoreId);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        invalidateCachedCerts();
    }

    private void invalidateCachedCerts() {
        cachedSslInfo.set(null);
        cachedCaInfo.set(null);
    }

    private SsgKeyEntry getCachedEntry(AtomicReference<SsgKeyEntry> cache, String propertyName, boolean required) throws IOException, ObjectNotFoundException {
        SsgKeyEntry ret;
        for (;;) {
            ret = cache.get();
            if (ret != null)
                break;

            try {
                ret = lookupEntry(propertyName);
            } catch (KeyStoreException e) {
                throw new IOException("Unable to retrieve key for " + propertyName + ": " + ExceptionUtils.getMessage(e), e);
            } catch (FindException e) {
                String reason = ExceptionUtils.getMessage(e);
                logger.log(Level.INFO, "Private key for " + propertyName + " not available: " + reason);
                ret = NULL_ENTRY;
                /* FALLTHROUGH and cache the NULL_ENTRY */
            }

            if (cache.compareAndSet(null, ret))
                break;

            /* We lost the race, but a value is now available in the cache.  Loop and try the lookup again. */
        }

        if (required && (ret == null || ret == NULL_ENTRY))
            throw new ObjectNotFoundException("Required key unavailable: " + propertyName);
        return ret == NULL_ENTRY ? null : ret;
    }

    private Pair<Long, String> getKeyStoreOidAndAlias(String propertyName) throws IOException {
        String propVal = serverConfig.getPropertyUncached(propertyName);
        if (propVal == null || propVal.length() < 1)
            return null;

        Matcher matcher = KEYSTORE_ID_AND_ALIAS_PATTERN.matcher(propVal);
        if (!matcher.matches())
            throw new IOException("Badly formatted value for serverconfig property " + propertyName + ": \"" + propVal + "\"");

        try {
            long keystoreOid = Long.parseLong(matcher.group(1));
            String keyAlias = matcher.group(2);
            return new Pair<Long, String>(keystoreOid, keyAlias);
        } catch (NumberFormatException nfe) {
            throw new IOException("Badly formatted value for serverconfig property " + propertyName + ": " + propVal);
        }
    }

    private SsgKeyEntry lookupEntry(String propertyName) throws IOException, FindException, KeyStoreException {
        Pair<Long, String> keyaddr = getKeyStoreOidAndAlias(propertyName);
        if (keyaddr == null)
            return NULL_ENTRY;

        try {
            return keyStoreManager.lookupKeyByKeyAlias(keyaddr.right, keyaddr.left);
        } catch (ObjectNotFoundException e) {
            // HACK:  If looking for SSL key, check for  "tomcat" alias before giving up  (Bug #6912)
            if (SC_PROP_SSL_KEY.equals(propertyName)) {
                SsgKeyEntry ret = keyStoreManager.lookupKeyByKeyAlias("tomcat", -1);
                try {
                    return new SsgKeyEntry(ret.getKeystoreId(), "tomcat", ret.getCertificateChain(), ret.getPrivateKey());
                } catch (UnrecoverableKeyException e1) {
                    // Can't happen
                    logger.log(Level.SEVERE, "Unable to read 'tomcat' alias private key: " + ExceptionUtils.getMessage(e), e);
                    return NULL_ENTRY;
                }
            }

            return NULL_ENTRY;
        }
    }
}
