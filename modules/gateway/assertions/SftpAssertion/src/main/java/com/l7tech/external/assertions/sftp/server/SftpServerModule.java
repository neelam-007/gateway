package com.l7tech.external.assertions.sftp.server;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.TransportDescriptor;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.*;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.audit.Auditor;
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
import org.apache.sshd.SshServer;
import org.apache.sshd.common.*;
import org.apache.sshd.common.cipher.*;
import org.apache.sshd.common.compression.CompressionNone;
import org.apache.sshd.common.mac.HMACMD5;
import org.apache.sshd.common.mac.HMACMD596;
import org.apache.sshd.common.mac.HMACSHA1;
import org.apache.sshd.common.mac.HMACSHA196;
import org.apache.sshd.common.random.BouncyCastleRandom;
import org.apache.sshd.common.random.JceRandom;
import org.apache.sshd.common.random.SingletonRandomFactory;
import org.apache.sshd.common.signature.SignatureDSA;
import org.apache.sshd.common.signature.SignatureRSA;
import org.apache.sshd.common.util.OsUtils;
import org.apache.sshd.common.util.SecurityUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.ForwardingFilter;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.channel.ChannelDirectTcpip;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.kex.DHG1;
import org.apache.sshd.server.kex.DHG14;
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates and controls an embedded SFTP server for each configured SsgConnector with a SFTP scheme.
 */
