package com.l7tech.server.transport.http;

import com.l7tech.common.io.SSLSocketWrapper;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.ResourceUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.util.Comparator;

/**
 * This factor creates a GLOBAL instance of an SSL client socket factory.
 *
 * <p>All instances of any subclass MUST be equivalent (for connection pooling purposes).</p>
 *
 * <p>DO NOT modify this class to add (for example) client certificate selection.</p>
 */
public abstract class SslClientSocketFactorySupport extends SSLSocketFactory implements Comparator {

    //- PUBLIC

    /**
     * Check socket factories for equivalence.
     *
     * <p>This factory is the same as any other SslClientSocketFactory</p>
     *
     * <p>This method is used with connection pooling, if removed connections will not be pooled.</p>
     *
     * <p>NOTE: the actual objects passed are Strings ie. "com.l7tech.server.transport.http.SslClientSocketFactory" </p>
     */
    @Override
    public final int compare(Object o1, Object o2) {
        return o1!=null && o2!=null && o1.equals(o2) ? 0 : -1;
    }

    @Override
    public final String[] getDefaultCipherSuites() {
        return getSocketFactory().getDefaultCipherSuites();
    }

    @Override
    public final String[] getSupportedCipherSuites() {
        return getSocketFactory().getSupportedCipherSuites();
    }

    @Override
    public final Socket createSocket() throws IOException {
        Socket socket = getSocketFactory().createSocket();
        return doNotifyCreated( socket, null, null, -1, null, -1 );
    }

    @Override
    public final Socket createSocket( final Socket s,
                                      final String host,
                                      final int port,
                                      final boolean autoClose ) throws IOException {
        final Socket socket = getSocketFactory().createSocket(s, host, port, autoClose);
        return doNotifyCreated( socket, host, null, port, null, -1 );
    }

    @Override
    public final Socket createSocket( final String host,
                                      final int port ) throws IOException {
        Socket socket = getSocketFactory().createSocket(host, port);
        return doNotifyCreated( socket, host, null, port, null, -1 );
    }

    @Override
    public final Socket createSocket( final String host,
                                      final int port,
                                      final InetAddress localAddress,
                                      final int localPort ) throws IOException {
        Socket socket = getSocketFactory().createSocket(host, port, localAddress, localPort);
        return doNotifyCreated( socket, host, null, port, localAddress, localPort );
    }

    @Override
    public final Socket createSocket( final InetAddress address,
                                      final int port ) throws IOException {
        Socket socket = getSocketFactory().createSocket(address, port);
        return doNotifyCreated( socket, null, address, port, null, -1 );
    }

    @Override
    public final Socket createSocket( final InetAddress address,
                                      final int port,
                                      final InetAddress localAddress,
                                      final int localPort ) throws IOException {
        Socket socket = getSocketFactory().createSocket(address, port, localAddress, localPort);
        return doNotifyCreated( socket, null, address, port, localAddress, localPort );
    }

    @Override
    public final Socket createSocket( final Socket s,
                                      final InputStream consumed,
                                      final boolean autoClose) throws IOException {
        Socket socket = getSocketFactory().createSocket(s, consumed, autoClose);
        return doNotifyCreated(socket, null, null, -1, null, -1);
    }

    //- PROTECTED

    protected abstract X509TrustManager getTrustManager();

    protected KeyManager[] getDefaultKeyManagers() {
        return null;
    }

    protected SSLContext buildSSLContext() {
        final KeyManager[] keyManagers = getDefaultKeyManagers();

        final X509TrustManager trustManager = getTrustManager();
        if ( trustManager == null )
            throw new IllegalStateException("TrustManager must be set before first use");

        final int timeout = ConfigFactory.getIntProperty( PROP_SSL_SESSION_TIMEOUT, DEFAULT_SSL_SESSION_TIMEOUT );

        final SSLContext context;
        try {
            context = SSLContext.getInstance("TLS");
            context.init( keyManagers, new TrustManager[]{ trustManager }, null );
            context.getClientSessionContext().setSessionTimeout(timeout);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Couldn't initialize LDAP client SSL context", e);
        }
        
        return context;
    }

