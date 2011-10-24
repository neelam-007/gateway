package com.l7tech.common.http.prov.apache;

import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 *
 */
class MockProtocolSocketFactory implements ProtocolSocketFactory {

    @Override
    public Socket createSocket( final String host, final int port, final InetAddress localAddress, final int localPort ) throws IOException {
        return onCreateSocket( host, port );
    }

    @Override
    public Socket createSocket( final String host, final int port, final InetAddress localAddress, final int localPort, final HttpConnectionParams params ) throws IOException {
        return onCreateSocket( host, port );
    }

    @Override
    public Socket createSocket( final String host, final int port ) throws IOException {
        return onCreateSocket( host, port );
    }

    protected Socket onCreateSocket(  final String host, final int port ) throws IOException {
        return null;
    }
}
