package com.l7tech.common.io;

import com.l7tech.util.Functions;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Wrapper for an SSL Socket Factory.
 */
public class SSLSocketFactoryWrapper extends SSLSocketFactory {

    //- PUBLIC

    public SSLSocketFactoryWrapper( final SSLSocketFactory delegate ) {
        this (new Functions.Nullary<SSLSocketFactory>(){
            @Override
            public SSLSocketFactory call() {
                return delegate;
            }
        });
    }

    public SSLSocketFactoryWrapper( final Functions.Nullary<SSLSocketFactory> delegate ) {
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

    private final Functions.Nullary<SSLSocketFactory> delegate;

}
