package com.l7tech.server.transport.ftp;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.util.InetAddressUtil;
import org.apache.ftpserver.*;
import org.apache.ftpserver.command.CommandFactory;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.impl.DefaultFtpServer;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class SsgFtpServerFactory {
    private static final String SECURE_LISTENER_NAME = "secure";
    private static final String DEFAULT_LISTENER_NAME = "default";
    private static final String DEFAULT_FTPLET_NAME = "default";

    private static final String PROPERTY_SESSION_IDLE_TIMEOUT = "ftp.sessionIdleTimeout"; // defines both the listener idle timeout and user maximum idle time
    private static final String PROPERTY_MAX_REQUEST_PROCESSING_THREADS = "ftp.maxRequestProcessingThreads";
    private static final String PROPERTY_ANONYMOUS_LOGINS_ENABLED = "ftp.anonymousLoginsEnabled";
    private static final String PROPERTY_MAX_ANONYMOUS_LOGINS = "ftp.maxAnonymousLogins";
    private static final String PROPERTY_MAX_CONCURRENT_LOGINS = "ftp.maxConcurrentLogins";
    private static final String PROPERTY_USER_MAX_CONCURRENT_LOGINS = "ftp.userMaxConcurrentLogins";
    private static final String PROPERTY_USER_MAX_CONCURRENT_LOGINS_PER_IP = "ftp.userMaxConcurrentLoginsPerIp";

    @Autowired
    private FtpSslFactory ftpSslFactory;

    @Autowired
    private SsgFtpletFactory ssgFtpletFactory;

    @Autowired
    private FtpRequestProcessorFactory ftpRequestProcessorFactory;

    @Autowired
    private SsgConnectorManager connectorManager;

    @Autowired
    private ServerConfig serverConfig;

    public FtpServer create(SsgConnector connector) throws ListenerException {
        return new DefaultFtpServer(createServerContext(connector));
    }

    private SsgFtpServerContext createServerContext(SsgConnector connector) throws ListenerException {
        CommandFactory commandFactory = createCommandFactory(connector);

        ConnectionConfig connectionConfig = createConnectionConfig(connector);

        FtpRequestProcessor requestProcessor = createRequestProcessor(connector, connectionConfig);

        UserManager userManager = createUserManager(connector);

        SsgFtpServerContext context =
                new SsgFtpServerContext(commandFactory, connectionConfig, requestProcessor, userManager);

        Listener listener = createListener(connector);

        context.addListener(listener.isImplicitSsl() ? SECURE_LISTENER_NAME : DEFAULT_LISTENER_NAME, listener);

        context.addFtplet(DEFAULT_FTPLET_NAME, createFtplet());

        return context;
    }

    private CommandFactory createCommandFactory(SsgConnector connector) {
        boolean supportExtendedCommands =
                connector.getBooleanProperty(SsgConnector.PROP_SUPPORT_EXTENDED_FTP_COMMANDS);

        return supportExtendedCommands
                ? new ExtendedCommandFactory()
                : new UploadOnlyCommandFactory();
    }

    /**
     * Creates a control socket ConnectionConfig based on the relevant cluster properties.
     *
     * @return a new ConnectionConfig
     */
    private ConnectionConfig createConnectionConfig(SsgConnector connector) throws ListenerException {
        boolean anonymousLoginsEnabled;

        String anonymousLoginEnabledListenerProperty = connector.getProperty(PROPERTY_ANONYMOUS_LOGINS_ENABLED);

        if (null != anonymousLoginEnabledListenerProperty) {
            anonymousLoginsEnabled = Boolean.parseBoolean(anonymousLoginEnabledListenerProperty);
        } else {
            anonymousLoginsEnabled =
                    Boolean.parseBoolean(serverConfig.getProperty(PROPERTY_ANONYMOUS_LOGINS_ENABLED));
        }

        int maxAnonymousLogins = getIntegerConfigurationProperty(connector, PROPERTY_MAX_ANONYMOUS_LOGINS);

        int maxLogins = getIntegerConfigurationProperty(connector, PROPERTY_MAX_CONCURRENT_LOGINS);

        int maxThreads = getIntegerConfigurationProperty(connector, PROPERTY_MAX_REQUEST_PROCESSING_THREADS);

        ConnectionConfigFactory factory = new ConnectionConfigFactory();

        factory.setAnonymousLoginEnabled(anonymousLoginsEnabled);
        factory.setMaxAnonymousLogins(maxAnonymousLogins);
        factory.setMaxLogins(maxLogins);
        factory.setMaxThreads(maxThreads);

        return factory.createConnectionConfig();
    }

    /**
     * Creates a new FtpRequestProcessor based on the factory SsgConnector settings.
     *
     * @return the new FtpRequestProcessor
     * @throws ListenerException on invalid overridden content type specified in SsgConnector
     */
    private FtpRequestProcessor createRequestProcessor(SsgConnector connector,
                                                       ConnectionConfig connectionConfig) throws ListenerException {
        return ftpRequestProcessorFactory.create(connector, connectionConfig);
    }

    /**
     * Creates a new FtpUserManager based on the factory SsgConnector settings.
     *
     * @return the new FtpUserManager
     */
    private UserManager createUserManager(SsgConnector connector) throws ListenerException {
        int userMaxConcurrentLogins =
                getIntegerConfigurationProperty(connector, PROPERTY_USER_MAX_CONCURRENT_LOGINS);

        int userMaxConcurrentLoginsPerIP =
                getIntegerConfigurationProperty(connector, PROPERTY_USER_MAX_CONCURRENT_LOGINS_PER_IP);

        int idleTimeout = getIntegerConfigurationProperty(connector, PROPERTY_SESSION_IDLE_TIMEOUT);

        return new FtpUserManager(userMaxConcurrentLogins, userMaxConcurrentLoginsPerIP, idleTimeout);
    }

    /**
     * Creates an FTP(S) Listener based on the factory SsgConnector settings.
     *
     * @return a new Listener
     */
    private Listener createListener(SsgConnector connector) throws ListenerException {
        ListenerFactory factory = new ListenerFactory();

        int idleTimeout = getIntegerConfigurationProperty(connector, PROPERTY_SESSION_IDLE_TIMEOUT);

        factory.setIdleTimeout(idleTimeout);
        factory.setPort(connector.getPort());

        String address = connectorManager.translateBindAddress(
                connector.getProperty(SsgConnector.PROP_BIND_ADDRESS), connector.getPort());

        if (null == address) {
            address = InetAddressUtil.isUseIpv6() ? "::" : "0.0.0.0";
        }

        factory.setServerAddress(address);

        boolean secure = SsgConnector.SCHEME_FTPS.equals(connector.getScheme());

        factory.setImplicitSsl(secure);

        SslConfiguration sslConfiguration = secure ? createFtpSslConfiguration(connector) : null;

        factory.setSslConfiguration(sslConfiguration);
        factory.setDataConnectionConfiguration(createDataConnectionConfiguration(connector, sslConfiguration));

        return factory.createListener();
    }

    /**
     * Creates a DataConnectionConfiguration based on the factory SsgConnector settings, any associated cluster
     * properties, and the specified SslConfiguration. This DataConnectionConfiguration will always define passive
     * connections, as active data connections are not currently supported.
     *
     * @param sslConfiguration the SslConfiguration to associate with the DataConnectionConfiguration if FTPS with
     *                         implicit SSL, or null if not FTPS
     * @return a new DataConnectionConfiguration
     * @throws ListenerException
     */
    private DataConnectionConfiguration createDataConnectionConfiguration(SsgConnector connector,
                                                                          SslConfiguration sslConfiguration)
            throws ListenerException {
        int portStart = toInt("FTP port range start", connector.getProperty(SsgConnector.PROP_PORT_RANGE_START));
        int portEnd = toInt("FTP port range count", connector.getProperty(SsgConnector.PROP_PORT_RANGE_COUNT)) +
                portStart - 1;

        DataConnectionConfigurationFactory factory = new DataConnectionConfigurationFactory();

        factory.setActiveEnabled(false); // SSG default - active data connections are unsupported
        factory.setPassivePorts(portStart + "-" + portEnd);
        factory.setImplicitSsl(null != sslConfiguration);
        factory.setSslConfiguration(sslConfiguration);

        return factory.createDataConnectionConfiguration();
    }

    /**
     * Creates an FtpSsl configuration based on the factory SsgConnector settings.
     *
     * @return a new SslConfiguration
     */
    private SslConfiguration createFtpSslConfiguration(SsgConnector connector) throws ListenerException {
        return ftpSslFactory.create(connector);
    }

    private Ftplet createFtplet() throws ListenerException {
        return ssgFtpletFactory.create();
    }

    /**
     * Helper to get the integer value of a connector configuration property, or the default value for that property
     * from a cluster property in the case there is no overriding property defined for the connector.
     *
     * @throws ListenerException if a property value is not formatted correctly, or if the default cluster property
     * does not exist
     */
    private int getIntegerConfigurationProperty(SsgConnector connector, String property) throws ListenerException {
        int value;

        String connectorPropertyValue = connector.getProperty(property);

        if (null != connectorPropertyValue) {
            value = toInt(property, connectorPropertyValue);
        } else {
            value = toInt(property, serverConfig.getProperty(property));
        }

        return value;
    }

    private int toInt(String name, String stringValue) throws ListenerException {
        try {
            return Integer.parseInt(stringValue);
        } catch (NumberFormatException e) {
            throw new ListenerException("Invalid value for parameter '" + name + "': " + stringValue, e);
        }
    }
}
