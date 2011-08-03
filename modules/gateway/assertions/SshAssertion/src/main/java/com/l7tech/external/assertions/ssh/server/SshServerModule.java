package com.l7tech.external.assertions.ssh.server;

import com.l7tech.external.assertions.ssh.SshRouteAssertion;
import com.l7tech.external.assertions.ssh.keyprovider.PemSshHostKeyProvider;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.TransportDescriptor;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.*;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.server.transport.TransportModule;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.*;
import org.apache.sshd.common.cipher.*;
import org.apache.sshd.common.compression.CompressionDelayedZlib;
import org.apache.sshd.common.compression.CompressionNone;
import org.apache.sshd.common.compression.CompressionZlib;
import org.apache.sshd.common.mac.HMACMD5;
import org.apache.sshd.common.mac.HMACMD596;
import org.apache.sshd.common.mac.HMACSHA1;
import org.apache.sshd.common.mac.HMACSHA196;
import org.apache.sshd.common.random.JceRandom;
import org.apache.sshd.common.random.SingletonRandomFactory;
import org.apache.sshd.common.signature.SignatureDSA;
import org.apache.sshd.common.signature.SignatureRSA;
import org.apache.sshd.common.util.OsUtils;
import org.apache.sshd.common.util.SecurityUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.ForwardingFilter;
import org.apache.sshd.server.channel.ChannelDirectTcpip;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.kex.DHG1;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.Security;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates and controls an embedded SSH server for each configured SsgConnector with a SSH scheme.
 * Supports SCP and SFTP over SSH2.
 */
