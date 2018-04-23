package com.l7tech.server.transport.email;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * This is a hack. For StartTLS, the initial connection must be unencrypted, but after the STARTTLS command
 * is sent, the socket must be recreated as an SSL socket. Unfortunately both cases must be covered using
 * the same SocketFactory.
 * Note: It is used by Email Notifier, Email Listener and Email Assertion modules.
 */
public abstract class StartTlsSocketFactory extends SSLSocketFactory {

    /**
     * Provide a known SSL Socket Factory to work on.
     * @return
     */
    abstract protected SSLSocketFactory getSslFactory();

    @Override
    public String[] getDefaultCipherSuites() {
        return getSslFactory().getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return getSslFactory().getSupportedCipherSuites();
    }

    /**
     * Wrap existing socket with SSL
     */
    @Override
    public Socket createSocket(final Socket socket, final String string, final int i, final boolean b) throws IOException {
        return getSslFactory().createSocket(socket, string, i, b);
    }

    @Override
    public Socket createSocket() throws IOException {
        return new Socket();
    }

    @Override
    public Socket createSocket(final String string, final int i) throws IOException {
        return new Socket(string, i);
    }

    @Override
    public Socket createSocket(final String string, final int i, final InetAddress inetAddress, final int i1) throws IOException {
        return new Socket(string, i, inetAddress, i1);
    }

    @Override
    public Socket createSocket(final InetAddress inetAddress, final int i) throws IOException {
        return new Socket(inetAddress, i);
    }

    @Override
    public Socket createSocket(final InetAddress inetAddress, final int i, final InetAddress inetAddress1, final int i1) throws IOException {
        return new Socket(inetAddress, i, inetAddress1, i1);
    }

    /**
     * New method in JDK8: Creates a server mode Socket layered over an existing connected socket, and is able to read
     * data which has already been consumed/removed from the Socket's underlying InputStream.
     * @param s
     * @param consumed
     * @param autoClose
     * @return  ssl socket
     * @throws IOException
     */
    @Override
    public Socket createSocket(final Socket s, final InputStream consumed, final boolean autoClose) throws IOException {
        return getSslFactory().createSocket(s, consumed, autoClose);
    }
}
