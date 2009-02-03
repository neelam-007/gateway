package com.l7tech.skunkworks;

import com.l7tech.util.Functions;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple TCP proxy that forwards all connections to a prespecified target host and port.
 * <p/>
 * This class is intended to be highly configurable by subclassing to support different thread/socket/copying/logging behaviors.
 * <p/>
 * For a simple quickstart, see the {@link #createSimpleProxy} method.
 */
public class TcpPlugProxy implements Closeable {
    private static final Logger logger = Logger.getLogger(TcpPlugProxy.class.getName());

    private final ServerSocket serverSocket;
    private final ExecutorService executorService;
    private final String targetHostname;
    private final int targetPort;

    /**
     * Create a TCP proxy that will accept connections on the specified server socket and forward
     * them to the specified target host and port, using the specified ExecutorService to
     * run each connection's forwarding loop to completion.
     * <p/>
     * This constructor initializes fields and then returns immediately without invoking
     * any methods on the serverSocket or the executorService.
     *
     * @param serverSocket an already-configured ServerSocket on which to accept new connections.  Required.
     * @param executorService an ExecutorService on which each connection's forwarding loops will be executed.
     *                        This would typically be a ThreadPoolExecutor to allow forwarding multiple connections
     *                        in the background, but other threading arrangements are possible.
     *                        <p/>
     *                        Two forwarding loop jobs will be submitted per connection: one to forward
     *                        from the client to the target, and one to forward from the target back to the client.
     *                        Keep this in mind when using a ThreadPoolExector, and provide an even number of threads
     *                        for best results.
     *                        <p/>
     *                        The job to forward from client to target will be submitted first, followed by the
     *                        job from target to client.
     * @param targetHostname the host to which connections should be forwarded.  Required.
     * @param targetPort  the port on the targetHostname to which connections should be forwarded.  Required.
     */
    public TcpPlugProxy(ServerSocket serverSocket, ExecutorService executorService, String targetHostname, int targetPort) {
        this.serverSocket = serverSocket;
        this.executorService = executorService;
        this.targetHostname = targetHostname;
        this.targetPort = targetPort;
    }


    /**
     * Quick-start method to create a simple proxy that listens on the specified port and forwards connections
     * to the specified target host and port, with up to the specified number of connections handled concurrently.
     * <p/>
     * When this method returns normally, threads have been created, sockets bound, and the returned proxy is already
     * accepting connections.  Call its close method to turn it off.
     *
     * @param listenPort  the local port on which to listen for connections.
     * @param concurrency the maximum number of concurrent connections to handle.  Twice this many threads will be
     *                    created to run copying jobs.
     * @param targetHostname the host to which connections should be forwarded.  Required.
     * @param targetPort  the port on the targetHostname to which connections should be forwarded.  Required.
     * @return a Closeable that can be used to shut off the proxy when it is no longer needed.
     *                     Call its close() method to shut down the proxy and free its sockets and threads.
     * @throws java.io.IOException if there is an IOException while creating the server socket.
     */
    public static Closeable createSimpleProxy(int listenPort, int concurrency, String targetHostname, int targetPort) throws IOException {

        return createCustomProxy(listenPort, concurrency, targetHostname, targetPort,
                                 new Functions.Quaternary<TcpPlugProxy, ServerSocket, ExecutorService, String, Integer>() {
            public TcpPlugProxy call(ServerSocket serverSocket, ExecutorService executorService, String hostname, Integer port) {
                return new TcpPlugProxy(serverSocket, executorService, hostname, port);
            }
        });
    }

    /**
     * Quick-start method to create a simple proxy that listens on the specified port and forwards connections
     * to the specified target host and port with the specified concurrency; but allowing the caller control
     * of how the actual proxy is instantiated, to allow for customization via subclassing.
     *
     * @param listenPort  the local port on which to listen for connections.
     * @param concurrency the maximum number of concurrent connections to handle.  Twice this many threads will be
     *                    created to run copying jobs.
     * @param targetHostname the host to which connections should be forwarded.  Required.
     * @param targetPort  the port on the targetHostname to which connections should be forwarded.  Required.
     * @param proxyFactory a Functions.Quaternary< TcpPlugProxy, ServerSocket, ExecutorService, String, Integer > that,
     *                     when invoked with an already-initialized server socket, an already-configured ExecutorService,
     *                     and the target hostname and port, creates and returns a new TcpPlugProxy instance (and
     *                     that never returns null). Required.
     * @return a Closeable that can be used to shut off the proxy when it is no longer needed.
     *                     Call its close() method to shut down the proxy and free its sockets and threads.
     * @throws java.io.IOException if there is an IOException while creating the server socket.
     */
    public static Closeable createCustomProxy(int listenPort, int concurrency, String targetHostname, int targetPort,
                                              Functions.Quaternary<TcpPlugProxy, ServerSocket, ExecutorService, String, Integer> proxyFactory)
            throws IOException
    {
        final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(concurrency * 2);
        final ExecutorService threadPool = new ThreadPoolExecutor(concurrency, concurrency, 5L, TimeUnit.MINUTES, workQueue);
        final ServerSocket serverSocket = new ServerSocket(listenPort);
        final TcpPlugProxy proxy = proxyFactory.call(serverSocket, threadPool, targetHostname, targetPort);
        final Thread acceptorThread = new Thread("TcpPlugProxy-acceptor") {
            public void run() {
                proxy.handleConnections();
            }
        };
        acceptorThread.start();
        return new Closeable() {
            public void close() throws IOException {
                try {
                    threadPool.shutdownNow();
                } finally {
                    try {
                        proxy.close();
                    } finally {
                        acceptorThread.interrupt();
                    }
                }
            }
        };
    }

    /**
     * Handle connections forever, or until the close() method is invoked on another thread.
     */
    public void handleConnections() {
        while (!serverSocket.isClosed()) {
            try {
                handleOneConnection();
            } catch (IOException e) {
                onConnectionException(e);
            }
        }
    }

    /**
     * Method invoked by handleConnections() if there is an IOException while handling a connection.
     * This can be overridden to add custom handling.
     * <p/>
     * This method just logs the exception.
     *
     * @param e the IOException that was caught while handling a connection.
     */
    protected void onConnectionException(IOException e) {
        logger.log(Level.WARNING, "Exception while handling connection: " + ExceptionUtils.getMessage(e), e);
    }

    /**
     * Accepts the next connection on the serverSocket and submits a job to the exectorService to run
     * the forwarding loop on it to completion.
     * <p/>
     * This method returns as soon as both calls to the ExecutorService submit method have returned.
     *
     * @return a pair of Future representing the job to copy from client to target, and the job to copy
     *         from target back to client, respectively.
     * @throws java.io.IOException if there is an IOException while accepting a connection from a client,
     *                             or while opening a connection to the target.
     */
    public Pair<Future<Long>, Future<Long>> handleOneConnection() throws IOException {
        Socket client = acceptConnection();
        Socket target = createConnectionToTarget();

        final InputStream fromClient = getFromClientInputStream(client);
        final OutputStream toTarget = getToTargetOutputStream(target);

        Future<Long> jobFromClient = executorService.submit(createFromClientCopyJob(fromClient, toTarget));

        final InputStream fromTarget = getFromTargetInputStream(target);
        final OutputStream toClient = getToClientOutputStream(client);

        Future<Long> jobFromTarget = executorService.submit(createFromTargetCopyJob(fromTarget, toClient));

        return new Pair<Future<Long>, Future<Long>>(jobFromClient, jobFromTarget);
    }

    /**
     * Accept a client connection on the server socket.
     * Can be overridden to change the way connections are accepted.
     * <p/>
     * This method just calls accept() on the serverSocket.
     *
     * @return a new Socket connected to a client.  Never null.
     * @throws java.io.IOException if there is an IOException while accepting a client connection.
     */
    protected Socket acceptConnection() throws IOException {
        return serverSocket.accept();
    }

    /**
     * Creates a new Socket connected to the target port of the target hostname.
     * Can be overridden to change the way outbound sockets are created.
     * <p/>
     * This method just invokes new {@link java.net.Socket(String, int)}.
     *
     * @return a Socket connected to the target host and port.  Never null.
     * @throws java.io.IOException if there is an IOException while creating the socket.
     */
    protected Socket createConnectionToTarget() throws IOException {
        return new Socket(targetHostname, targetPort);
    }

    /**
     * Called by handleOneConnection to create a job that copies from the target host back to the client host.
     * <p/>
     * Can be overridden to implement different copying behavior.
     * <p/>
     * This method creates a job that just calls {@link com.l7tech.common.util.HexUtils#copyStream(java.io.InputStream, java.io.OutputStream)}
     * and then closes both streams.
     *
     * @param fromTarget an InputStream that, when read, produces information received from the target.  Required.
     * @param toClient an OutputStream that, when written, transmits information to the client.  Required.
     * @return a Callable that, when the job completes, returns the number of bytes that were copied; or, if
     *                    an IOException occurs while copying, can be used to obtain the IOException.  Never null.
     */
    protected Callable<Long> createFromTargetCopyJob(final InputStream fromTarget, final OutputStream toClient) {
        if (fromTarget == null) throw new NullPointerException("fromTarget");
        if (toClient == null) throw new NullPointerException("toClient");
        return new Callable<Long>() {
            public Long call() throws Exception {
                try {
                    return IOUtils.copyStream(fromTarget, toClient);
                } finally {
                    fromTarget.close();
                    toClient.close();
                }
            }
        };
    }

    /**
     * Called by handleOneConnection to create a job that copies from the client to the target host.
     * <p/>
     * Can be overridden to create different copying behavior.
     * <p/>
     * This method creates a job that just calls {@link com.l7tech.common.util.HexUtils#copyStream(java.io.InputStream, java.io.OutputStream)}
     * and then closes both streams.
     *
     * @param fromClient an InputStream that, when read, produces information received from the client.
     * @param toTarget an OutputStream that, when written, transmits information to the target.  Required.
     * @return a Callable that, when the job completes, returns the number of bytes that were copied; or, if
     *                    an IOException occurs while copying, can be used to obtain the IOException.  Never null.
     */
    protected Callable<Long> createFromClientCopyJob(final InputStream fromClient, final OutputStream toTarget) {
        if (fromClient == null) throw new NullPointerException("fromClient");
        if (toTarget == null) throw new NullPointerException("toTarget");
        return new Callable<Long>() {
            public Long call() throws IOException {
                try {
                    return IOUtils.copyStream(fromClient, toTarget);
                } finally {
                    fromClient.close();
                    toTarget.close();
                }
            }
        };
    }

    /**
     * Obtain an OutputStream that, when written, transmits to the specified already-connected client socket.
     * Can be overwridden to wrap the OutputStream.
     * <p/>
     * This method just calls getOutputStream on the client socket.
     *
     * @param client a socket connected to a client.  Required.
     * @return an OutputStream that, when written, transmits to the client.  Never null.
     * @throws java.io.IOException if there is an IOException while obtaining the OutputStream.
     */
    protected OutputStream getToClientOutputStream(Socket client) throws IOException {
        return client.getOutputStream();
    }

    /**
     * Obtain an InputStream that, when read, produces information received from the target host.
     * Can be overridden to wrap the InputStream.
     * <p/>
     * This method just calls getInputStream on the target socket.
     *
     * @param target a socket connected to the target host.  Required.
     * @return an InputStream that, when read, produces information received from the target host.  Never null.
     * @throws java.io.IOException if there is an IOException while obtaining the InputStream.
     */
    protected InputStream getFromTargetInputStream(Socket target) throws IOException {
        return target.getInputStream();
    }

    /**
     * Obtain an OutputStream that, when written, transmits to the specified already-connected target socket.
     * Can be overwridden to wrap the OutputStream.
     * <p/>
     * This method just calls getOutputStream on the target socket.
     *
     * @param target a socket connected to the target host.  Required.
     * @return an OutputStream that, when written, transmits to the target.  Never null.
     * @throws java.io.IOException if there is an IOException while obtaining the OutputStream.
     */
    protected OutputStream getToTargetOutputStream(Socket target) throws IOException {
        return target.getOutputStream();
    }

    /**
     * Obtain an InputStream that, when read, produces information received from the client.
     * Can be overridden to wrap the InputStream.
     * <p/>
     * This method just calls getInputStream on the client socket.
     *
     * @param client a socket connected to the client host.  Required.
     * @return an InputStream that, when read, produces information received from the client host.  Never null.
     * @throws java.io.IOException if there is an IOException while obtaining the InputStream.
     */
    protected InputStream getFromClientInputStream(Socket client) throws IOException {
        return client.getInputStream();
    }

    /**
     * Stop handling connections.  If a call to handleConnections() is currently in progress on another
     * thread, this method will cause it to stop accepting new connections and return normally
     * as soon as possible.
     *
     * @throws java.io.IOException if there is an IOException while forcing closed the server socket.
     */
    public void close() throws IOException {
        serverSocket.close();
    }
}
