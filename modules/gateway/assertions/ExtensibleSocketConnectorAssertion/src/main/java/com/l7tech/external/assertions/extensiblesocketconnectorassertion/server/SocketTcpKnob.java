package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import com.l7tech.message.TcpKnob;

import java.net.InetSocketAddress;

/**
 * An implementation of TcpKnob that gets its relevant information from a passed-in socket.
 */
public class SocketTcpKnob implements TcpKnob {
    private final InetSocketAddress localAddress;
    private final InetSocketAddress remoteAddress;

    public SocketTcpKnob(InetSocketAddress localAddress, InetSocketAddress remoteAddress) {
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public String getRemoteAddress() {
        return remoteAddress.getAddress().getHostAddress();
    }

    @Override
    public String getRemoteHost() {
        return remoteAddress.getAddress().getHostName();
    }

    @Override
    public int getRemotePort() {
        return remoteAddress.getPort();
    }

    @Override
    public String getLocalAddress() {
        return localAddress.getAddress().getHostAddress();
    }

    @Override
    public String getLocalHost() {
        return localAddress.getAddress().getHostName();
    }

    @Override
    public int getLocalPort() {
        return localAddress.getPort();
    }

    @Override
    public int getLocalListenerPort() {
        return localAddress.getPort();
    }
}
