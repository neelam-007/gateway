package com.l7tech.proxy;

import com.l7tech.common.security.JceProvider;
import com.l7tech.proxy.datamodel.SsgFinder;
import com.l7tech.proxy.processor.MessageProcessor;
import com.l7tech.proxy.util.ClientLogger;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.util.MultiException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

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

    private static final ClientLogger log = ClientLogger.getInstance(ClientProxy.class);
    public static final String PROXY_CONFIG =
            System.getProperties().getProperty("user.home") + File.separator + ".l7tech";

    /**
     * This is the suffix appended to the local endpoint to form local WSDL discovery URLs.
     * For example, an Agent listening on http://localhost:7700/ssg3 will treat anything addressed
     * to http://localhost:7700/ssg3/wsdl as a WSDL discovery request to the corresponding SSG.
     */
    public static final String WSDL_SUFFIX = "/wsdl";
    public static final String WSIL_SUFFIX = "/wsil";

    private SsgFinder ssgFinder;
    private HttpServer httpServer;
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
     * @param ssgFinder provides the list of SSGs to which we are proxying.
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

    /**
     * Get our RequestHandler.
     * @return the RequestHandler we are using.
     */
    public synchronized RequestHandler getRequestHandler() {
        if (requestHandler == null) {
            requestHandler = new RequestHandler(this, ssgFinder, messageProcessor, bindPort);
        }

        return requestHandler;
    }

    /**
     * Get our HttpServer.
     */
    private synchronized HttpServer getHttpServer() throws UnknownHostException {
        mustNotBeDestroyed();
        if (httpServer == null) {
            httpServer = new HttpServer();
            final SocketListener socketListener;
            socketListener = new SocketListener();
            socketListener.setMaxThreads(maxThreads);
            socketListener.setMinThreads(minThreads);
            socketListener.setHost("127.0.0.1");
            socketListener.setPort(bindPort);
            final HttpContext context = new HttpContext(httpServer, "/");
            context.addHandler(getRequestHandler());
            httpServer.addContext(context);
            httpServer.addListener(socketListener);
        }
        return httpServer;
    }

    public int getBindPort() {
        return bindPort;
    }

    /**
     * Start up the client proxy.
     * @return the client proxy's base URL.
     * @throws MultiException if the proxy could not be started
     */
    public synchronized URL start() throws MultiException, KeyManagementException, NoSuchAlgorithmException, NoSuchProviderException {
        mustNotBeRunning();
        if (!isInitialized)
            init();
        try {
            getHttpServer().start();
        } catch (IOException e) {
            log.error("Unable to start HTTP server: ", e);
            MultiException me = new MultiException();
            me.add(e);
            throw me;
        }
        isRunning = true;
        URL url;
        try {
            url = new URL("http", "127.0.0.1", bindPort, "/");
        } catch (MalformedURLException e) {
            log.error(e);
            throw new MultiException();
        }

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
            } catch (IOException e) {
                log.warn("impossible error: ", e); // can't happen
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

