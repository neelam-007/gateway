package com.l7tech.server.cluster;

import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.security.sharedkey.SharedKeyManager;
import org.springframework.beans.factory.InitializingBean;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class GatewayHazelcast implements InitializingBean {
    private static final Logger logger = Logger.getLogger(GatewayHazelcast.class.getName());

    private static final String DEFAULT_GROUP_NAME = "gateway";
    private static final String DEFAULT_INSTANCE_NAME = "gateway-hazelcast";

    // PROTOCOLS
    private static final String PROTOCOL_TCPIP = "tcpip";
    private static final String PROTOCOL_MULTICAST = "multicast";   // currently unsupported
    // Support can be added later for "aws"

    @Inject
    private ClusterInfoManager clusterInfoManager;

    @Inject
    private SharedKeyManager sharedKeyManager;

    @Inject
    private ServerConfig serverConfig;

    private AtomicBoolean shutdown = new AtomicBoolean(false);

    /**
     * Get the Gateway HazelcastInstance
     */
    public HazelcastInstance getHazelcastInstance() {
        checkShutdown();

        return getDefaultHazelcastInstance();
    }

    /**
     * Gracefully shut down the Gateway HazelcastInstance
     */
    public void shutdown() {
        checkShutdown();
        shutdown.set(true);

        HazelcastInstance instance = getDefaultHazelcastInstance();

        if (null != instance) {
            instance.shutdown();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        createHazelcastInstance();
    }

    private HazelcastInstance getDefaultHazelcastInstance() {
        return Hazelcast.getHazelcastInstanceByName(DEFAULT_INSTANCE_NAME);
    }

    private void createHazelcastInstance() {
        logger.info("Creating Gateway Hazelcast instance");

        Config config = new Config();
        config.setInstanceName(DEFAULT_INSTANCE_NAME);

        String groupPassword;

        try {
            groupPassword = new String(sharedKeyManager.getSharedKey());
        } catch (FindException e) {
            logger.log(Level.WARNING, "Cannot retrieve shared key. Hazelcast will not start.");
            throw new IllegalStateException(e);
        }

        GroupConfig groupConfig = config.getGroupConfig();
        groupConfig.setName(DEFAULT_GROUP_NAME);
        groupConfig.setPassword(groupPassword);

        config.getNetworkConfig()
                .setPort(Integer.valueOf(serverConfig.getProperty(ServerConfigParams.PARAM_DATA_GRID_PORT)))
                // Only use the configured port
                .setPortAutoIncrement(false);

        final String protocol = serverConfig.getProperty(ServerConfigParams.PARAM_DATA_GRID_PROTOCOL);

        switch (protocol) {
            case PROTOCOL_TCPIP:
                setTcpIp(config);
                break;
            case PROTOCOL_MULTICAST:
            default:
                String errMsg = protocol + " is not supported";
                logger.log(Level.WARNING, errMsg);
                throw new IllegalArgumentException(errMsg);
        }

        Hazelcast.newHazelcastInstance(config);

        logger.info("Gateway Hazelcast instance created");
    }

    /**
     * Set Hazelcast config to use TCP/IP.
     *
     * Per Hazelcast documentation, the TCP/IP join mechanism relies on one or more well known members. When a new
     * member wants to join a cluster, it will try to connect to one of the well known members. If it is able to
     * connect, it will know about all members in the cluster and won't rely on these well known members anymore.
     *
     * It is unnecessary to update the instance as Gateway nodes join/leave the cluster.
     *
     * @param config Hazelcast config
     */
    private void setTcpIp(final Config config) {
        // disable other detection protocols
        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getAwsConfig().setEnabled(false);

        // setup TCP/IP
        TcpIpConfig tcpIpConfig = joinConfig.getTcpIpConfig();
        tcpIpConfig.setEnabled(true);
        tcpIpConfig.setConnectionTimeoutSeconds(Integer.valueOf(serverConfig
                .getProperty(ServerConfigParams.PARAM_DATA_GRID_TCPIP_CONNECTION_TIMEOUT)));

        // add members
        try {
            for (ClusterNodeInfo nodeInfo : clusterInfoManager.retrieveClusterStatus()) {
                tcpIpConfig.addMember(nodeInfo.getAddress());
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Cannot retrieve list of cluster nodes to connect. Hazelcast will not start.");
            throw new IllegalStateException(e);
        }
    }

    private void checkShutdown() {
        if (shutdown.get()) throw new IllegalStateException("HazelcastInstance has already been shutdown");
    }
}
