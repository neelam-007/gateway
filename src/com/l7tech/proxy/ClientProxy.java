package com.l7tech.proxy;

import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgFinder;
import org.apache.log4j.Category;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.util.MultiException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Properties;

/**
 * Encapsulates an HTTP proxy that processes SOAP messages.
 * User: mike
 * Date: Jun 5, 2003
 * Time: 1:32:33 PM
 */
public class ClientProxy {
    private static final Category log = Category.getInstance(ClientProxy.class);
    public static final String PROXY_CONFIG =
            System.getProperties().getProperty("user.home") + File.separator + ".l7tech";
    public static final File TRUST_STORE_FILE =
            new File(PROXY_CONFIG + File.separator + "trustStore");
    public static final String TRUST_STORE_PASSWORD = "password";

    private SsgFinder ssgFinder;
    private HttpServer httpServer;
    private RequestHandler requestHandler;
    private MessageProcessor messageProcessor;

    private int maxThreads;
    private int minThreads;
    private int bindPort;

    private boolean isRunning = false;
    private boolean isDestroyed = false;
    private boolean isInitialized = false;

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

    public synchronized void init() {
        if (isInitialized)
            return;

        // Set up SSL trust store
        // TODO: we still need better cert management than this grody hack
        Properties props = System.getProperties();
        props.put("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        props.put("javax.net.ssl.trustStore", TRUST_STORE_FILE.getAbsolutePath());
        props.put("javax.net.ssl.trustStorePassword", TRUST_STORE_PASSWORD);

        isInitialized = true;
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
        if (!isInitialized)
            init();
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

    public synchronized static void importCertificate(Ssg ssg, Certificate cert)
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException
    {
        String alias = "ssg" + ssg.getId();

        KeyStore ks = KeyStore.getInstance("JKS");
        try {
            FileInputStream ksfis = new FileInputStream(ClientProxy.TRUST_STORE_FILE);
            try {
                ks.load(ksfis, ClientProxy.TRUST_STORE_PASSWORD.toCharArray());
            } finally {
                ksfis.close();
            }
        } catch (FileNotFoundException e) {
            // Create a new one.
            ks.load(null, ClientProxy.TRUST_STORE_PASSWORD.toCharArray());
        }

        log.info("Adding certificate: " + cert);
        ks.setCertificateEntry(alias, cert);

        FileOutputStream ksfos = null;
        try {
            ksfos = new FileOutputStream(ClientProxy.TRUST_STORE_FILE);
            ks.store(ksfos, ClientProxy.TRUST_STORE_PASSWORD.toCharArray());
        } finally {
            if (ksfos != null)
                ksfos.close();
        }

        ssg.setHasCertificate(true);
    }
}
