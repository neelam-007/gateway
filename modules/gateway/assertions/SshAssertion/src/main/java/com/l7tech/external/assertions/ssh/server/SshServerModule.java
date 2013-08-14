package com.l7tech.external.assertions.ssh.server;

import com.l7tech.external.assertions.ssh.SshCredentialAssertion;
import com.l7tech.external.assertions.ssh.SshRouteAssertion;
import com.l7tech.external.assertions.ssh.server.cipher.AES128CTR;
import com.l7tech.external.assertions.ssh.server.cipher.AES192CTR;
import com.l7tech.external.assertions.ssh.server.cipher.AES256CTR;
import com.l7tech.external.assertions.ssh.server.keyprovider.PemSshHostKeyProvider;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.TransportDescriptor;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.keys.PemUtils;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.GatewayState;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.server.transport.TransportModule;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.InjectingConstructor;
import com.l7tech.server.util.Injector;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.*;
import com.l7tech.util.Functions.Unary;
import org.apache.commons.lang.StringUtils;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.sshd.SshServer;
import org.apache.sshd.client.future.DefaultOpenFuture;
import org.apache.sshd.client.future.OpenFuture;
import org.apache.sshd.common.*;
import org.apache.sshd.common.Session.AttributeKey;
import org.apache.sshd.common.cipher.*;
import org.apache.sshd.common.compression.CompressionDelayedZlib;
import org.apache.sshd.common.compression.CompressionNone;
import org.apache.sshd.common.compression.CompressionZlib;
import org.apache.sshd.common.future.CloseFuture;
import org.apache.sshd.common.future.SshFutureListener;
import org.apache.sshd.common.mac.HMACSHA1;
import org.apache.sshd.common.mac.HMACSHA196;
import org.apache.sshd.common.random.JceRandom;
import org.apache.sshd.common.random.SingletonRandomFactory;
import org.apache.sshd.common.session.AbstractSession;
import org.apache.sshd.common.signature.SignatureDSA;
import org.apache.sshd.common.signature.SignatureRSA;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.common.util.SecurityUtils;
import org.apache.sshd.server.*;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.channel.OpenChannelException;
import org.apache.sshd.server.kex.DHG1;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.session.SessionFactory;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.l7tech.gateway.common.Component.GW_SSHRECV;
import static com.l7tech.server.GatewayFeatureSets.SERVICE_SSH_MESSAGE_INPUT;
import static com.l7tech.util.CollectionUtils.caseInsensitiveSet;
import static com.l7tech.util.CollectionUtils.list;
import static com.l7tech.util.Either.left;
import static com.l7tech.util.Either.right;
import static com.l7tech.util.Eithers.lefts;
import static com.l7tech.util.Eithers.rights;
import static com.l7tech.util.ExceptionUtils.getDebugException;
import static com.l7tech.util.ExceptionUtils.getMessage;
import static com.l7tech.util.Functions.grep;
import static com.l7tech.util.Functions.map;
import static com.l7tech.util.Option.optional;
import static com.l7tech.util.Option.some;
import static com.l7tech.util.TextUtils.isNotEmpty;
import static com.l7tech.util.TextUtils.split;
import static java.util.Collections.singletonList;

/**
 * Creates and controls an embedded SSH server for each configured SsgConnector with a SSH scheme.
 * Supports SCP and SFTP over SSH2.
 */
public class SshServerModule extends TransportModule implements ApplicationListener {
    // Listen port properties
    static final String LISTEN_PROP_AUTHORIZED_USER_PASSWORD_LIST = "l7.ssh.authorizedUserPasswordList";
    static final String LISTEN_PROP_AUTHORIZED_USER_PUBLIC_KEY_LIST = "l7.ssh.authorizedUserPublicKeyList";
    static final String LISTEN_PROP_ENABLED_CIPHER_LIST = "l7.ssh.enabledCipherList";
    static final String LISTEN_PROP_IS_PASSWORD_CREDENTIAL_FORBIDDEN = "l7.ssh.isPasswordCredentialForbidden";
    static final String LISTEN_PROP_IS_PUBLIC_KEY_CREDENTIAL_REQUIRED = "l7.ssh.isPublicKeyCredentialRequired";
    static final String LISTEN_PROP_MESSAGE_PROCESSOR_THREAD_WAIT_SECONDS = "l7.ssh.messageProcessorThreadWaitSeconds";
    static final String LISTEN_PROP_ACCEPT_BACLKOG = "l7.ssh.acceptBacklog";
    static final String LISTEN_PROP_MAX_CHANNELS = "l7.ssh.maxChannels";
    static final String LISTEN_PROP_SFTP_MAX_READ_BUFFER_SIZE = "l7.ssh.sftp.maxReadBufferSize";
    static final String LISTEN_PROP_SFTP_MAX_WRITE_BUFFER_SIZE = "l7.ssh.sftp.maxWriteBufferSize";
    static final String LISTEN_PROP_SFTP_MAX_WRITE_TIME_MILLIS = "l7.ssh.sftp.maxWriteTime";
    static final String LISTEN_PROP_SFTP_MAX_READ_TIME_MILLIS = "l7.ssh.sftp.maxReadTime";

