package com.l7tech.server.transport.ftp;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.util.InetAddressUtil;
import org.apache.ftpserver.*;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.impl.DefaultFtpServer;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class SsgFtpServerFactory {
    private static final int IDLE_TIMEOUT_DEFAULT = 60;
    private static final int MAX_LOGINS_DEFAULT = 10;
    private static final int MAX_THREADS_DEFAULT = 10;

    private static final String SECURE_LISTENER_NAME = "secure";
    private static final String DEFAULT_LISTENER_NAME = "default";
    private static final String DEFAULT_FTPLET_NAME = "default";

    // TODO jwilliams: these settings should come from the connector, not cluster properties
//    private static final String CP_FTP_TIMEOUT_POLL_INTERVAL = "ftp.connection.timeout_poll_interval";  // TODO jwilliams: document removal - poll interval not settable any more because handled by MINA
    private static final String CP_FTP_MAX_CONNECTIONS = "ftp.connection.max"; // TODO jwilliams: document that this now corresponds to max threads? or change/add cp?
    private static final String CP_FTP_MAX_LOGINS = "ftp.connection.max_login";
    private static final String CP_FTP_IDLE_TIMEOUT = "ftp.connection.idle_timeout";

    @Autowired
    private FtpSslFactory ftpSslFactory;

    @Autowired
    private SsgFtpletFactory ssgFtpletFactory;

    @Autowired
    private FtpRequestProcessorFactory ftpRequestProcessorFactory;

    @Autowired
    private SsgConnectorManager connectorManager;

    @Autowired
    private ClusterPropertyManager clusterPropertyManager;

    public FtpServer create(SsgConnector connector) throws ListenerException {
        return new DefaultFtpServer(createServerContext(connector));
    }

    private SsgFtpServerContext createServerContext(SsgConnector connector) throws ListenerException {
        ConnectionConfig connectionConfig = createConnectionConfig();

        SsgFtpServerContext context =
                new SsgFtpServerContext(connectionConfig, createRequestProcessor(connector, connectionConfig));

        Listener listener = createListener(connector);

        context.addListener(listener.isImplicitSsl() ? SECURE_LISTENER_NAME : DEFAULT_LISTENER_NAME, listener);

        context.addFtplet(DEFAULT_FTPLET_NAME, createFtplet());

        return context;
    }

    /**
     * Creates a control socket ConnectionConfig based on the relevant cluster properties.
     *
     * @return a new ConnectionConfig
     */
    private ConnectionConfig createConnectionConfig() throws ListenerException {
        ConnectionConfigFactory factory = new ConnectionConfigFactory();

        int maxLogins = MAX_LOGINS_DEFAULT;
        int maxThreads = MAX_THREADS_DEFAULT;

        try {
            String maxLoginsProperty = clusterPropertyManager.getProperty(CP_FTP_MAX_LOGINS);

            if (null != maxLoginsProperty) {
                maxLogins = toInt(maxLoginsProperty, "Max logins");
            }
        } catch (FindException e) {
            // ignore
        }

        try {
            String maxConnectionsProperty = clusterPropertyManager.getProperty(CP_FTP_MAX_CONNECTIONS);

            if (null != maxConnectionsProperty) {
                maxThreads = toInt(maxConnectionsProperty, "Max connections");
            }
        } catch (FindException e) {
            // ignore
        }

        factory.setMaxLogins(maxLogins);
        factory.setMaxThreads(maxThreads);
        factory.setMaxAnonymousLogins(maxLogins);

        // factory default values that we don't currently have any settings for
//        factory.setMaxLoginFailures(maxLoginFailures); // 3
//        factory.setLoginFailureDelay(loginFailureDelay); // 500

        return factory.createConnectionConfig();
    }

    /**
     * Creates a new FtpRequestProcessor based on the factory SsgConnector settings.
     *
     * @return the new FtpRequestProcessor
     * @throws ListenerException on invalid overridden content type specified in SsgConnector
     */
    private FtpRequestProcessor createRequestProcessor(SsgConnector connector, ConnectionConfig connectionConfig) throws ListenerException {
        return ftpRequestProcessorFactory.create(connector, connectionConfig);
    }

    /**
     * Creates an FTP(S) Listener based on the factory SsgConnector settings.
     *
     * @return a new Listener
     */
    private Listener createListener(SsgConnector connector) throws ListenerException {
        ListenerFactory factory = new ListenerFactory();

        int idleTimeout = IDLE_TIMEOUT_DEFAULT;

        try {
            String idleTimeoutProperty = clusterPropertyManager.getProperty(CP_FTP_IDLE_TIMEOUT);

            if (null != idleTimeoutProperty) {
                idleTimeout = toInt(idleTimeoutProperty, "Default idle timeout");
            }
        } catch (FindException e) {
            // ignore
        }

        factory.setIdleTimeout(idleTimeout);
        factory.setPort(connector.getPort());

        String address = connectorManager.translateBindAddress(connector.getProperty(SsgConnector.PROP_BIND_ADDRESS), connector.getPort());

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
                                                                          SslConfiguration sslConfiguration) throws ListenerException {
        DataConnectionConfigurationFactory factory = new DataConnectionConfigurationFactory();

        factory.setActiveEnabled(false); // SSG default - active data connections are unsupported

        int portStart = toInt(connector.getProperty(SsgConnector.PROP_PORT_RANGE_START), "FTP port range start");
        int portEnd = portStart + toInt(connector.getProperty(SsgConnector.PROP_PORT_RANGE_COUNT), "FTP port range count") - 1;

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

    private int toInt(String str, String name) throws ListenerException {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            throw new ListenerException("Invalid parameter: " + name + ": " + str, e);
        }
    }
}
