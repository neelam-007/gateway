package com.l7tech.common.io;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

/**
 * Delegating wrapper for SSLSockets.
 *
 * @author $Author$
 * @version $Revision$
 */
public class SSLSocketWrapper extends SSLSocket {

    //- PUBLIC

    public SSLSocketWrapper(SSLSocket wrapped) {
        delegate = wrapped;
    }

    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    public String[] getEnabledCipherSuites() {
        return delegate.getEnabledCipherSuites();
    }

    public void setEnabledCipherSuites(String[] strings) {
        delegate.setEnabledCipherSuites(strings);
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

    public SSLSession getSession() {
        return delegate.getSession();
    }

    public void addHandshakeCompletedListener(HandshakeCompletedListener handshakeCompletedListener) {
        delegate.addHandshakeCompletedListener(handshakeCompletedListener);
    }

    public void removeHandshakeCompletedListener(HandshakeCompletedListener handshakeCompletedListener) {
        delegate.removeHandshakeCompletedListener(handshakeCompletedListener);
    }

    public void startHandshake() throws IOException {
        delegate.startHandshake();
    }

    public void setUseClientMode(boolean b) {
        delegate.setUseClientMode(b);
    }

    public boolean getUseClientMode() {
        return delegate.getUseClientMode();
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

    public void setEnableSessionCreation(boolean b) {
        delegate.setEnableSessionCreation(b);
    }

    public boolean getEnableSessionCreation() {
        return delegate.getEnableSessionCreation();
    }

    public void connect(SocketAddress endpoint) throws IOException {
        delegate.connect(endpoint);
    }

    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        delegate.connect(endpoint, timeout);
    }

    public void bind(SocketAddress bindpoint) throws IOException {
        delegate.bind(bindpoint);
    }

    public InetAddress getInetAddress() {
        return delegate.getInetAddress();
    }

    public InetAddress getLocalAddress() {
        return delegate.getLocalAddress();
    }

    public int getPort() {
        return delegate.getPort();
    }

    public int getLocalPort() {
        return delegate.getLocalPort();
    }

    public SocketAddress getRemoteSocketAddress() {
        return delegate.getRemoteSocketAddress();
    }

    public SocketAddress getLocalSocketAddress() {
        return delegate.getLocalSocketAddress();
    }

    public SocketChannel getChannel() {
        return delegate.getChannel();
    }

    public InputStream getInputStream() throws IOException {
        return delegate.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return delegate.getOutputStream();
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        delegate.setTcpNoDelay(on);
    }

    public boolean getTcpNoDelay() throws SocketException {
        return delegate.getTcpNoDelay();
    }

    public void setSoLinger(boolean on, int linger) throws SocketException {
        delegate.setSoLinger(on, linger);
    }

    public int getSoLinger() throws SocketException {
        return delegate.getSoLinger();
    }

    public void sendUrgentData(int data) throws IOException {
        delegate.sendUrgentData(data);
    }

    public void setOOBInline(boolean on) throws SocketException {
        delegate.setOOBInline(on);
    }

    public boolean getOOBInline() throws SocketException {
        return delegate.getOOBInline();
    }

    public synchronized void setSoTimeout(int timeout) throws SocketException {
        delegate.setSoTimeout(timeout);
    }

    public synchronized int getSoTimeout() throws SocketException {
        return delegate.getSoTimeout();
    }

    public synchronized void setSendBufferSize(int size) throws SocketException {
        delegate.setSendBufferSize(size);
    }

    public synchronized int getSendBufferSize() throws SocketException {
        return delegate.getSendBufferSize();
    }

    public synchronized void setReceiveBufferSize(int size) throws SocketException {
        delegate.setReceiveBufferSize(size);
    }

    public synchronized int getReceiveBufferSize() throws SocketException {
        return delegate.getReceiveBufferSize();
    }

    public void setKeepAlive(boolean on) throws SocketException {
        delegate.setKeepAlive(on);
    }

    public boolean getKeepAlive() throws SocketException {
        return delegate.getKeepAlive();
    }

    public void setTrafficClass(int tc) throws SocketException {
        delegate.setTrafficClass(tc);
    }

    public int getTrafficClass() throws SocketException {
        return delegate.getTrafficClass();
    }

    public void setReuseAddress(boolean on) throws SocketException {
        delegate.setReuseAddress(on);
    }

    public boolean getReuseAddress() throws SocketException {
        return delegate.getReuseAddress();
    }

    public synchronized void close() throws IOException {
        delegate.close();
    }

    public void shutdownInput() throws IOException {
        delegate.shutdownInput();
    }

    public void shutdownOutput() throws IOException {
        delegate.shutdownOutput();
    }

    public String toString() {
        return delegate.toString();
    }

    public boolean isConnected() {
        return delegate.isConnected();
    }

    public boolean isBound() {
        return delegate.isBound();
    }

    public boolean isClosed() {
        return delegate.isClosed();
    }

    public boolean isInputShutdown() {
        return delegate.isInputShutdown();
    }

    public boolean isOutputShutdown() {
        return delegate.isOutputShutdown();
    }

    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        delegate.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    //- PRIVATE

    private final SSLSocket delegate;
}
