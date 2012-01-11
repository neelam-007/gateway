package com.l7tech.common.http.prov.apache;

import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * A delegating ProtocolSocketFactory with additional scope.
 *
 * <p>This ProtocolSocketFactory can be used if there is any connection state
 * that means connections are not equivalent. The given state object is
 * compared when comparing ProtocolSocketFactory equivalence. If the state
 * objects are not equivalent then the ProtocolSocketFactories are distinct and
 * HTTP connections using those factories will be from distinct connection
 * pools.</p>
 *
 * <p>NOTE: Use of a scoped factory violates the connections-per-host
 * configuration in the same way as a SecureProtocolSocketFactory with a
 * custom SSLSocketFactory. I.e. the host will be considered distinct even
 * though the actual host is the same.</p>
 */
class DelegatingScopedProtocolSocketFactory implements ProtocolSocketFactory {

    //- PUBLIC

    @Override
    public Socket createSocket( final String host, final int port, final InetAddress localAddress, final int localPort ) throws IOException {
        return delegate.createSocket( host, port, localAddress, localPort );
    }

    @Override
    public Socket createSocket( final String host, final int port, final InetAddress localAddress, final int localPort, final HttpConnectionParams params ) throws IOException {
        return delegate.createSocket( host, port, localAddress, localPort, params );
    }

    @Override
    public Socket createSocket( final String host, final int port ) throws IOException {
        return delegate.createSocket( host, port );
    }

    @SuppressWarnings({ "RedundantIfStatement" })
    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        final DelegatingScopedProtocolSocketFactory that = (DelegatingScopedProtocolSocketFactory) o;

        if ( !delegate.equals( that.delegate ) ) return false;
        if ( !scope.equals( that.scope ) ) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = delegate.hashCode();
        result = 31 * result + scope.hashCode();
        return result;
    }

    //- PACKAGE

    static ProtocolSocketFactory wrapWithScope( @NotNull final ProtocolSocketFactory delegate,
                                                @NotNull final Object scope ) {
        return new DelegatingScopedProtocolSocketFactory( delegate, scope );
    }

    DelegatingScopedProtocolSocketFactory( final ProtocolSocketFactory delegate,
                                           final Object scope ) {
        this.delegate = delegate;
        this.scope = scope;
    }

    //- PRIVATE

    private final ProtocolSocketFactory delegate;
    private final Object scope;

}
