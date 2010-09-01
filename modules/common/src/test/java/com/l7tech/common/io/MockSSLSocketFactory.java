package com.l7tech.common.io;

import com.l7tech.util.Functions;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;

/**
 *
 */
public class MockSSLSocketFactory extends SSLSocketFactory {

    private final Functions.Nullary<Socket> factory;

    public MockSSLSocketFactory( final Functions.Nullary<Socket> factory ) {
        this.factory = factory;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return new String[0];
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return new String[0];
    }

    @Override
    public Socket createSocket() throws IOException {
        return factory.call();
    }

    @Override
    public Socket createSocket( final String s, final int i, final InetAddress inetAddress, final int i1 ) throws IOException, UnknownHostException {
        return factory.call();
    }

    @Override
    public Socket createSocket( final String s, final int i ) throws IOException, UnknownHostException {
        return factory.call();
    }

    @Override
    public Socket createSocket( final InetAddress inetAddress, final int i, final InetAddress inetAddress1, final int i1 ) throws IOException {
        return factory.call();
    }

    @Override
    public Socket createSocket( final InetAddress inetAddress, final int i ) throws IOException {
        return factory.call();
    }

    @Override
    public Socket createSocket( final Socket socket, final String s, final int i, final boolean b ) throws IOException {
        return factory.call();
    }

    /**
     * A poorly mocked SSL socket with predefined input stream content.
     */
    public static final class MockSSLSocket extends SSLSocket {
        private final byte[] data;

        public MockSSLSocket( final byte[] data ) {
            this.data = data;
        }        

        @Override
        public void addHandshakeCompletedListener( final HandshakeCompletedListener handshakeCompletedListener ) {
        }

        @Override
        public String[] getEnabledCipherSuites() {
            return new String[0];
        }

        @Override
        public String[] getEnabledProtocols() {
            return new String[0];
        }

        @Override
        public boolean getEnableSessionCreation() {
            return false;
        }

        @Override
        public boolean getNeedClientAuth() {
            return false;
        }

        @Override
        public SSLSession getSession() {
            return null;
        }

        @Override
        public SSLParameters getSSLParameters() {
            return null;
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return new String[0];
        }

        @Override
        public String[] getSupportedProtocols() {
            return new String[0];
        }

        @Override
        public boolean getUseClientMode() {
            return false;
        }

        @Override
        public boolean getWantClientAuth() {
            return false;
        }

        @Override
        public void removeHandshakeCompletedListener( final HandshakeCompletedListener handshakeCompletedListener ) {
        }

        @Override
        public void setEnabledCipherSuites( final String[] strings ) {
        }

        @Override
        public void setEnabledProtocols( final String[] strings ) {
        }

        @Override
        public void setEnableSessionCreation( final boolean b ) {
        }

        @Override
        public void setNeedClientAuth( final boolean b ) {
        }

        @Override
        public void setSSLParameters( final SSLParameters sslParameters ) {
        }

        @Override
        public void setUseClientMode( final boolean b ) {
        }

        @Override
        public void setWantClientAuth( final boolean b ) {
        }

        @Override
        public void startHandshake() throws IOException {
        }

        @Override
        public void bind( final SocketAddress bindpoint ) throws IOException {
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public void connect( final SocketAddress endpoint ) throws IOException {
        }

        @Override
        public void connect( final SocketAddress endpoint, final int timeout ) throws IOException {
        }

        @Override
        public SocketChannel getChannel() {
            return null;
        }

        @Override
        public InetAddress getInetAddress() {
            return null;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        @Override
        public boolean getKeepAlive() throws SocketException {
            return false;
        }

        @Override
        public InetAddress getLocalAddress() {
            return null;
        }

        @Override
        public int getLocalPort() {
            return 1;
        }

        @Override
        public SocketAddress getLocalSocketAddress() {
            return null;
        }

        @Override
        public boolean getOOBInline() throws SocketException {
            return false;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return new NullOutputStream();
        }

        @Override
        public int getPort() {
            return 1;
        }

        @Override
        public int getReceiveBufferSize() throws SocketException {
            return 1024;
        }

        @Override
        public SocketAddress getRemoteSocketAddress() {
            return null;
        }

        @Override
        public boolean getReuseAddress() throws SocketException {
            return false;
        }

        @Override
        public int getSendBufferSize() throws SocketException {
            return 1024;
        }

        @Override
        public int getSoLinger() throws SocketException {
            return -1;
        }

        @Override
        public int getSoTimeout() throws SocketException {
            return 0;
        }

        @Override
        public boolean getTcpNoDelay() throws SocketException {
            return true;
        }

        @Override
        public int getTrafficClass() throws SocketException {
            return 0;
        }

        @Override
        public boolean isBound() {
            return true;
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public boolean isInputShutdown() {
            return false;
        }

        @Override
        public boolean isOutputShutdown() {
            return false;
        }

        @Override
        public void sendUrgentData( final int data ) throws IOException {
        }

        @Override
        public void setKeepAlive( final boolean on ) throws SocketException {
        }

        @Override
        public void setOOBInline( final boolean on ) throws SocketException {
        }

        @Override
        public void setPerformancePreferences( final int connectionTime, final int latency, final int bandwidth ) {
        }

        @Override
        public void setReceiveBufferSize( final int size ) throws SocketException {
        }

        @Override
        public void setReuseAddress( final boolean on ) throws SocketException {
        }

        @Override
        public void setSendBufferSize( final int size ) throws SocketException {
        }

        @Override
        public void setSoLinger( final boolean on, final int linger ) throws SocketException {
        }

        @Override
        public void setSoTimeout( final int timeout ) throws SocketException {
        }

        @Override
        public void setTcpNoDelay( final boolean on ) throws SocketException {
        }

        @Override
        public void setTrafficClass( final int tc ) throws SocketException {
        }

        @Override
        public void shutdownInput() throws IOException {
        }

        @Override
        public void shutdownOutput() throws IOException {
        }
    }
}
