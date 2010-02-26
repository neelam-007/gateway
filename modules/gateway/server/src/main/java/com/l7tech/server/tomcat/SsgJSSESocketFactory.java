package com.l7tech.server.tomcat;

import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.ExceptionUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyStoreException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gateway's TLS socket factory for Tomcat, which knows how to obtain key, cert and socket information with the rest of the SSG.
 */
public class SsgJSSESocketFactory extends org.apache.tomcat.util.net.ServerSocketFactory {
    protected static final Logger logger = Logger.getLogger(SsgJSSESocketFactory.class.getName());

    public static final String ATTR_CIPHERNAMES = "ciphers"; // comma separated list of enabled ciphers, ie TLS_RSA_WITH_AES_128_CBC_SHA
    public static final String ATTR_KEYSTOREOID = "keystoreOid"; // identifies a keystore available from SsgKeyStoreManager instead of one from disk
    public static final String ATTR_KEYALIAS = "keyAlias"; // alias of private key within the keystore

    private boolean initialized = false;
    private SSLServerSocketFactory sslServerSocketFactory;
    private HttpTransportModule httpTransportModule;
    private long transportModuleId;
    private long connectorOid;
    private String[] enabledCiphers;
    private boolean allowUnsafeLegacyRenegotiation;
    private boolean needClientAuth;
    private boolean wantClientAuth;

    //
    // Public
    //

    public SsgJSSESocketFactory() {
    }

    public ServerSocket createSocket(int port) throws IOException {
        if (!initialized) initialize();
        ServerSocket socket = sslServerSocketFactory.createServerSocket(port);
        configureServerSocket(socket);
        return socket;
    }

    public ServerSocket createSocket(int port, int backlog) throws IOException {
        if (!initialized) initialize();
        ServerSocket socket = sslServerSocketFactory.createServerSocket(port, backlog);
        configureServerSocket(socket);
        return socket;
    }

    public ServerSocket createSocket(int port, int backlog, InetAddress ifAddress) throws IOException {
        if (!initialized) initialize();
        ServerSocket socket = sslServerSocketFactory.createServerSocket(port, backlog, ifAddress);
        configureServerSocket(socket);
        return socket;
    }

    public void handshake(Socket sock) throws IOException {
        ((SSLSocket)sock).startHandshake();

        if (!allowUnsafeLegacyRenegotiation) {
            // CVE-2009-3555 - Remove all cipher suites to prevent any SSL renegotiations from succeeding.
            // This work-around is taken from Tomcat, present as of Tomcat 6.0.21
            ((SSLSocket)sock).setEnabledCipherSuites(new String[0]);
        }
    }

    public Socket acceptSocket(ServerSocket socket) throws IOException {
        try {
            SSLSocket asock = (SSLSocket) socket.accept();
            return SsgServerSocketFactory.wrapSocket(transportModuleId, connectorOid, asock);
        } catch (SSLException e){
            SocketException se = new SocketException("SSL handshake error: " + ExceptionUtils.getMessage(e));
            se.initCause(e);
            throw se;
        }
    }

    //
    // Private
    //

