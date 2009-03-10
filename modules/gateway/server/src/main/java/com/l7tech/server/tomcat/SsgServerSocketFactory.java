package com.l7tech.server.tomcat;

import com.l7tech.common.io.SSLSocketWrapper;
import com.l7tech.common.io.SocketWrapper;
import com.l7tech.server.transport.http.HttpTransportModule;
import org.apache.tomcat.util.net.ServerSocketFactory;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
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
    private long transportModuleId = -1;
    private long connectorOid = -1;

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
    public Socket acceptSocket(ServerSocket serverSocket) throws IOException {
        final Socket accepted = delegate.acceptSocket(serverSocket);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Accepted connection.");
        }

        return wrapSocket(getTransportModuleId(), getConnectorOid(), accepted);
    }

    private long getTransportModuleId() {
        synchronized (this) {
            if (transportModuleId != -1)
                return transportModuleId;
            Object instanceId = attributes.get(HttpTransportModule.CONNECTOR_ATTR_TRANSPORT_MODULE_ID);
            if (instanceId == null)
                return -1;
            return transportModuleId = Long.parseLong(instanceId.toString());
        }
    }

    private long getConnectorOid() {
        synchronized (this) {
            if (connectorOid != -1)
                return connectorOid;
            Object oid = attributes.get(HttpTransportModule.CONNECTOR_ATTR_CONNECTOR_OID);
            if (oid == null)
                return -1;
            return connectorOid = Long.parseLong(oid.toString());
        }
    }

    public static Socket wrapSocket(final long transportModuleId, final long connectorOid, final Socket accepted) {
        HttpTransportModule.onSocketOpened(transportModuleId, connectorOid, accepted);

        final Socket wrapped;

        if (accepted instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket)accepted;
            wrapped = new SSLSocketWrapper(sslSocket) {
                private final DispatchSupport ds = new DispatchSupport(transportModuleId, connectorOid, accepted);

                public SocketChannel getChannel() {
                    ds.maybeDispatch();
                    return super.getChannel();
                }

                public InputStream getInputStream() throws IOException {
                    ds.maybeDispatch();
                    return super.getInputStream();
                }

                public synchronized void close() throws IOException {
                    ds.onClose();
                    super.close();
                }
            };
        } else {
            wrapped = new SocketWrapper(accepted) {
                private final DispatchSupport ds = new DispatchSupport(transportModuleId, connectorOid, accepted);

                public SocketChannel getChannel() {
                    ds.maybeDispatch();
                    return super.getChannel();
                }

                public InputStream getInputStream() throws IOException {
                    ds.maybeDispatch();
                    return super.getInputStream();
                }

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
    public ServerSocket createSocket(int port) throws IOException, InstantiationException {
        return delegate.createSocket(port);
    }

    /**
     * Invokes delegate
     */
    public ServerSocket createSocket(int port, int backlog) throws IOException, InstantiationException {
        return delegate.createSocket(port, backlog);
    }

    /**
     * Invokes delegate
     */
    public ServerSocket createSocket(int port, int backlog, InetAddress inetAddress) throws IOException, InstantiationException {
        return delegate.createSocket(port, backlog, inetAddress);
    }

    /**
     * Invokes delegate
     */
    public void handshake(Socket socket) throws IOException {
        delegate.handshake(socket);
    }

    /**
     * Invokes delegate
     */
    public void initSocket(Socket socket) {
        delegate.initSocket(socket);
    }

    /**
     * Invokes delegate
     */
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
        public void onGetInputStream(long transportModuleInstanceId, long connectorOid, Socket accepted);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SsgServerSocketFactory.class.getName());
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

        public void onGetInputStream(long transportModuleInstanceId, long connectorOid, Socket accepted) {
            for (Listener listener : listeners) {
                try {
                    listener.onGetInputStream(transportModuleInstanceId, connectorOid, accepted);
                }
                catch (Exception e) {
                    logger.log(Level.WARNING, "Unexpected exception in listener.", e);
                }
            }
        }
    }

    private static class DispatchSupport {
        private final long transportModuleId;
        private final long connectorOid;
        private final Socket accepted;
        private final AtomicBoolean dispatched = new AtomicBoolean(false);

        public DispatchSupport(long transportModuleId, long connectorOid, Socket accepted) {
            this.transportModuleId = transportModuleId;
            this.connectorOid = connectorOid;
            this.accepted = accepted;
        }

        public void maybeDispatch() {
            final boolean wasDispatched = dispatched.getAndSet(true);
            if (!wasDispatched)
                dispatchingListener.onGetInputStream(transportModuleId, connectorOid, accepted);
        }

        public void onClose() {
            HttpTransportModule.onSocketClosed(transportModuleId, connectorOid, accepted);
        }
    }

}
