package com.l7tech.server.transport.http;

import com.l7tech.util.SyspropUtil;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.common.io.SocketWrapper;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocket;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.security.GeneralSecurityException;
import java.net.Socket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.io.IOException;

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

    //- PROTECTED

    protected abstract X509TrustManager getTrustManager();

    protected KeyManager[] getDefaultKeyManagers() {
        return new KeyManager[0];
    }

    protected SSLContext buildSSLContext() {
        KeyManager[] keyManagers = getDefaultKeyManagers();
        if ( keyManagers == null )
            throw new IllegalStateException("KeyManagers must be set before first use");

        X509TrustManager trustManager = getTrustManager();
        if ( trustManager == null )
            throw new IllegalStateException("TrustManager must be set before first use");

        int timeout = SyspropUtil.getInteger(PROP_SSL_SESSION_TIMEOUT, DEFAULT_SSL_SESSION_TIMEOUT);

        SSLContext context;
        try {
            context = SSLContext.getInstance("SSL");
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
    protected Socket notifyCreated( final Socket socket,
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
            return new SocketWrapper(sslSocket){
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
                        InetSocketAddress inetEndpoint = (InetSocketAddress) endpoint;
                        final String host = getHost(inetEndpoint);
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
    private static final String PROP_SSL_SESSION_TIMEOUT = SslClientSocketFactory.class.getName() + ".sslSessionTimeoutSeconds";
    private static final int DEFAULT_SSL_SESSION_TIMEOUT = 10 * 60;

    /**
     * Regex for matching addresses in the formats:
     *
     * - hostname:port
     * - ipv4:port
     * - ipv6:port
     * - /ipv4:port
     * - /ipv6:port
     * - hostname/ipv4:port
     * - hostname/ipv6:port
     */
    private static final Pattern HOST_PATTERN = Pattern.compile( "([A-Za-z0-9_\\-\\.:]{1,1024})?/?([A-Za-z0-9_\\-\\.:]{1,1024}):[0-9]{1,5}" );

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

    // hack to access hostname without causing reverse lookup
    private static String getHost( final InetSocketAddress address ) {
        String host;

        // TODO switch to getHostString when JDK7 is required
        Matcher matcher = HOST_PATTERN.matcher( address.toString() );
        if ( matcher.matches() ) {
            host = matcher.group(1); // hostname, if present
            if ( host == null ) {
                host = matcher.group(2); // was unresolved, so this could be an IP address or hostname
            }
        } else {
            // fallback that can cause reverse DNS lookup
            host = address.getHostName();
        }

        return host;
    }
}
