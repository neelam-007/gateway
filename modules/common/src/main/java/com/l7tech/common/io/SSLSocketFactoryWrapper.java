package com.l7tech.common.io;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions.Nullary;
import static com.l7tech.util.Functions.nullary;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Wrapper for an SSL Socket Factory.
 */
public class SSLSocketFactoryWrapper extends SSLSocketFactory {

    //- PUBLIC

    public SSLSocketFactoryWrapper( final SSLSocketFactory delegate ) {
        this ( nullary ( delegate ) );
    }

    public SSLSocketFactoryWrapper( final Nullary<SSLSocketFactory> delegate ) {
        this.delegate = delegate;
    }

    @Override
    public Socket createSocket( final Socket socket, final String s, final int i, final boolean b ) throws IOException {
        return notifySocket(delegate.call().createSocket( socket, s, i, b ));
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
    public Socket createSocket() throws IOException {
        return notifySocket(delegate.call().createSocket());
    }

    @Override
    public Socket createSocket( final InetAddress inetAddress, final int i ) throws IOException {
        return notifySocket(delegate.call().createSocket( inetAddress, i ));
    }

    @Override
    public Socket createSocket( final InetAddress inetAddress, final int i, final InetAddress inetAddress1, final int i1 ) throws IOException {
        return notifySocket(delegate.call().createSocket( inetAddress, i, inetAddress1, i1 ));
    }

    @Override
    public Socket createSocket( final String s, final int i ) throws IOException {
        return notifySocket(delegate.call().createSocket( s, i ));
    }

    @Override
    public Socket createSocket( final String s, final int i, final InetAddress inetAddress, final int i1 ) throws IOException {
        return notifySocket(delegate.call().createSocket( s, i, inetAddress, i1 ));
    }

    @Override
    public Socket createSocket(final Socket s, final InputStream inputStream, final boolean b) throws IOException {
        return notifySocket(delegate.call().createSocket(s, inputStream, b));
    }

    /**
     * Convenience method for the case of wrapping a socket factory to override the TLS version and/or enabled cipher suites.
     *
     * @param socketFactory the SSLSocketFactory to wrap. Required.
     * @param tlsVersions the enabledProtocols array to enable for created sockets, or null to leave them alone.
     * @param tlsCipherSuites the enabledCipherSuites array to enable for created sockets, or null to leave them alone.
     * @return the new wrapper.  Never null.
     */
    public static SSLSocketFactoryWrapper wrapAndSetTlsVersionAndCipherSuites(SSLSocketFactory socketFactory, final String[] tlsVersions, final String[] tlsCipherSuites) {
        return new SSLSocketFactoryWrapper(socketFactory) {
            @Override
            protected Socket notifySocket( final Socket socket ) {
                if ( socket instanceof SSLSocket) {
                    final SSLSocket sslSocket = (SSLSocket) socket;
                    if (tlsVersions != null) {
                        try {
                            sslSocket.setEnabledProtocols(tlsVersions);
                        } catch (IllegalArgumentException e) {
                            throw new UnsupportedTlsVersionsException("Specified TLS version is not available in the current configuration: " + ExceptionUtils.getMessage(e), e);
                        }
                    }
                    if (tlsCipherSuites != null)
                        sslSocket.setEnabledCipherSuites(tlsCipherSuites);
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
    protected Socket notifySocket( final Socket socket ) {
        return socket;
    }

    //- PRIVATE

    private final Nullary<SSLSocketFactory> delegate;

}