    private synchronized void initialize() throws IOException {
        try {
            transportModuleId = getRequiredLongAttr(HttpTransportModule.CONNECTOR_ATTR_TRANSPORT_MODULE_ID);
            connectorOid = getRequiredLongAttr(HttpTransportModule.CONNECTOR_ATTR_CONNECTOR_OID);
            httpTransportModule = HttpTransportModule.getInstance(transportModuleId);
            if (httpTransportModule == null)
                throw new IllegalStateException("No HttpTransportModule with ID " + transportModuleId + " was found");

            gatherClientAuthFlags();
            SSLContext sslContext = createSslContext();

            // Configure SSL session cache
            int sessionCacheSize = getIntAttr("sessionCacheSize", 0);
            int sessionCacheTimeout = getIntAttr("sessionCacheTimeout", 86400);
            SSLSessionContext sessionContext = sslContext.getServerSessionContext();
            if (sessionContext != null) {
                sessionContext.setSessionCacheSize(sessionCacheSize);
                sessionContext.setSessionTimeout(sessionCacheTimeout);
            }

            sslServerSocketFactory = sslContext.getServerSocketFactory();
            enabledCiphers = getEnabledCiphers(getStringAttr("ciphers", null), sslServerSocketFactory.getSupportedCipherSuites());
            allowUnsafeLegacyRenegotiation = Boolean.valueOf(getStringAttr("allowUnsafeLegacyRenegotiation", "false"));

            // Detect early if accepts will always fail (Bug #7553)
            checkSocketConfig();
            initialized = true;

        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Unable to initialize TLS socket factory: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private SSLContext createSslContext() throws Exception {
        String protocol = getStringAttr("protocol", "TLS");
        String protocolProvider = getStringAttr("protocolProvider", null);
        String protocols = getStringAttr("protocols", null);

        Level lvl = Level.INFO;
        if (logger.isLoggable(lvl))
            logger.log(lvl, "Connector " + connectorOid + " protocol list: " + protocols);

        SSLContext context;
        if (protocolProvider != null) {
            logger.log(lvl, "Attempting to create SSLContext using custom provider named " + protocolProvider);
            context = SSLContext.getInstance(protocol, protocolProvider);
        } else {
            String serv;
            if (protocols != null && (protocols.contains("TLSv1.1") || protocols.contains("TLSv1.2"))) {
                serv = JceProvider.SERVICE_TLS12;
            } else {
                serv = JceProvider.SERVICE_TLS10;
            }
            Provider provider = JceProvider.getInstance().getProviderFor(serv);
            if (provider == null) {
                logger.log(lvl, "Attempting to create SSLContext using default provider");
                context = SSLContext.getInstance(protocol);
            } else {
                if (logger.isLoggable(lvl))
                    logger.log(lvl, "Attempting to create SSLContext using " + serv + " provider named " + provider.getName());
                context = SSLContext.getInstance(protocol, provider);
            }
        }

        context.init(getKeyManagers(), getTrustManagers(), new SecureRandom());
        return context;
    }

    private void checkSocketConfig() throws IOException {
        // Ensures that accept is not going to throw Tomcat into an infinite loop of stack traces
        // due to incompatible cipher suites.  Layer 7 bug #7553, Tomcat bug 45528.
        // This code is originally from Tomcat, present as of Tomcat 6.0.19.
        ServerSocket socket = sslServerSocketFactory.createServerSocket();
        configureServerSocket(socket);
        try {
            socket.setSoTimeout(1);
            socket.accept();
            /* NEVER REACHED */
        } catch (SSLException e) {
            throw new IOException("Connector " + connectorOid + " cannot be started with its current server cert and/or cipher suite configuration: " + ExceptionUtils.getMessage(e), e);
        } catch (Exception e) {
            // Ignore it; it's either the expected SocketTimeoutException, or else something that will recur and
            // can be reported later, when the real server socket is later created.
        } finally {
            if (!socket.isClosed())
                socket.close();
        }
    }

    private void configureServerSocket(ServerSocket serverSocket) {
        SSLServerSocket sslServerSocket = (SSLServerSocket)serverSocket;

        // Set cipher suites
        if (enabledCiphers != null)
            sslServerSocket.setEnabledCipherSuites(enabledCiphers);

        // Set TLS versions
        String[] protocols = getEnabledProtocols(getStringAttr("protocols", null), sslServerSocket);
        if (protocols != null)
            sslServerSocket.setEnabledProtocols(protocols);

        // Set client auth
        if (wantClientAuth){
            sslServerSocket.setWantClientAuth(wantClientAuth);
        } else {
            sslServerSocket.setNeedClientAuth(needClientAuth);
        }
    }

    private String[] getEnabledProtocols(String requestedProtocols, SSLServerSocket socket){
        return intersection(requestedProtocols, socket.getSupportedProtocols(), null);
    }

    private String[] getEnabledCiphers(String requested, String supported[]) {
        if (requested == null || requested.trim().length() < 1)
            return sslServerSocketFactory.getDefaultCipherSuites();

        Set<String> ciphers = new HashSet<String>(Arrays.asList(requested.split("\\s*,\\s*")));
        Set<String> have = new HashSet<String>(Arrays.asList(supported));
        ciphers.retainAll(have);
        return ciphers.toArray(new String[ciphers.size()]);
    }

    private void gatherClientAuthFlags() {
        String clientAuthStr = getStringAttr("clientauth", "false");
        if ("true".equalsIgnoreCase(clientAuthStr) || "yes".equalsIgnoreCase(clientAuthStr)) {
            needClientAuth = true;
        } else {
            if ("want".equalsIgnoreCase(clientAuthStr)) {
                wantClientAuth = true;
            }
        }
    }

    private String[] intersection(String left, String[] right, String[] defaultValue) {
        if (left == null || left.trim().length() < 1)
            return defaultValue;
        return ArrayUtils.intersection(left.split("\\s*,\\s*"), right);
    }

    private String getStringAttr(String attrName, String defaultValue) {
        String value = (String)attributes.get(attrName);
        return value != null ? value : defaultValue;
    }

    private int getIntAttr(String attrName, int defaultValue) {
        return Integer.parseInt(getStringAttr(attrName, Integer.toString(defaultValue)));
    }

    private String getRequiredStringAttr(String attrName) {
        String value = (String)attributes.get(attrName);
        if (value == null)
            throw new IllegalStateException("Required attribute \"" + attrName + "\" was not provided");
        return value;
    }

    private long getRequiredLongAttr(String attrName) {
        return Long.parseLong(getRequiredStringAttr(attrName));
    }

    protected KeyManager[] getKeyManagers() throws Exception {
        SsgKeyEntry keyEntry = getServerCertKeyEntry();
        return new KeyManager[]{new SingleCertX509KeyManager(keyEntry.getCertificateChain(), keyEntry.getPrivateKey())};
    }

    protected SsgKeyEntry getServerCertKeyEntry() throws FindException, KeyStoreException {
        long keystoreOid = getRequiredLongAttr(ATTR_KEYSTOREOID);
        SsgKeyStoreManager ksm = httpTransportModule.getSsgKeyStoreManager();
        String keyAlias = getRequiredStringAttr(ATTR_KEYALIAS);
        return ksm.lookupKeyByKeyAlias(keyAlias, keystoreOid);
    }

    protected TrustManager[] getTrustManagers() throws Exception {
        X509TrustManager tm;

        String overriddenIssuers = ClientTrustingTrustManager.getConfiguredIssuerCerts(httpTransportModule.getServerConfig());
        if (overriddenIssuers != null) {
            // Use globally-overridden issuers list, even if it is empty.
            tm = new ClientTrustingTrustManager(ClientTrustingTrustManager.parseIssuerCerts(overriddenIssuers));
        } else {
            tm = new ClientTrustingTrustManager(new Callable<X509Certificate[]>() {
                final AtomicReference<X509Certificate[]> issuers = new AtomicReference<X509Certificate[]>(null);
                final AtomicLong lastUpdated = new AtomicLong(0);

                // TODO one second is probably much too short given the potential cost of looking up all the certs,
                // but much longer and XVC will not be able to connect immediately after obtaining a client cert automatically
                long updateInterval = Long.getLong("com.l7tech.transport.tls.acceptedIssuersCacheTimeout", 1000L);

                @Override
                public X509Certificate[] call() throws Exception {
                    long age = System.currentTimeMillis() - lastUpdated.get();
                    X509Certificate[] certs = issuers.get();
                    if (certs == null || age > updateInterval) {
                        synchronized (this) {
                            age = System.currentTimeMillis() - lastUpdated.get();
                            certs = issuers.get();
                            if (certs == null || age > updateInterval) {
                                certs = ClientTrustingTrustManager.deduplicate(httpTransportModule.getAcceptedIssuersForConnector(connectorOid));
                                issuers.set(certs);
                                lastUpdated.set(System.currentTimeMillis());
                            }
                        }
                    }
                    return certs;
                }
            });
        }

        // Detect errors early on
        tm.getAcceptedIssuers();

        return new TrustManager[] { tm };
    }
}
