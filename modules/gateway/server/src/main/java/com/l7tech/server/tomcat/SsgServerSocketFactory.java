package com.l7tech.server.tomcat;

import com.l7tech.common.io.SSLSocketWrapper;
import com.l7tech.common.io.SocketWrapper;
import com.l7tech.common.io.SocketWrapper.TraceSupport;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.ResourceUtils;
import org.apache.tomcat.util.net.ServerSocketFactory;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * ServerSocketFactory wrapper that supports connection listeners.
 */
public class SsgServerSocketFactory extends ServerSocketFactory {
    private long transportModuleId = -1L;
    private Goid connectorGoid = null;

    //- PUBLIC

    /**
     * Create an SsgServerSocketFactory that wraps the default instance.
     *
     * @see ServerSocketFactory#getDefault
     */
    public SsgServerSocketFactory() {
        this(getDefault());
    }

    /**
     * Create and SsgServerSocketFactory that wraps the given instance.
     *
     * @param delegate The ServerSocketFactory to wrap
     */
    public SsgServerSocketFactory(ServerSocketFactory delegate) {
        this.delegate = delegate;
    }

    /**
     * Accept a connection on the given ServerSocket.
     *
     * <p>This factory delegate the acceptance to the wrapped factory. After
     * the connection is made any listeners are notified of the new connection.</p>
     *
     * @param serverSocket The socket on which to accept a connection.
     * @return The accepted Socket
     * @throws IOException on error
     */
    @Override
    public Socket acceptSocket(ServerSocket serverSocket) throws IOException {
        final Socket accepted = delegate.acceptSocket(serverSocket);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Accepted connection.");
        }