public class SshServerModule extends TransportModule implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(SshServerModule.class.getName());
    private static final String SCHEME_SSH = "SSH2";
    private static final String SPLIT_DELIMITER = "<split>";

    private static final Set<String> SUPPORTED_SCHEMES = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    static {
        SUPPORTED_SCHEMES.addAll(Arrays.asList(SCHEME_SSH));
    }

    private final ApplicationEventProxy applicationEventProxy;
    private final GatewayState gatewayState;
    private final MessageProcessor messageProcessor;
    private final EventChannel messageProcessingEventChannel;
    private final SoapFaultManager soapFaultManager;
    private final StashManagerFactory stashManagerFactory;

    private final Map<Long, Pair<SsgConnector, SshServer>> activeConnectors = new ConcurrentHashMap<Long, Pair<SsgConnector, SshServer>>();

    private final Audit auditor;

    public SshServerModule(ApplicationEventProxy applicationEventProxy,
                           LicenseManager licenseManager,
                           SsgConnectorManager ssgConnectorManager,
                           TrustedCertServices trustedCertServices,
                           DefaultKey defaultKey,
                           ServerConfig serverConfig,
                           GatewayState gatewayState,
                           MessageProcessor messageProcessor,
                           StashManagerFactory stashManagerFactory,
                           SoapFaultManager soapFaultManager,
                           EventChannel messageProcessingEventChannel,
                           AuditFactory auditFactory)
    {
        super("SSH server module", logger, GatewayFeatureSets.SERVICE_SSH_MESSAGE_INPUT, licenseManager, ssgConnectorManager, trustedCertServices, defaultKey, serverConfig);
        this.applicationEventProxy = applicationEventProxy;
        this.gatewayState = gatewayState;
        this.messageProcessor = messageProcessor;
        this.messageProcessingEventChannel = messageProcessingEventChannel;
        this.soapFaultManager = soapFaultManager;
        this.stashManagerFactory = stashManagerFactory;
        this.auditor = auditFactory.newInstance( this, logger );
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
        ServerConfig serverConfig = getBean(appContext, "serverConfig", ServerConfig.class);
        GatewayState gatewayState = getBean(appContext, "gatewayState", GatewayState.class);
        MessageProcessor messageProcessor = getBean(appContext, "messageProcessor", MessageProcessor.class);
        StashManagerFactory stashManagerFactory = getBean(appContext, "stashManagerFactory", StashManagerFactory.class);
        ApplicationEventProxy applicationEventProxy = getBean(appContext, "applicationEventProxy", ApplicationEventProxy.class);
        SoapFaultManager soapFaultManager = getBean(appContext, "soapFaultManager", SoapFaultManager.class);
        EventChannel messageProcessingEventChannel = getBean(appContext, "messageProcessingEventChannel", EventChannel.class);
        AuditFactory auditFactory = getBean(appContext, "auditFactory", AuditFactory.class);

        return new SshServerModule(applicationEventProxy, licenseManager, ssgConnectorManager, trustedCertServices,
                defaultKey, serverConfig, gatewayState, messageProcessor, stashManagerFactory, soapFaultManager,
                messageProcessingEventChannel, auditFactory);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        super.onApplicationEvent(applicationEvent);

        if (applicationEvent instanceof ReadyForMessages) {
            try {
                startInitialConnectors();
            } catch (FindException e) {
                logger.log(Level.SEVERE, "Unable to access initial SSH connectors: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
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
                            logger.log(Level.WARNING, "Unable to start " + connector.getScheme() + " connector on port " + connector.getPort() +
                                        ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                        } else {
                            logger.log(Level.WARNING, "Unable to start " + connector.getScheme() + " connector on port " + connector.getPort() +
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
                logger.log(Level.SEVERE, "Unable to access initial SSH connectors: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
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
            logger.log(Level.WARNING, "ignoring connector with unrecognized scheme " + scheme);
        }
    }

    private void addSshConnector(SsgConnector connector) throws ListenerException {
        if (!isLicensed())
            return;

        String bindAddress = connector.getProperty(SsgConnector.PROP_BIND_ADDRESS);
        if ( ! InetAddressUtil.isAnyHostAddress(bindAddress) ) {
            bindAddress = ssgConnectorManager.translateBindAddress(bindAddress, connector.getPort());
        } else {
            bindAddress = InetAddressUtil.getAnyHostAddress();
        }

        // configure and start sshd
        try {
            SshServer sshd = setUpSshServer();

            // undocumented support for authorized lists, Apache SSHD will callback and set user and public key
            MessageProcessingPasswordAuthenticator user = new MessageProcessingPasswordAuthenticator();
            String authorizedUserPasswordList = connector.getProperty(SshRouteAssertion.LISTEN_PROP_AUTHORIZED_USER_PASSWORD_LIST);
            if (authorizedUserPasswordList != null) {
                user.setAuthorizedUserPasswordKeys(authorizedUserPasswordList.split(SPLIT_DELIMITER));
            }
            sshd.setPasswordAuthenticator(user);
            MessageProcessingPublicKeyAuthenticator userPublicKey = new MessageProcessingPublicKeyAuthenticator();
            String authorizedUserPublicKeyList = connector.getProperty(SshRouteAssertion.LISTEN_PROP_AUTHORIZED_USER_PUBLIC_KEY_LIST);
            if (authorizedUserPublicKeyList != null) {
                userPublicKey.setAuthorizedUserPublicKeys(authorizedUserPublicKeyList.split(SPLIT_DELIMITER));
            }
            sshd.setPublickeyAuthenticator(userPublicKey);

            // enable SCP, SFTP
            final boolean enableScp = Boolean.parseBoolean(connector.getProperty(SshRouteAssertion.LISTEN_PROP_ENABLE_SCP));
            if (enableScp) {
                sshd.setCommandFactory(new MessageProcessingScpCommand.Factory(
                        connector, messageProcessor, stashManagerFactory, soapFaultManager, messageProcessingEventChannel, user, userPublicKey));
            }
            final boolean enableSftp = Boolean.parseBoolean(connector.getProperty(SshRouteAssertion.LISTEN_PROP_ENABLE_SFTP));
            if (enableSftp) {
                sshd.setSubsystemFactories(Arrays.<NamedFactory<Command>>asList(new MessageProcessingSftpSubsystem.Factory(
                        connector, messageProcessor, stashManagerFactory, soapFaultManager, messageProcessingEventChannel, user, userPublicKey)));
            }
            sshd.setPort(connector.getPort());

            // set server host private key
            String hostPrivateKey = connector.getProperty(SshRouteAssertion.LISTEN_PROP_HOST_PRIVATE_KEY);
            sshd.setKeyPairProvider(new PemSshHostKeyProvider(hostPrivateKey));

            // configure connection idle timeout in ms (min=60sec*1000ms)
            String idleTimeoutMins = connector.getProperty(SshRouteAssertion.LISTEN_PROP_IDLE_TIMEOUT_MINUTES);
            if (!StringUtils.isEmpty(idleTimeoutMins)) {
                long idleTimeoutMs = Long.parseLong(idleTimeoutMins) * 60 * 1000;
                sshd.getProperties().put(SshServer.IDLE_TIMEOUT, String.valueOf(idleTimeoutMs));
            }

            // configure maximum concurrent open session count per user
            // 2011/08/03 TL: Apache SSHD does not currently support configurable max total connections
            String maxConcurrentSessionsPerUser = connector.getProperty(SshRouteAssertion.LISTEN_PROP_MAX_CONCURRENT_SESSIONS_PER_USER);
            if (!StringUtils.isEmpty(maxConcurrentSessionsPerUser)) {
                sshd.getProperties().put(SshServer.MAX_CONCURRENT_SESSIONS, maxConcurrentSessionsPerUser);
            }

            auditStart("connector OID " + connector.getOid() + ", on port " + connector.getPort());
            sshd.start();
            activeConnectors.put(connector.getOid(), new Pair<SsgConnector, SshServer>(connector, sshd));
        } catch (IOException e) {
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
                logger.log(Level.SEVERE, "Unable to remove sshd: " + ExceptionUtils.getMessage(e), e);
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
    private SshServer setUpSshServer() {
        // customized for Gateway, we don't want Apache's SecurityUtils to explicitly register BouncyCastle, let the Gateway decide
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) != null) {
            SecurityUtils.setRegisterBouncyCastle(true);
            SecurityUtils.getSecurityProvider();
        } else {
            SecurityUtils.setRegisterBouncyCastle(false);
            SecurityUtils.setSecurityProvider(null);
        }

        SshServer sshd = new SshServer();
        sshd.setKeyExchangeFactories(Arrays.<NamedFactory<KeyExchange>>asList(new DHG1.Factory()));
        sshd.setRandomFactory(new SingletonRandomFactory(new JceRandom.Factory()));
        setUpSshCiphers(sshd);

        sshd.setCompressionFactories(Arrays.<NamedFactory<Compression>>asList(
                new CompressionNone.Factory(),
                new CompressionZlib.Factory(),
                new CompressionDelayedZlib.Factory()));
        sshd.setCompressionFactories(Arrays.<NamedFactory<Compression>>asList(
                new CompressionNone.Factory()));
        sshd.setMacFactories(Arrays.<NamedFactory<Mac>>asList(
                new HMACMD5.Factory(),
                new HMACSHA1.Factory(),
                new HMACMD596.Factory(),
                new HMACSHA196.Factory()));
        sshd.setChannelFactories(Arrays.<NamedFactory<Channel>>asList(
                new ChannelSession.Factory(),
                new ChannelDirectTcpip.Factory()));
        sshd.setSignatureFactories(Arrays.<NamedFactory<Signature>>asList(
                new SignatureDSA.Factory(),
                new SignatureRSA.Factory()));
        sshd.setFileSystemFactory(new VirtualFileSystemFactory());   // customized for Gateway

        if (OsUtils.isUNIX()) {
            sshd.setShellFactory(new ProcessShellFactory(new String[] { "/bin/sh", "-i", "-l" },
                    EnumSet.of(ProcessShellFactory.TtyOptions.ONlCr)));
        } else {
            sshd.setShellFactory(new ProcessShellFactory(new String[] { "cmd.exe "},
                    EnumSet.of(ProcessShellFactory.TtyOptions.Echo, ProcessShellFactory.TtyOptions.ICrNl, ProcessShellFactory.TtyOptions.ONlCr)));
        }

        sshd.setForwardingFilter(new ForwardingFilter() {
            @Override
            public boolean canForwardAgent(ServerSession session) {
                return true;
            }

            @Override
            public boolean canForwardX11(ServerSession session) {
                return true;
            }

            @Override
            public boolean canListen(InetSocketAddress address, ServerSession session) {
                return true;
            }

            @Override
            public boolean canConnect(InetSocketAddress address, ServerSession session) {
                return true;
            }
        });

        return sshd;
    }

    /*
     * This method is based on org.apache.sshd.SshServer.setUpDefaultCiphers(...)
     */
    private void setUpSshCiphers(SshServer sshd) {
        List<NamedFactory<Cipher>> avail = new LinkedList<NamedFactory<Cipher>>();
        avail.add(new AES128CBC.Factory());
        avail.add(new TripleDESCBC.Factory());
        avail.add(new BlowfishCBC.Factory());
        avail.add(new AES192CBC.Factory());
        avail.add(new AES256CBC.Factory());

        for (Iterator<NamedFactory<Cipher>> i = avail.iterator(); i.hasNext();) {
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
        sshd.setCipherFactories(avail);
    }
}
