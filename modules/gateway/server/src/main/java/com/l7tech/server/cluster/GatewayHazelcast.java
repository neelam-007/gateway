package com.l7tech.server.cluster;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.security.sharedkey.SharedKeyManager;
import com.l7tech.server.service.FirewallRulesManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import javax.inject.Inject;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
@ManagedResource(description = "Hazelcast Data Grid", objectName = "l7tech:type=Hazelcast")
public class GatewayHazelcast implements InitializingBean {
    private static final Logger logger = Logger.getLogger(GatewayHazelcast.class.getName());

    private static final String DEFAULT_GROUP_NAME = "gateway";
    private static final String DEFAULT_INSTANCE_NAME = "gateway-hazelcast";

    // NETWORK CONFIGURATION
    private static final int DEFAULT_INBOUND_PORT = 8777;

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

    @Inject
    private FirewallRulesManager firewallRulesManager;

    private AtomicBoolean started = new AtomicBoolean(false);
    private AtomicBoolean shutdown = new AtomicBoolean(false);

    /**
     * Get the Gateway HazelcastInstance
     */
    public HazelcastInstance getHazelcastInstance() {
        checkShutdown();
        checkStarted();

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
            logger.log(Level.INFO, "Shutting down Gateway Hazelcast instance");
            instance.shutdown();
        }
    }

    @ManagedAttribute(description = "Group Members", currencyTimeLimit = 30)
    public List<String> getMemberIpAddresses() {
        checkShutdown();
        checkStarted();

        HazelcastInstance hazelcastInstance = Hazelcast.getHazelcastInstanceByName(DEFAULT_INSTANCE_NAME);

        final List<String> addresses = new ArrayList<>();

        for (Member member : hazelcastInstance.getCluster().getMembers()) {
            try {
                String address = member.getAddress().getInetAddress().getHostAddress();
                addresses.add(address);
            } catch (UnknownHostException e) {
                logger.log(Level.WARNING, "Could not determine member address: " + e.getMessage());
            }
        }

        return Collections.unmodifiableList(addresses);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            createHazelcastInstance();
            started.set(true);
        } catch (Exception e) {
            logger.severe("Could not create Gateway Hazelcast instance: " + e.getMessage());
            started.set(false);
        }
    }

    private HazelcastInstance getDefaultHazelcastInstance() {
        return Hazelcast.getHazelcastInstanceByName(DEFAULT_INSTANCE_NAME);
    }

    private void createHazelcastInstance() throws Exception {
        logger.info("Creating Gateway Hazelcast instance");

        Config config = new Config();
        config.setInstanceName(DEFAULT_INSTANCE_NAME);

        // reduce the delay in merging into cluster after instance startup
        config.setProperty("hazelcast.merge.first.run.delay.seconds", "10");
        config.setProperty("hazelcast.merge.next.run.delay.seconds", "10");
        config.setProperty("hazelcast.max.no.heartbeat.seconds", "10");
        config.setProperty("hazelcast.operation.call.timeout.millis", "10000");

        // disable REST and memcache features
        config.setProperty("hazelcast.rest.enabled", "false");
        config.setProperty("hazelcast.memcache.enabled", "false");

        // disable phone home
        config.setProperty("hazelcast.phone.home.enabled", "false");

        String groupPassword;

        try {
            groupPassword = new String(sharedKeyManager.getSharedKey());
        } catch (FindException e) {
            logger.log(Level.WARNING, "Cannot retrieve shared key");
            throw new IllegalStateException(e);
        }

        GroupConfig groupConfig = config.getGroupConfig();
        groupConfig.setName(DEFAULT_GROUP_NAME);
        groupConfig.setPassword(groupPassword);

        NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig
                .setPort(DEFAULT_INBOUND_PORT)
                // Only use the configured port - it's the only one open
                .setPortAutoIncrement(false);

        // ensure the firewall is open so that traffic can be sent in
        // TODO jwilliams: check for existing rule and replace if port is different - needed when the port is user-defined
        firewallRulesManager.openPort(DEFAULT_INSTANCE_NAME, networkConfig.getPort());

        final String protocol = serverConfig.getProperty(ServerConfigParams.PARAM_DATA_GRID_PROTOCOL);

        switch (protocol) {
            case PROTOCOL_TCPIP:
                setTcpIp(networkConfig);
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
     * @param networkConfig Hazelcast network config
     */
    private void setTcpIp(final NetworkConfig networkConfig) {
        // disable other detection protocols
        JoinConfig joinConfig = networkConfig.getJoin();
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
                final String member = nodeInfo.getAddress();
                logger.log(Level.INFO, "Adding member: {0}", member);
                tcpIpConfig.addMember(member);
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Cannot retrieve list of cluster nodes to connect");
            throw new IllegalStateException(e);
        }
    }

    private void checkStarted() {
        if (!started.get()) throw new IllegalStateException("Hazelcast instance is not available");
    }

    private void checkShutdown() {
        if (shutdown.get()) throw new IllegalStateException("Hazelcast instance has already been shutdown");
    }
}