    // Session properties
    static final AttributeKey<Option<String>> MINA_SESSION_ATTR_CRED_USERNAME = new AttributeKey<Option<String>>();
    static final AttributeKey<Option<String>> MINA_SESSION_ATTR_CRED_PASSWORD = new AttributeKey<Option<String>>();
    static final AttributeKey<Option<String>> MINA_SESSION_ATTR_CRED_PUBLIC_KEY = new AttributeKey<Option<String>>();
    static final AttributeKey<AtomicInteger> MINA_SESSION_ATTR_CHANNEL_COUNT = new AttributeKey<AtomicInteger>();

    private static final Logger LOGGER = Logger.getLogger(SshServerModule.class.getName());
    protected static final String SCHEME_SSH = "SSH2";
    private static final Pattern SPLIT_DELIMITER = Pattern.compile("<split>");
    private static final Set<String> SUPPORTED_SCHEMES = caseInsensitiveSet( SCHEME_SSH );
    private static final String SSHD_PROPERTY_PREFIX = "sshd.";
    private static final String JSCAPE_KEX_PROVIDER = "jscape.kex.provider";

    // Configuration defaults
    private static final String DEFAULT_SERVER_VERSION = "GatewaySSH-1.0";
    private static final String DEFAULT_AUTH_TIMEOUT_MILLIS = Integer.toString(30000);
    private static final String DEFAULT_MAX_AUTH_ATTEMPTS = Integer.toString(3);
    private static final String DEFAULT_MAX_OPEN_FILES = Integer.toString(1);
    private static final int DEFAULT_ACCEPT_BACKLOG = 50;
    private static final int DEFAULT_MAX_SESSIONS = 10;
    private static final int DEFAULT_MAX_CHANNELS = 1;

    private enum SupportedCipher {
        AES128CTR(new AES128CTR.Factory()),
        AES128CBC(new AES128CBC.Factory()),
        TripleDESCBC(new TripleDESCBC.Factory()),
        BlowfishCBC(new BlowfishCBC.Factory()),
        AES192CTR(new AES192CTR.Factory()),
        AES192CBC(new AES192CBC.Factory()),
        AES256CTR(new AES256CTR.Factory()),
        AES256CBC(new AES256CBC.Factory());

        private final NamedFactory<Cipher> factory;

        private SupportedCipher( final NamedFactory<Cipher> factory ) {
            this.factory = factory;
        }

        NamedFactory<Cipher> getFactory() {
            return factory;
        }
    }

    private final ApplicationEventProxy applicationEventProxy;
    private final GatewayState gatewayState;
    private final SecurePasswordManager securePasswordManager;
    private final ThreadPoolBean sftpMessageProcessingThreadPool;
    private final ThreadPoolBean sshResponseDownloadThreadPool;
    private final Injector injector;
    private final Map<Goid, Pair<SsgConnector, SshServer>> activeConnectors = new ConcurrentHashMap<Goid, Pair<SsgConnector, SshServer>>();

    @Inject
    public SshServerModule( final ApplicationEventProxy applicationEventProxy,
                            final LicenseManager licenseManager,
                            final SsgConnectorManager ssgConnectorManager,
                            final TrustedCertServices trustedCertServices,
                            final DefaultKey defaultKey,
                            final Config config,
                            final GatewayState gatewayState,
                            final SecurePasswordManager securePasswordManager,
                            @Named("sftpMessageProcessingThreadPool") final ThreadPoolBean sftpMessageProcessingThreadPool,
                            @Named("sshResponseDownloadThreadPool") final ThreadPoolBean sshResponseDownloadThreadPool,
                            final Injector injector,
                            final ApplicationContext applicationContext )
    {
        super("SSH server module", GW_SSHRECV, LOGGER, SERVICE_SSH_MESSAGE_INPUT, licenseManager, ssgConnectorManager, trustedCertServices, defaultKey, config );
        this.applicationEventProxy = applicationEventProxy;
        this.gatewayState = gatewayState;
        this.securePasswordManager = securePasswordManager;
        this.sftpMessageProcessingThreadPool = sftpMessageProcessingThreadPool;
        this.sshResponseDownloadThreadPool = sshResponseDownloadThreadPool;
        this.injector = injector;
        setApplicationContext( applicationContext );
    }

