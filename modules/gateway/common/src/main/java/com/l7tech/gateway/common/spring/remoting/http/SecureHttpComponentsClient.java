package com.l7tech.gateway.common.spring.remoting.http;

import com.l7tech.gateway.common.spring.remoting.ssl.SSLTrustFailureHandler;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.SyspropUtil;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.CoreConnectionPNames;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * Extension of HttpClient that sets up SSL for the Manager (or other client)
 */
public class SecureHttpComponentsClient extends DefaultHttpClient {

    //- PUBLIC

    public static final String PROP_MAX_CONNECTIONS = "com.l7tech.gateway.remoting.maxConnections";
    public static final String PROP_MAX_HOSTCONNECTIONS = "com.l7tech.gateway.remoting.maxConnectionsPerHost";
    public static final String PROP_CONNECTION_TIMEOUT = "com.l7tech.gateway.remoting.connectionTimeout";
    public static final String PROP_READ_TIMEOUT = "com.l7tech.gateway.remoting.readTimeout";
    public static final String PROP_PROTOCOLS = "https.protocols";

    public static final String DEFAULT_PROTOCOLS = null; // "TLSv1";

    public SecureHttpComponentsClient() {
        this( getDefaultKeyManagers() );
    }

    public SecureHttpComponentsClient(KeyManager[] keyManagers) {
        super(new PoolingClientConnectionManager());

        this.keyManagers = keyManagers;

        PoolingClientConnectionManager connectionManager = (PoolingClientConnectionManager) getConnectionManager();

        connectionManager.setMaxTotal(ConfigFactory.getIntProperty(PROP_MAX_CONNECTIONS, 20));
        connectionManager.setDefaultMaxPerRoute(ConfigFactory.getIntProperty(PROP_MAX_HOSTCONNECTIONS, 20));
        getParams().setLongParameter(ClientPNames.CONN_MANAGER_TIMEOUT, ConfigFactory.getIntProperty(PROP_CONNECTION_TIMEOUT, 30000));
        getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, ConfigFactory.getIntProperty(PROP_READ_TIMEOUT, 60000));
        getParams().setBooleanParameter("http.protocol.expect-continue", Boolean.TRUE);

        updateHostConfiguration();
    }

    /**
     * Sets the <code>SSLFailureHandler</code> to be called if the server trust
     * failed. If <b>null</b> clears the existing handler.
     *
     * @param trustFailureHandler the new SSL failure handler
     * @see com.l7tech.gateway.common.spring.remoting.ssl.SSLTrustFailureHandler
     */
    public static void setTrustFailureHandler(SSLTrustFailureHandler trustFailureHandler) {
        currentTrustFailureHandler = trustFailureHandler;
    }

    /**
     * Sets the key manager to be used in the secure http client.
     * @param keyManager the key manager to use.  required.
     */
    public static void setKeyManager(X509KeyManager keyManager) {
        SecureHttpComponentsClient.keyManager = keyManager;
    }

    //- PRIVATE

    private static final String PROTOCOLS = ConfigFactory.getProperty(PROP_PROTOCOLS, SyspropUtil.getString(PROP_PROTOCOLS, DEFAULT_PROTOCOLS));

    private static X509KeyManager keyManager;
    private static SSLTrustFailureHandler currentTrustFailureHandler;

    private void updateHostConfiguration(){
        getConnectionManager().getSchemeRegistry().register(getScheme(getSSLSocketFactory()));
    }

    private final KeyManager[] keyManagers;

    private SSLSocketFactory getSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManager[] keyManagers = getKeyManagers();
            TrustManager[] trustManagers = getTrustManagers();
            sslContext.init(keyManagers, trustManagers, null);
            return sslContext.getSocketFactory();
        }
        catch(GeneralSecurityException gse) {
            throw new RuntimeException("Error initializing SSL", gse);
        }
    }

    private Scheme getScheme(final SSLSocketFactory sockFac) {
        return new Scheme("https", 443, new SecureDirectSocketFactory(sockFac, null));
    }

    private static class SecureDirectSocketFactory extends org.apache.http.conn.ssl.SSLSocketFactory {

        private SecureDirectSocketFactory(javax.net.ssl.SSLSocketFactory socketfactory, X509HostnameVerifier hostnameVerifier) {
            super(socketfactory, hostnameVerifier);
        }

        protected void prepareSocket(SSLSocket socket) throws IOException {
            if(socket != null) {
                configureSslClientSocket(socket);
            }
        }

    }

    private static void configureSslClientSocket(Socket s) {
        SSLSocket sslSocket = (SSLSocket) s;
        if ( PROTOCOLS != null) {
            String[] protos = PROTOCOLS.trim().split("\\s*,\\s*");
            sslSocket.setEnabledProtocols(protos);
        }
    }



    private KeyManager[] getKeyManagers() {
        return keyManagers;
    }

    private static KeyManager[] getDefaultKeyManagers() {
        X509KeyManager manager = keyManager;
        if (manager == null) {
            manager = new X509KeyManager() {
                @Override
                public String[] getClientAliases(String string, Principal[] principals) {
                    return new String[0];
                }

                @Override
                public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
                    return null;
                }

                @Override
                public String[] getServerAliases(String string, Principal[] principals) {
                    return new String[0];
                }

                @Override
                public String chooseServerAlias(String string, Principal[] principals, Socket socket) {
                    return null;
                }

                @Override
                public X509Certificate[] getCertificateChain(String string) {
                    return new X509Certificate[0];
                }

                @Override
                public PrivateKey getPrivateKey(String string) {
                    return null;
                }
            };
        }
        return new KeyManager[] { manager };
    }

    private TrustManager[] getTrustManagers() {
        try {
            String tmalg = ConfigFactory.getProperty("com.l7tech.console.trustMananagerFactoryAlgorithm",
                    TrustManagerFactory.getDefaultAlgorithm());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmalg);
            tmf.init((KeyStore)null);
            TrustManager[] trustManagers = tmf.getTrustManagers();
            for (int t=0; t<trustManagers.length; t++) {
                TrustManager trustManager = trustManagers[t];
                if (trustManager instanceof X509TrustManager) {
                    trustManagers[t] = getWrappedX509TrustManager((X509TrustManager) trustManager);
                }
            }
            return trustManagers;
        }
        catch(GeneralSecurityException gse) {
            throw new RuntimeException("Error initializing SSL", gse);
        }
    }

    private X509TrustManager getWrappedX509TrustManager(final X509TrustManager wrapme) {
        /**
         * An X509 Trust manager that trusts everything.
         */
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                throw new CertificateException("This trust manager is for server checks only.");
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                SSLTrustFailureHandler sslTrustFailureHandler = currentTrustFailureHandler;

                if (sslTrustFailureHandler == null) {  // not specified
                    wrapme.checkServerTrusted(chain, authType);
                } else {
                    try {
                        // we call the failure handler here to allow it to object to the host
                        // name by throwing an exception, this is is bit of a hack and can
                        // be removed if we don't want to check the host name for trusted
                        // certificates.
                        sslTrustFailureHandler.handle(null, chain, authType, false);
                        wrapme.checkServerTrusted(chain, authType);
                    } catch (CertificateException e) {
                        if (!sslTrustFailureHandler.handle(e, chain, authType, true)) {
                            throw e;
                        }
                    }
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return wrapme.getAcceptedIssuers();
            }
        };
    }

    /**
     * Resets the connection by closing any idle connections and recreates the SSL connections.
     */
    public void resetConnection() {
        this.getConnectionManager().closeIdleConnections(0, TimeUnit.MILLISECONDS);
        updateHostConfiguration();
    }
}