public class SftpServerModule extends TransportModule implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(SftpServerModule.class.getName());
    private static final String SCHEME_SFTP = "SFTP(SSH2)";

    private static final Set<String> SUPPORTED_SCHEMES = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    static {
        SUPPORTED_SCHEMES.addAll(Arrays.asList(SCHEME_SFTP));
    }

    private static BlockingQueue<Runnable> requestQueue = new LinkedBlockingQueue<Runnable>();
    private static final int CORE_POOL_SIZE = 25;
    private static final int MAX_POOL_SIZE = 50;
    private static final long KEEPALIVE_SECONDS = 5 * 60;
    private static ExecutorService requestExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEPALIVE_SECONDS, TimeUnit.SECONDS, requestQueue);

    private final ApplicationEventProxy applicationEventProxy;
    private final GatewayState gatewayState;
    private final MessageProcessor messageProcessor;
    private final EventChannel messageProcessingEventChannel;
    private final SoapFaultManager soapFaultManager;
    private final StashManagerFactory stashManagerFactory;

    private final Map<Long, Pair<SsgConnector, SshServer>> activeConnectors = new ConcurrentHashMap<Long, Pair<SsgConnector, SshServer>>();

    private Auditor auditor;

    public SftpServerModule(ApplicationEventProxy applicationEventProxy,
                            LicenseManager licenseManager,
                            SsgConnectorManager ssgConnectorManager,
                            TrustedCertServices trustedCertServices,
                            DefaultKey defaultKey,
                            ServerConfig serverConfig,
                            GatewayState gatewayState,
                            MessageProcessor messageProcessor,
                            StashManagerFactory stashManagerFactory,
                            SoapFaultManager soapFaultManager,
                            EventChannel messageProcessingEventChannel)
    {
        super("SFTP server module", logger, GatewayFeatureSets.SERVICE_SFTP_MESSAGE_INPUT, licenseManager, ssgConnectorManager, trustedCertServices, defaultKey, serverConfig);
        this.applicationEventProxy = applicationEventProxy;
        this.gatewayState = gatewayState;
        this.messageProcessor = messageProcessor;
        this.messageProcessingEventChannel = messageProcessingEventChannel;
        this.soapFaultManager = soapFaultManager;
        this.stashManagerFactory = stashManagerFactory;
    }

    private static <T> T getBean(BeanFactory beanFactory, String beanName, Class<T> beanClass) {
        T got = beanFactory.getBean(beanName, beanClass);
        if (got != null && beanClass.isAssignableFrom(got.getClass()))
            return got;
        throw new IllegalStateException("Unable to get bean from application context: " + beanName);
    }

    static SftpServerModule createModule(ApplicationContext appContext) {
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

        return new SftpServerModule(applicationEventProxy, licenseManager, ssgConnectorManager, trustedCertServices,
                defaultKey, serverConfig, gatewayState, messageProcessor, stashManagerFactory, soapFaultManager, messageProcessingEventChannel);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        super.onApplicationEvent(applicationEvent);

        if (applicationEvent instanceof ReadyForMessages) {
            try {
                startInitialConnectors();
            } catch (FindException e) {
                logger.log(Level.SEVERE, "Unable to access initial SFTP connectors: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
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
                logger.log(Level.SEVERE, "Unable to access initial SFTP connectors: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }
    }

    void registerApplicationEventListener() {
        applicationEventProxy.addApplicationListener(this);
    }

    private void registerCustomProtocols() {
        TransportDescriptor sftpInfo = new TransportDescriptor();
        sftpInfo.setScheme(SCHEME_SFTP);
        sftpInfo.setSupportsSpecifiedContentType(true);
        sftpInfo.setRequiresSpecifiedContentType(true);
        sftpInfo.setSupportsHardwiredServiceResolution(true);
        sftpInfo.setRequiresHardwiredServiceResolutionForNonXml(true);
        sftpInfo.setRequiresHardwiredServiceResolutionAlways(false);
        // sftpInfo.setCustomPropertiesPanelClassname("com.l7tech.external.assertions.sftp.console.SftpPropertiesPanel");
        // sftpInfo.setModularAssertionClassname(SftpAssertion.class.getName());
        ssgConnectorManager.registerTransportProtocol(sftpInfo, this);
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
        if (SCHEME_SFTP.equalsIgnoreCase(scheme)) {
            addSftpConnector(connector);
        } else {
            // Can't happen
            logger.log(Level.WARNING, "ignoring connector with unrecognized scheme " + scheme);
        }
    }

    private void addSftpConnector(SsgConnector connector) throws ListenerException {
        if (!isLicensed())
            return;

        String bindAddress = connector.getProperty(SsgConnector.PROP_BIND_ADDRESS);
        if ( ! InetAddressUtil.isAnyHostAddress(bindAddress) ) {
            bindAddress = ssgConnectorManager.translateBindAddress(bindAddress, connector.getPort());
        } else {
            bindAddress = InetAddressUtil.getAnyHostAddress();
        }

        ExecutorService executor = requestExecutor;
        boolean executorNeedsClose = false;
        int poolSize = connector.getIntProperty(SsgConnector.PROP_THREAD_POOL_SIZE, 0);
        if (poolSize > 0) {
            executor = new ThreadPoolExecutor(poolSize, poolSize, KEEPALIVE_SECONDS, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
            executorNeedsClose = true;
        }

        try {

            SshServer sshd = setUpSshServer();
            sshd.setSubsystemFactories(Arrays.<NamedFactory<Command>>asList(
                    new MessageProcessingSftpSubsystem.Factory(connector, messageProcessor, stashManagerFactory, soapFaultManager, messageProcessingEventChannel)));
            sshd.setPort(connector.getPort());

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
        getAuditor().logAndAudit(SystemMessages.SFTPSERVER_START, msg);
    }

    private void auditStop(String listener) {
        getAuditor().logAndAudit(SystemMessages.SFTPSERVER_STOP, listener);
    }

    private void auditError(String message, Exception exception) {
        getAuditor().logAndAudit(SystemMessages.SFTPSERVER_ERROR, new String[]{message}, exception);
    }

    private Auditor getAuditor() {
        Auditor auditor = this.auditor;

        if (auditor == null) {
            auditor = new Auditor(this, getApplicationContext(), logger);
            this.auditor = auditor;
        }

        return auditor;
    }

    /*
     * This method is based on org.apache.sshd.SshServer.setUpDefaultServer(...).
     */
    private SshServer setUpSshServer() {
        SshServer sshd = new SshServer();
        // DHG14 uses 2048 bits key which are not supported by the default JCE provider
        if (SecurityUtils.isBouncyCastleRegistered()) {
            sshd.setKeyExchangeFactories(Arrays.<NamedFactory<KeyExchange>>asList(
                    new DHG14.Factory(),
                    new DHG1.Factory()));
            sshd.setRandomFactory(new SingletonRandomFactory(new BouncyCastleRandom.Factory()));
        } else {
            sshd.setKeyExchangeFactories(Arrays.<NamedFactory<KeyExchange>>asList(
                    new DHG1.Factory()));
            sshd.setRandomFactory(new SingletonRandomFactory(new JceRandom.Factory()));
        }
        setUpSshCiphers(sshd);
        // Compression is not enabled by default
        // sshd.setCompressionFactories(Arrays.<NamedFactory<Compression>>asList(
        //         new CompressionNone.Factory(),
        //         new CompressionZlib.Factory(),
        //         new CompressionDelayedZlib.Factory()));
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

        if (SecurityUtils.isBouncyCastleRegistered()) {
            sshd.setKeyPairProvider(new PEMGeneratorHostKeyProvider("key.pem"));
        } else {
            sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("key.ser"));
        }
        if (OsUtils.isUNIX()) {
            sshd.setShellFactory(new ProcessShellFactory(new String[] { "/bin/sh", "-i", "-l" },
                    EnumSet.of(ProcessShellFactory.TtyOptions.ONlCr)));
        } else {
            sshd.setShellFactory(new ProcessShellFactory(new String[] { "cmd.exe "},
                    EnumSet.of(ProcessShellFactory.TtyOptions.Echo, ProcessShellFactory.TtyOptions.ICrNl, ProcessShellFactory.TtyOptions.ONlCr)));
        }

        // customized for Gateway
        sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
            public boolean authenticate(String username, String password, ServerSession session) {
                // allow all access, defer authentication to Gateway policy assertion
                return true;
            }
        });

        sshd.setPublickeyAuthenticator(new PublickeyAuthenticator() {
            public boolean authenticate(String username, PublicKey key, ServerSession session) {
                //File f = new File("/Users/" + username + "/.ssh/authorized_keys");
                return true;
            }
        });

        sshd.setForwardingFilter(new ForwardingFilter() {
            public boolean canForwardAgent(ServerSession session) {
                return true;
            }

            public boolean canForwardX11(ServerSession session) {
                return true;
            }

            public boolean canListen(InetSocketAddress address, ServerSession session) {
                return true;
            }

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