    static SshServerModule createModule( final ApplicationContext appContext ) {
        if (SyspropUtil.getString(JSCAPE_KEX_PROVIDER, null) == null) {
            Provider prov = JceProvider.getInstance().getProviderFor(JceProvider.SERVICE_DIFFIE_HELLMAN_SOFTWARE);
            if (prov != null) {
                SyspropUtil.setProperty(JSCAPE_KEX_PROVIDER, prov.getName());
            }
        }
        
        final InjectingConstructor injector = appContext.getBean( InjectingConstructor.class );
        return injector.injectNew( SshServerModule.class );
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        super.onApplicationEvent( applicationEvent );

        if (applicationEvent instanceof ReadyForMessages && activeConnectors.isEmpty()) {
            try {
                startInitialConnectors();
            } catch (FindException e) {
                auditError( SCHEME_SSH, "Unable to access initial SSH connectors: " + getMessage( e ), e );
            }
        }
    }

    @Override
    public void reportMisconfiguredConnector(Goid connectorGoid) {
        // Ignore, can't currently happen for SSH
        logger.log(Level.WARNING, "SSH connector reported misconfigured: OID " + connectorGoid);
    }

    private void startInitialConnectors() throws FindException {
        final boolean wasSystem = AuditContextUtils.isSystem();
        try {
            AuditContextUtils.setSystem(true);
            Collection<SsgConnector> connectors = ssgConnectorManager.findAll();
            for (SsgConnector connector : connectors) {
                if (connector.isEnabled() && connectorIsOwnedByThisModule(connector)) {
                    try {
                        addConnector(connector);
                    } catch ( Exception e ) {
                        auditError( SCHEME_SSH, "Unable to start " + connector.getScheme() + " connector on port " + connector.getPort() +
                                    ": " + getMessage( e ), getDebugException( e ) );
                    }
                }
            }

        } finally {
            AuditContextUtils.setSystem(wasSystem);
        }
    }

    @Override
    protected void doStart() throws LifecycleException {
        registerCustomProtocols();
        sftpMessageProcessingThreadPool.start();
        sshResponseDownloadThreadPool.start();
        if (gatewayState.isReadyForMessages()) {
            try {
                startInitialConnectors();
            } catch (FindException e) {
                auditError( SCHEME_SSH, "Unable to access initial SSH connectors: " + getMessage( e ), e );
            }
        }
    }

    @Override
    protected void doStop() throws LifecycleException {
        try {
            final List<Goid> oidsToStop = new ArrayList<Goid>(activeConnectors.keySet());
            for ( final Goid goid : oidsToStop) {
                removeConnector(goid);
            }
        }
        catch(Exception e) {
            auditError( SCHEME_SSH, "Error while shutting down.", e);
        }
    }

    private void registerCustomProtocols() {
        final TransportDescriptor ssh = new TransportDescriptor();
        ssh.setScheme(SCHEME_SSH);
        ssh.setSupportsSpecifiedContentType(true);
        ssh.setRequiresSpecifiedContentType(true);
        ssh.setSupportsHardwiredServiceResolution(true);
        ssh.setRequiresHardwiredServiceResolutionForNonXml(true);
        ssh.setRequiresHardwiredServiceResolutionAlways(false);
        ssh.setCustomPropertiesPanelClassname("com.l7tech.external.assertions.ssh.console.SshTransportPropertiesPanel");
        ssh.setModularAssertionClassname(SshRouteAssertion.class.getName());
        ssgConnectorManager.registerTransportProtocol(ssh, this);
    }

    @Override
    protected void doClose() throws LifecycleException {
        sftpMessageProcessingThreadPool.shutdown();
        sshResponseDownloadThreadPool.shutdown();
        unregisterApplicationEventListenerAndCustomProtocols();
    }

    void unregisterApplicationEventListenerAndCustomProtocols() {
        for (String scheme : SUPPORTED_SCHEMES) {
            ssgConnectorManager.unregisterTransportProtocol(scheme);
        }
        applicationEventProxy.removeApplicationListener(this);
    }

    @Override
    protected boolean isCurrent( Goid goid, int version ) {
        boolean current;

        Pair<SsgConnector, SshServer> entry = activeConnectors.get(goid);
        current = entry != null && entry.left.getVersion()==version;

        return current;
    }

