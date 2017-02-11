package com.l7tech.common.io;

import com.l7tech.util.ArrayUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions.Nullary;
import static com.l7tech.util.Functions.nullary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * Wrapper for an SSL Server Socket Factory.
 */
public class SSLServerSocketFactoryWrapper extends SSLServerSocketFactory {

    //- PUBLIC

    public SSLServerSocketFactoryWrapper( final SSLServerSocketFactory delegate ) {
        this ( nullary(delegate) );
    }

    public SSLServerSocketFactoryWrapper( final Nullary<SSLServerSocketFactory> delegate ) {
        this.delegate = delegate;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.call().getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.call().getSupportedCipherSuites();
    }

    @Override
    public ServerSocket createServerSocket() throws IOException {
        return notifyServerSocket( delegate.call().createServerSocket() );
    }

    @Override
    public ServerSocket createServerSocket( final int port ) throws IOException {
        return notifyServerSocket( delegate.call().createServerSocket( port ) );
    }

    @Override
    public ServerSocket createServerSocket( final int port, final int backlog ) throws IOException {
        return notifyServerSocket( delegate.call().createServerSocket( port, backlog ) );
    }

    @Override
    public ServerSocket createServerSocket( final int port, final int backlog, final InetAddress inetAddress ) throws IOException {
        return notifyServerSocket( delegate.call().createServerSocket( port, backlog, inetAddress ) );
    }

    /**
     * Convenience method for the case of wrapping a socket factory to override the TLS version and/or enabled cipher suites.
     *
     * @param socketFactory the SSLServerSocketFactory to wrap. Required.
     * @param desiredTlsVersions the enabledProtocols array to enable for created sockets, or null to leave them alone.
     * @param desiredTlsCipherSuites the enabledCipherSuites array to enable for created sockets, or null to leave them alone.
     * @return the new wrapper.  Never null.
     */
    @NotNull
    public static SSLServerSocketFactoryWrapper wrapAndSetTlsVersionAndCipherSuites( @NotNull  final SSLServerSocketFactory socketFactory,
                                                                                     @Nullable final String[] desiredTlsVersions,
                                                                                     @Nullable final String[] desiredTlsCipherSuites) {
        return new SSLServerSocketFactoryWrapper(socketFactory) {
            @SuppressWarnings("Duplicates")
            @Override
            protected ServerSocket notifyServerSocket( final ServerSocket socket ) {
                if ( socket instanceof SSLServerSocket ) {
                    final SSLServerSocket sslSocket = (SSLServerSocket) socket;
                    if ( desiredTlsVersions != null ) {
                        final String[] tlsVersions = ArrayUtils.intersection(desiredTlsVersions, sslSocket.getSupportedProtocols());
                        if (desiredTlsVersions.length > 0 && tlsVersions.length == 0) {
                            throw new UnsupportedTlsVersionsException("None of the specified TLS versions are supported by the underlying TLS provider");
                        }
                        try {
                            sslSocket.setEnabledProtocols( tlsVersions );
                        } catch (IllegalArgumentException e) {
                            throw new UnsupportedTlsVersionsException("Specified TLS version is not available in the current configuration: " + ExceptionUtils.getMessage( e ), e);
                        }
                    }
                    if ( desiredTlsCipherSuites != null ) {
                        final String[] tlsCipherSuites = ArrayUtils.intersection(desiredTlsCipherSuites, sslSocket.getSupportedCipherSuites());
                        if (desiredTlsCipherSuites.length > 0 && tlsCipherSuites.length == 0) {
                            throw new UnsupportedTlsCiphersException("None of the specified TLS ciphers are supported by the underlying TLS provider");
                        }
                        sslSocket.setEnabledCipherSuites(tlsCipherSuites);
                    }
                }
                return socket;
            }
        };
    }

    //- PROTECTED

    /**
     * Notification of socket creation.
     *
     * @param socket The socket being created.
     * @return The socket to use (which could be wrapped)
     */
    protected ServerSocket notifyServerSocket( final ServerSocket socket ) {
        return socket;
    }

    //- PRIVATE

    private final Nullary<SSLServerSocketFactory> delegate;

}
