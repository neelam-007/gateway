package com.l7tech.external.assertions.rawtcp.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;

/**
* A SocketImpl to use for testing.
*/
class StubSocketImpl extends SocketImpl {
    String sawConnectHost;
    boolean closed = false;
    InputStream inputStream;
    OutputStream outputStream;
    boolean sawShutOut = false;
    boolean sawShutIn = false;

    StubSocketImpl(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    @Override
    protected void create(boolean stream) throws IOException {
    }

    @Override
    protected void connect(String host, int port) throws IOException {
        if (sawConnectHost != null) throw new IOException("Already connected");
        sawConnectHost = host;
    }

    @Override
    protected void connect(InetAddress address, int port) throws IOException {
        if (sawConnectHost != null) throw new IOException("Already connected");
        sawConnectHost = address.toString();
    }

    @Override
    protected void connect(SocketAddress address, int timeout) throws IOException {
        if (sawConnectHost != null) throw new IOException("Already connected");
        sawConnectHost = address.toString();
    }

    @Override
    protected void bind(InetAddress host, int port) throws IOException {
        throw new UnsupportedOperationException("Not supported by test SocketImpl");
    }

    @Override
    protected void listen(int backlog) throws IOException {
        throw new UnsupportedOperationException("Not supported by test SocketImpl");
    }

    @Override
    protected void accept(SocketImpl s) throws IOException {
        throw new UnsupportedOperationException("Not supported by test SocketImpl");
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        return inputStream;
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        return outputStream;
    }

    @Override
    protected int available() throws IOException {
        return 0;
    }

    @Override
    protected void close() throws IOException {
        closed = true;
    }

    @Override
    protected void sendUrgentData(int data) throws IOException {
        throw new UnsupportedOperationException("Not supported by test SocketImpl");
    }

    @Override
    public void setOption(int optID, Object value) throws SocketException {
        if (SocketOptions.SO_TIMEOUT == optID)
            return;
        throw new SocketException("Option not supported by test SocketImpl: " + optID);
    }

    @Override
    public Object getOption(int optID) throws SocketException {
        if (SocketOptions.SO_TIMEOUT == optID)
            return 10;
        throw new SocketException("Option not supported by test SocketImpl: " + optID);
    }

    @Override
    protected void shutdownOutput() throws IOException {
        sawShutOut = true;
    }

    @Override
    protected void shutdownInput() throws IOException {
        sawShutIn = true;
    }
}
