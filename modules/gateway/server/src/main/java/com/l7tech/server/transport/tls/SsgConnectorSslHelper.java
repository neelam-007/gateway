package com.l7tech.server.transport.tls;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.TransportModule;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.CachedCallable;
import com.l7tech.util.ExceptionUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Generic SSL socket factory for use by Tomcat (HTTP) and Apache FtpServer (FTPS) SSL integration.
 * May be of use to other transport modules whose SsgConnectors will be used for SSL connections.
 */
public class SsgConnectorSslHelper {
    protected static final Logger logger = Logger.getLogger(SsgConnectorSslHelper.class.getName());

    public static final String SYSPROP_ACCEPTED_ISSUERS_CACHE_TIMEOUT = "com.l7tech.transport.tls.acceptedIssuersCacheTimeout";

    private static final Pattern SPLITTER = Pattern.compile("\\s*,\\s*");
    private static final SecureRandom secureRandom = new SecureRandom();
    
    private final TransportModule transportModule;
    private final SsgConnector ssgConnector;
    private final SSLContext sslContext;
    private final SSLServerSocketFactory sslServerSocketFactory;
    private final String[] enabledTlsVersions;
    private final String[] enabledCiphers;
    private final boolean allowUnsafeLegacyRenegotiation;

    /**
     * Create an SSL helper for the specified connector.
     *
     * @param transportModule the TransportModule that owns this connector.
     * @param c the connector to use to configure SSL parameters.
     * @throws ListenerException if there is a problem gathering necessary information
     * @throws GeneralSecurityException if there is a problem with a provider, key, certificate, or algorithm
     */
    public SsgConnectorSslHelper(TransportModule transportModule, SsgConnector c) throws ListenerException, GeneralSecurityException {
        if (transportModule == null)
            throw new IllegalArgumentException("transportModule is required");
        if (c == null)
            throw new IllegalArgumentException("an SsgConnector is required");

        this.transportModule = transportModule;
        this.ssgConnector = c;

        // Take note of the enabled TLS versions (SSLv3.0, TLSv1, SSLv2Hello, TLSv1.2)
        Set<String> desiredTlsVersionsSet = getDesiredTlsVersions(ssgConnector);

        // Create and initialize SSL context
        sslContext = createSslContext(ssgConnector, desiredTlsVersionsSet);
        SsgKeyEntry keyEntry = transportModule.getKeyEntry(ssgConnector);
        sslContext.init(getKeyManagers(keyEntry), getTrustManagers(), secureRandom);
        sslServerSocketFactory = sslContext.getServerSocketFactory();

        // Configure SSL session cache
        int sessionCacheSize = getIntProperty(SsgConnector.PROP_TLS_SESSION_CACHE_SIZE, 0);
        int sessionCacheTimeout = getIntProperty(SsgConnector.PROP_TLS_SESSION_CACHE_TIMEOUT, 86400);
        SSLSessionContext sessionContext = sslContext.getServerSessionContext();
        if (sessionContext != null) {
            sessionContext.setSessionCacheSize(sessionCacheSize);
            sessionContext.setSessionTimeout(sessionCacheTimeout);
        }

        // Enable all ciphers suites that are both supported by this SSLContext and enabled for the SsgConnector
        String desiredCiphersString = c.getProperty(SsgConnector.PROP_TLS_CIPHERLIST);
        String[] desiredCiphers = desiredCiphersString != null && desiredCiphersString.trim().length() >= 1
                ? SPLITTER.split(desiredCiphersString)
                : sslServerSocketFactory.getDefaultCipherSuites();
        enabledCiphers = ArrayUtils.intersection(desiredCiphers, sslServerSocketFactory.getSupportedCipherSuites());
        allowUnsafeLegacyRenegotiation = Boolean.valueOf(getStringProperty(SsgConnector.PROP_TLS_ALLOW_UNSAFE_LEGACY_RENEGOTIATION, "false"));

        try {
            // Detect early if accepts will always fail (Bug #7553)
            enabledTlsVersions = getEnabledTlsVersionsAndCheckSocketConfig(sslServerSocketFactory, desiredTlsVersionsSet, enabledCiphers, ssgConnector.getClientAuth());
        } catch (IOException e) {
            throw new ListenerException("Unable to open listen port with the specified SSL configuration: " + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * Get access to the SSL context.
     *
     * @return the SSLContext.  Never null.
     */
    public SSLContext getSslContext() {
        return sslContext;
    }

    /**
     * Get access to the SSL server socket factory.
     *
     * @return the server socket factory.  Never null.
     */
    public SSLServerSocketFactory getSslServerSocketFactory() {
        return sslServerSocketFactory;
    }

    /**
     * Get access to the SSL socket factory.
     *
     * @return the socket factory.
     */
    public SSLSocketFactory getSocketFactory() {
        return sslContext.getSocketFactory();
    }

    /**
     * Configure a server socket created by our server socket factory according to the current configuration.
     *
     * @param sslServerSocket the socket to configure.  Required.
     */
    public void configureServerSocket(SSLServerSocket sslServerSocket) {
        configureServerSocket(sslServerSocket, enabledTlsVersions, enabledCiphers, ssgConnector.getClientAuth());
    }

    /**
     * Configure an SSLSocket created by our socket factory according to the current configuration.
     *
     * @param socket the SSLSocket to configure.  Required.
     * @param clientMode true if this socket should play the client role during the SSL handshake.
     */
    public void configureSocket(SSLSocket socket, boolean clientMode) {
        socket.setUseClientMode(clientMode);
        socket.setEnabledCipherSuites(enabledCiphers);
        socket.setEnabledProtocols(enabledTlsVersions);

        // The listen port's client auth setting only applies if we will use the server role during the handshake
        if (!clientMode) {
            final int clientAuth = ssgConnector.getClientAuth();
            if (SsgConnector.CLIENT_AUTH_OPTIONAL == clientAuth) {
                socket.setWantClientAuth(true);
            } else {
                socket.setNeedClientAuth(SsgConnector.CLIENT_AUTH_NEVER != clientAuth);
            }
        }
    }

    /**
     * Start the SSL handshake on the specified SSLSocket.
     * <p/>
     * This will also implement the CVE-2009-3555 hack unless allowUnsafeLegacyRenegotiation is true.
     *
     * @param sock the socket to configure.
     * @throws IOException on network level error
     */
    public void startHandshake(SSLSocket sock) throws IOException {
        sock.startHandshake();

        if (!allowUnsafeLegacyRenegotiation) {
            // CVE-2009-3555 - Remove all cipher suites to prevent any SSL renegotiations from succeeding.
            // This work-around is taken from Tomcat, present as of Tomcat 6.0.21
            sock.setEnabledCipherSuites(new String[0]);
        }
    }

    /**
     * Utility method to find the TLS protocol to pass to SSLContext.getInstance(String) for a given connector.
     *
     * @param ssgConnector  the connector about which to inquire.  Required.
     * @return the SSLContext protocol to use.  Never null or empty.
     */
    public static String getTlsProtocol(SsgConnector ssgConnector) {
        String tlsProtocol = ssgConnector.getProperty(SsgConnector.PROP_TLS_PROTOCOL);
        if (tlsProtocol == null || tlsProtocol.trim().length() < 1)
            tlsProtocol = "TLS";
        return tlsProtocol;
    }

    //
    // Private
    //

    private static SSLContext createSslContext(SsgConnector ssgConnector, Set<String> desiredTlsVersions) throws ListenerException, NoSuchProviderException, NoSuchAlgorithmException {

        String tlsProtocol = getTlsProtocol(ssgConnector);
        String customTlsProvider = ssgConnector.getProperty(SsgConnector.PROP_TLS_PROTOCOL_PROVIDER);

        if (customTlsProvider != null) {
            // Use the manually-specified provider.
            logger.log(Level.FINE, "Attempting to create SSLContext using custom provider named " + customTlsProvider);
            return SSLContext.getInstance(tlsProtocol, customTlsProvider);
        }

        // Auto-select a TLS provider depending on whether TLS 1.1 or TLS 1.2 is enabled.
        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Connector " + ssgConnector.getOid() + " TLS versions: " + desiredTlsVersions);

        String sslContextService =
                desiredTlsVersions != null && (desiredTlsVersions.contains("TLSv1.1") || desiredTlsVersions.contains("TLSv1.2"))
                        ? JceProvider.SERVICE_TLS12
                        : JceProvider.SERVICE_TLS10;

        Provider provider = JceProvider.getInstance().getProviderFor(sslContextService);
        if (provider == null) {
            logger.log(Level.FINE, "Attempting to create SSLContext using default provider");
            return SSLContext.getInstance(tlsProtocol);
        }

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Attempting to create SSLContext using " + sslContextService + " provider named " + provider.getName());
        return SSLContext.getInstance(tlsProtocol, provider);
    }

    /**
     * Get a connector property with a default.
     *
     * @param name the property name.  Required.
     * @param defaultValue the value to return if the property is null.  May be null.
     * @return the property value or default.  Never null as long as defaultValue is non-null.
     */
    protected String getStringProperty(String name, String defaultValue) {
        String value = ssgConnector.getProperty(name);
        return value != null ? value : defaultValue;
    }

    /**
     * Get a connector property as an integer with a default value.
     *
     * @param name the property name.  Required.
     * @param defaultValue the value to return if the property is not set, or is not a valid integer.
     * @return the property value or default.
     */
    protected int getIntProperty(String name, int defaultValue) {
        try {
            return Integer.parseInt(getStringProperty(name, Integer.toString(defaultValue)));
        } catch (NumberFormatException nfe) {
            logger.warning("Connector property must be numeric for connector oid " + ssgConnector.getOid() + ": " + name);
            return defaultValue;
        }
    }

    private static String[] getEnabledTlsVersionsAndCheckSocketConfig(SSLServerSocketFactory sslServerSocketFactory,
                                                                        Set<String> desiredTlsVersions,
                                                                        String[] enabledCiphers,
                                                                        int clientAuth)
            throws IOException
    {
        // Ensures that accept is not going to throw Tomcat into an infinite loop of stack traces
        // due to incompatible cipher suites.  Layer 7 bug #7553, Tomcat bug 45528.
        // This code is originally from Tomcat, present as of Tomcat 6.0.19.
        SSLServerSocket socket = (SSLServerSocket)sslServerSocketFactory.createServerSocket();
        final String[] enabledTlsVersions = ArrayUtils.intersection(desiredTlsVersions.toArray(new String[desiredTlsVersions.size()]), socket.getSupportedProtocols());
        configureServerSocket(socket, enabledTlsVersions, enabledCiphers, clientAuth);

        try {
            socket.setSoTimeout(1);
            socket.accept();
            /* NEVER REACHED */
        } catch (SSLException e) {
            throw new IOException(e);
        } catch (Exception e) {
            // Ignore it; it's either the expected SocketTimeoutException, or else something that will recur and
            // can be reported later, when the real server socket is later created.
        } finally {
            if (!socket.isClosed())
                socket.close();
        }

        return enabledTlsVersions;
    }

    private static void configureServerSocket(SSLServerSocket socket, String[] enabledTlsVersions, String[] enabledCiphers, int clientAuth) {
        socket.setEnabledCipherSuites(enabledCiphers);
        socket.setEnabledProtocols(enabledTlsVersions);
        if (SsgConnector.CLIENT_AUTH_OPTIONAL == clientAuth) {
            socket.setWantClientAuth(true);
        } else {
            socket.setNeedClientAuth(SsgConnector.CLIENT_AUTH_NEVER != clientAuth);
        }
    }

    protected KeyManager[] getKeyManagers(SsgKeyEntry keyEntry) throws UnrecoverableKeyException {
        return new KeyManager[] { new SingleCertX509KeyManager(keyEntry.getCertificateChain(), keyEntry.getPrivateKey()) };
    }

    protected TrustManager[] getTrustManagers() {
        X509TrustManager tm;

        String overriddenIssuers = ClientTrustingTrustManager.getConfiguredIssuerCerts(transportModule.getServerConfig());
        if (overriddenIssuers != null) {
            // Use globally-overridden issuers list, even if it is empty.
            tm = new ClientTrustingTrustManager(ClientTrustingTrustManager.parseIssuerCerts(overriddenIssuers));
        } else {
            // TODO one second is probably much too short given the potential cost of looking up all the certs,
            // but much longer and XVC will not be able to connect immediately after obtaining a client cert automatically
            long updateInterval = Long.getLong(SYSPROP_ACCEPTED_ISSUERS_CACHE_TIMEOUT, 1000L);
            tm = new ClientTrustingTrustManager(new CachedCallable<X509Certificate[]>(updateInterval, new Callable<X509Certificate[]>() {
                    @Override
                    public X509Certificate[] call() throws Exception {
                        return CertUtils.deduplicate(transportModule.getAcceptedIssuersForConnector(ssgConnector));
                    }
            }));
        }

        // Detect errors early on
        tm.getAcceptedIssuers();

        return new TrustManager[] { tm };
    }

    protected Set<String> getDesiredTlsVersions(SsgConnector c) {
        // Allow the admin to micro-manage the protocols directly, bypassing the somewhat-inflexible GUI
        String overrideProtocols = c.getProperty(SsgConnector.PROP_TLS_OVERRIDE_PROTOCOLS);
        if (overrideProtocols != null && overrideProtocols.trim().length() > 0)
            return new HashSet<String>(Arrays.asList(SPLITTER.split(overrideProtocols)));

        String protocols = c.getProperty(SsgConnector.PROP_TLS_PROTOCOLS);
        if (protocols == null) protocols = "TLSv1";

        Set<String> protos = new HashSet<String>(Arrays.asList(SPLITTER.split(protocols.trim())));
        if ((protos.contains("SSLv3") || protos.contains("TLSv1")) && !protos.contains("SSLv2Hello")) {
            // Always allow clients to use SSL 2.0 Client Hello encapsulation with SSL 3.0 and TLS 1.0,
            // unless it is explicitly disabled using an advanced setting
            if (!Boolean.valueOf(c.getProperty(SsgConnector.PROP_TLS_NO_SSLV2_HELLO)))
                protos.add("SSLv2Hello");
        }

        return protos;
    }
}
