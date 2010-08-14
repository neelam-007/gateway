package com.l7tech.external.assertions.rawtcp.server;

import com.l7tech.message.TcpKnob;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * An implementation of TcpKnob that gets its relevant information from a passed-in socket.
 */
public class SocketTcpKnob implements TcpKnob {
    private final Socket sock;

    /**
     * Create a TcpKnob that will return answers based on the specified socket.
     *
     * @param sock a Socket.  Should be a socket that uses an InetSocketAddress.  (If not, all methods will return null/0).
     */
    public SocketTcpKnob(Socket sock) {
        this.sock = sock;
    }

    @Override
    public String getRemoteAddress() {
        SocketAddress sa = sock.getRemoteSocketAddress();
        if (sa instanceof InetSocketAddress) {
            InetSocketAddress isa = (InetSocketAddress) sa;
            return isa.getAddress().getHostAddress();
        }
        return null;
    }

    @Override
    public String getRemoteHost() {
        SocketAddress sa = sock.getRemoteSocketAddress();
        if (sa instanceof InetSocketAddress) {
            InetSocketAddress isa = (InetSocketAddress) sa;
            return isa.getAddress().getHostName();
        }
        return null;
    }

    @Override
    public int getRemotePort() {
        SocketAddress sa = sock.getRemoteSocketAddress();
        if (sa instanceof InetSocketAddress) {
            InetSocketAddress isa = (InetSocketAddress) sa;
            return isa.getPort();
        }
        return 0;
    }

    @Override
    public String getLocalAddress() {
        SocketAddress sa = sock.getLocalSocketAddress();
        if (sa instanceof InetSocketAddress) {
            InetSocketAddress isa = (InetSocketAddress) sa;
            return isa.getAddress().getHostAddress();
        }
        return null;
    }

    @Override
    public String getLocalHost() {
        SocketAddress sa = sock.getLocalSocketAddress();
        if (sa instanceof InetSocketAddress) {
            InetSocketAddress isa = (InetSocketAddress) sa;
            return isa.getAddress().getHostName();
        }
        return null;
    }

    @Override
    public int getLocalPort() {
        SocketAddress sa = sock.getLocalSocketAddress();
        if (sa instanceof InetSocketAddress) {
            InetSocketAddress isa = (InetSocketAddress) sa;
            return isa.getPort();
        }
        return 0;
    }
}
