package com.l7tech.spring.remoting.rmi.ssl;

import sun.security.x509.X500Name;

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
     * Sets the <code>KeyManagerFactory</code> used to determine the clients credentials.
     *
     * @param keyManagerFactory the factory to use
     * @see javax.net.ssl.KeyManagerFactory
     */
    public static void setKeyManagerFactory(KeyManagerFactory keyManagerFactory) {
        SslRMIClientSocketFactory.currentKeyManagerFactory = keyManagerFactory;
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
    public Socket createSocket(String host, int port) throws IOException {
        checkInit();

        // Retrieve the SSLSocketFactory
        //
        final SocketFactory sslSocketFactory = getClientSocketFactory(host);
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
                        equal = true;
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
        return 17 * (trustFailureHandler==null ? SslRMIClientSocketFactory.class.hashCode() : trustFailureHandler.hashCode());
    }

    //- PRIVATE

    private static final long serialVersionUID = -8310631444933958385L;
    private static final Logger logger = Logger.getLogger(SslRMIClientSocketFactory.class.getName());

    private static KeyManagerFactory currentKeyManagerFactory;
    private static SSLTrustFailureHandler currentTrustFailureHandler;

    //
    private transient SSLTrustFailureHandler trustFailureHandler;
    private transient Map socketFactoryByHost;

    /**
     * The internal <code>X509TrustManager</code> that invokes the trust failure
     * handler if it has been set
     */
    private static class SSLClientTrustManager implements X509TrustManager {
        private final X509TrustManager delegate;
        private final SSLTrustFailureHandler trustFailureHandler;
        private final String serverHostname;

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
            this.trustFailureHandler = trustFailureHandler;
            this.serverHostname = serverHostname.toLowerCase();
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

            if (trustFailureHandler == null) {  // not specified
                delegate.checkServerTrusted(chain, authType);
            } else {
                try {
                    // we call the failure handler here to allow it to object to the host
                    // name by throwing an exception, this is is bit of a hack and can
                    // be removed if we don't want to check the host name for trusted
                    // certificates.
                    trustFailureHandler.handle(null, chain, authType);
                    if (delegate.getAcceptedIssuers().length == 0) {
                        throw new CertificateException("No trusted issuers.");
                    }
                    delegate.checkServerTrusted(chain, authType);
                } catch (CertificateException e) {
                    if (!trustFailureHandler.handle(e, chain, authType)) {
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
        SslRMIClientSocketFactory resolved = this;

        if(resolved.trustFailureHandler != currentTrustFailureHandler) {
            // if the trust failure handler has changed we must create a new factory.
            resolved = new SslRMIClientSocketFactory();
        }

        return resolved;
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
                    sslContext.init(
                            currentKeyManagerFactory !=null ? currentKeyManagerFactory.getKeyManagers() : null, 
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
