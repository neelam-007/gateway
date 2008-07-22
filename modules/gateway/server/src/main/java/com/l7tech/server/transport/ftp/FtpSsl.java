package com.l7tech.server.transport.ftp;

import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import org.apache.ftpserver.interfaces.Ssl;

import javax.net.ssl.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ssl implementation for the SSG.
 * <p/>
 * Users must remember to call {@link #setPrivateKey} before attempting to make use of this.
 *
 * @author Steve Jones
 */
public class FtpSsl implements Ssl {

    //- PUBLIC

    public FtpSsl() {
        // create ssl context map - the key is the
        // SSL protocol and the value is SSLContext.
        this.sslContextMap = new ConcurrentHashMap<String, SSLContext>();
    }

    public boolean getClientAuthenticationRequired() {
        return clientAuthentication;
    }

    public SSLContext getSSLContext() throws GeneralSecurityException {
        return getSSLContext(sslProtocol);
    }

    public void setPrivateKey(SsgKeyEntry privateKey) {
        this.privateKey = privateKey;
    }

    /**
     * Get SSL Context.
     */
    public SSLContext getSSLContext(String protocol) throws GeneralSecurityException {
        // null value check
        if(protocol == null) {
            protocol = sslProtocol;
        }

        // if already stored - return it
        SSLContext ctx = sslContextMap.get(protocol);
        if(ctx != null) {
            return ctx;
        }

        if (privateKey == null)
            throw new IllegalStateException("Unable to create SSL context: No private key has been set on this FtpSsl instance");

        // note that extended key manager is required for engine use
        X509ExtendedKeyManager keyManager = new SingleCertX509KeyManager(privateKey.getCertificateChain(),
                                                                         privateKey.getPrivateKey(),
                                                                         "ftpssl-" + privateKey.getKeystoreId() + "-" + privateKey.getAlias());

        // create SSLContext
        ctx = SSLContext.getInstance(protocol);
        ctx.init(new KeyManager[]{keyManager},
                 null,
                 null);

        // store it in map
        sslContextMap.put(protocol, ctx);
        return ctx;
    }

    /**
     * Create secure server socket.
     */
    public ServerSocket createServerSocket(String protocol,
                                           InetAddress addr,
                                           int port) throws Exception {
        // get server socket factory
        SSLContext ctx = getSSLContext(protocol);
        SSLServerSocketFactory ssocketFactory = ctx.getServerSocketFactory();

        // create server socket
        final SSLServerSocket serverSocket;
        if(addr == null) {
            serverSocket = (SSLServerSocket) ssocketFactory.createServerSocket(port, 100);
        }
        else {
            serverSocket = (SSLServerSocket) ssocketFactory.createServerSocket(port, 100, addr);
        }

        // initialize server socket
        serverSocket.setNeedClientAuth(false);
        serverSocket.setWantClientAuth(clientAuthentication);

        return serverSocket;
    }

    /**
     * Returns a socket layered over an existing socket.
     */
    public Socket createSocket(String protocol,
                               Socket soc,
                               boolean clientMode) throws Exception {
        // already wrapped - no need to do anything
        if(soc instanceof SSLSocket) {
            return soc;
        }

        // get socket factory
        SSLContext ctx = getSSLContext(protocol);
        SSLSocketFactory socFactory = ctx.getSocketFactory();

        // create socket
        String host = soc.getInetAddress().getHostAddress();
        int port = soc.getLocalPort();
        SSLSocket ssoc = (SSLSocket)socFactory.createSocket(soc, host, port, true);
        ssoc.setUseClientMode(clientMode);

        // initialize socket
        ssoc.setNeedClientAuth(clientAuthentication);

        return ssoc;
    }

    /**
     * Create a secure socket.
     */
    public Socket createSocket(String protocol,
                               InetAddress addr,
                               int port,
                               boolean clientMode) throws Exception {
        // get socket factory
        SSLContext ctx = getSSLContext(protocol);
        SSLSocketFactory socFactory = ctx.getSocketFactory();

        // create socket
        SSLSocket ssoc = (SSLSocket)socFactory.createSocket(addr, port);
        ssoc.setUseClientMode(clientMode);

        return ssoc;
    }

    /**
     * Create a secure socket.
     */
    public Socket createSocket(String protocol,
                               InetAddress host,
                               int port,
                               InetAddress localhost,
                               int localport,
                               boolean clientMode) throws Exception {
        // get socket factory
        SSLContext ctx = getSSLContext(protocol);
        SSLSocketFactory socFactory = ctx.getSocketFactory();

        // create socket
        SSLSocket ssoc = (SSLSocket)socFactory.createSocket(host, port, localhost, localport);
        ssoc.setUseClientMode(clientMode);

        return ssoc;
    }

    //- PRIVATE

    private final Map<String, SSLContext> sslContextMap;
    private SsgKeyEntry privateKey;
    private String sslProtocol = "TLS";
    private boolean clientAuthentication = false;
}
