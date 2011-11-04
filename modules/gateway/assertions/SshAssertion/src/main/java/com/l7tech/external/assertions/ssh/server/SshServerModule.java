package com.l7tech.external.assertions.ssh.server;

import com.l7tech.external.assertions.ssh.SshCredentialAssertion;
import com.l7tech.external.assertions.ssh.SshRouteAssertion;
import com.l7tech.external.assertions.ssh.keyprovider.SshKeyUtil;
import com.l7tech.external.assertions.ssh.server.keyprovider.PemSshHostKeyProvider;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.TransportDescriptor;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.*;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.server.transport.TransportModule;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.util.*;
import static com.l7tech.util.CollectionUtils.caseInsensitiveSet;
import static java.util.Collections.singletonList;
import org.apache.commons.lang.StringUtils;
import org.apache.mina.core.session.IoSession;
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
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.Security;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    // Session properties
    static final AttributeKey<String> MINA_SESSION_ATTR_CRED_USERNAME = new AttributeKey<String>();
    static final AttributeKey<String> MINA_SESSION_ATTR_CRED_PASSWORD = new AttributeKey<String>();
    static final AttributeKey<String> MINA_SESSION_ATTR_CRED_PUBLIC_KEY = new AttributeKey<String>();
    static final AttributeKey<AtomicInteger> MINA_SESSION_ATTR_CHANNEL_COUNT = new AttributeKey<AtomicInteger>();

    private static final Logger LOGGER = Logger.getLogger(SshServerModule.class.getName());
    private static final String SCHEME_SSH = "SSH2";
    private static final String SPLIT_DELIMITER = "<split>";
    private static final Set<String> SUPPORTED_SCHEMES = caseInsensitiveSet( SCHEME_SSH );
    private static final String SSHD_PROPERTY_PREFIX = "sshd.";

    // Configuration defaults
    private static final String DEFAULT_SERVER_VERSION = "GatewaySSH-1.0";
    private static final String DEFAULT_AUTH_TIMEOUT_MILLIS = Integer.toString(30000);
    private static final String DEFAULT_MAX_AUTH_ATTEMPTS = Integer.toString(3);
    private static final String DEFAULT_MAX_OPEN_FILES = Integer.toString(1);
    private static final int DEFAULT_ACCEPT_BACKLOG = 50;
    private static final int DEFAULT_MAX_SESSIONS = 10;
    private static final int DEFAULT_MAX_CHANNELS = 1;

    private enum SupportedCiphers {AES128CBC, TripleDESCBC, BlowfishCBC, AES192CBC, AES256CBC}

    private final ApplicationEventProxy applicationEventProxy;
    private final GatewayState gatewayState;
    private final MessageProcessor messageProcessor;
    private final EventChannel messageProcessingEventChannel;
    private final SoapFaultManager soapFaultManager;
    private final StashManagerFactory stashManagerFactory;
    private final Map<Long, Pair<SsgConnector, SshServer>> activeConnectors = new ConcurrentHashMap<Long, Pair<SsgConnector, SshServer>>();
    private final Audit auditor;
    private final SecurePasswordManager securePasswordManager;

    public SshServerModule(ApplicationEventProxy applicationEventProxy,
                           LicenseManager licenseManager,
                           SsgConnectorManager ssgConnectorManager,
                           TrustedCertServices trustedCertServices,
                           DefaultKey defaultKey,
                           Config config,
                           GatewayState gatewayState,
                           MessageProcessor messageProcessor,
                           StashManagerFactory stashManagerFactory,
                           SoapFaultManager soapFaultManager,
                           EventChannel messageProcessingEventChannel,
                           AuditFactory auditFactory,
                           SecurePasswordManager securePasswordManager)
    {
        super("SSH server module", LOGGER, GatewayFeatureSets.SERVICE_SSH_MESSAGE_INPUT, licenseManager, ssgConnectorManager, trustedCertServices, defaultKey, config );
        this.applicationEventProxy = applicationEventProxy;
        this.gatewayState = gatewayState;
        this.messageProcessor = messageProcessor;
        this.messageProcessingEventChannel = messageProcessingEventChannel;
        this.soapFaultManager = soapFaultManager;
        this.stashManagerFactory = stashManagerFactory;
        this.auditor = auditFactory.newInstance( this, LOGGER);
        this.securePasswordManager = securePasswordManager;
    }

    private static <T> T getBean(BeanFactory beanFactory, String beanName, Class<T> beanClass) {
        T got = beanFactory.getBean(beanName, beanClass);
        if (got != null)
            return got;
        throw new IllegalStateException("Unable to get bean from application context: " + beanName);
    }

    static SshServerModule createModule(ApplicationContext appContext) {
        LicenseManager licenseManager = getBean(appContext, "licenseManager", LicenseManager.class);
        SsgConnectorManager ssgConnectorManager = getBean(appContext, "ssgConnectorManager", SsgConnectorManager.class);
        TrustedCertServices trustedCertServices = getBean(appContext, "trustedCertServices", TrustedCertServices.class);
        DefaultKey defaultKey = getBean(appContext, "defaultKey", DefaultKey.class);
        Config config = getBean(appContext, "serverConfig", ServerConfig.class);
        GatewayState gatewayState = getBean(appContext, "gatewayState", GatewayState.class);
        MessageProcessor messageProcessor = getBean(appContext, "messageProcessor", MessageProcessor.class);
        StashManagerFactory stashManagerFactory = getBean(appContext, "stashManagerFactory", StashManagerFactory.class);
        ApplicationEventProxy applicationEventProxy = getBean(appContext, "applicationEventProxy", ApplicationEventProxy.class);
        SoapFaultManager soapFaultManager = getBean(appContext, "soapFaultManager", SoapFaultManager.class);
        EventChannel messageProcessingEventChannel = getBean(appContext, "messageProcessingEventChannel", EventChannel.class);
        AuditFactory auditFactory = getBean(appContext, "auditFactory", AuditFactory.class);
        SecurePasswordManager securePasswordManager = getBean(appContext,"securePasswordManager", SecurePasswordManager.class);

        return new SshServerModule(applicationEventProxy, licenseManager, ssgConnectorManager, trustedCertServices,
                defaultKey, config, gatewayState, messageProcessor, stashManagerFactory, soapFaultManager,
                messageProcessingEventChannel, auditFactory, securePasswordManager);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        super.onApplicationEvent(applicationEvent);

        if (applicationEvent instanceof ReadyForMessages) {
            try {
                startInitialConnectors();
            } catch (FindException e) {
                LOGGER.log(Level.SEVERE, "Unable to access initial SSH connectors: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }
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
                        if ( ExceptionUtils.getMessage(e).contains("java.net.BindException: ") ) { // The exception cause is not chained ...
                            LOGGER.log(Level.WARNING, "Unable to start " + connector.getScheme() + " connector on port " + connector.getPort() +
                                        ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                        } else {
                            LOGGER.log(Level.WARNING, "Unable to start " + connector.getScheme() + " connector on port " + connector.getPort() +
                                        ": " + ExceptionUtils.getMessage(e), e);
                        }
                    }
                }
            }

        } finally {
            AuditContextUtils.setSystem(wasSystem);
        }
    }

    @Override
    protected void doStart() throws LifecycleException {
        super.doStart();
        registerCustomProtocols();
        if (gatewayState.isReadyForMessages()) {
            try {
                startInitialConnectors();
            } catch (FindException e) {
                LOGGER.log(Level.SEVERE, "Unable to access initial SSH connectors: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }
    }

    void registerApplicationEventListener() {
        applicationEventProxy.addApplicationListener(this);
    }

    private void registerCustomProtocols() {
        TransportDescriptor ssh = new TransportDescriptor();
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
        super.doClose();
        unregisterApplicationEventListenerAndCustomProtocols();
    }

    void unregisterApplicationEventListenerAndCustomProtocols() {
        for (String scheme : SUPPORTED_SCHEMES) {
            ssgConnectorManager.unregisterTransportProtocol(scheme);
        }
        applicationEventProxy.removeApplicationListener(this);
    }

    @Override
    protected boolean isCurrent( long oid, int version ) {
        boolean current;

        Pair<SsgConnector, SshServer> entry = activeConnectors.get(oid);
        current = entry != null && entry.left.getVersion()==version;

        return current;
    }

    @Override
    protected void addConnector(SsgConnector connector) throws ListenerException {
        if ( connector.getOid() == SsgConnector.DEFAULT_OID )
            throw new ListenerException("Connector must be persistent.");

        if (isCurrent(connector.getOid(), connector.getVersion()))
            return;

        removeConnector(connector.getOid());
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
            auditStart("connector OID " + connector.getOid() + ", on port " + connector.getPort());
            sshd.start();
            activeConnectors.put(connector.getOid(), new Pair<SsgConnector, SshServer>(connector, sshd));
        } catch (Exception e) {
            auditError("Error during startup, unable to create sshd.", e);
            throw new ListenerException("Unable to create sshd: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    protected void removeConnector(long oid) {
        final Pair<SsgConnector, SshServer> entry;
        entry = activeConnectors.remove(oid);
        if (entry == null) return;
        SshServer sshd = entry.right;
        if (sshd != null) {
            try {
                auditStop("connector OID " + oid + ", on port " + entry.left.getPort());
                sshd.stop();
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Unable to remove sshd: " + ExceptionUtils.getMessage(e), e);
                auditError("Error while shutting down, unable to remove sshd.", e);
            }
        }
    }

    @Override
    protected Set<String> getSupportedSchemes() {
        return SUPPORTED_SCHEMES;
    }

    private void auditStart(String msg) {
        getAuditor().logAndAudit(SystemMessages.SSH_SERVER_START, msg);
    }

    private void auditStop(String listener) {
        getAuditor().logAndAudit(SystemMessages.SSH_SERVER_STOP, listener);
    }

    private void auditError(String message, Exception exception) {
        getAuditor().logAndAudit(SystemMessages.SSH_SERVER_ERROR, new String[]{message}, exception);
    }

    private Audit getAuditor() {
        return auditor;
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
        final long hostPrivateKeyOid = connector.getLongProperty(SshCredentialAssertion.LISTEN_PROP_HOST_PRIVATE_KEY, SecurePassword.DEFAULT_OID);
        final SecurePassword securePassword = securePasswordManager.findByPrimaryKey(hostPrivateKeyOid);
        if (securePassword != null) {
            final String encryptedHostPrivateKey = securePassword.getEncodedPassword();
            final char[] hostPrivateKey = securePasswordManager.decryptPassword(encryptedHostPrivateKey);
            sshd.setKeyPairProvider(new PemSshHostKeyProvider(String.valueOf(hostPrivateKey)));
        } else {
            LOGGER.log(Level.WARNING, "Unable to find private key OID: " + hostPrivateKeyOid + ".  KeyPairProvider not set.");
        }

        // configure connection idle timeout in ms (min=60sec*1000ms)
        final Long idleTimeoutMins = connector.getLongProperty( SshCredentialAssertion.LISTEN_PROP_IDLE_TIMEOUT_MINUTES, 0L );
        if ( idleTimeoutMins > 0L ) {
            final long idleTimeoutMs = TimeUnit.MINUTES.toMillis( idleTimeoutMins );
            sshdProperties.put( SshServer.IDLE_TIMEOUT, String.valueOf( idleTimeoutMs ) );
        }

        // configure resource limits
        final String maxConcurrentSessionsPerUser = connector.getProperty(SshCredentialAssertion.LISTEN_PROP_MAX_CONCURRENT_SESSIONS_PER_USER);
        if (!StringUtils.isEmpty(maxConcurrentSessionsPerUser)) {
            sshdProperties.put( SshServer.MAX_CONCURRENT_SESSIONS, maxConcurrentSessionsPerUser );
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
        List<NamedFactory<Cipher>> namedFactoryCipherList = new LinkedList<NamedFactory<Cipher>>();

        // if enabledCiphers list exists, enable accordingly, otherwise enable all supported ciphers
        String enabledCipherList = connector.getProperty(LISTEN_PROP_ENABLED_CIPHER_LIST);
        if (enabledCipherList != null) {
            String[] enabledCiphers = enabledCipherList.split(SPLIT_DELIMITER);
             if (enabledCiphers != null && enabledCiphers.length > 0) {
                 for (String enabledCipher : enabledCiphers) {
                     try {
                         namedFactoryCipherList.add(createNamedFactoryCipher(SupportedCiphers.valueOf(enabledCipher)));
                     } catch (IllegalArgumentException iae) {
                         LOGGER.log(Level.WARNING, "Unrecognized cipher: " + ExceptionUtils.getMessage(iae), ExceptionUtils.getDebugException(iae));
                     }
                 }
             }
        } else {
            for (SupportedCiphers enabledCipher : SupportedCiphers.values()) {
                namedFactoryCipherList.add(createNamedFactoryCipher(enabledCipher));
            }
        }

        if (namedFactoryCipherList != null && !namedFactoryCipherList.isEmpty()) {
            for (Iterator<NamedFactory<Cipher>> i = namedFactoryCipherList.iterator(); i.hasNext();) {
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

    private NamedFactory<Cipher> createNamedFactoryCipher(SupportedCiphers cipher) {
        switch(cipher) {
            case AES128CBC:
                return new AES128CBC.Factory();
            case BlowfishCBC:
                return new BlowfishCBC.Factory();
            case TripleDESCBC:
                return new TripleDESCBC.Factory();
            case AES192CBC:
                return new AES192CBC.Factory();
            case AES256CBC:
                return new AES256CBC.Factory();
        }
        throw new IllegalArgumentException(cipher.name());
    }

    /*
     * public key authentication logic
     */
    private PublickeyAuthenticator configurePublicKeyAuthentication(final SsgConnector connector) {
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
                    String authorizedUserPublicKeyList = connector.getProperty(LISTEN_PROP_AUTHORIZED_USER_PUBLIC_KEY_LIST);
                    if (authorizedUserPublicKeyList != null) {
                        String[] authorizedUserPublicKeys = authorizedUserPublicKeyList.split(SPLIT_DELIMITER);
                        if (authorizedUserPublicKeys != null && authorizedUserPublicKeys.length > 0 && publicKey != null) {
                            isAllowedAccess = false;
                            String publicKeyString = SshKeyUtil.writeKey(publicKey);
                            if (!StringUtils.isEmpty(publicKeyString)) {
                                publicKeyString = publicKeyString.replace( SyspropUtil.getProperty("line.separator"), "" );
                                for (String authorizedUserPublicKey : authorizedUserPublicKeys) {
                                    if (publicKeyString.equals(authorizedUserPublicKey)) {
                                        isAllowedAccess = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    // authenticating with public key, remove password (if any)
                    session.getIoSession().removeAttribute(MINA_SESSION_ATTR_CRED_PASSWORD);

                    if (isAllowedAccess) {
                        session.getIoSession().setAttribute(MINA_SESSION_ATTR_CRED_USERNAME, userName);

                        String publicKeyStr = SshKeyUtil.writeKey(publicKey);
                        if (!StringUtils.isEmpty(publicKeyStr)) {
                            publicKeyStr = publicKeyStr.replace(SyspropUtil.getProperty("line.separator"), "");
                        }
                        session.getIoSession().setAttribute(MINA_SESSION_ATTR_CRED_PUBLIC_KEY, publicKeyStr);
                    } else {
                        session.getIoSession().removeAttribute(MINA_SESSION_ATTR_CRED_USERNAME);
                        session.getIoSession().removeAttribute(MINA_SESSION_ATTR_CRED_PUBLIC_KEY);
                    }
                    return isAllowedAccess;
                }
            };
    }

    /*
     * password authentication logic
     */
    private PasswordAuthenticator configurePasswordAuthentication(final SsgConnector connector) {
        return new PasswordAuthenticator() {
                @Override
                public boolean authenticate(String userName, String password, ServerSession session) {
                    // if password authentication is forbidden or public key authentication is required, don't allow access
                    if (connector.getBooleanProperty(LISTEN_PROP_IS_PASSWORD_CREDENTIAL_FORBIDDEN)
                            || connector.getBooleanProperty(LISTEN_PROP_IS_PUBLIC_KEY_CREDENTIAL_REQUIRED)) {
                        return false;
                    }

                    // by default allow all access, defer authentication to Gateway policy assertion
                    boolean isAllowedAccess = true;

                    // if authorizedUserPasswords list exists, perform authentication against it
                    String authorizedUserPasswordList = connector.getProperty(LISTEN_PROP_AUTHORIZED_USER_PASSWORD_LIST);
                    if (authorizedUserPasswordList != null) {
                        String[] authorizedUserPasswords = authorizedUserPasswordList.split(SPLIT_DELIMITER);
                        if (authorizedUserPasswords != null && authorizedUserPasswords.length > 0) {
                            isAllowedAccess = false;
                            for (String authorizedUserPassword : authorizedUserPasswords) {
                                if (!StringUtils.isEmpty(authorizedUserPassword) && authorizedUserPassword.equals(password)) {
                                    isAllowedAccess = true;
                                    break;
                                }
                            }
                        }
                    }

                    // authenticating with password, remove public key (if any)
                    session.getIoSession().removeAttribute(MINA_SESSION_ATTR_CRED_PUBLIC_KEY);

                    if (isAllowedAccess) {
                        session.getIoSession().setAttribute(MINA_SESSION_ATTR_CRED_USERNAME, userName);
                        session.getIoSession().setAttribute(MINA_SESSION_ATTR_CRED_PASSWORD, password);
                    } else {
                        session.getIoSession().removeAttribute(MINA_SESSION_ATTR_CRED_USERNAME);
                        session.getIoSession().removeAttribute(MINA_SESSION_ATTR_CRED_PASSWORD);
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
                return new MessageProcessingScpCommand(splitCommandString(command), connector, messageProcessor,
                        stashManagerFactory, soapFaultManager, messageProcessingEventChannel);
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
                return new MessageProcessingSftpSubsystem(connector, messageProcessor, stashManagerFactory, soapFaultManager, messageProcessingEventChannel);
            }

            @Override
            public String getName() {
                return "sftp";
            }
        };
    }

    /**
     * Builds a session factory with a listener to track the number of active sessions
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
