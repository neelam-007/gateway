package com.l7tech.server.transport.ftp;

import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.X509ExtendedKeyManager;

import org.apache.ftpserver.interfaces.Ssl;

import com.l7tech.common.security.SingleCertX509KeyManager;
import com.l7tech.server.KeystoreUtils;

/**
 * Ssl implementation for the SSG.
 *
 * @author Steve Jones
 */
public class FtpSsl implements Ssl {

    //- PUBLIC

    public FtpSsl() {
        // create ssl context map - the key is the
        // SSL protocol and the value is SSLContext.
        this.sslContextMap = new ConcurrentHashMap();
    }

    public void setKeystoreUtils(final KeystoreUtils keystoreUtils) {
        this.keystoreUtils = keystoreUtils;
    }

    public boolean getClientAuthenticationRequired() {
        return clientAuthentication;
    }

    public SSLContext getSSLContext() throws GeneralSecurityException {
        return getSSLContext(sslProtocol);
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
        SSLContext ctx = (SSLContext)sslContextMap.get(protocol);
        if(ctx != null) {
            return ctx;
        }

        // note that extended key manager is required for engine use
        X509ExtendedKeyManager keyManager = new SingleCertX509KeyManager(
                keystoreUtils.getSSLCertChain(),
                keystoreUtils.getSSLPrivateKey(),
                "ftpssl");
        
        // create SSLContext
        ctx = SSLContext.getInstance(protocol);
        ctx.init(new KeyManager[]{keyManager},
                 TrustManagerFactory.getInstance("AXPK").getTrustManagers(),
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
        SSLServerSocket serverSocket = null;
        if(addr == null) {
            serverSocket = (SSLServerSocket) ssocketFactory.createServerSocket(port, 100);
        }
        else {
            serverSocket = (SSLServerSocket) ssocketFactory.createServerSocket(port, 100, addr);
        }

        // initialize server socket
        String cipherSuites[] = serverSocket.getSupportedCipherSuites();
        serverSocket.setEnabledCipherSuites(cipherSuites);
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
        String cipherSuites[] = ssoc.getSupportedCipherSuites();
        ssoc.setEnabledCipherSuites(cipherSuites);
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

        // initialize socket
        String cipherSuites[] = ssoc.getSupportedCipherSuites();
        ssoc.setEnabledCipherSuites(cipherSuites);

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

        // initialize socket
        String cipherSuites[] = ssoc.getSupportedCipherSuites();
        ssoc.setEnabledCipherSuites(cipherSuites);
        
        return ssoc;
    }

    //- PRIVATE

    private final Map sslContextMap;
    private KeystoreUtils keystoreUtils;
    private String sslProtocol = "TLS";
    private boolean clientAuthentication = false;
}
