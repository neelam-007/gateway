package com.l7tech.remote.rmi.ssl;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;

/**
 * <p>An <code>SslRMIServerSocketFactory</code> instance is used by the RMI
 * runtime in order to obtain server sockets for RMI calls via SSL.</p>
 * <p/>
 * <p>This class implements <code>RMIServerSocketFactory</code> over
 * the Secure Sockets Layer (SSL) or Transport Layer Security (TLS)
 * protocols.</p>
 * <p/>
 * <p>This class creates SSL sockets using the default
 * <code>SSLSocketFactory</code> (see {@link
 * SSLSocketFactory#getDefault}) or the default
 * <code>SSLServerSocketFactory</code> (see {@link
 * SSLServerSocketFactory#getDefault}).  Therefore, all instances of
 * this class share the same keystore, and the same truststore, when
 * client authentication is required by the server.  This behavior
 * can be modified in subclasses by overriding the {@link
 * #createServerSocket(int)} method; in that case, {@link
 * #equals(Object) equals} and {@link #hashCode() hashCode} may also
 * need to be overridden.</p>
 *
 * @see javax.net.ssl.SSLSocketFactory
 * @see javax.net.ssl.SSLServerSocketFactory
 * @see SslRMIClientSocketFactory
 */
public class SslRMIServerSocketFactory implements RMIServerSocketFactory {

    private String keyStoreFile;
    private String keyStoreType = KeyStore.getDefaultType();
    private String algorithm = "SunX509";
    private String protocol = "TLS";
    private String keyStorePassword;

    /**
     * <p>Creates a new <code>SslRMIServerSocketFactory</code> with
     * the default SSL socket configuration.</p>
     * <p/>
     * <p>SSL connections accepted by server sockets created by this
     * factory have the default cipher suites and protocol versions
     * enabled and do not require client authentication.</p>
     */
    public SslRMIServerSocketFactory() {
        this(null, null, false);
    }

    /**
     * <p>Creates a new <code>SslRMIServerSocketFactory</code> with
     * the specified SSL socket configuration.</p>
     *
     * @param enabledCipherSuites names of all the cipher suites to
     *                            enable on SSL connections accepted by server sockets created by
     *                            this factory, or <code>null</code> to use the cipher suites
     *                            that are enabled by default
     * @param enabledProtocols    names of all the protocol versions to
     *                            enable on SSL connections accepted by server sockets created by
     *                            this factory, or <code>null</code> to use the protocol versions
     *                            that are enabled by default
     * @param needClientAuth      <code>true</code> to require client
     *                            authentication on SSL connections accepted by server sockets
     *                            created by this factory; <code>false</code> to not require
     *                            client authentication
     * @throws IllegalArgumentException when one or more of the cipher
     *                                  suites named by the <code>enabledCipherSuites</code> parameter is
     *                                  not supported, when one or more of the protocols named by the
     *                                  <code>enabledProtocols</code> parameter is not supported or when
     *                                  a problem is encountered while trying to check if the supplied
     *                                  cipher suites and protocols to be enabled are supported.
     * @see SSLSocket#setEnabledCipherSuites
     * @see SSLSocket#setEnabledProtocols
     * @see SSLSocket#setNeedClientAuth
     */
    public SslRMIServerSocketFactory(String[] enabledCipherSuites,
                                     String[] enabledProtocols,
                                     boolean needClientAuth)
      throws IllegalArgumentException, IllegalStateException {

        // Initialize the configuration parameters.
        //
        this.enabledCipherSuites = enabledCipherSuites == null ?
          null : (String[])enabledCipherSuites.clone();
        this.enabledProtocols = enabledProtocols == null ?
          null : (String[])enabledProtocols.clone();
        this.needClientAuth = needClientAuth;

        // Force the initialization of the default at construction time,
        // rather than delaying it to the first time createServerSocket()
        // is called.
        //
        SSLServerSocket sslSocket = null;
        if (this.enabledCipherSuites != null || this.enabledProtocols != null) {
            try {
                final SSLServerSocketFactory sslSocketFactory = getDefaultSSLSocketFactory();
                sslSocket = (SSLServerSocket)sslSocketFactory.createServerSocket();
            } catch (Exception e) {
                final String msg = "Unable to check if the cipher suites " +
                  "and protocols to enable are supported";
                throw (IllegalArgumentException)
                  new IllegalArgumentException(msg).initCause(e);
            }
        }

        // Check if all the cipher suites and protocol versions to enable
        // are supported by the underlying SSL/TLS implementation and if
        // true create lists from arrays.
        //
        if (this.enabledCipherSuites != null) {
            sslSocket.setEnabledCipherSuites(this.enabledCipherSuites);
            enabledCipherSuitesList =
              Arrays.asList((String[])this.enabledCipherSuites);
        }
        if (this.enabledProtocols != null) {
            sslSocket.setEnabledProtocols(this.enabledProtocols);
            enabledProtocolsList =
              Arrays.asList((String[])this.enabledProtocols);
        }
    }

    /**
     * <p>Returns the names of the cipher suites enabled on SSL
     * connections accepted by server sockets created by this factory,
     * or <code>null</code> if this factory uses the cipher suites
     * that are enabled by default.</p>
     *
     * @return an array of cipher suites enabled, or <code>null</code>
     * @see SSLSocket#setEnabledCipherSuites
     */
    public final String[] getEnabledCipherSuites() {
        return enabledCipherSuites == null ?
          null : (String[])enabledCipherSuites.clone();
    }

