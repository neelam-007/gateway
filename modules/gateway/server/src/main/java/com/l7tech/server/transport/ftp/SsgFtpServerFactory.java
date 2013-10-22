package com.l7tech.server.transport.ftp;

import com.l7tech.gateway.common.transport.SsgConnector;
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

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class SsgFtpServerFactory {
    private static final String SECURE_LISTENER_NAME = "secure";
    private static final String DEFAULT_LISTENER_NAME = "default";

    private final SsgConnector connector;
    private final SsgConnectorManager connectorManager;
    private final FtpServerManager ftpServerManager;
    private final ClusterPropertyManager clusterPropertyManager;

    private final HashMap<String, Ftplet> ftplets = new HashMap<>();

    public SsgFtpServerFactory(SsgConnector connector, SsgConnectorManager connectorManager,
                               FtpServerManager ftpServerManager, ClusterPropertyManager clusterPropertyManager) {
        this.connector = connector;
        this.connectorManager = connectorManager;
        this.ftpServerManager = ftpServerManager;
        this.clusterPropertyManager = clusterPropertyManager;
    }

    @SafeVarargs
    public SsgFtpServerFactory(SsgConnector connector, SsgConnectorManager connectorManager,
                               FtpServerManager ftpServerManager, ClusterPropertyManager clusterPropertyManager,
                               Map.Entry<String, Ftplet>... ftpletEntries) {
        this(connector, connectorManager, ftpServerManager, clusterPropertyManager);

        for (Map.Entry<String, Ftplet> entry : ftpletEntries) {
            addFtplet(entry.getKey(), entry.getValue());
        }
    }

    public FtpServer create() throws ListenerException {
        return new DefaultFtpServer(createServerContext());
    }

    public void addFtplet(String name, Ftplet ftplet) {
        ftplets.put(name, ftplet);
    }

    private SsgFtpServerContext createServerContext() throws ListenerException {
        SsgFtpServerContext context = new SsgFtpServerContext(createConnectionConfig());

        Listener listener = createListener();

        context.addListener(listener.isImplicitSsl() ? SECURE_LISTENER_NAME : DEFAULT_LISTENER_NAME, listener);

        for (Map.Entry<String, Ftplet> entry : ftplets.entrySet()) {
            context.addFtplet(entry.getKey(), entry.getValue());
        }

        return context;
    }

    /**
     * Creates a control socket ConnectionConfig based on the factory SsgConnection settings.
     *
     * @return a new ConnectionConfig
     */
    private ConnectionConfig createConnectionConfig() {
        ConnectionConfigFactory factory = new ConnectionConfigFactory();

        factory.setMaxLogins(10); // 10
        factory.setAnonymousLoginEnabled(true); // true
        factory.setMaxAnonymousLogins(10); // 10
        factory.setMaxLoginFailures(3); // 3
        factory.setLoginFailureDelay(500); // 500
        factory.setMaxThreads(0); // 0

        return factory.createConnectionConfig();
    }

    /**
     * Creates an FTP(S) Listener based on the factory SsgConnector settings.
     *
     * @return a new Listener
     */
    private Listener createListener() throws ListenerException {
        ListenerFactory factory = new ListenerFactory();

        factory.setPort(connector.getPort());

        String address = connectorManager.translateBindAddress(connector.getProperty(SsgConnector.PROP_BIND_ADDRESS), connector.getPort());

        if (null == address) {
            address = InetAddressUtil.isUseIpv6() ? "::" : "0.0.0.0";
        }

        factory.setServerAddress(address);

        boolean secure = SsgConnector.SCHEME_FTPS.equals(connector.getScheme());

        factory.setImplicitSsl(secure);

        SslConfiguration sslConfiguration = secure ? createFtpSslConfiguration() : null;

        factory.setSslConfiguration(sslConfiguration);

        factory.setDataConnectionConfiguration(createDataConnectionConfiguration(sslConfiguration));

//        factory.setIdleTimeout(300); // unset
//        factory.setIpFilter(null); // old implementation used a permissive IpRestrictor that was not configurable

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
    private DataConnectionConfiguration createDataConnectionConfiguration(SslConfiguration sslConfiguration) throws ListenerException {
        DataConnectionConfigurationFactory factory = new DataConnectionConfigurationFactory();

        factory.setActiveEnabled(false); // SSG default - active data connections are unsupported

        int portStart = toInt(connector.getProperty(SsgConnector.PROP_PORT_RANGE_START), "FTP port range start");
        int portEnd = portStart + toInt(connector.getProperty(SsgConnector.PROP_PORT_RANGE_COUNT), "FTP port range count");

        factory.setPassivePorts(portStart + "-" + portEnd);
        factory.setImplicitSsl(null != sslConfiguration);
        factory.setSslConfiguration(sslConfiguration);

//        factory.setActiveIpCheck(false); // old properties file default, but seems unnecessary
//        factory.setActiveLocalAddress(); // unused
//        factory.setActiveLocalPort(); // unused
//        factory.setPassiveAddress(); // unused
//        factory.setPassiveExternalAddress(); // unused
//        factory.setIdleTime(/* ??? */); // doesn't seem to be set

        return factory.createDataConnectionConfiguration();
    }

    /**
     * Creates an FtpSsl configuration based on the factory SsgConnector settings.
     *
     * @return a new SslConfiguration
     */
    private SslConfiguration createFtpSslConfiguration() throws ListenerException {
        return new FtpSslConfigurationFactory(ftpServerManager, connector).createFtpSslConfiguration();
    }

    private int toInt(String str, String name) throws ListenerException {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            throw new ListenerException("Invalid parameter: " + name + ": " + str, e);
        }
    }
}
