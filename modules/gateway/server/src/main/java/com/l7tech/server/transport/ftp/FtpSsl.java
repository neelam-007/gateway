package com.l7tech.server.transport.ftp;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.TransportModule;
import com.l7tech.server.transport.tls.SsgConnectorSslHelper;
import com.l7tech.util.ExceptionUtils;
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
 * Users must remember to call {@link #setTransportModule(com.l7tech.server.transport.TransportModule)} and
 * {@link #setSsgConnector(com.l7tech.gateway.common.transport.SsgConnector)} before attempting to make use of this.
 *
 * @author Steve Jones
 */
public class FtpSsl implements Ssl {

    //- PUBLIC

    public FtpSsl() {
        // create ssl context map - the key is the
        // SSL protocol and the value is SSLContext.
        this.sslContextMap = new ConcurrentHashMap<String, SsgConnectorSslHelper>();
    }

    public boolean getClientAuthenticationRequired() {
        return clientAuthentication;
    }

    public SSLContext getSSLContext() throws GeneralSecurityException {
        return getSSLContext(null);
    }

    public void setTransportModule(TransportModule transportModule) {
        this.transportModule = transportModule;
    }

    public void setSsgConnector(SsgConnector ssgConnector) {
        this.ssgConnector = ssgConnector;
    }

    /**
     * Get SSL Context.
     */
    public SSLContext getSSLContext(String protocol) throws GeneralSecurityException {
        return getSslHelper(protocol).getSslContext();
    }

    private SsgConnectorSslHelper getSslHelper(String protocol) throws GeneralSecurityException {
        if (transportModule == null)
            throw new IllegalStateException("Unable to create SSL context: No transport module has been set on this FtpSsl instance");
        if (ssgConnector == null)
            throw new IllegalStateException("Unable to create SSL context: No connector has been set on this FtpSsl instance");

        // null value check
        if(protocol == null) {
            protocol = SsgConnectorSslHelper.getTlsProtocol(ssgConnector);
        }

        // if already stored - return it
        SsgConnectorSslHelper helper = sslContextMap.get(protocol);
        if(helper != null) {
            return helper;
        }

        try {
            helper = new SsgConnectorSslHelper(transportModule, ssgConnector);
        } catch (ListenerException e) {
            throw new GeneralSecurityException("Unable to create SSL context: " + ExceptionUtils.getMessage(e), e);
        }

        // TODO update to more recent version of Apache FtpServer that properly distinguishes WANT from NEED clientauth
        clientAuthentication = ssgConnector.getClientAuth() == SsgConnector.CLIENT_AUTH_ALWAYS;

        // store it in map
        sslContextMap.put(protocol, helper);
        return helper;
    }

    /**
     * Create secure server socket.
     */
    public ServerSocket createServerSocket(String protocol,
                                           InetAddress addr,
                                           int port) throws Exception
    {
        SsgConnectorSslHelper sslHelper = getSslHelper(protocol);

        // get server socket factory
        SSLServerSocketFactory ssocketFactory = sslHelper.getSslServerSocketFactory();

        // create server socket
        final SSLServerSocket serverSocket;
        if(addr == null) {
            serverSocket = (SSLServerSocket) ssocketFactory.createServerSocket(port, 100);
        }
        else {
            serverSocket = (SSLServerSocket) ssocketFactory.createServerSocket(port, 100, addr);
        }

        sslHelper.configureServerSocket(serverSocket);

        return serverSocket;
    }

    /**
     * Returns a socket layered over an existing socket.
     */
    public Socket createSocket(String protocol,
                               Socket soc,
                               boolean clientMode) throws Exception
    {
        // already wrapped - no need to do anything
        if(soc instanceof SSLSocket) {
            return soc;
        }

        SsgConnectorSslHelper sslHelper = getSslHelper(protocol);

        // get socket factory
        SSLSocketFactory socFactory = sslHelper.getSocketFactory();

        // create socket
        String host = soc.getInetAddress().getHostAddress();
        int port = soc.getLocalPort();
        SSLSocket ssoc = (SSLSocket)socFactory.createSocket(soc, host, port, true);

        sslHelper.configureSocket(ssoc, clientMode);

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
        SsgConnectorSslHelper sslHelper = getSslHelper(protocol);
        SSLSocketFactory socFactory = sslHelper.getSocketFactory();

        // create socket
        SSLSocket ssoc = (SSLSocket)socFactory.createSocket(addr, port);
        sslHelper.configureSocket(ssoc, clientMode);

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
        SsgConnectorSslHelper sslHelper = getSslHelper(protocol);
        SSLSocketFactory socFactory = sslHelper.getSocketFactory();

        // create socket
        SSLSocket ssoc = (SSLSocket)socFactory.createSocket(host, port, localhost, localport);
        sslHelper.configureSocket(ssoc, clientMode);

        return ssoc;
    }

    //- PRIVATE

    private final Map<String, SsgConnectorSslHelper> sslContextMap;
    private TransportModule transportModule;
    private SsgConnector ssgConnector;
    private boolean clientAuthentication = false;
}