        return wrapSocket(getTransportModuleId(), getConnectorOid(), accepted);
    }

    private long getTransportModuleId() {
        synchronized (this) {
            if (transportModuleId != -1L )
                return transportModuleId;
            Object instanceId = attributes.get(HttpTransportModule.CONNECTOR_ATTR_TRANSPORT_MODULE_ID);
            if (instanceId == null)
                return -1L;
            return transportModuleId = Long.parseLong(instanceId.toString());
        }
    }

    private Goid getConnectorOid() {
        synchronized (this) {
            if (connectorGoid != null )
                return connectorGoid;
            Object oid = attributes.get(HttpTransportModule.CONNECTOR_ATTR_CONNECTOR_OID);
            if (oid == null)
                return null;
            return connectorGoid = GoidUpgradeMapper.mapId(EntityType.SSG_CONNECTOR, oid.toString());
        }
    }

    public static Socket wrapSocket( final Socket accepted ) {
        return new SocketWrapper(accepted) {
            private final TraceSupport ts = new TraceSupport(accepted, "https", traceSecureLogger);

            @Override
            public InputStream getInputStream() throws IOException {
                return ts.getInputStream();
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                return ts.getOutputStream();
            }
        };
    }

    public static Socket wrapSocket(final long transportModuleId, final Goid connectorGoid, final Socket accepted) {
        final Socket wrapped;

        if (accepted instanceof SSLSocket) {
            final SSLSocket sslSocket = (SSLSocket)accepted;

            // See if renegotiation should be enabled
            boolean allowRenegotiation = false;
            HttpTransportModule module = HttpTransportModule.getInstance(transportModuleId);
            if (module != null) {
                try {
                    SsgConnector connector = module.getActiveConnectorByGoid(connectorGoid);
                    if (connector.getBooleanProperty(SsgConnector.PROP_TLS_ALLOW_UNSAFE_LEGACY_RENEGOTIATION))
                        allowRenegotiation = true;
                } catch (ListenerException e) {
                    /* FALLTHROUGH and assume renegotiation should not be permitted */
                }
            }

            if (!allowRenegotiation) {
                sslSocket.addHandshakeCompletedListener(new HandshakeCompletedListener() {
                    boolean initialHandshakeCompleted = false;

                    @Override
                    public void handshakeCompleted(HandshakeCompletedEvent event) {
                        if (initialHandshakeCompleted) {
                            ResourceUtils.closeQuietly(sslSocket);
                        }
                        initialHandshakeCompleted = true;
                    }
                });
            }

            wrapped = new SSLSocketWrapper(sslSocket) {
                private final DispatchSupport ds = new DispatchSupport(transportModuleId, connectorGoid, accepted);
                private final TraceSupport ts = new TraceSupport(accepted, "http", traceLogger);

                @Override
                public SocketChannel getChannel() {
                    ds.maybeDispatch();
                    return super.getChannel();
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    ds.maybeDispatch();
                    return ts.getInputStream();
                }

                @Override
                public OutputStream getOutputStream() throws IOException {
                    return ts.getOutputStream();
                }

                @Override
                public void addHandshakeCompletedListener( final HandshakeCompletedListener handshakeCompletedListener ) {
                    // Adding listeners after handshaking has started can trigger
                    // the JDk bug :
                    //
                    //   http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7065972
                    //
                    // Tomcats JSSESupport adds an (unnecessary) listener that we
                    // will ignore for now.
                    //
                    // TODO [jdk7] remove this method once the JDK bug is fixed (7u2?)
                    logger.fine("Ignoring SSL handshake listener");
                }

                @Override
                public synchronized void close() throws IOException {
                    ds.onClose();
                    super.close();
                }
            };
        } else {
            wrapped = new SocketWrapper(accepted) {
                private final DispatchSupport ds = new DispatchSupport(transportModuleId, connectorGoid, accepted);
                private final TraceSupport ts = new TraceSupport(accepted, "http", traceLogger);

                @Override
                public SocketChannel getChannel() {
                    ds.maybeDispatch();
                    return super.getChannel();
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    ds.maybeDispatch();
                    return ts.getInputStream();
                }

                @Override
                public OutputStream getOutputStream() throws IOException {
                    return ts.getOutputStream();
                }

                @Override
                public synchronized void close() throws IOException {
                    ds.onClose();
                    super.close();
                }
            };
        }

        return wrapped;
    }

    /**
     * Calls delegate
     */
    @Override
    public ServerSocket createSocket(int port) throws IOException, InstantiationException {
        return delegate.createSocket(port);
    }

    /**
     * Invokes delegate
     */
    @Override
    public ServerSocket createSocket(int port, int backlog) throws IOException, InstantiationException {
        return delegate.createSocket(port, backlog);
    }

    /**
     * Invokes delegate
     */
    @Override
    public ServerSocket createSocket(int port, int backlog, InetAddress inetAddress) throws IOException, InstantiationException {
        return delegate.createSocket(port, backlog, inetAddress);
    }

    /**
     * Invokes delegate
     */
    @Override
    public void handshake(Socket socket) throws IOException {
        delegate.handshake(socket);
    }

    /**
     * Invokes delegate
     */
    @Override
    public void initSocket(Socket socket) {
        delegate.initSocket(socket);
    }

    /**
     * Invokes delegate
     */
    @Override
    public void setAttribute(String s, Object o) {
        super.setAttribute(s, o);
        delegate.setAttribute(s, o);
    }

    public static void addListener(Listener listener) {
        dispatchingListener.addListener(listener);
    }

    /**
     * Invokes delegate
     */
    public static interface Listener {
        public void onGetInputStream(long transportModuleInstanceId, Goid connectorGoid, Socket accepted);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SsgServerSocketFactory.class.getName());
    private static final Logger traceSecureLogger = Logger.getLogger("com.l7tech.server.transport.https.trace");
    private static final Logger traceLogger = Logger.getLogger("com.l7tech.server.transport.http.trace");
    private static final DispatchingListener dispatchingListener = new DispatchingListener();

    private final ServerSocketFactory delegate;

    /**
     * Listener that dispatches to a List of registered listeners.
     */
    private static class DispatchingListener implements Listener {
        private static final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();

        void addListener(Listener listener) {
            listeners.add(listener);
        }

        @Override
        public void onGetInputStream(long transportModuleInstanceId, Goid connectorGoid, Socket accepted) {
            for (Listener listener : listeners) {
                try {
                    listener.onGetInputStream(transportModuleInstanceId, connectorGoid, accepted);
                }
                catch (Exception e) {
                    logger.log(Level.WARNING, "Unexpected exception in listener.", e);
                }
            }
        }
    }

    private static class DispatchSupport {
        private final long transportModuleId;
        private final Goid connectorGoid;
        private final Socket accepted;
        private final AtomicBoolean dispatched = new AtomicBoolean(false);

        private DispatchSupport(long transportModuleId, Goid connectorGoid, Socket accepted) {
            this.transportModuleId = transportModuleId;
            this.connectorGoid = connectorGoid;
            this.accepted = accepted;
        }

        public void maybeDispatch() {
            final boolean wasDispatched = dispatched.getAndSet(true);
            if (!wasDispatched)
                dispatchingListener.onGetInputStream(transportModuleId, connectorGoid, accepted);
        }

        public void onClose() {
        }
    }
}
