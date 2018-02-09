package com.l7tech.gateway.common.spring.remoting.http;

import com.l7tech.gateway.common.spring.remoting.ssl.SSLTrustFailureHandler;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.Background;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.SyspropUtil;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extension of HttpClient that sets up SSL for the Manager (or other client)
 */
public class SecureHttpComponentsClient extends DefaultHttpClientWithHttpContext implements InitializingBean, DisposableBean {
    private static final Logger logger = Logger.getLogger(SecureHttpComponentsClient.class.getName());

    private static final String PROP_MAX_CONNECTIONS = "com.l7tech.gateway.remoting.maxConnections";
    private static final String PROP_MAX_HOSTCONNECTIONS = "com.l7tech.gateway.remoting.maxConnectionsPerHost";
    private static final String PROP_CONNECTION_TIMEOUT = "com.l7tech.gateway.remoting.connectionTimeout";
    private static final String PROP_READ_TIMEOUT = "com.l7tech.gateway.remoting.readTimeout";
    private static final String PROP_IDLE_CONNECTION_INTERVAL = "com.l7tech.gateway.remoting.idleConnectionInterval";
    private static final String PROP_IDLE_CONNECTION_TIMEOUT = "com.l7tech.gateway.remoting.idleConnectionTimeout";
    private static final String PROP_PROTOCOLS = "https.protocols";

    private static final String DEFAULT_PROTOCOLS = null; // "TLSv1.2";
    private static final long DEFAULT_IDLE_CONNECTION_TIMEOUT = 0L;
    private static final long DEFAULT_IDLE_CONNECTION_INTERVAL = 60000L;

    private TimerTask idleTimeoutTask = null;

    public SecureHttpComponentsClient() {
        this(getDefaultKeyManagers());
    }

    private SecureHttpComponentsClient(KeyManager[] keyManagers) {
        super(new PoolingClientConnectionManager());

        this.keyManagers = keyManagers;

        PoolingClientConnectionManager connectionManager = (PoolingClientConnectionManager) getConnectionManager();

        connectionManager.setMaxTotal(ConfigFactory.getIntProperty(PROP_MAX_CONNECTIONS, 20));
        connectionManager.setDefaultMaxPerRoute(ConfigFactory.getIntProperty(PROP_MAX_HOSTCONNECTIONS, 20));
        getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT , ConfigFactory.getIntProperty(PROP_CONNECTION_TIMEOUT, 30000));
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
    private static final long IDLE_CONNECTION_TIMEOUT = ConfigFactory.getLongProperty(PROP_IDLE_CONNECTION_TIMEOUT, DEFAULT_IDLE_CONNECTION_TIMEOUT);

    private static X509KeyManager keyManager;
    private static SSLTrustFailureHandler currentTrustFailureHandler;

    private void updateHostConfiguration() {
        getConnectionManager().getSchemeRegistry().register(getScheme(getSSLSocketFactory()));
    }

    private final KeyManager[] keyManagers;

    private SSLSocketFactory getSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            JceProvider.getInstance().prepareSslContext(sslContext);
            KeyManager[] keyManagers = getKeyManagers();
            TrustManager[] trustManagers = getTrustManagers();
            sslContext.init(keyManagers, trustManagers, null);
            return sslContext.getSocketFactory();
        } catch (GeneralSecurityException gse) {
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

        @Override
        public Socket connectSocket(final Socket socket, final InetSocketAddress remoteAddress, final InetSocketAddress localAddress, final HttpParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
            if (socket instanceof SSLSocket) {
                configureSNIHeader((SSLSocket) socket, remoteAddress.getHostString());
            }
            return super.connectSocket(socket, remoteAddress, localAddress, params);
        }

        @Override
        protected void prepareSocket(SSLSocket socket) throws IOException {
            if (socket != null) {
                configureSslClientSocket(socket);
            }
        }

        private static void configureSNIHeader(final SSLSocket socket, final String host) {
            final SSLParameters parameters = socket.getSSLParameters();
            parameters.setServerNames(Collections.singletonList(new SNIHostName(host)));
            socket.setSSLParameters(parameters);
        }

        private static void configureSslClientSocket(Socket s) {
            SSLSocket sslSocket = (SSLSocket) s;
            if (PROTOCOLS != null) {
                String[] protos = PROTOCOLS.trim().split("\\s*,\\s*");
                sslSocket.setEnabledProtocols(protos);
            }
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
        return new KeyManager[]{manager};
    }

    private TrustManager[] getTrustManagers() {
        try {
            String tmalg = ConfigFactory.getProperty("com.l7tech.console.trustMananagerFactoryAlgorithm",
                    TrustManagerFactory.getDefaultAlgorithm());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmalg);
            tmf.init((KeyStore) null);
            TrustManager[] trustManagers = tmf.getTrustManagers();
            for (int t = 0; t < trustManagers.length; t++) {
                TrustManager trustManager = trustManagers[t];
                if (trustManager instanceof X509TrustManager) {
                    trustManagers[t] = getWrappedX509TrustManager((X509TrustManager) trustManager);
                }
            }
            return trustManagers;
        } catch (GeneralSecurityException gse) {
            throw new RuntimeException("Error initializing SSL", gse);
        }
    }

    private X509TrustManager getWrappedX509TrustManager(final X509TrustManager wrapme) {
        /*
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
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Closing idle connections.");
        }

        this.getConnectionManager().closeIdleConnections(IDLE_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
        updateHostConfiguration();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.log(Level.FINE, "Scheduling idle connection cleanup task.");

        idleTimeoutTask = new TimerTask() {
            @Override
            public void run() {
                resetConnection();
            }
        };

        final long interval = ConfigFactory.getLongProperty(PROP_IDLE_CONNECTION_INTERVAL, DEFAULT_IDLE_CONNECTION_INTERVAL);
        Background.scheduleRepeated(idleTimeoutTask, interval, interval);
    }

    @Override
    public void destroy() throws Exception {
        logger.log(Level.FINE, "Stopping idle connection cleanup task.");

        if (idleTimeoutTask != null) {
            Background.cancel(idleTimeoutTask);
            idleTimeoutTask = null;
        }
    }
}