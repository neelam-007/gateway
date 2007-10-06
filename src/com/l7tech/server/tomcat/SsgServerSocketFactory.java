package com.l7tech.server.tomcat;

import com.l7tech.common.io.SSLSocketWrapper;
import com.l7tech.common.io.SocketWrapper;
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
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * ServerSocketFactory wrapper that supports connection listeners.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class SsgServerSocketFactory extends ServerSocketFactory {

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

        return wrapSocket(accepted);
    }

    public static Socket wrapSocket(final Socket accepted) {
        final Socket wrapped;
        if (accepted instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket)accepted;
            wrapped = new SSLSocketWrapper(sslSocket) {
                boolean dispatched = false;

                public SocketChannel getChannel() {
                    maybeDispatch();
                    return super.getChannel();
                }

                public InputStream getInputStream() throws IOException {
                    maybeDispatch();
                    return super.getInputStream();
                }

                private void maybeDispatch() {
                    if (!dispatched) {
                        dispatchingListener.onGetInputStream(accepted);
                        dispatched = true;
                    }
                }
            };
        } else {
            wrapped = new SocketWrapper(accepted) {
                boolean dispatched = false;

                public SocketChannel getChannel() {
                    maybeDispatch();
                    return super.getChannel();
                }

                public InputStream getInputStream() throws IOException {
                    maybeDispatch();
                    return super.getInputStream();
                }

                private void maybeDispatch() {
                    if (!dispatched) {
                        dispatchingListener.onGetInputStream(accepted);
                        dispatched = true;
                    }
                }
            };
        }

        return wrapped;
    }

    /**
     * Calls delegate
     */
    public ServerSocket createSocket(int i) throws IOException, InstantiationException {
        return delegate.createSocket(i);
    }

    /**
     * Invokes delegate
     */
    public ServerSocket createSocket(int i, int i1) throws IOException, InstantiationException {
        return delegate.createSocket(i, i1);
    }

    /**
     * Invokes delegate
     */
    public ServerSocket createSocket(int i, int i1, InetAddress inetAddress) throws IOException, InstantiationException {
        return delegate.createSocket(i, i1, inetAddress);
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
        delegate.setAttribute(s, o);
    }

    public static void addListener(Listener listener) {
        dispatchingListener.addListener(listener);
    }

    /**
     * Invokes delegate
     */
    public static interface Listener {
        public void onGetInputStream(Socket accepted);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SsgServerSocketFactory.class.getName());
    private static final DispatchingListener dispatchingListener = new DispatchingListener();

    private final ServerSocketFactory delegate;
    private String[] sslCipherNames = null;

    /**
     * Listener that dispatches to a List of registered listeners.
     */
    private static class DispatchingListener implements Listener {
        private static final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();

        void addListener(Listener listener) {
            listeners.add(listener);
        }

        public void onGetInputStream(Socket accepted) {
            for (Listener listener : listeners) {
                try {
                    listener.onGetInputStream(accepted);
                }
                catch (Exception e) {
                    logger.log(Level.WARNING, "Unexpected exception in listener.", e);
                }
            }
        }
    }
}
