package com.l7tech.proxy;

import com.l7tech.proxy.datamodel.SsgFinder;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import com.l7tech.proxy.ssl.ClientProxyKeyManager;
import com.l7tech.proxy.ssl.ClientProxySecureProtocolSocketFactory;
import com.l7tech.proxy.ssl.ClientProxyTrustManager;
import com.l7tech.proxy.processor.MessageProcessor;
import com.l7tech.common.util.SslUtils;
import com.l7tech.common.protocol.SecureSpanConstants;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.log4j.Category;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.util.MultiException;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.JDKKeyPairGenerator;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

/**
 * Encapsulates an HTTP proxy that processes SOAP messages.
 * User: mike
 * Date: Jun 5, 2003
 * Time: 1:32:33 PM
 */
public class ClientProxy {
    private static final Category log = Category.getInstance(ClientProxy.class);
    public static final String CVS_REVISION = "$Revision$"; // sorry emil
    public static final String PROXY_CONFIG =
            System.getProperties().getProperty("user.home") + File.separator + ".l7tech";

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

    private ClientProxyTrustManager trustManager = null;
    private ClientProxyKeyManager keyManager = null;

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

    private ClientProxyTrustManager getTrustManager() {
        if (trustManager == null)
            trustManager = new ClientProxyTrustManager();
        return trustManager;
    }

    private ClientProxyKeyManager getKeyManager() {
        if (keyManager == null)
            keyManager = new ClientProxyKeyManager();
        return keyManager;
    }

    /** Used by the tests to configure a "trust-all" trust manager, for simple SSL tests. */
    void setTrustManager(ClientProxyTrustManager trustManager) {
        this.trustManager = trustManager;
    }

    /** Used by the tests to configure a client-cert-less key manager, for simple SSL tests. */
    void setKeyManager(ClientProxyKeyManager keyManager) {
        this.keyManager = keyManager;
    }

    /** Used by ClientProxyStub, a fake CP for testing GUI widgets. */
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
            throws NoSuchAlgorithmException, NoSuchProviderException, KeyManagementException
    {
        if (isInitialized)
            return;
        System.setProperty("httpclient.useragent", SecureSpanConstants.USER_AGENT);
        initializeSsl();
        isInitialized = true;
    }

    /**
     * Establish or reestablish the global SSL state.  Must be called after any change to client
     * or server certificates used during any SSL handshake, otherwise the implementation may cache
     * undesirable information.  (The cache is seperate from the session cache, too, so you can't just
     * flush the sessions to fix it.)
     *
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws KeyManagementException
     */
    public synchronized void initializeSsl()
            throws NoSuchAlgorithmException, NoSuchProviderException, KeyManagementException
    {
        // Set up SSL context
        ClientProxyKeyManager keyManager = getKeyManager();
        ClientProxyTrustManager trustManager = getTrustManager();
        SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
        sslContext.init(new X509KeyManager[] {keyManager},
                        new X509TrustManager[] {trustManager},
                        null);
        Protocol https = new Protocol("https", new ClientProxySecureProtocolSocketFactory(sslContext), 443);
        Protocol.registerProtocol("https", https);
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

    /**
     * Generate a Certificate Signing Request, and apply to the Ssg for a certificate for the
     * current user.  If this method returns, the certificate will have been downloaded and saved
     * locally, and the SSL context for this Client Proxy will have been reinitialized.
     *
     * @param ssg   the Ssg to which we are sending our application
     * @throws ServerCertificateUntrustedException if we haven't yet discovered the Ssg server cert
     * @throws GeneralSecurityException   if there was a problem making the CSR
     * @throws GeneralSecurityException   if we were unable to complete SSL handshake with the Ssg
     * @throws IOException                if there was a network problem
     * @throws BadCredentialsException    if the SSG rejected the credentials we provided
     * @throws OperationCanceledException if the user cancels the logon prompt
     */
    public void obtainClientCertificate(Ssg ssg)
            throws GeneralSecurityException, IOException, OperationCanceledException, BadCredentialsException
    {
        if (!ssg.isCredentialsConfigured())
            Managers.getCredentialManager().getCredentials(ssg);

        KeyPair keyPair;
        PKCS10CertificationRequest csr;
        try {
            log.info("Generating new RSA key pair (could take several seconds)...");
            Managers.getCredentialManager().notifyLengthyOperationStarting(ssg, "Generating new client certificate...");
            JDKKeyPairGenerator.RSA kpg = new JDKKeyPairGenerator.RSA();
            keyPair = kpg.generateKeyPair();
            csr = SslUtils.makeCsr(ssg.getUsername(), keyPair.getPublic(), keyPair.getPrivate());
        } finally {
            Managers.getCredentialManager().notifyLengthyOperationFinished(ssg);
        }

        // Since generating the RSA key takes so long, we do our own credential retry loop here
        // rather than delegating to the main policy loop.
        int attempts = 0;
        for (;;) {
            try {
                X509Certificate caCert = SsgKeyStoreManager.getServerCert(ssg);
                if (caCert == null)
                    throw new ServerCertificateUntrustedException(); // fault in the SSG cert
                X509Certificate cert = SslUtils.obtainClientCertificate(ssg.getServerCertRequestUrl(),
                                                                        ssg.getUsername(),
                                                                        ssg.password(),
                                                                        csr,
                                                                        caCert);
                // make sure private key is stored on disk encrypted with the password that was used to obtain it
                SsgKeyStoreManager.saveClientCertificate(ssg, keyPair.getPrivate(), cert);
                initializeSsl(); // reset global SSL state
                return;
            } catch (SslUtils.BadCredentialsException e) {  // note: not the same class BadCredentialsException
                if (++attempts > 3)
                    throw new BadCredentialsException(e);

                Managers.getCredentialManager().notifyInvalidCredentials(ssg);
                if (SsgKeyStoreManager.isClientCertAvailabile(ssg)) // shouldn't be necessary, but just in case
                    SsgKeyStoreManager.deleteClientCert(ssg);
                Managers.getCredentialManager().getCredentials(ssg);
                // retry with new password
            } catch (SslUtils.CertificateAlreadyIssuedException e) {
                Managers.getCredentialManager().notifyCertificateAlreadyIssued(ssg);
                throw new OperationCanceledException();
            }
        }
    }
}

