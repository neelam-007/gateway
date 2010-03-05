package com.l7tech.server.tomcat;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.server.transport.tls.SsgConnectorSslHelper;
import com.l7tech.util.ExceptionUtils;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Logger;

/**
 * Gateway's TLS socket factory for Tomcat, which knows how to obtain key, cert and socket information with the rest of the SSG.
 */
public class SsgJSSESocketFactory extends org.apache.tomcat.util.net.ServerSocketFactory {
    protected static final Logger logger = Logger.getLogger(SsgJSSESocketFactory.class.getName());

    private SsgConnectorSslHelper sslHelper = null;
    private long transportModuleId = -1;
    private long connectorOid = -1;

    //
    // Public
    //

    public SsgJSSESocketFactory() {
    }

    public ServerSocket createSocket(int port) throws IOException {
        if (sslHelper == null) initialize();
        ServerSocket socket = sslHelper.getSslServerSocketFactory().createServerSocket(port);
        sslHelper.configureServerSocket((SSLServerSocket) socket);
        return socket;
    }

    public ServerSocket createSocket(int port, int backlog) throws IOException {
        if (sslHelper == null) initialize();
        ServerSocket socket = sslHelper.getSslServerSocketFactory().createServerSocket(port, backlog);
        sslHelper.configureServerSocket((SSLServerSocket) socket);
        return socket;
    }

    public ServerSocket createSocket(int port, int backlog, InetAddress ifAddress) throws IOException {
        if (sslHelper == null) initialize();
        ServerSocket socket = sslHelper.getSslServerSocketFactory().createServerSocket(port, backlog, ifAddress);
        sslHelper.configureServerSocket((SSLServerSocket) socket);
        return socket;
    }

    public void handshake(Socket sock) throws IOException {
        if (sslHelper == null) initialize();
        sslHelper.startHandshake((SSLSocket) sock);
    }

    public Socket acceptSocket(ServerSocket socket) throws IOException {
        try {
            SSLSocket asock = (SSLSocket) socket.accept();
            return SsgServerSocketFactory.wrapSocket(transportModuleId, connectorOid, asock);
        } catch (SSLException e){
            SocketException se = new SocketException("SSL handshake error: " + ExceptionUtils.getMessage(e));
            se.initCause(e);
            throw se;
        }
    }

    //
    // Private
    //

    private synchronized void initialize() throws IOException {
        if (sslHelper != null)
            return;
        try {
            transportModuleId = getRequiredLongAttr(HttpTransportModule.CONNECTOR_ATTR_TRANSPORT_MODULE_ID);
            connectorOid = getRequiredLongAttr(HttpTransportModule.CONNECTOR_ATTR_CONNECTOR_OID);
            HttpTransportModule httpTransportModule = HttpTransportModule.getInstance(transportModuleId);
            if (httpTransportModule == null)
                throw new IllegalStateException("No HttpTransportModule with ID " + transportModuleId + " was found");
            SsgConnector ssgConnector = httpTransportModule.getActiveConnectorByOid(connectorOid);
            sslHelper = new SsgConnectorSslHelper(httpTransportModule, ssgConnector);
        } catch (Exception e) {
            throw new IOException("Unable to initialize TLS socket factory: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private String getRequiredStringAttr(String attrName) {
        String value = (String)attributes.get(attrName);
        if (value == null)
            throw new IllegalStateException("Required attribute \"" + attrName + "\" was not provided");
        return value;
    }

    private long getRequiredLongAttr(String attrName) {
        return Long.parseLong(getRequiredStringAttr(attrName));
    }
}
