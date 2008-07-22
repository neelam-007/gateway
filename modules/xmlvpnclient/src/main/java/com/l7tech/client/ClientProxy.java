package com.l7tech.client;

import com.l7tech.security.prov.JceProvider;
import com.l7tech.proxy.datamodel.SsgFinder;
import com.l7tech.proxy.processor.MessageProcessor;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.thread.QueuedThreadPool;

import java.net.URL;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates an HTTP proxy that processes SOAP messages.
 * User: mike
 * Date: Jun 5, 2003
 * Time: 1:32:33 PM
 */
public class ClientProxy {
    static {
        JceProvider.init();
    }

    private static final Logger log = Logger.getLogger(ClientProxy.class.getName());

    /**
     * This is the suffix appended to the local endpoint to form local WSDL discovery URLs.
     * For example, a Bridge listening on http://localhost:7700/ssg3 will treat anything addressed
     * to http://localhost:7700/ssg3/wsdl as a WSDL discovery request to the corresponding SSG.
     */
    public static final String WSDL_SUFFIX = "/wsdl";
    public static final String WSIL_SUFFIX = "/wsil";

    private SsgFinder ssgFinder;
    private Server httpServer;
    private RequestHandler requestHandler;
    private MessageProcessor messageProcessor;

    private int maxThreads;
    private int minThreads;
    private int bindPort;

    private volatile boolean isRunning = false;
    private volatile boolean isDestroyed = false;
    private volatile boolean isInitialized = false;

    /**
     * Create a ClientProxy with the specified settings.
     *
     * @param ssgFinder provides The list of SSGs to which we are proxying.  Required.
     * @param messageProcessor  The (client-side) MessageProcessor to which requests are to be submitted.  Required.
     * @param bindPort The port on which to listen for HTTP connections.  Required.
     * @param minThreads  Minimum number of threads to keep in the request handling thread pool.  Required.
     * @param maxThreads  Maximum number of threads to allow in the request handling thread pool.  Required.
     */
    public ClientProxy(final SsgFinder ssgFinder,
                       final MessageProcessor messageProcessor,
                       final int bindPort,
                       final int minThreads,
                       final int maxThreads)
    {
        this.ssgFinder = ssgFinder;
        this.messageProcessor = messageProcessor;
        this.bindPort = bindPort;
        this.minThreads = minThreads;
        this.maxThreads = maxThreads;
    }

    /**
     * Used by ClientProxyStub, a fake CP for testing GUI widgets.
     *
     * @param bindPort The port on which to pretend to listen for HTTP connections.
     * @deprecated Do not use this constructor except while writing GUI test code that needs a fake ClientProxy.
     */
    protected ClientProxy(int bindPort) {
        this.bindPort = bindPort;
    }

    private void mustNotBeDestroyed() {
        if (isDestroyed)
            throw new IllegalStateException("ClientProxy has been destroyed");
    }

    private void mustNotBeRunning() {
        mustNotBeDestroyed();
        if (isRunning)
            throw new IllegalStateException("ClientProxy is currently running");
    }

    private synchronized void init()
    {
        if (isInitialized)
            return;
        isInitialized = true;
    }

    public  SsgFinder getSsgFinder() {
        return ssgFinder;
    }
    /**
     * Get our RequestHandler.
     * @return the RequestHandler we are using.
     */
    public synchronized RequestHandler getRequestHandler() {
        if (requestHandler == null)
            requestHandler = new RequestHandler(ssgFinder, messageProcessor);
        return requestHandler;
    }

    private synchronized Server getHttpServer() throws UnknownHostException {
        mustNotBeDestroyed();
        if (httpServer == null) {
            httpServer = new Server();

            QueuedThreadPool threadPool = new QueuedThreadPool();
            threadPool.setMinThreads(minThreads);
            threadPool.setMaxThreads(maxThreads);
            httpServer.setThreadPool(threadPool);

            Connector socketListener = new SocketConnector();
            socketListener.setHost("127.0.0.1");
            socketListener.setPort(bindPort);
            httpServer.addConnector(socketListener);

            httpServer.addHandler(getRequestHandler());
        }
        return httpServer;
    }

    public int getBindPort() {
        return bindPort;
    }

    /**
     * Start up the client proxy and return immediately.
     *
     * @return the client proxy's base URL.
     * @throws Exception if the proxy could not be started
     */
    public synchronized URL start() throws Exception {
        mustNotBeRunning();
        if (!isInitialized)
            init();
        getHttpServer().start();
        isRunning = true;
        URL url = new URL("http", "127.0.0.1", bindPort, "/");

        log.info("ClientProxy started; listening on " + url);
        log.info("Using asymmetric cryptography provider: " + JceProvider.getAsymmetricJceProvider().getName());
        log.info("Using symmetric cryptography provider: " + JceProvider.getSymmetricJceProvider().getName());

        return url;
    }

    /**
     * Stop the client proxy.
     * It can later be started again.
     */
    public synchronized void stop() {
        if (isRunning) {
            try {
                getHttpServer().stop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.log(Level.SEVERE, "impossible error: ", e); // can't happen
            }
        }

        isRunning = false;
    }

    /**
     * Shutdown the client proxy and free the resources it's using.
     */
    public synchronized void destroy() {
        if (isDestroyed)
            return;

        if (isRunning)
            stop();

        if (httpServer != null) {
            httpServer.destroy();
            httpServer = null;
        }

        isDestroyed = true;
    }
}

