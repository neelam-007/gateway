package com.l7tech.proxy;

import com.l7tech.proxy.datamodel.SsgFinder;
import org.mortbay.http.SocketListener;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.util.MultiException;

/**
 * Encapsulates an HTTP proxy that processes SOAP messages.
 * User: mike
 * Date: Jun 5, 2003
 * Time: 1:32:33 PM
 */
public class ClientProxy {
    private SsgFinder ssgFinder;
    private HttpServer httpServer;
    private RequestHandler requestHandler;
    private boolean isRunning = false;
    private boolean isDestroyed = false;

    /**
     * Create a ClientProxy with the specified settings.
     * @param ssgFinder provides the list of SSGs to which we are proxying.
     */
    ClientProxy(final SsgFinder ssgFinder) {
        this.ssgFinder = ssgFinder;
    }

    private void mustNotBeDestroyed() {
        if (isDestroyed)
            throw new IllegalStateException("ClientProxy has been destroyed");
    }

    private void mustBeRunning() {
        mustNotBeDestroyed();
        if (!isRunning)
            throw new IllegalStateException("ClientProxy is not currently running");
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
    public RequestHandler getRequestHandler() {
        if (requestHandler == null) {
            requestHandler = new RequestHandler(ssgFinder);
        }

        return requestHandler;
    }

    /**
     * Get our HttpServer.
     */
    private HttpServer getHttpServer() {
        mustNotBeDestroyed();
        if (httpServer == null) {
            httpServer = new HttpServer();
            final SocketListener socketListener = new SocketListener();
            socketListener.setMaxThreads(100);
            socketListener.setMinThreads(6);
            socketListener.setPort(5555);
            final HttpContext context = new HttpContext(httpServer, "/");
            context.addHandler(getRequestHandler());
            httpServer.addContext(context);
            httpServer.addListener(socketListener);
        }
        return httpServer;
    }

    /**
     * Start up the client proxy.
     * @throws MultiException
     */
    public synchronized void start() throws MultiException {
        mustNotBeRunning();
        getHttpServer().start();
        isRunning = true;
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
