package com.l7tech.common.http.prov.apache;

import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.Socket;

/**
 * A delegating SecureProtocolSocketFactory with additional scope.
 */
class DelegatingScopedSecureProtocolSocketFactory extends DelegatingScopedProtocolSocketFactory implements SecureProtocolSocketFactory {

    //- PUBLIC

    @Override
    public Socket createSocket( final Socket socket, final String host, final int port, final boolean autoClose ) throws IOException {
        return delegate.createSocket( socket, host, port, autoClose );
    }

    //- PACKAGE

    static SecureProtocolSocketFactory wrapSecureWithScope( @NotNull final SecureProtocolSocketFactory delegate,
                                                            @NotNull final Object scope ) {
        return new DelegatingScopedSecureProtocolSocketFactory( delegate, scope );
    }

    //- PRIVATE

    private final SecureProtocolSocketFactory delegate;

    private DelegatingScopedSecureProtocolSocketFactory( final SecureProtocolSocketFactory delegate,
                                                         final Object scope ) {
        super( delegate, scope );
        this.delegate = delegate;
    }
}