    @Override
    protected void addConnector(SsgConnector connector) throws ListenerException {
        if ( connector.getGoid().equals( SsgConnector.DEFAULT_GOID ))
            throw new ListenerException("Connector must be persistent.");

        if (isCurrent(connector.getGoid(), connector.getVersion()))
            return;

        removeConnector(connector.getGoid());
        if (!connectorIsOwnedByThisModule(connector))
            return;

        connector = connector.getReadOnlyCopy();
        final String scheme = connector.getScheme();
        if (SCHEME_SSH.equalsIgnoreCase(scheme)) {
            addSshConnector(connector);
        } else {
            // Can't happen
            LOGGER.log(Level.WARNING, "ignoring connector with unrecognized scheme " + scheme);
        }
    }

    private void addSshConnector(SsgConnector connector) throws ListenerException {
        if (!isLicensed())
            return;

        // configure and start sshd
        try {
            final SshServer sshd = buildSshServer( connector );
            auditStart( connector.getScheme(), describe( connector ) );
            sshd.start();
            activeConnectors.put(connector.getGoid(), new Pair<SsgConnector, SshServer>(connector, sshd));
        } catch (IllegalArgumentException iae) {
            throw new ListenerException(getMessage( iae ), getDebugException ( iae ));
        } catch (Exception e) {
            auditError( connector.getScheme(), "Error starting connector " + describe( connector ), e);
            throw new ListenerException("Unable to create sshd: " + getMessage( e ), e);
        }
    }

    @Override
    protected void removeConnector(Goid goid) {
        final Pair<SsgConnector, SshServer> entry;
        entry = activeConnectors.remove(goid);
        if (entry == null) return;
        SshServer sshd = entry.right;
        if (sshd != null) {
            try {
                auditStop( entry.left.getScheme(), describe( entry.left ) );
                sshd.stop();
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Unable to remove sshd: " + getMessage( e ), getDebugException( e ));
                auditError( entry.left.getScheme(), "Error while shutting down, unable to remove sshd.", e);
            }
        }
    }

    @Override
    protected Set<String> getSupportedSchemes() {
        return SUPPORTED_SCHEMES;
    }