    /**
     * <p>Returns the names of the protocol versions enabled on SSL
     * connections accepted by server sockets created by this factory,
     * or <code>null</code> if this factory uses the protocol versions
     * that are enabled by default.</p>
     *
     * @return an array of protocol versions enabled, or
     *         <code>null</code>
     * @see SSLSocket#setEnabledProtocols
     */
    public final String[] getEnabledProtocols() {
        return enabledProtocols == null ?
          null : (String[])enabledProtocols.clone();
    }

    /**
     * <p>Returns <code>true</code> if client authentication is
     * required on SSL connections accepted by server sockets created
     * by this factory.</p>
     *
     * @return <code>true</code> if client authentication is required
     * @see SSLSocket#setNeedClientAuth
     */
    public final boolean getNeedClientAuth() {
        return needClientAuth;
    }

    /**
     * Set the algorithm. default is "SunX509"
     * @param algorithm
     */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * Set the keystore file location
     * @param keyStoreFile
     */
    public void setKeyStoreFile(String keyStoreFile) {
        this.keyStoreFile = keyStoreFile;
    }

    /**
     * Set the keystore password
     * @param keyStorePassword the keystore password
     */
    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    /**
     * Set the keystore type, the default is "JKS"
     * @param keyStoreType
     */
    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    /**
     * set the protocol. The default is "TLS"
     * @param protocol
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * <p>Creates a server socket that accepts SSL connections
     * configured according to this factory's SSL socket configuration
     * parameters.</p>
     */
    public ServerSocket createServerSocket(int port) throws IOException {
        try {
            final SSLServerSocketFactory sslSocketFactory = getDefaultSSLSocketFactory();
            return sslSocketFactory.createServerSocket(port);
        } catch (Exception e) {
            IOException ioException = new IOException("Error creating SSL server socket");
            ioException.initCause(e);
            throw ioException;
        }
    }

    /**
     * <p>Indicates whether some other object is "equal to" this one.</p>
     * <p/>
     * <p>Two <code>SslRMIServerSocketFactory</code> objects are equal
     * if they have been constructed with the same SSL socket
     * configuration parameters.</p>
     * <p/>
     * <p>A subclass should override this method (as well as
     * {@link #hashCode()}) if it adds instance state that affects
     * equality.</p>
     */
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof SslRMIServerSocketFactory))
            return false;
        SslRMIServerSocketFactory that = (SslRMIServerSocketFactory)obj;
        return (getClass().equals(that.getClass()) && checkParameters(that));
    }

    private boolean checkParameters(SslRMIServerSocketFactory that) {
        // needClientAuth flag
        //
        if (needClientAuth != that.needClientAuth)
            return false;

        // enabledCipherSuites
        //
        if ((enabledCipherSuites == null && that.enabledCipherSuites != null) ||
          (enabledCipherSuites != null && that.enabledCipherSuites == null))
            return false;
        if (enabledCipherSuites != null && that.enabledCipherSuites != null) {
            List thatEnabledCipherSuitesList =
              Arrays.asList((String[])that.enabledCipherSuites);
            if (!enabledCipherSuitesList.equals(thatEnabledCipherSuitesList))
                return false;
        }

        // enabledProtocols
        //
        if ((enabledProtocols == null && that.enabledProtocols != null) ||
          (enabledProtocols != null && that.enabledProtocols == null))
            return false;
        if (enabledProtocols != null && that.enabledProtocols != null) {
            List thatEnabledProtocolsList =
              Arrays.asList((String[])that.enabledProtocols);
            if (!enabledProtocolsList.equals(thatEnabledProtocolsList))
                return false;
        }

        return true;
    }

    /**
     * <p>Returns a hash code value for this
     * <code>SslRMIServerSocketFactory</code>.</p>
     *
     * @return a hash code value for this
     *         <code>SslRMIServerSocketFactory</code>.
     */
    public int hashCode() {
        return getClass().hashCode() +
          (needClientAuth ? Boolean.TRUE.hashCode() : Boolean.FALSE.hashCode()) +
          (enabledCipherSuites == null ? 0 : enabledCipherSuitesList.hashCode()) +
          (enabledProtocols == null ? 0 : enabledProtocolsList.hashCode());
    }

    private static SSLServerSocketFactory defaultSSLSocketFactory = null;

    private SSLServerSocketFactory getDefaultSSLSocketFactory()
      throws NoSuchAlgorithmException, KeyStoreException,
             IOException, UnrecoverableKeyException, CertificateException, KeyManagementException {
        SSLServerSocketFactory ssf = null;
        // set up key manager to do server authentication
        SSLContext ctx;
        KeyManagerFactory kmf;
        KeyStore ks = null;
        KeyManager[] keyManagers = null;
        char[] pwd = null;

        ctx = SSLContext.getInstance(protocol);
        kmf = KeyManagerFactory.getInstance(algorithm);
        ks = KeyStore.getInstance(keyStoreType);

        if (keyStorePassword !=null && keyStoreFile !=null) {
            pwd = keyStorePassword.toCharArray();
            ks.load(new FileInputStream(keyStoreFile), pwd);
        }
        kmf.init(ks, pwd);
        keyManagers = kmf.getKeyManagers();
        ctx.init(keyManagers, null, null);
        ssf = ctx.getServerSocketFactory();
        defaultSSLSocketFactory = ssf;
        return defaultSSLSocketFactory;
    }

    private final String[] enabledCipherSuites;
    private final String[] enabledProtocols;
    private final boolean needClientAuth;
    private List enabledCipherSuitesList;
    private List enabledProtocolsList;
}
