package com.l7tech.external.assertions.rawtcp.server;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * A fake SocketFactory that can be used for testing TCP routing.
 */
class StubSocketFactory extends SocketFactory {
    public Socket fakeSocket;

    StubSocketFactory(Socket fakeSocket) {
        this.fakeSocket = fakeSocket;
    }

    @Override
    public Socket createSocket(String s, int i) throws IOException {
        fakeSocket.connect(new InetSocketAddress(s, i));
        return fakeSocket;
    }

    @Override
    public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException {
        throw new UnsupportedOperationException("createSocket(String,int,InetAddress,int) not supported by StubSocketFactory");
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        fakeSocket.connect(new InetSocketAddress(inetAddress, i));
        return fakeSocket;
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
        throw new UnsupportedOperationException("createSocket(InetAddress,int,InetAddress,int) not supported by StubSocketFactory");
    }
}
