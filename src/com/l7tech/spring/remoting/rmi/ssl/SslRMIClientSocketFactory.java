package com.l7tech.spring.remoting.rmi.ssl;

import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.KeyStore;
import java.util.StringTokenizer;

/**
 * <p>An <code>SslRMIClientSocketFactory</code> instance is used by the RMI
 * runtime in order to obtain client sockets for RMI calls via SSL.</p>
 * <p/>
 * <p>This class implements <code>RMIClientSocketFactory</code> over
 * the Secure Sockets Layer (SSL) or Transport Layer Security (TLS)
 * protocols.</p>
 * <p/>
 * <p>This class creates SSL sockets using the default
 * <code>SSLSocketFactory</code> (see {@link
 * SSLSocketFactory#getDefault}).  All instances of this class are
 * functionally equivalent.  In particular, they all share the same
 * truststore, and the same keystore when client authentication is
 * required by the server.  This behavior can be modified in
 * subclasses by overriding the {@link #createSocket(String,int)}
 * method; in that case, {@link #equals(Object) equals} and {@link
 * #hashCode() hashCode} may also need to be overridden.</p>
 * <p/>
 * <p>If the system property
 * <code>javax.rmi.ssl.client.enabledCipherSuites</code> is specified,
 * the {@link #createSocket(String,int)} method will call {@link
 * SSLSocket#setEnabledCipherSuites(String[])} before returning the
 * socket.  The value of this system property is a string that is a
 * comma-separated list of SSL/TLS cipher suites to enable.</p>
 * <p/>
 * <p>If the system property
 * <code>javax.rmi.ssl.client.enabledProtocols</code> is specified,
 * the {@link #createSocket(String,int)} method will call {@link
 * SSLSocket#setEnabledProtocols(String[])} before returning the
 * socket.  The value of this system property is a string that is a
 * comma-separated list of SSL/TLS protocol versions to enable.</p>
 *
 * @see SSLSocketFactory
 * @see SslRMIServerSocketFactory
 */
