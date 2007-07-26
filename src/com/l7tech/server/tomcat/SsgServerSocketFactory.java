package com.l7tech.server.tomcat;

import org.apache.tomcat.util.net.ServerSocketFactory;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.channels.SocketChannel;

import com.l7tech.common.io.SSLSocketWrapper;
import com.l7tech.common.io.SocketWrapper;


/**
 * ServerSocketFactory wrapper that supports connection listeners.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class SsgServerSocketFactory extends ServerSocketFactory {
    private static final String ATTR_CIPHERNAMES = "ciphernames"; // comma separated list of enabled ciphers, ie TLS_RSA_WITH_AES_128_CBC_SHA 

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

        final Socket wrapped;
        if (accepted instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket)accepted;
            if (sslCipherNames != null) {
                logger.log(Level.FINE, "Setting custom cipher suites on SSL connection");
                sslSocket.setEnabledCipherSuites(sslCipherNames);
            }
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
        if (ATTR_CIPHERNAMES.equalsIgnoreCase(s)) {
            String[] strings = o == null ? null : o.toString().split("[, ]");
            logger.log(Level.INFO, o == null ? "Using no custom SSL cipher list" : ("Using custom SSL cipher list: " + o.toString()));
            if (logger.isLoggable(Level.INFO)) {
                if (strings == null) {
                    logger.info("Using no custom SSL cipher list");
                } else {
                    StringBuffer sb = new StringBuffer("Using custom cipher list: ");
                    for (String string : strings) {
                        sb.append(string).append(",");
                    }
                    logger.info(sb.toString());
                }
            }
            sslCipherNames = strings;
        }
    }

    /**
     * Invokes delegate
     */
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
        private static final List listeners = new CopyOnWriteArrayList();

        void addListener(Listener listener) {
            listeners.add(listener);
        }

        public void onGetInputStream(Socket accepted) {
            for (Object listener1 : listeners) {
                Listener listener = (Listener)listener1;
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
