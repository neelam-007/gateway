package com.l7tech.proxy;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.CertificateDownloader;
import com.l7tech.common.util.SslUtils;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgFinder;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.HttpChallengeRequiredException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.PolicyRetryableException;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import com.l7tech.proxy.processor.MessageProcessor;
import com.l7tech.proxy.ssl.ClientProxySecureProtocolSocketFactory;
import com.l7tech.proxy.util.ClientLogger;
import org.apache.commons.httpclient.protocol.Protocol;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.JDKKeyPairGenerator;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.util.MultiException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.X509Certificate;

/**
 * Encapsulates an HTTP proxy that processes SOAP messages.
 * User: mike
 * Date: Jun 5, 2003
 * Time: 1:32:33 PM
 */
public class ClientProxy {
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
        System.setProperty("httpclient.useragent", SecureSpanConstants.USER_AGENT);
        Protocol https = new Protocol("https", ClientProxySecureProtocolSocketFactory.getInstance(), 443);
        Protocol.registerProtocol("https", https);
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
     * @param request   the request we are working on to which we are sending our application
     * @throws ServerCertificateUntrustedException if we haven't yet discovered the Ssg server cert
     * @throws GeneralSecurityException   if there was a problem making the CSR
     * @throws GeneralSecurityException   if we were unable to complete SSL handshake with the Ssg
     * @throws IOException                if there was a network problem
     * @throws BadCredentialsException    if the SSG rejected the credentials we provided
     * @throws OperationCanceledException if the user cancels the logon prompt
     * @throws PolicyRetryableException   if the policy should be updated and the operation retried
     */
    public void obtainClientCertificate(PendingRequest request)
            throws  ServerCertificateUntrustedException, GeneralSecurityException, IOException,
                    OperationCanceledException, BadCredentialsException, HttpChallengeRequiredException, PolicyRetryableException
    {
        Ssg ssg = request.getSsg();
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
                try {
                    X509Certificate caCert = SsgKeyStoreManager.getServerCert(ssg);
                    if (caCert == null)
                        throw new ServerCertificateUntrustedException(); // fault in the SSG cert
                    X509Certificate cert = SslUtils.obtainClientCertificate(ssg.getServerCertRequestUrl(),
                                                                            request.getUsername(),
                                                                            request.getPassword(),
                                                                            csr,
                                                                            caCert);
                    // make sure private key is stored on disk encrypted with the password that was used to obtain it
                    SsgKeyStoreManager.saveClientCertificate(ssg, keyPair.getPrivate(), cert);
                    ssg.resetSslContext(); // reset cached SSL state
                    return;
                } catch (SslUtils.BadCredentialsException e) {  // note: not the same class BadCredentialsException
                    if (++attempts > 3)
                        throw new BadCredentialsException(e);

                    if (SsgKeyStoreManager.isClientCertAvailabile(ssg)) // shouldn't be necessary, but just in case
                        SsgKeyStoreManager.deleteClientCert(ssg);
                    request.getNewCredentials();
                    // FALLTHROUGH -- retry with new password
                } catch (SslUtils.CertificateAlreadyIssuedException e) {
                    // Bug #380 - if we haven't updated policy yet, try that first - mlyons
                    if (!request.isPolicyUpdated()) {
                        Managers.getPolicyManager().flushPolicy(request);
                        throw new PolicyRetryableException();
                    } else {
                        Managers.getCredentialManager().notifyCertificateAlreadyIssued(ssg);
                        throw new OperationCanceledException();
                    }
                }
            } catch (KeyStoreCorruptException e) {
                Managers.getCredentialManager().notifyKeyStoreCorrupt(ssg);
                SsgKeyStoreManager.deleteKeyStore(ssg);
                // FALLTHROUGH -- retry with newly-emptied keystore
            }
        }
    }

    /**
     * Download and install the SSG certificate.  If this completes successfully, the
     * next attempt to connect to the SSG via SSL should at least get past the SSL handshake.  Uses the
     * specified credentials for the download.
     *
     * @throws IOException if there was a network problem downloading the server cert
     * @throws IOException if there was a problem reading or writing the keystore for this SSG
     * @throws BadCredentialsException if the downloaded cert could not be verified with the SSG username and password
     * @throws OperationCanceledException if credentials were needed but the user declined to enter them
     * @throws GeneralSecurityException for miscellaneous and mostly unlikely certificate or key store problems
     */
    public static void installSsgServerCertificate(Ssg ssg, PasswordAuthentication credentials)
            throws IOException, BadCredentialsException, OperationCanceledException, GeneralSecurityException, KeyStoreCorruptException
    {
        CertificateDownloader cd = new CertificateDownloader(ssg.getServerUrl(),
                                                             credentials.getUserName(),
                                                             credentials.getPassword());

        boolean isValidated = cd.downloadCertificate();
        if (!isValidated) {
            if (cd.isUserUnknown()) {
                // We got no cert-check headers, so this might be an LDAP user.  Prompt for manual
                // certificate verification.
                Managers.getCredentialManager().notifySsgCertificateUntrusted(ssg, cd.getCertificate());
            } else
                throw new BadCredentialsException("The downloaded Gateway server certificate could not be verified with the current username and password.");
        }

        SsgKeyStoreManager.saveSsgCertificate(ssg, cd.getCertificate());
    }
}

