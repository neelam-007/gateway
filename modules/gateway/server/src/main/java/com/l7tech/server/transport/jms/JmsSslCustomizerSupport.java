package com.l7tech.server.transport.jms;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntity;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.transport.http.SslClientSocketFactory;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.Pair;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.objectmodel.ObjectModelException;

import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import java.util.Map;
import java.util.HashMap;
import java.security.GeneralSecurityException;

/**
 *
 */
public class JmsSslCustomizerSupport {

    //- PUBLIC

    /**
     * Get an SSLSocketFactory that is initialized to use the given key.
     *
     * @param keystoreIdStr The ID of the keystore to use (or null for none)
     * @param alias The alias of the key to use (or null for none)
     * @return The SSLSocketFactory or null
     * @throws JmsConfigException If an error occurs
     */
    public synchronized static SSLSocketFactory getSocketFactory( final String keystoreIdStr, final String alias ) throws JmsConfigException {
        return doGetSocketFactory( keystoreIdStr, alias );
    }

    /**
     * Get an SSLContext that is initialized to use the given key (or no client auth).
     *
     * @param keystoreIdStr The ID of the keystore to use (or null for none)
     * @param alias The alias of the key to use (or null for none)
     * @return The SSLContext or null
     * @throws JmsConfigException If an error occurs
     */
    public synchronized static SSLContext getSSLContext( final String keystoreIdStr, final String alias ) throws JmsConfigException {
        return doGetSSLContext( keystoreIdStr, alias );
    }

    /**
     * Configuration of outbound trust
     */
    public synchronized static void setTrustManager( final X509TrustManager trustManager ) {
        JmsSslCustomizerSupport.trustManager = trustManager;
    }

    /**
     * Configuration of key manager
     */
    public synchronized static void setSsgKeyStoreManager( final SsgKeyStoreManager ssgKeyStoreManager ) {
        JmsSslCustomizerSupport.ssgKeyStoreManager = ssgKeyStoreManager;
    }

    //- PRIVATE

    /**
     * This name is used for backwards compatibility, do not change.
     */
    private static final String PROP_SSL_SESSION_TIMEOUT = SslClientSocketFactory.class.getName() + ".sslSessionTimeoutSeconds";
    private static final int DEFAULT_SSL_SESSION_TIMEOUT = 10 * 60;
    private static final Map<Pair<Goid,String>, SSLContext> instancesByKeyEntryId = new HashMap<Pair<Goid,String>, SSLContext>();

    private static SsgKeyStoreManager ssgKeyStoreManager;
    private static X509TrustManager trustManager;

    /**
     *
     */
    private static SSLSocketFactory doGetSocketFactory( final String keystoreIdStr, final String alias ) throws JmsConfigException {
        return doGetSSLContext(keystoreIdStr, alias).getSocketFactory();
    }
        
    /**
     *
     */
    private static SSLContext doGetSSLContext( final String keystoreIdStr, final String alias ) throws JmsConfigException {
        if (ssgKeyStoreManager == null) throw new IllegalStateException("Gateway Keystore Manager must be set first");
        if (trustManager == null) throw new IllegalStateException("TrustManager must be set before first use");

        final Pair<Goid,String> keyId;
        if ( keystoreIdStr==null && alias == null ) {
            keyId = new Pair<Goid,String>(GoidEntity.DEFAULT_GOID, "");
        } else {
            // process keystore
            Goid keystoreId;
            try {
                keystoreId = GoidUpgradeMapper.mapId(EntityType.SSG_KEYSTORE, keystoreIdStr);
            } catch ( IllegalArgumentException iae ) {
                throw new JmsConfigException("Bad keystore ID: " + keystoreIdStr);
            }

            keyId = new Pair<Goid,String>(keystoreId, alias);
        }

        SSLContext instance = instancesByKeyEntryId.get(keyId);
        if (instance == null) {
            try {
                KeyManager[] keyManagers;
                if ( keystoreIdStr==null && alias == null ) {
                    keyManagers = null;
                } else {
                    SsgKeyEntry entry = ssgKeyStoreManager.lookupKeyByKeyAlias(alias, keyId.left);
                    KeyManager keyManager = new SingleCertX509KeyManager(entry.getCertificateChain(), entry.getPrivateKey());
                    keyManagers = new KeyManager[] { keyManager };
                }

                final SSLContext context = SSLContext.getInstance("TLS");
                context.init(keyManagers,
                             new TrustManager[] { trustManager } ,
                             null);
                int timeout = ConfigFactory.getIntProperty( PROP_SSL_SESSION_TIMEOUT, DEFAULT_SSL_SESSION_TIMEOUT );
                context.getClientSessionContext().setSessionTimeout(timeout);
                instance = context;
            } catch (GeneralSecurityException e) {
                throw new JmsConfigException("Couldn't initialize LDAP client SSL context : " + ExceptionUtils.getMessage( e ), e);
            } catch (ObjectModelException e) {
                throw new JmsConfigException("Couldn't initialize LDAP client SSL context : " + ExceptionUtils.getMessage( e ), e);
            }

            instancesByKeyEntryId.put(keyId, instance);
        }

        return instance;
    }
}