public class SslRMIClientSocketFactory
  implements RMIClientSocketFactory, Serializable {
    private static SSLTrustFailureHandler trustFailureHandler;

    /**
     * <p>Creates a new <code>SslRMIClientSocketFactory</code>.</p>
     */
    public SslRMIClientSocketFactory() {
        // We don't force the initialization of the default SSLSocketFactory
        // at construction time - because the RMI client socket factory is
        // created on the server side, where that initialization is a priori
        // meaningless, unless both server and client run in the same JVM.
        // We could possibly override readObject() to force this initialization,
        // but it might not be a good idea to actually mix this with possible
        // deserialization problems.
        // So contrarily to what we do for the server side, the initialization
        // of the SSLSocketFactory will be delayed until the first time
        // createSocket() is called - note that the default SSLSocketFactory
        // might already have been initialized anyway if someone in the JVM
        // already called SSLSocketFactory.getDefault().
        //
    }

    /**
     * <p>Creates an SSL socket.</p>
     * <p/>
     * <p>If the system property
     * <code>javax.rmi.ssl.client.enabledCipherSuites</code> is
     * specified, this method will call {@link
     * SSLSocket#setEnabledCipherSuites(String[])} before returning
     * the socket. The value of this system property is a string that
     * is a comma-separated list of SSL/TLS cipher suites to
     * enable.</p>
     * <p/>
     * <p>If the system property
     * <code>javax.rmi.ssl.client.enabledProtocols</code> is
     * specified, this method will call {@link
     * SSLSocket#setEnabledProtocols(String[])} before returning the
     * socket. The value of this system property is a string that is a
     * comma-separated list of SSL/TLS protocol versions to
     * enable.</p>
     */
    public Socket createSocket(String host, int port) throws IOException {
        // Retrieve the SSLSocketFactory
        //
        final SocketFactory sslSocketFactory = getDefaultClientSocketFactory();
        // Create the SSLSocket
        //
        final SSLSocket sslSocket = (SSLSocket)
          sslSocketFactory.createSocket(host, port);
        // Set the SSLSocket Enabled Cipher Suites
        //
        final String enabledCipherSuites = (String)
          System.getProperty("javax.rmi.ssl.client.enabledCipherSuites");
        if (enabledCipherSuites != null) {
            StringTokenizer st = new StringTokenizer(enabledCipherSuites, ",");
            int tokens = st.countTokens();
            String enabledCipherSuitesList[] = new String[tokens];
            for (int i = 0; i < tokens; i++) {
                enabledCipherSuitesList[i] = st.nextToken();
            }
            try {
                sslSocket.setEnabledCipherSuites(enabledCipherSuitesList);
            } catch (IllegalArgumentException e) {
                throw (IOException)
                  new IOException(e.getMessage()).initCause(e);
            }
        }
        // Set the SSLSocket Enabled Protocols
        //
        final String enabledProtocols = (String)
          System.getProperty("javax.rmi.ssl.client.enabledProtocols");
        if (enabledProtocols != null) {
            StringTokenizer st = new StringTokenizer(enabledProtocols, ",");
            int tokens = st.countTokens();
            String enabledProtocolsList[] = new String[tokens];
            for (int i = 0; i < tokens; i++) {
                enabledProtocolsList[i] = st.nextToken();
            }
            try {
                sslSocket.setEnabledProtocols(enabledProtocolsList);
            } catch (IllegalArgumentException e) {
                throw (IOException)
                  new IOException(e.getMessage()).initCause(e);
            }
        }
        // Return the preconfigured SSLSocket
        return sslSocket;
    }

    /**
     * Sets the <code>SSLFailureHandler</code> to be called if the server trust
     * failed. If <b>null</b> clears the existing handler
     *
     * @param trustFailureHandler the new SSL failure handler
     * @see SSLTrustFailureHandler
     */
    public static synchronized void setTrustFailureHandler(SSLTrustFailureHandler trustFailureHandler) {
        SslRMIClientSocketFactory.trustFailureHandler = trustFailureHandler;
    }

    public static synchronized void resetSocketFactory() {
        SslRMIClientSocketFactory.defaultSocketFactory = null;
    }

    /**
     * <p>Indicates whether some other object is "equal to" this one.</p>
     * <p/>
     * <p>Because all instances of this class are functionally equivalent
     * (they all use the default
     * <code>SSLSocketFactory</code>), this method simply returns
     * <code>this.getClass().equals(obj.getClass())</code>.</p>
     * <p/>
     * <p>A subclass should override this method (as well
     * as {@link #hashCode()}) if its instances are not all
     * functionally equivalent.</p>
     */
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        return this.getClass().equals(obj.getClass());
    }

    /**
     * <p>Returns a hash code value for this
     * <code>SslRMIClientSocketFactory</code>.</p>
     *
     * @return a hash code value for this
     *         <code>SslRMIClientSocketFactory</code>.
     */
    public int hashCode() {
        return this.getClass().hashCode();
    }

    private static SocketFactory defaultSocketFactory = null;

    private synchronized SocketFactory getDefaultClientSocketFactory() {
        if (defaultSocketFactory == null) {
            String algorithm = TrustManagerFactory.getDefaultAlgorithm();
            SSLContext sslContext = null;
            try {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
                tmf.init((KeyStore)null);
                sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, getTrustManagers(tmf), null);
                defaultSocketFactory = sslContext.getSocketFactory();
            } catch (Exception e) {
                throw new RuntimeException("Error initializing the SSL socket factory", e);
            }
        }
        return defaultSocketFactory;
    }


    /**
     * Update and return the array of the {@link TrustManager} instances created
     * by the {@link TrustManagerFactory} where the first instance of a
     * {@link X509TrustManager} is replaces with <code>SSLTrustManager</code>.
     *
     * @param tmf <b>non null</b> Trust Manager Factory
     * @return the updated array of trust managers
     */
    private static TrustManager[] getTrustManagers(TrustManagerFactory tmf) {
        TrustManager[] trustManagers = tmf.getTrustManagers();
        for (int i = 0; i < trustManagers.length; i++) {
            TrustManager trustManager = trustManagers[i];
            if (trustManager instanceof X509TrustManager) {
                trustManagers[i] = new SSLClientTrustManager((X509TrustManager)trustManager);
                break;
            }
        }
        return trustManagers;
    }

    /**
     * The internal <code>X509TrustManager</code> that invokes the trust failure
     * handler if it has been set
     */
    private static class SSLClientTrustManager implements X509TrustManager {
        private X509TrustManager delegate;

        /**
         * Package subclassing
         *
         * @param delegate the delegating {@link X509TrustManager} instance. For example
         *                 the {@link javax.net.ssl.TrustManagerFactory#getTrustManagers()}
         */
        SSLClientTrustManager(X509TrustManager delegate) {
            if (delegate == null) {
                throw new IllegalArgumentException("The X509 Trust Manager is required");
            }
            this.delegate = delegate;
        }

        /**
         * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
         */
        public X509Certificate[] getAcceptedIssuers() {
            return delegate.getAcceptedIssuers();
        }

        /**
         * @see javax.net.ssl.X509TrustManager#checkClientTrusted(java.security.cert.X509Certificate[], String)
         */
        public void checkClientTrusted(X509Certificate[] chain, String authType)
          throws CertificateException {
            delegate.checkClientTrusted(chain, authType);
        }

        /**
         * @see javax.net.ssl.X509TrustManager#checkServerTrusted(java.security.cert.X509Certificate[], String)
         */
        public void checkServerTrusted(X509Certificate[] chain, String authType)
          throws CertificateException {
            SSLTrustFailureHandler trustFailureHandler = SslRMIClientSocketFactory.trustFailureHandler;

            if (trustFailureHandler == null) {  // not specified
                delegate.checkServerTrusted(chain, authType);
            } else {
                try {
                    delegate.checkServerTrusted(chain, authType);
                } catch (CertificateException e) {
                    if (!trustFailureHandler.handle(e, chain, authType)) {
                        throw e;
                    }
                }
            }
        }
    }

    private static final long serialVersionUID = -8310631444933958385L;
}
