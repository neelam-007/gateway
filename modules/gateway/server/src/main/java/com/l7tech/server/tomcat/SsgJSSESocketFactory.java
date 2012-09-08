package com.l7tech.server.tomcat;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.server.transport.tls.SsgConnectorSslHelper;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import org.apache.tomcat.util.net.ServerSocketFactory;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import static com.l7tech.server.tomcat.SsgServerSocketFactory.wrapSocket;

/**
 * Gateway's TLS socket factory for Tomcat, which knows how to obtain key, cert and socket information with the rest of the SSG.
 */
public class SsgJSSESocketFactory extends org.apache.tomcat.util.net.ServerSocketFactory {

    //
    // Public
    //

    public SsgJSSESocketFactory() {
    }

    @Override
    public ServerSocket createSocket(int port) throws IOException, InstantiationException {
        return strategy.createSocket( port );
    }

    @Override
    public ServerSocket createSocket(int port, int backlog) throws IOException, InstantiationException {
        return strategy.createSocket( port, backlog );
    }

    @Override
    public ServerSocket createSocket(int port, int backlog, InetAddress ifAddress) throws IOException, InstantiationException {
        return strategy.createSocket( port, backlog, ifAddress );
    }

    @Override
    public void handshake(Socket sock) throws IOException {
        strategy.handshake( sock );
    }

    @Override
    public Socket acceptSocket(ServerSocket socket) throws IOException {
        try {
            return strategy.acceptSocket( socket );
        } catch (SSLException e){
            SocketException se = new SocketException("SSL handshake error: " + ExceptionUtils.getMessage(e));
            se.initCause(e);
            throw se;
        }
    }

    //
    // Private
    //

    private final SocketStrategy strategy = ConfigFactory.getBooleanProperty( "com.l7tech.server.tomcat.enableHttpsTrace", true ) ?
            new HttpsTraceSupportSocketStrategy( attributes ) :
            new DirectSocketStrategy( attributes );


    /**
     * Strategy to use for socket creation
     */
    private static abstract class SocketStrategy {
        private final Hashtable attributes;
        protected final ServerSocketFactory socketFactory = ServerSocketFactory.getDefault();
        protected SsgConnectorSslHelper sslHelper = null;
        protected long transportModuleId = -1L;
        protected long connectorOid = -1L;

        protected SocketStrategy( final Hashtable attributes ) {
            this.attributes = attributes;
        }

        protected abstract ServerSocket createSocket(int port) throws IOException, InstantiationException;
        protected abstract ServerSocket createSocket(int port, int backlog) throws IOException, InstantiationException;
        protected abstract ServerSocket createSocket(int port, int backlog, InetAddress ifAddress) throws IOException, InstantiationException;
        protected abstract Socket acceptSocket(ServerSocket socket) throws IOException;
        protected void handshake(Socket sock) throws IOException {
            if (sslHelper == null) initialize();
            sslHelper.startHandshake((SSLSocket) sock);
        }

        protected final synchronized void initialize() throws IOException {
            if (sslHelper != null)
                return;
            HttpTransportModule httpTransportModule = null;
            try {
                transportModuleId = getRequiredLongAttr(HttpTransportModule.CONNECTOR_ATTR_TRANSPORT_MODULE_ID);
                connectorOid = getRequiredLongAttr(HttpTransportModule.CONNECTOR_ATTR_CONNECTOR_OID);
                httpTransportModule = HttpTransportModule.getInstance(transportModuleId);
                if (httpTransportModule == null)
                    throw new IllegalStateException("No HttpTransportModule with ID " + transportModuleId + " was found");
                SsgConnector ssgConnector = httpTransportModule.getActiveConnectorByOid(connectorOid);
                sslHelper = new SsgConnectorSslHelper(httpTransportModule, ssgConnector);
            } catch (Exception e) {
                if (httpTransportModule != null)
                    httpTransportModule.reportMisconfiguredConnector(connectorOid);
                delay(TimeUnit.SECONDS.toMillis(30));
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

    private static void delay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Direct socket strategy creates an SSLSocket and does not support tracing of HTTPS
     */
    private static class DirectSocketStrategy extends SocketStrategy {
        protected DirectSocketStrategy( final Hashtable attributes ) {
            super( attributes );
        }

        @Override
        protected ServerSocket createSocket( final int port ) throws IOException, InstantiationException {
            if (sslHelper == null) initialize();
            final ServerSocket socket = sslHelper.getSslContext().getServerSocketFactory().createServerSocket(port);
            sslHelper.configureServerSocket((SSLServerSocket) socket);
            return socket;
        }

        @Override
        protected ServerSocket createSocket( final int port, final int backlog ) throws IOException, InstantiationException {
            if (sslHelper == null) initialize();
            final ServerSocket socket = sslHelper.getSslContext().getServerSocketFactory().createServerSocket(port, backlog);
            sslHelper.configureServerSocket((SSLServerSocket) socket);
            return socket;
        }

        @Override
        protected ServerSocket createSocket( final int port, final int backlog, final InetAddress ifAddress ) throws IOException, InstantiationException {
            if (sslHelper == null) initialize();
            final ServerSocket socket = sslHelper.getSslContext().getServerSocketFactory().createServerSocket(port, backlog, ifAddress);
            sslHelper.configureServerSocket((SSLServerSocket) socket);
            return socket;
        }

        @Override
        protected Socket acceptSocket( final ServerSocket socket ) throws IOException {
            SSLSocket asock = (SSLSocket) socket.accept();
            return SsgServerSocketFactory.wrapSocket(transportModuleId, connectorOid, asock);
        }
    }

    /**
     * HTTPS trace support strategy creates a plain socket for listening and
     * wraps with an SSL socket on accept.
     */
    private static class HttpsTraceSupportSocketStrategy extends SocketStrategy {
        protected HttpsTraceSupportSocketStrategy( final Hashtable attributes ) {
            super( attributes );
        }

        @Override
        protected ServerSocket createSocket( final int port ) throws IOException, InstantiationException {
            return socketFactory.createSocket( port );
        }

        @Override
        protected ServerSocket createSocket( final int port, final int backlog ) throws IOException, InstantiationException {
            return  socketFactory.createSocket( port, backlog );
        }

        @Override
        protected ServerSocket createSocket( final int port, final int backlog, final InetAddress ifAddress ) throws IOException, InstantiationException {
            return socketFactory.createSocket( port, backlog, ifAddress );
        }

        @Override
        protected Socket acceptSocket( final ServerSocket socket ) throws IOException {
            if (sslHelper == null) initialize();
            return wrapSocket( transportModuleId, connectorOid, sslHelper.wrapAndConfigureSocketForSsl( wrapSocket( socket.accept() ), false ) );
        }
    }
}