    /**
     * Override to recieve notification of created sockets.
     *
     * <p>One of host or address will be non-null.</p>
     *
     * @param socket The new socket
     * @param host The host (may be null)
     * @param address The address (may be null)
     * @param port The remote port
     * @param localAddress The local address (may be null)
     * @param localPort The local port (-1 if not available)
     * @throws IOException Throw if the socket creation should not be permitted.
     */
    protected Socket notifyCreated( Socket socket,
                                    final String host,
                                    final InetAddress address,
                                    final int port,
                                    final InetAddress localAddress,
                                    final int localPort ) throws IOException {
        return socket;
    }

    /**
     * Verify the hostname for the given host or address.
     *
     * @param verifier The hostname verifier to use
     * @param sslSocket The socket to verify
     * @param host The host to check (may be null)
     * @param address The address to check (may be null)
     * @return The socket that should be used for hostname verification
     * @throws IOException If the hostname can be immediately checked is invalid
     */
    protected final Socket doVerifyHostname( final HostnameVerifier verifier,
                                             final SSLSocket sslSocket,
                                             final String host,
                                             final InetAddress address ) throws IOException {
        if ( host == null && address == null ) {
            // we'll have to wrap the socket and wait for connect
            return new SSLSocketWrapper(sslSocket){
                @Override
                public void connect( final SocketAddress endpoint ) throws IOException {
                    super.connect( endpoint );
                    verifyHost( endpoint );
                }

                @Override
                public void connect( final SocketAddress endpoint, final int timeout ) throws IOException {
                    super.connect( endpoint, timeout );
                    verifyHost( endpoint );
                }

                private void verifyHost( final SocketAddress endpoint ) throws IOException {
                    if ( endpoint instanceof InetSocketAddress ) {
                        // Ensure connection is completed before performing hostname verification                        
                        sslSocket.startHandshake();

                        InetSocketAddress inetEndpoint = (InetSocketAddress) endpoint;
                        final String host = InetAddressUtil.getHost( inetEndpoint );
                        if ( !verifier.verify( host, sslSocket.getSession() ) ) {
                            ResourceUtils.closeQuietly( sslSocket );
                            throw new IOException("Host name does not match certificate '" + host + "'.");
                        }
                    }
                }
            };
        } else {
            // verify now and throw on failure
            String hostname = host;
            if ( hostname == null ) {
                hostname = address.getHostName();
            }
            if ( !verifier.verify( hostname, sslSocket.getSession() ) ) {
                throw new IOException("Host name does not match certificate '" + host + "'.");
            }
        }

        return sslSocket;
    }    

    protected SslClientSocketFactorySupport() {
        this.sslContext = buildSSLContext();
    }

    //- PRIVATE

    /**
     * This name is used for backwards compatibility, do not change.
     */
    @SuppressWarnings({ "ClassReferencesSubclass" })
    private static final String PROP_SSL_SESSION_TIMEOUT = SslClientSocketFactory.class.getName() + ".sslSessionTimeoutSeconds";
    private static final int DEFAULT_SSL_SESSION_TIMEOUT = 10 * 60;

    private final SSLContext sslContext;

    private SSLSocketFactory getSocketFactory() {
        return sslContext.getSocketFactory();
    }

    private Socket doNotifyCreated( final Socket socket,
                                    final String host,
                                    final InetAddress address,
                                    final int port,
                                    final InetAddress localAddress,
                                    final int localPort ) throws IOException {
        try {
            return notifyCreated( socket, host, address, port, localAddress, localPort );
        } catch ( Exception e ) {
            ResourceUtils.closeQuietly( socket );

            if ( e instanceof IOException ) {
                throw (IOException) e;
            } else {
                throw ExceptionUtils.wrap( e );
            }
        }
    }
}