    /*
     * This method is based on org.apache.sshd.SshServer.setUpDefaultServer(...).
     */
    private SshServer buildSshServer( final SsgConnector connector ) throws ListenerException, FindException, ParseException {
        // customized for Gateway, we don't want Apache's SecurityUtils to explicitly register BouncyCastle, let the Gateway decide
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) != null) {
            SecurityUtils.setRegisterBouncyCastle(true);
            SecurityUtils.getSecurityProvider();
        } else {
            SecurityUtils.setRegisterBouncyCastle(false);
            SecurityUtils.setSecurityProvider(null);
        }

        final SshServer sshd = new SshServer();
        sshd.setKeyExchangeFactories(Arrays.<NamedFactory<KeyExchange>>asList(new DHG1.Factory()));
        sshd.setRandomFactory(new SingletonRandomFactory(new JceRandom.Factory()));

        sshd.setCompressionFactories(Arrays.<NamedFactory<Compression>>asList(
                new CompressionNone.Factory(),
                new CompressionZlib.Factory(),
                new CompressionDelayedZlib.Factory()));

        // removed MD5 & MD596 for Gateway
        sshd.setMacFactories(Arrays.<NamedFactory<Mac>>asList(
                new HMACSHA1.Factory(),
                new HMACSHA196.Factory()));

        final AtomicInteger maxSessions = new AtomicInteger( DEFAULT_MAX_SESSIONS );
        final AtomicInteger maxChannels = new AtomicInteger( DEFAULT_MAX_CHANNELS );
        final AtomicInteger sessionCount = new AtomicInteger( 0 );
        sshd.setSessionFactory( buildResourceLimitingSessionFactory( maxSessions, sessionCount ) );
        sshd.setChannelFactories( singletonList( buildResourceLimitingChannelFactory( maxChannels ) ) );
        sshd.setSignatureFactories(Arrays.<NamedFactory<Signature>>asList(
                new SignatureDSA.Factory(),
                new SignatureRSA.Factory()));
        sshd.setFileSystemFactory(new VirtualFileSystemFactory());   // customized for Gateway

        ForwardingAcceptorFactory faf = new ForwardingAcceptorFactory() {
            @Override
            public NioSocketAcceptor createNioSocketAcceptor(ServerSession serverSession) {
                return null;
            }
        };
        sshd.setTcpipForwardNioSocketAcceptorFactory(faf);
        sshd.setX11ForwardNioSocketAcceptorFactory(faf);

        configureSshServer( sshd, connector, maxSessions, maxChannels );

        return sshd;
    }

    /*
     * Configure SSH server using listener properties
     */
    private void configureSshServer( final SshServer sshd,
                                     final SsgConnector connector,
                                     final AtomicInteger maxSessions,
                                     final AtomicInteger maxChannels ) throws FindException, ListenerException, ParseException {
        final Map<String, String> sshdProperties = sshd.getProperties();

        // configure ciphers to enable
        setUpSshCiphers(sshd, connector);

        // configure authentication
        sshd.setPublickeyAuthenticator(configurePublicKeyAuthentication(connector));
        sshd.setPasswordAuthenticator(configurePasswordAuthentication(connector));

        // enable SCP, SFTP
        if (connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SCP)) {
            sshd.setCommandFactory(configureMessageProcessingScpCommand(connector));
        }
        if (connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP)) {
            sshd.setSubsystemFactories(singletonList(configureMessageProcessingSftpSubsystem( connector )));
        }

        // set host and port
        final String bindAddressProperty = connector.getProperty(SsgConnector.PROP_BIND_ADDRESS);
        final String bindAddress;
        if ( !InetAddressUtil.isAnyHostAddress(bindAddressProperty) ) {
            bindAddress = ssgConnectorManager.translateBindAddress(bindAddressProperty, connector.getPort());
        } else {
            bindAddress = InetAddressUtil.getAnyHostAddress();
        }
        sshd.setHost(bindAddress);
        sshd.setPort( connector.getPort() );
        sshd.setBacklog( connector.getIntProperty( LISTEN_PROP_ACCEPT_BACLKOG, DEFAULT_ACCEPT_BACKLOG ) );

        // set server host private key
        final Goid hostPrivateKeyGoid = GoidUpgradeMapper.mapId(EntityType.SECURE_PASSWORD, connector.getProperty(SshCredentialAssertion.LISTEN_PROP_HOST_PRIVATE_KEY));
        final SecurePassword securePassword = securePasswordManager.findByPrimaryKey(hostPrivateKeyGoid != null ? hostPrivateKeyGoid : SecurePassword.DEFAULT_GOID);
        if (securePassword != null) {
            final String encryptedHostPrivateKey = securePassword.getEncodedPassword();
            final char[] hostPrivateKey = securePasswordManager.decryptPassword(encryptedHostPrivateKey);
            sshd.setKeyPairProvider(new PemSshHostKeyProvider(String.valueOf(hostPrivateKey)));
        } else {
            LOGGER.log(Level.WARNING, "Unable to find private key GOID: " + hostPrivateKeyGoid + ".  KeyPairProvider not set.");
        }

        // configure connection idle timeout in ms (min=60sec*1000ms)
        final Long idleTimeoutMins = connector.getLongProperty( SshCredentialAssertion.LISTEN_PROP_IDLE_TIMEOUT_MINUTES, 0L );
        if ( idleTimeoutMins > 0L ) {
            final long idleTimeoutMs = TimeUnit.MINUTES.toMillis( idleTimeoutMins );
            sshdProperties.put( SshServer.IDLE_TIMEOUT, String.valueOf( idleTimeoutMs ) );
        }

        // configure resource limits
        final int maxConcurrentSessionsPerUser = connector.getIntProperty( SshCredentialAssertion.LISTEN_PROP_MAX_CONCURRENT_SESSIONS_PER_USER, 0 );
        if (maxConcurrentSessionsPerUser > 0) {
            sshdProperties.put( SshServer.MAX_CONCURRENT_SESSIONS, Integer.toString(maxConcurrentSessionsPerUser) );
        }
        maxSessions.set( connector.getIntProperty( SshCredentialAssertion.LISTEN_PROP_MAX_SESSIONS, maxSessions.get() ) );
        maxSessions.compareAndSet( 0, Integer.MAX_VALUE ); // zero for unlimited
        maxChannels.set( connector.getIntProperty( LISTEN_PROP_MAX_CHANNELS, maxChannels.get() ) );
        maxChannels.compareAndSet( 0, Integer.MAX_VALUE ); // zero for unlimited

        // These defaults can be overridden by advanced properties on the connector (prefixed with "sshd.")
        sshdProperties.put( ServerFactoryManager.SERVER_IDENTIFICATION, DEFAULT_SERVER_VERSION );
        sshdProperties.put( ServerFactoryManager.AUTH_TIMEOUT, DEFAULT_AUTH_TIMEOUT_MILLIS );
        sshdProperties.put( ServerFactoryManager.MAX_AUTH_REQUESTS, DEFAULT_MAX_AUTH_ATTEMPTS );
        sshdProperties.put( SftpSubsystem.MAX_OPEN_HANDLES_PER_SESSION, DEFAULT_MAX_OPEN_FILES );
        for ( final String property : connector.getPropertyNames() ) {
            if ( property.startsWith( SSHD_PROPERTY_PREFIX ) && property.length() > SSHD_PROPERTY_PREFIX.length()+1 ) {
                sshdProperties.put(
                        property.substring( SSHD_PROPERTY_PREFIX.length() ),
                        connector.getProperty( property ) );
            }
        }
    }

    /*
     * This method is based on org.apache.sshd.SshServer.setUpDefaultCiphers(...)
     */
    private void setUpSshCiphers(SshServer sshd, final SsgConnector connector) {
        // If enabledCiphers list exists, enable accordingly, otherwise enable all supported ciphers
        final List<Either<String,SupportedCipher>> configuredCiphers =
                map( getListProperty( connector, LISTEN_PROP_ENABLED_CIPHER_LIST ), supportedCipher() );
        final Collection<SupportedCipher> ciphers;
        if ( configuredCiphers.isEmpty() ) {
            ciphers = EnumSet.allOf( SupportedCipher.class );
        }  else {
            ciphers = rights( configuredCiphers );
            final Collection<String> invalidCiphers = lefts( configuredCiphers );
            if ( !invalidCiphers.isEmpty() ) {
                 LOGGER.log(Level.WARNING, "Ignoring unrecognized ciphers " + invalidCiphers);
            }
        }

        final List<NamedFactory<Cipher>> namedFactoryCipherList = map( ciphers, factory() );
        if ( !namedFactoryCipherList.isEmpty() ) {
            for ( final Iterator<NamedFactory<Cipher>> i = namedFactoryCipherList.iterator(); i.hasNext();) {
                final NamedFactory<Cipher> f = i.next();
                try {
                    final Cipher c = f.create();
                    final byte[] key = new byte[c.getBlockSize()];
                    final byte[] iv = new byte[c.getIVSize()];
                    c.init(Cipher.Mode.Encrypt, key, iv);
                } catch (InvalidKeyException e) {
                    i.remove();
                } catch (Exception e) {
                    i.remove();
                }
            }
            sshd.setCipherFactories(namedFactoryCipherList);
        }
    }

    private Collection<String> getListProperty( final SsgConnector connector, final String property ) {
        final List<String> valueList = list(
                optional( connector.getProperty( property ) )
                        .orElse( some( "" ) )
                        .map( split( SPLIT_DELIMITER ) )
                        .some() );
        return grep( valueList, isNotEmpty() );
    }

    private Unary<NamedFactory<Cipher>,SupportedCipher> factory() {
        return new Unary<NamedFactory<Cipher>,SupportedCipher>(){
            @Override
            public NamedFactory<Cipher> call( final SupportedCipher supportedCipher ) {
                return supportedCipher.getFactory();
            }
        };
    }

    private Unary<Either<String,SupportedCipher>,String> supportedCipher() {
        return new Unary<Either<String,SupportedCipher>,String>(){
            @Override
            public Either<String, SupportedCipher> call( final String cipher ) {
                try {
                    return right( SupportedCipher.valueOf( cipher ) );
                } catch (IllegalArgumentException iae) {
                    return left( cipher );
                }
            }
        };
    }

    private static void clearAuthenticationAttributes( final AbstractSession session ) {
        session.setAttribute(MINA_SESSION_ATTR_CRED_USERNAME, Option.<String>none());
        session.setAttribute(MINA_SESSION_ATTR_CRED_PASSWORD, Option.<String>none());
        session.setAttribute(MINA_SESSION_ATTR_CRED_PUBLIC_KEY, Option.<String>none());
    }

    /*
     * public key authentication logic
     */
    private PublickeyAuthenticator configurePublicKeyAuthentication(final SsgConnector connector) {
        final Collection<String> authorizedUserPublicKeys = getListProperty( connector, LISTEN_PROP_AUTHORIZED_USER_PUBLIC_KEY_LIST );
        return new PublickeyAuthenticator() {
                /**
                 * Note that this method can be called prior to key validation
                 */
                @Override
                public boolean authenticate(String userName, PublicKey publicKey, ServerSession session) {
                    // if public key authentication is required and public key is empty, don't allow access
                    if (connector.getBooleanProperty(LISTEN_PROP_IS_PUBLIC_KEY_CREDENTIAL_REQUIRED) &&
                            (publicKey == null || StringUtils.isEmpty(publicKey.toString()))) {
                        return false;
                    }

                    // by default allow all access, defer authentication to Gateway policy assertion
                    boolean isAllowedAccess = true;

                    // if authorizedUserPublicKeys list exists, perform authentication against it
                    if ( !authorizedUserPublicKeys.isEmpty() && publicKey != null) {
                        isAllowedAccess = false;
                        final String publicKeyString = PemUtils.writeKey( publicKey );
                        if (!StringUtils.isEmpty(publicKeyString)) {
                            isAllowedAccess = authorizedUserPublicKeys.contains( publicKeyString );
                        }
                    }

                    // authenticating with public key, remove password (if any)
                    clearAuthenticationAttributes( session );

                    if ( isAllowedAccess ) {
                        final String publicKeyStr = PemUtils.writeKey( publicKey );
                        session.setAttribute( MINA_SESSION_ATTR_CRED_USERNAME, optional(userName) );
                        session.setAttribute( MINA_SESSION_ATTR_CRED_PUBLIC_KEY, optional(publicKeyStr) );
                    }

                    return isAllowedAccess;
                }
            };
    }

    /*
     * password authentication logic
     */
    private PasswordAuthenticator configurePasswordAuthentication(final SsgConnector connector) {
        final Collection<String> authorizedUserPasswords = getListProperty( connector, LISTEN_PROP_AUTHORIZED_USER_PASSWORD_LIST );
        return new PasswordAuthenticator() {
                @Override
                public boolean authenticate(String userName, String password, ServerSession session) {
                    // if password authentication is forbidden or public key authentication is required, don't allow access
                    if (connector.getBooleanProperty(LISTEN_PROP_IS_PASSWORD_CREDENTIAL_FORBIDDEN)
                            || connector.getBooleanProperty(LISTEN_PROP_IS_PUBLIC_KEY_CREDENTIAL_REQUIRED)) {
                        return false;
                    }

                    // by default allow all access, defer authentication to Gateway policy assertion
                    // if authorizedUserPasswords list is not empty, perform authentication against it
                    final boolean isAllowedAccess =
                            authorizedUserPasswords.isEmpty() ||
                            authorizedUserPasswords.contains( password );

                    // authenticating with password, remove public key (if any)
                    clearAuthenticationAttributes( session );

                    if ( isAllowedAccess ) {
                        session.setAttribute(MINA_SESSION_ATTR_CRED_USERNAME, optional(userName));
                        session.setAttribute(MINA_SESSION_ATTR_CRED_PASSWORD, optional(password));
                    }
                    return isAllowedAccess;
                }
            };
    }

    /*
     * configure SCP
     */
    private CommandFactory configureMessageProcessingScpCommand(final SsgConnector connector) {
        return new CommandFactory() {
            /**
             * Parses a command string and verifies that the basic syntax is
             * correct. If parsing fails the responsibility is delegated to
             * the configured {@link org.apache.sshd.server.CommandFactory} instance; if one exist.
             *
             * @param command command to parse
             * @return configured {@link org.apache.sshd.server.Command} instance
             * @throws IllegalArgumentException
             */
            @Override
            public Command createCommand(String command) {
                final MessageProcessingScpCommand scpCommand = new MessageProcessingScpCommand( splitCommandString(command), connector );
                injector.inject( scpCommand );
                return scpCommand;
            }

            private String[] splitCommandString(String command) {
                if (!command.trim().startsWith("scp")) {
                    throw new IllegalArgumentException("Unknown command, does not begin with 'scp'");
                }

                String[] args = command.split(" ");
                List<String> parts = new ArrayList<String>();
                parts.add(args[0]);
                for (int i = 1; i < args.length; i++) {
                    if (!args[i].trim().startsWith("-")) {
                        parts.add(concatenateWithSpace(args, i));
                        break;
                    } else {
                        parts.add(args[i]);
                    }
                }
                return parts.toArray(new String[parts.size()]);
            }

            private String concatenateWithSpace(String[] args, int from) {
                StringBuilder sb = new StringBuilder();

                for (int i = from; i < args.length; i++) {
                    sb.append( args[i] ).append( " " );
                }
                return sb.toString().trim();
            }
        };
    }

    /*
     * configure SFTP
     */
    private NamedFactory<Command> configureMessageProcessingSftpSubsystem(final SsgConnector connector) {
        return new NamedFactory<Command>(){
            @Override
            public Command create() {
                final MessageProcessingSftpSubsystem sftpSubsystem = new MessageProcessingSftpSubsystem(connector);
                injector.inject( sftpSubsystem );
                return sftpSubsystem;
            }

            @Override
            public String getName() {
                return "sftp";
            }
        };
    }

    /**
     * Builds a session factory with a listener to track the number of active sessions
     * @param maxSessions max sessions
     * @param sessionCount session count
     * @return session factory
     */
    private SessionFactory buildResourceLimitingSessionFactory( final AtomicInteger maxSessions,
                                                                final AtomicInteger sessionCount ) {
        final SessionFactory sessionFactory = new GatewaySshSessionFactory( maxSessions, sessionCount );
        sessionFactory.addListener( new SessionListener() {
            @Override
            public void sessionCreated( final Session session ) {
                sessionCount.incrementAndGet();
            }

            @Override
            public void sessionClosed( final Session session ) {
                sessionCount.decrementAndGet();
            }
        } );
        return sessionFactory;
    }

    /**
     * Builds a factory for channels that limits the number of channels available per session
     * @param maxChannels max channels
     * @return named channel factory
     */
    private NamedFactory<Channel> buildResourceLimitingChannelFactory( final AtomicInteger maxChannels ) {
        return new NamedFactory<Channel>(){
            @Override
            public String getName() {
                return "session";
            }
            @Override
            public Channel create() {
                return new GatewaySshChannelSession( maxChannels );
            }
        };
    }

    /**
     * Session factory that limits the total number of sessions available
     */
    private static class GatewaySshSessionFactory extends SessionFactory {
        private final AtomicInteger maxSessions;
        private final AtomicInteger sessionCount;

        private GatewaySshSessionFactory( final AtomicInteger maxSessions,
                                          final AtomicInteger sessionCount ) {
            this.maxSessions = maxSessions;
            this.sessionCount = sessionCount;
        }

        /**
         * Overridden to immediately close the connection if no more sessions are available.
         */
        @Override
        public void sessionCreated( final IoSession ioSession ) throws Exception {
            if ( sessionCount.get() >= maxSessions.get() ) {
                // Create an abstract session to avoid key exchange messages
                final AbstractSession session = new AbstractSession(server, ioSession){
                    {
                        sendIdentification( "SSH-2.0-" + DEFAULT_SERVER_VERSION );
                    }

                    @Override
                    protected void handleMessage( final Buffer buffer ) throws Exception {
                        // ignore any messages
                    }

                    @Override
                    protected boolean readIdentification( final Buffer buffer ) throws IOException {
                        // no need to read the client identification since we're closing the connection
                        return false;
                    }
                };
                AbstractSession.attachSession(ioSession, session);
                session.disconnect(SshConstants.SSH2_DISCONNECT_TOO_MANY_CONNECTIONS, "Too many connections");
            } else {
                super.sessionCreated( ioSession );
            }
        }

        /**
         * Override to set attributes
         */
        @Override
        protected AbstractSession doCreateSession( final IoSession ioSession ) throws Exception {
            final AbstractSession session = super.doCreateSession( ioSession );
            session.setAttribute( MINA_SESSION_ATTR_CHANNEL_COUNT, new AtomicInteger() );
            clearAuthenticationAttributes( session );
            return session;
        }
    }

    private static class GatewaySshChannelSession extends ChannelSession {
        private final AtomicInteger maxChannels;

        private GatewaySshChannelSession( final AtomicInteger maxChannels ) {
            this.maxChannels = maxChannels;
        }

        @Override
        public OpenFuture open( final int recipient, final int rwsize, final int rmpsize, final Buffer buffer ) {
            final AtomicInteger sessionChannelCount = getSessionChannelCount();
            if ( sessionChannelCount.incrementAndGet() > maxChannels.get() ) {
                DefaultOpenFuture openFuture = new DefaultOpenFuture(this);
                openFuture.setException( new OpenChannelException( SshConstants.SSH_OPEN_RESOURCE_SHORTAGE, "Too many channels") );
                return openFuture;
            } else {
                return super.open( recipient, rwsize, rmpsize, buffer );
            }
        }

        @Override
        protected void addEnvVariable( final String name, final String value ) {
            // Ignore all variables
            if ( log != null ) {
                log.debug( "Ignoring environment variable: " + name );
            }
        }

        @Override
        public CloseFuture close( final boolean immediately ) {
            return super.close( immediately ).addListener( new SshFutureListener<CloseFuture>() {
                @Override
                public void operationComplete( final CloseFuture closeFuture ) {
                    final AtomicInteger sessionChannelCount = getSessionChannelCount();
                    sessionChannelCount.decrementAndGet();
                }
            } );
        }

        private AtomicInteger getSessionChannelCount() {
            final Session session = getSession();
            return session.getAttribute( MINA_SESSION_ATTR_CHANNEL_COUNT );
        }
    }
}
