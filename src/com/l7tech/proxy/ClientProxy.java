package com.l7tech.proxy;

import com.l7tech.proxy.datamodel.SsgFinder;
import org.apache.log4j.Category;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.util.MultiException;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Encapsulates an HTTP proxy that processes SOAP messages.
 * User: mike
 * Date: Jun 5, 2003
 * Time: 1:32:33 PM
 */
public class ClientProxy {
    private final Category log = Category.getInstance(ClientProxy.class);

    private SsgFinder ssgFinder;
    private HttpServer httpServer;
    private RequestHandler requestHandler;
    private MessageProcessor messageProcessor;

    private int maxThreads;
    private int minThreads;
    private int bindPort;

    private boolean isRunning = false;
    private boolean isDestroyed = false;

    /**
     * Create a ClientProxy with the specified settings.
     * @param ssgFinder provides the list of SSGs to which we are proxying.
     */
    public ClientProxy(final SsgFinder ssgFinder, final MessageProcessor messageProcessor,
                final int bindPort, final int minThreads, final int maxThreads) {
        this.ssgFinder = ssgFinder;
        this.messageProcessor = messageProcessor;
        this.bindPort = bindPort;
        this.minThreads = minThreads;
        this.maxThreads = maxThreads;
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

    /**
     * Get our RequestHandler.
     * @return the RequestHandler we are using.
     */
    public synchronized RequestHandler getRequestHandler() {
        if (requestHandler == null) {
            requestHandler = new RequestHandler(ssgFinder, messageProcessor);
        }

        return requestHandler;
    }

    /**
     * Get our HttpServer.
     */
    private synchronized HttpServer getHttpServer() {
        mustNotBeDestroyed();
        if (httpServer == null) {
            httpServer = new HttpServer();
            final SocketListener socketListener = new SocketListener();
            socketListener.setMaxThreads(maxThreads);
            socketListener.setMinThreads(minThreads);
            socketListener.setPort(bindPort);
            final HttpContext context = new HttpContext(httpServer, "/");
            context.addHandler(getRequestHandler());
            httpServer.addContext(context);
            httpServer.addListener(socketListener);
        }
        return httpServer;
    }

    /**
     * Start up the client proxy.
     * @return the client proxy's base URL.
     * @throws MultiException if the proxy could not be started
     */
    public synchronized URL start() throws MultiException {
        mustNotBeRunning();
        getHttpServer().start();
        isRunning = true;
        URL url;
        try {
            url = new URL("http", "localhost", bindPort, "/");
        } catch (MalformedURLException e) {
            log.error(e);
            throw new MultiException();
        }

        log.info("ClientProxy started; listening on " + url);

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
