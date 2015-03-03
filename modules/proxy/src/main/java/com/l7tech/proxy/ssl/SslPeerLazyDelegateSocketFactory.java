package com.l7tech.proxy.ssl;

import java.io.InputStream;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import javax.net.ssl.SSLSocketFactory;

/**
 * SSLSocket factory that delegates via an SSLContext.
 *
 * <p>The delegate is lazily initialized but can be initialized on request via
 * the initialize() method. This allows for use from other threads that do not
 * have a CurrentSslPeer.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class SslPeerLazyDelegateSocketFactory extends SSLSocketFactory {

    //- PUBLIC

    public SslPeerLazyDelegateSocketFactory(SslContextHaver contextSource) {
        this.sslContextSource = contextSource;
        this.delegate = null;
    }

    /**
     * Initialize the delegate SSLSocketFactory if necessary.
     */
    public void initialize() {
        checkInit();
    }

    public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
        checkInit();
        return delegate.createSocket(socket, s, i, b);
    }

    public String[] getDefaultCipherSuites() {
        checkInit();
        return delegate.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        checkInit();
        return delegate.getSupportedCipherSuites();
    }

    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        checkInit();
        return delegate.createSocket(inetAddress, i);
    }

    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
        checkInit();
        return delegate.createSocket(inetAddress, i, inetAddress1, i1);
    }

    public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
        checkInit();
        return delegate.createSocket(s, i);
    }

    public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException, UnknownHostException {
        checkInit();
        return delegate.createSocket(s, i, inetAddress, i1);
    }

    public Socket createSocket() throws IOException {
        checkInit();
        return delegate.createSocket();
    }

    /**
     * New method in JDK8: Creates a server mode Socket layered over an existing connected socket, and is able to read
     * data which has already been consumed/removed from the Socket's underlying InputStream.
     * @param s Socket
     * @param consumed InputStream
     * @param autoClose boolean
     * @return  ssl socket
     * @throws IOException
     */
    @Override
    public Socket createSocket(Socket s, InputStream consumed, boolean autoClose) throws IOException {
        checkInit();
        return delegate.createSocket(s, consumed, autoClose);
    }

    //- PRIVATE

    private final SslContextHaver sslContextSource;
    private SSLSocketFactory delegate;

    private void checkInit() {
        if (delegate == null) {
            delegate = sslContextSource.getSslContext().getSocketFactory();
        }
    }
}
