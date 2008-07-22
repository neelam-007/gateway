package com.l7tech.gateway.common.spring.remoting.rmi.ssl;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import javax.net.ssl.SSLServerSocket;

/**
 * Delegating wrapper for SSLServerSockets.
 *
 * @author $Author$
 * @version $Revision$
 */
public class SSLServerSocketWrapper extends SSLServerSocket {

    public SSLServerSocketWrapper(SSLServerSocket wrapped) throws IOException {
        delegate = wrapped;
    }

    public String[] getEnabledCipherSuites() {
        return delegate.getEnabledCipherSuites();
    }

    public void setEnabledCipherSuites(String[] strings) {
        delegate.setEnabledCipherSuites(strings);
    }

    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    public String[] getSupportedProtocols() {
        return delegate.getSupportedProtocols();
    }

    public String[] getEnabledProtocols() {
        return delegate.getEnabledProtocols();
    }

    public void setEnabledProtocols(String[] strings) {
        delegate.setEnabledProtocols(strings);
    }

    public void setNeedClientAuth(boolean b) {
        delegate.setNeedClientAuth(b);
    }

    public boolean getNeedClientAuth() {
        return delegate.getNeedClientAuth();
    }

    public void setWantClientAuth(boolean b) {
        delegate.setWantClientAuth(b);
    }

    public boolean getWantClientAuth() {
        return delegate.getWantClientAuth();
    }

    public void setUseClientMode(boolean b) {
        delegate.setUseClientMode(b);
    }

    public boolean getUseClientMode() {
        return delegate.getUseClientMode();
    }

    public void setEnableSessionCreation(boolean b) {
        delegate.setEnableSessionCreation(b);
    }

    public boolean getEnableSessionCreation() {
        return delegate.getEnableSessionCreation();
    }

    public void bind(SocketAddress endpoint) throws IOException {
        delegate.bind(endpoint);
    }

    public void bind(SocketAddress endpoint, int backlog) throws IOException {
        delegate.bind(endpoint, backlog);
    }

    public InetAddress getInetAddress() {
        return delegate.getInetAddress();
    }

    public int getLocalPort() {
        return delegate.getLocalPort();
    }

    public SocketAddress getLocalSocketAddress() {
        return delegate.getLocalSocketAddress();
    }

    public Socket accept() throws IOException {
        return delegate.accept();
    }

    public void close() throws IOException {
        delegate.close();
    }

    public ServerSocketChannel getChannel() {
        return delegate.getChannel();
    }

    public boolean isBound() {
        return delegate.isBound();
    }

    public boolean isClosed() {
        return delegate.isClosed();
    }

    public synchronized void setSoTimeout(int timeout) throws SocketException {
        delegate.setSoTimeout(timeout);
    }

    public synchronized int getSoTimeout() throws IOException {
        return delegate.getSoTimeout();
    }

    public void setReuseAddress(boolean on) throws SocketException {
        delegate.setReuseAddress(on);
    }

    public boolean getReuseAddress() throws SocketException {
        return delegate.getReuseAddress();
    }

    public String toString() {
        return delegate.toString();
    }

    public synchronized void setReceiveBufferSize(int size) throws SocketException {
        delegate.setReceiveBufferSize(size);
    }

    public synchronized int getReceiveBufferSize() throws SocketException {
        return delegate.getReceiveBufferSize();
    }

    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        delegate.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    //- PRIVATE

    private final SSLServerSocket delegate;
}
