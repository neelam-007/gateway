package com.l7tech.server.transport.jms;

import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.transport.http.SslClientSocketFactory;
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
import java.util.Collections;
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
     * @param keystoreIdStr The ID of the keystore to use
     * @param alias The alias of the key to use
     * @return The SSLSocketFactory or null
     * @throws JmsConfigException If an error occurs
     */
    public synchronized static SSLSocketFactory getSocketFactory( final String keystoreIdStr, final String alias ) throws JmsConfigException {
        return doGetSocketFactory( keystoreIdStr, alias );
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
    private static final Map<Pair<Long,String>, SSLContext> instancesByKeyEntryId = new HashMap<Pair<Long,String>, SSLContext>();

    private static SsgKeyStoreManager ssgKeyStoreManager;
    private static X509TrustManager trustManager;

    /**
     *
     */
    private static SSLSocketFactory doGetSocketFactory( final String keystoreIdStr, final String alias ) throws JmsConfigException {
        if (ssgKeyStoreManager == null) throw new IllegalStateException("SSG Keystore Manager must be set first");
        if (trustManager == null) throw new IllegalStateException("TrustManager must be set before first use");

        // process keystore
        long keystoreId;
        try {
            keystoreId = Long.parseLong( keystoreIdStr );
        } catch ( NumberFormatException nfe ) {
            throw new JmsConfigException("Bad keystore ID: " + keystoreIdStr);
        }

        final Pair<Long,String> keyId = new Pair<Long,String>(keystoreId, alias);
        SSLContext instance = instancesByKeyEntryId.get(keyId);
        if (instance == null) {
            try {
                SsgKeyEntry entry = ssgKeyStoreManager.lookupKeyByKeyAlias(alias, keystoreId);
                KeyManager keyManager = new SingleCertX509KeyManager(entry.getCertificateChain(), entry.getPrivateKey());

                final SSLContext context = SSLContext.getInstance("SSL");
                context.init(new KeyManager[] { keyManager },
                             new TrustManager[] { trustManager } ,
                             null);
                int timeout = Integer.getInteger(PROP_SSL_SESSION_TIMEOUT, DEFAULT_SSL_SESSION_TIMEOUT);
                context.getClientSessionContext().setSessionTimeout(timeout);
                instance = context;
            } catch (GeneralSecurityException e) {
                throw new JmsConfigException("Couldn't initialize LDAP client SSL context", e);
            } catch (ObjectModelException e) {
                throw new JmsConfigException("Couldn't initialize LDAP client SSL context", e);
            }

            instancesByKeyEntryId.put(keyId, instance);
        }

        return instance.getSocketFactory();
    }
}
