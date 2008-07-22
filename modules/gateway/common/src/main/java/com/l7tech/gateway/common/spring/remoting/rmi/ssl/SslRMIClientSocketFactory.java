package com.l7tech.gateway.common.spring.remoting.rmi.ssl;

import com.l7tech.util.SyspropUtil;

import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.KeyStore;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>An <code>SslRMIClientSocketFactory</code> instance is used by the RMI
 * runtime in order to obtain client sockets for RMI calls via SSL.</p>
 *
 * <p>This class implements <code>RMIClientSocketFactory</code> over
 * the Secure Sockets Layer (SSL) or Transport Layer Security (TLS)
 * protocols.</p>
 *
 * <p>This class creates SSL sockets using the default
 * <code>SSLSocketFactory</code> (see {@link
 * SSLSocketFactory#getDefault}).<p/>
 *
 * <p>If the system property
 * <code>javax.rmi.ssl.client.enabledCipherSuites</code> is specified,
 * the {@link #createSocket(String,int)} method will call {@link
 * SSLSocket#setEnabledCipherSuites(String[])} before returning the
 * socket.  The value of this system property is a string that is a
 * comma-separated list of SSL/TLS cipher suites to enable.</p>
 *
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
public final class SslRMIClientSocketFactory implements RMIClientSocketFactory, Serializable {

    //- PUBLIC

    /**
     * Set the SSLTrustFailureHandler to be lazily initialized.
     *
     * <p>When set to true the handler is not created until a connection
     * attempt is made, else it is initialized during instance creation.</p>
     *
     * @param lazy True for lazy initialization.
     */
    public static void setLazyTrustFailureHandler(boolean lazy) {
        lazyInitTrustFailureHandler = lazy;
    }

    /**
     * Sets the <code>SSLFailureHandler</code> to be called if the server trust
     * failed. If <b>null</b> clears the existing handler.
     *
     * @param trustFailureHandler the new SSL failure handler
     * @see SSLTrustFailureHandler
     */
    public static void setTrustFailureHandler(SSLTrustFailureHandler trustFailureHandler) {
        SslRMIClientSocketFactory.currentTrustFailureHandler = trustFailureHandler;
    }

    /**
     * Check if a trust failure handler is currently installed.
     *
     * @return true if there is a trust failure handler
     */
    public static boolean hasTrustFailureHandler() {
        return SslRMIClientSocketFactory.currentTrustFailureHandler != null;
    }

    /**
     * Sets the <code>KeyManager</code> array used to determine the clients credentials.
     *
     * @param keyManagers the key managers to use
     * @see javax.net.ssl.KeyManagerFactory
     */
    public static void setKeyManagers(KeyManager[] keyManagers) {
        SslRMIClientSocketFactory.currentKeyManagers = keyManagers;
    }

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

        // Need to init the trust handler for server-side clients else equality
        // checking will fail.
        //
        // Initialization MUST be lazy on the client or cert discovery will
        // break.
        //
        if (!lazyInitTrustFailureHandler) checkInit();
    }

    /**
     * Allow the host to be set, this is necessary if the servers idea of its name
     * does not match the name the client uses to connect.
     *
     * @param host the host name to use
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * <p>Creates an SSL socket.</p>
     *
     * <p>If the system property
     * <code>javax.rmi.ssl.client.enabledCipherSuites</code> is
     * specified, this method will call {@link
     * SSLSocket#setEnabledCipherSuites(String[])} before returning
     * the socket. The value of this system property is a string that
     * is a comma-separated list of SSL/TLS cipher suites to
     * enable.</p>
     *
     * <p>If the system property
     * <code>javax.rmi.ssl.client.enabledProtocols</code> is
     * specified, this method will call {@link
     * SSLSocket#setEnabledProtocols(String[])} before returning the
     * socket. The value of this system property is a string that is a
     * comma-separated list of SSL/TLS protocol versions to
     * enable.</p>
     */
    public Socket createSocket(String csHost, int csPort) throws IOException {
        checkInit();

        // Derive the host
        final String connectHost = getHost(csHost);

        // Retrieve the SSLSocketFactory
        //
        final SocketFactory sslSocketFactory = getClientSocketFactory(connectHost);

        if(logger.isLoggable(Level.FINE)) {
            logger.fine("Connecting to host '"+connectHost+"', using socket factory '"+sslSocketFactory+"'.");
        }

        // Create the SSLSocket
        //
        final SSLSocket sslSocket = (SSLSocket)
          sslSocketFactory.createSocket(connectHost, csPort);
        // Set the SSLSocket Enabled Cipher Suites
        //
        final String enabledCipherSuites = (String)
          SyspropUtil.getProperty("javax.rmi.ssl.client.enabledCipherSuites");
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
          SyspropUtil.getProperty("javax.rmi.ssl.client.enabledProtocols");
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
     * <p>Indicates whether some other object is "equal to" this one.</p>
     *
     * <p>This factory is equal to another instance of the same class if the
     * instances share the same trust failure handler.</p>
     *
     * @param obj the object to check
     * @return true if equal
     */
    public boolean equals(Object obj) {
        boolean equal = false;

        if(obj!=null) {
            if (obj == this) {
                equal = true;
            }
            else {
                if(obj instanceof SslRMIClientSocketFactory) {
                    SslRMIClientSocketFactory other = (SslRMIClientSocketFactory) obj;
                    if(other.trustFailureHandler == this.trustFailureHandler) {
                        if(host==null || other.host==null) {
                            equal = host==null && other.host==null;
                        }
                        else {
                            equal = host.equals(other.host);
                        }
                    }
                }
            }
        }

        return equal;
    }

    /**
     * <p>Returns a hash code value for this
     * <code>SslRMIClientSocketFactory</code>.</p>
     *
     * @return a hash code value for this
     *         <code>SslRMIClientSocketFactory</code>.
     */
    public int hashCode() {
        return SslRMIClientSocketFactory.class.hashCode() +
                (host==null ? 0 : host.hashCode()) +
                (trustFailureHandler==null ? 0 : trustFailureHandler.hashCode());
    }

    //- PRIVATE

    private static final long serialVersionUID = 2L;
    private static final Logger logger = Logger.getLogger(SslRMIClientSocketFactory.class.getName());

    private static KeyManager[] currentKeyManagers;
    private static SSLTrustFailureHandler currentTrustFailureHandler;
    private static boolean lazyInitTrustFailureHandler = false;

    //
    private String host; // host if not the default
    private transient SSLTrustFailureHandler trustFailureHandler;
    private transient Map socketFactoryByHost;

    /**
     * The internal <code>X509TrustManager</code> that invokes the trust failure
     * handler if it has been set
     */
    private static class SSLClientTrustManager implements X509TrustManager {
        private final X509TrustManager delegate;
        private final SSLTrustFailureHandler sslTrustFailureHandler;

        /**
         * Package subclassing
         *
         * @param delegate the delegating {@link X509TrustManager} instance. For example
         *                 the {@link javax.net.ssl.TrustManagerFactory#getTrustManagers()}
         * @param serverHostname the hostname of the server we expect to be connecting to
         */
        SSLClientTrustManager(X509TrustManager delegate, SSLTrustFailureHandler trustFailureHandler, String serverHostname) {
            if (delegate == null) {
                throw new IllegalArgumentException("The X509 Trust Manager is required");
            }
            this.delegate = delegate;
            this.sslTrustFailureHandler = trustFailureHandler;
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

            if (sslTrustFailureHandler == null) {  // not specified
                delegate.checkServerTrusted(chain, authType);
            } else {
                try {
                    // we call the failure handler here to allow it to object to the host
                    // name by throwing an exception, this is is bit of a hack and can
                    // be removed if we don't want to check the host name for trusted
                    // certificates.
                    sslTrustFailureHandler.handle(null, chain, authType, false);
                    if (delegate.getAcceptedIssuers().length == 0) {
                        throw new CertificateException("No trusted issuers.");
                    }
                    delegate.checkServerTrusted(chain, authType);
                } catch (CertificateException e) {
                    if (!sslTrustFailureHandler.handle(e, chain, authType, true)) {
                        throw e;
                    }
                }
            }
        }
    }

    private void checkInit() {
        if(socketFactoryByHost==null) {
            socketFactoryByHost = new HashMap();
            trustFailureHandler = currentTrustFailureHandler;
        }
    }

    private Object readResolve() throws ObjectStreamException {
        checkInit();
        return this;
    }

    /**
     * Get the host.
     *
     * @param host the suggested host name.
     * @return the host name to connect to
     */
   private String getHost(String host) {
        String hostToUse = host;

        if(this.host!=null) {
            hostToUse = this.host;
        }

        return hostToUse;
    }

    private SocketFactory getClientSocketFactory(String serverHostname) {
        synchronized(socketFactoryByHost) {
            serverHostname = serverHostname.toLowerCase();
            SocketFactory sf = (SocketFactory) socketFactoryByHost.get(serverHostname);
            String tmalg = System.getProperty("com.l7tech.console.trustMananagerFactoryAlgorithm", TrustManagerFactory.getDefaultAlgorithm());
            if (sf == null) {
                try {
                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmalg);
                    tmf.init((KeyStore)null);
                    SSLContext sslContext = SSLContext.getInstance("SSL");
                    sslContext.init(currentKeyManagers,
                                    getTrustManagers(tmf, serverHostname),
                                    null);
                    sf = sslContext.getSocketFactory();
                    socketFactoryByHost.put(serverHostname, sf);
                } catch (Exception e) {
                    throw new RuntimeException("Error initializing the SSL socket factory", e);
                }
            }
            return sf;
        }
    }

    /**
     * Update and return the array of the {@link TrustManager} instances created
     * by the {@link TrustManagerFactory} where the first instance of a
     * {@link X509TrustManager} is replaces with <code>SSLTrustManager</code>.
     *
     * @param tmf <b>non null</b> Trust Manager Factory
     * @param serverHostname the hostname we expect to be connecting to
     * @return the updated array of trust managers
     */
    private TrustManager[] getTrustManagers(TrustManagerFactory tmf, String serverHostname) {
        TrustManager[] trustManagers = tmf.getTrustManagers();
        for (int i = 0; i < trustManagers.length; i++) {
            TrustManager trustManager = trustManagers[i];
            if (trustManager instanceof X509TrustManager) {
                trustManagers[i] = new SSLClientTrustManager((X509TrustManager) trustManager, trustFailureHandler, serverHostname);
                break;
            }
        }
        return trustManagers;
    }
}
