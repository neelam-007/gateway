package com.l7tech.proxy;

import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgFinder;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.log4j.Category;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.util.MultiException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
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

    private class ClientProxyKeyManager implements X509KeyManager {
        X509KeyManager defaultKeyManager = null;

        X509KeyManager getDefaultKeyManager() {
            try {
                return defaultKeyManager = (X509KeyManager) KeyManagerFactory.getInstance("SunX509", "SunJSSE").getKeyManagers()[0];
            } catch (NoSuchAlgorithmException e) {
                log.error(e);
                return null;
            } catch (NoSuchProviderException e) {
                log.error(e);
                return null;
            }
        }

        public PrivateKey getPrivateKey(String s) {
            log.info("ClientProxyKeyManager: getPrivateKey: s=" + s);
            return getDefaultKeyManager().getPrivateKey(s);
        }

        public X509Certificate[] getCertificateChain(String s) {
            log.info("ClientProxyKeyManager: getCertificateChain: s=" + s);
            return getDefaultKeyManager().getCertificateChain(s);
        }

        public String[] getClientAliases(String s, Principal[] principals) {
            log.info("ClientProxyKeyManager: getClientAliases");
            return getDefaultKeyManager().getClientAliases(s, principals);
        }

        public String[] getServerAliases(String s, Principal[] principals) {
            log.info("ClientProxyKeyManager: getServerAliases");
            return getDefaultKeyManager().getServerAliases(s, principals);
        }

        public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
            log.info("ClientProxyKeyManager: chooseServerAlias: s=" + s + "  principals=" + principals + "  socket=" + socket);
            return getDefaultKeyManager().chooseServerAlias(s, principals, socket);
        }

        public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
            InetAddress ia = socket.getInetAddress();
            String hostname = ia.getHostName();
            log.info("ClientProxyKeyManager: chooseClientAlias: ia=" + ia + "  hostname=" + hostname);

            return getDefaultKeyManager().chooseClientAlias(strings, principals, socket);
        }
    }

    private class ClientProxyTrustManager implements X509TrustManager {
        public X509Certificate[] getAcceptedIssuers() {
            log.info("ClientProxyTrustManager.getAcceptedIssuers()");
            return new X509Certificate[0];
        }

        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            log.info("ClientProxyTrustManager.checkClientTrusted()");
        }

        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            log.info("ClientProxyTrustManager.checkServerTrusted()");
        }
    }

    private class ClientProxySecureProtocolSocketFactory implements SecureProtocolSocketFactory {
        private SSLContext sslContext;

        ClientProxySecureProtocolSocketFactory(SSLContext ctx) {
            this.sslContext = ctx;
        }

        public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
                throws IOException, UnknownHostException
        {
            log.info("ClientProxySecureProtocolSocketFactory.createSocket1(): host=" + host);
            final SSLSocket sock = (SSLSocket) sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
            log.info("Socket is type: " + sock.getClass());
            return sock;
        }

        public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort)
                throws IOException, UnknownHostException
        {
            log.info("ClientProxySecureProtocolSocketFactory.createSocket2(): host=" + host);
            final SSLSocket sock = (SSLSocket) sslContext.getSocketFactory().createSocket(host, port, clientHost, clientPort);
            log.info("Socket is type: " + sock.getClass());
            return sock;
        }

        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
            log.info("ClientProxySecureProtocolSocketFactory.createSocket3(): host=" + host);
            final SSLSocket sock = (SSLSocket) sslContext.getSocketFactory().createSocket(host, port);
            log.info("Socket is type: " + sock.getClass());
            return sock;
        }
    }

    public synchronized void init()
            throws NoSuchAlgorithmException, NoSuchProviderException, KeyManagementException
    {
        if (isInitialized)
            return;

        // Set up SSL trust store
        // TODO: we still need better cert management than this grody hack
        Properties props = System.getProperties();
        props.put("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        props.put("javax.net.ssl.trustStore", TRUST_STORE_FILE.getAbsolutePath());
        props.put("javax.net.ssl.trustStorePassword", TRUST_STORE_PASSWORD);
        ClientProxyKeyManager keyManager = new ClientProxyKeyManager();
        ClientProxyTrustManager trustManager = new ClientProxyTrustManager();
        SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
        sslContext.init(new X509KeyManager[] {keyManager},
                        new X509TrustManager[] {trustManager},
                        null);
        Protocol https = new Protocol("https", new ClientProxySecureProtocolSocketFactory(sslContext), 443);
        Protocol.registerProtocol("https", https);
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
    }
}
