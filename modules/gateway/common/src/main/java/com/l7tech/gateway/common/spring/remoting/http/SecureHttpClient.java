package com.l7tech.gateway.common.spring.remoting.http;

import com.l7tech.gateway.common.spring.remoting.ssl.SSLTrustFailureHandler;
import com.l7tech.util.SyspropUtil;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Extension of HttpClient that sets up SSL for the Manager (or other client).
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class SecureHttpClient extends HttpClient {

    //- PUBLIC

    public SecureHttpClient() {
        this( getDefaultKeyManagers() );
    }

    public SecureHttpClient( KeyManager[] keyManagers ) {
        super(new MultiThreadedHttpConnectionManager());

        this.keyManagers = keyManagers;

        MultiThreadedHttpConnectionManager connectionManager =
                (MultiThreadedHttpConnectionManager) getHttpConnectionManager();

        HttpConnectionManagerParams params = connectionManager.getParams();
        params.setMaxTotalConnections(20);
        params.setMaxConnectionsPerHost(HostConfiguration.ANY_HOST_CONFIGURATION, 20);
        params.setConnectionTimeout(30000);
        params.setSoTimeout(60000);

        getHostConfiguration().setHost("127.0.0.1", 80, getProtocol(getSSLSocketFactory()));
        getParams().setBooleanParameter("http.protocol.expect-continue", Boolean.TRUE);
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
     * Check if a trust failure handler is currently installed.
     *
     * @return true if there is a trust failure handler
     */
    public static boolean hasTrustFailureHandler() {
        return currentTrustFailureHandler != null;
    }

    //- PRIVATE

    private static SSLTrustFailureHandler currentTrustFailureHandler;

    private final KeyManager[] keyManagers;

    private SSLSocketFactory getSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            KeyManager[] keyManagers = getKeyManagers();
            TrustManager[] trustManagers = getTrustManagers();
            sslContext.init(keyManagers, trustManagers, null);
            return sslContext.getSocketFactory();
        }
        catch(GeneralSecurityException gse) {
            throw new RuntimeException("Error initializing SSL", gse);
        }
    }

    private KeyManager[] getKeyManagers() {
        return keyManagers;
    }

    private static KeyManager[] getDefaultKeyManagers() {
        return new KeyManager[] { new X509KeyManager() {
            public String[] getClientAliases(String string, Principal[] principals) {
                return new String[0];
            }

            public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
                return null;
            }

            public String[] getServerAliases(String string, Principal[] principals) {
                return new String[0];
            }

            public String chooseServerAlias(String string, Principal[] principals, Socket socket) {
                return null;
            }

            public X509Certificate[] getCertificateChain(String string) {
                return new X509Certificate[0];
            }

            public PrivateKey getPrivateKey(String string) {
                return null;
            }
        } };
    }

    private TrustManager[] getTrustManagers() {
        try {
            String tmalg = SyspropUtil.getString("com.l7tech.console.trustMananagerFactoryAlgorithm",
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
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                throw new CertificateException("This trust manager is for server checks only.");
            }

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
                        if (wrapme.getAcceptedIssuers().length == 0) {
                            throw new CertificateException("No trusted issuers.");
                        }
                        wrapme.checkServerTrusted(chain, authType);
                    } catch (CertificateException e) {
                        if (!sslTrustFailureHandler.handle(e, chain, authType, true)) {
                            throw e;
                        }
                    }
                }
            }

            public X509Certificate[] getAcceptedIssuers() {
                return wrapme.getAcceptedIssuers();
            }
        };
    }

    private Protocol getProtocol(final SSLSocketFactory sockFac) {
        return new Protocol("https", (ProtocolSocketFactory) new SecureProtocolSocketFactory() {
            public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
                return sockFac.createSocket(socket, host, port, autoClose);
            }

            public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort) throws IOException {
                return sockFac.createSocket(host, port, clientAddress, clientPort);
            }

            public Socket createSocket(String host, int port) throws IOException {
                return sockFac.createSocket(host, port);
            }

            public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort, HttpConnectionParams httpConnectionParams) throws IOException {
                Socket socket = sockFac.createSocket();
                int connectTimeout = httpConnectionParams.getConnectionTimeout();

                socket.bind(new InetSocketAddress(clientAddress, clientPort));

                try {
                    socket.connect(new InetSocketAddress(host, port), connectTimeout);
                }
                catch(SocketTimeoutException ste) {
                    throw new ConnectTimeoutException("Timeout when connecting to host '"+host+"'.", ste);
                }

                return socket;
            }
        }, 443);
    }
}
