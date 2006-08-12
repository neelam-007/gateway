package com.l7tech.spring.remoting.http;

import com.l7tech.spring.remoting.rmi.ssl.SSLTrustFailureHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
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
        super(new MultiThreadedHttpConnectionManager());

        MultiThreadedHttpConnectionManager connectionManager =
                (MultiThreadedHttpConnectionManager) getHttpConnectionManager();

        connectionManager.setMaxTotalConnections(20);
        connectionManager.setMaxConnectionsPerHost(20);

        setConnectionTimeout(30000);
        setTimeout(60000);

        getHostConfiguration().setHost("127.0.0.1", 80, getProtocol(getSSLSocketFactory()));
    }

    /**
     * Sets the <code>SSLFailureHandler</code> to be called if the server trust
     * failed. If <b>null</b> clears the existing handler.
     *
     * @param trustFailureHandler the new SSL failure handler
     * @see com.l7tech.spring.remoting.rmi.ssl.SSLTrustFailureHandler
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

    private SSLSocketFactory getSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            TrustManager[] trustManagers = getTrustManagers();
            sslContext.init(null, trustManagers, null);
            return sslContext.getSocketFactory();
        }
        catch(GeneralSecurityException gse) {
            throw new RuntimeException("Error initializing SSL", gse);
        }
    }

    private TrustManager[] getTrustManagers() {
        try {
            String tmalg = System.getProperty("com.l7tech.console.trustMananagerFactoryAlgorithm", TrustManagerFactory.getDefaultAlgorithm());
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
                    if (wrapme.getAcceptedIssuers().length == 0) {
                        throw new CertificateException("No trusted issuers.");
                    }
                    wrapme.checkServerTrusted(chain, authType);
                } else {
                    try {
                        // we call the failure handler here to allow it to object to the host
                        // name by throwing an exception, this is is bit of a hack and can
                        // be removed if we don't want to check the host name for trusted
                        // certificates.
                        sslTrustFailureHandler.handle(null, chain, authType);
                        if (wrapme.getAcceptedIssuers().length == 0) {
                            throw new CertificateException("No trusted issuers.");
                        }
                        wrapme.checkServerTrusted(chain, authType);
                    } catch (CertificateException e) {
                        if (!sslTrustFailureHandler.handle(e, chain, authType)) {
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
        return new Protocol("https", new SecureProtocolSocketFactory() {
            public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
                return sockFac.createSocket(socket, host, port, autoClose);
            }

            public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort) throws IOException, UnknownHostException {
                return sockFac.createSocket(host, port, clientAddress, clientPort);
            }

            public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
                return sockFac.createSocket(host, port);
            }
        }, 443);
    }
}
