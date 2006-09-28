package com.l7tech.common.util;

import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.InetSocketAddress;
import java.io.IOException;
import javax.net.SocketFactory;

/**
 * SocketFactory that supports connection timeout.
 *
 * @author Steve Jones
 */
public class ConnectionTimeoutSocketFactory extends SocketFactory {

    //- PUBLIC

    public ConnectionTimeoutSocketFactory(SocketFactory delegate, Number timeout) {
        this.delegate = delegate;
        this.timeout = timeout;
    }

    /**
     * Note this does not connect the socket so we can't control timeouts here.
     */
    public Socket createSocket() throws IOException {
        return delegate.createSocket();
    }

    public Socket createSocket(InetAddress address, int port) throws IOException {
        Socket socket = createSocket();
        socket.connect(new InetSocketAddress(address, port), timeout.intValue());
        return socket;
    }

    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        Socket socket = createSocket();
        socket.bind(new InetSocketAddress(localAddress, localPort));
        socket.connect(new InetSocketAddress(address, port), timeout.intValue());
        return socket;
    }

    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        Socket socket = createSocket();
        socket.connect(new InetSocketAddress(host, port), timeout.intValue());
        return socket;
    }

    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException, UnknownHostException {
        Socket socket = createSocket();
        socket.bind(new InetSocketAddress(localAddress, localPort));
        socket.connect(new InetSocketAddress(host, port), timeout.intValue());
        return socket;
    }

    //- PRIVATE

    private final Number timeout;
    private final SocketFactory delegate;
}
