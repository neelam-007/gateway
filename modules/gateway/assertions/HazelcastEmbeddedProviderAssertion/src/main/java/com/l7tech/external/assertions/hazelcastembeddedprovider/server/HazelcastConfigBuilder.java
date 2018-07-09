package com.l7tech.external.assertions.hazelcastembeddedprovider.server;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.ListenerConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.MembershipListener;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.hazelcastembeddedprovider.server.HazelcastConfigParams.*;
import static com.l7tech.external.assertions.hazelcastembeddedprovider.server.HazelcastConfigParams.DEFAULT_INSTANCE_NAME;
import static com.l7tech.external.assertions.hazelcastembeddedprovider.server.HazelcastConfigParams.Protocol.TCPIP;
import static com.l7tech.external.assertions.hazelcastembeddedprovider.server.HazelcastConfigParams.SystemProperty.*;
import static com.l7tech.external.assertions.hazelcastembeddedprovider.server.HazelcastConfigParams.SystemProperty.HC_MERGE_FIRST_RUN_DELAY_SECONDS;
import static com.l7tech.external.assertions.hazelcastembeddedprovider.server.HazelcastConfigParams.SystemProperty.HC_MERGE_NEXT_RUN_DELAY_SECONDS;
import static java.util.logging.Level.WARNING;

/**
 * Utility class to hold and fill hazelcast configuration parameters.
 */
class HazelcastConfigBuilder {

    private static final Logger LOGGER = Logger.getLogger(HazelcastConfigBuilder.class.getName());

    private final Config config;

    private Integer port;
    private Protocol protocol;
    private Integer tcpIpConnectionTimeout;
    private String groupPassword;
    private Iterable<String> members;
    private MembershipListener membershipListener;

    HazelcastConfigBuilder() {
        this.config = new Config();
    }

    private void setDefaultConfig(){
        config.setInstanceName(DEFAULT_INSTANCE_NAME);

        config.setProperty(
                HC_MERGE_FIRST_RUN_DELAY_SECONDS.getPropertyName(),
                HC_MERGE_FIRST_RUN_DELAY_SECONDS.getDefaultValue()
        ).setProperty(
                HC_MERGE_NEXT_RUN_DELAY_SECONDS.getPropertyName(),
                HC_MERGE_NEXT_RUN_DELAY_SECONDS.getDefaultValue()
        ).setProperty(
                HC_MAX_NO_HEARTBEAT_SEC.getPropertyName(),
                HC_MAX_NO_HEARTBEAT_SEC.getDefaultValue()
        ).setProperty(
                HC_OPERATION_CALL_TIMEOUT_MILLIS.getPropertyName(),
                HC_OPERATION_CALL_TIMEOUT_MILLIS.getDefaultValue()
        ).setProperty(
                HC_REST_ENABLED.getPropertyName(),
                HC_REST_ENABLED.getDefaultValue()
        ).setProperty(
                HC_MEMCACHE_ENABLED.getPropertyName(),
                HC_MEMCACHE_ENABLED.getDefaultValue()
        ).setProperty(
                HC_PHONE_HOME_ENABLED.getPropertyName(),
                HC_PHONE_HOME_ENABLED.getDefaultValue()
        );
    }

    private void setDefaultGroupConfig() {
        config.getGroupConfig().setName(DEFAULT_GROUP_NAME);
    }

    private void setDefaultNetworkConfig() {
        config.getNetworkConfig()
                    // Only use the configured port - it's the only one open
                    .setPortAutoIncrement(HC_PORT_AUTO_INCREMENT_DEFAULT);
    }

    /**
     * Configure Hazelcast to use the specified protocol.
     * @param protocol protocol to use
     */
    private void configureForProtocol(Protocol protocol) {
        if (TCPIP == protocol) {
            setDefaultTcpIpConfig();
        } else {
            String errMsg = protocol.getName() + " is not supported";
            LOGGER.log(WARNING, errMsg);
            throw new IllegalArgumentException(errMsg);
        }
    }

    private void setDefaultTcpIpConfig() {
        // disable other detection protocols
        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getAwsConfig().setEnabled(false);

        // setup TCP/IP
        TcpIpConfig tcpIpConfig = joinConfig.getTcpIpConfig();
        tcpIpConfig.setEnabled(true);
    }

    private void addTcpIpClusterMembers() {
        if (members == null) {
            LOGGER.log(Level.INFO, "No other Hazelcast cluster members provided.");
            return;
        }
        TcpIpConfig tcpIpConfig = config.getNetworkConfig().getJoin().getTcpIpConfig();
        for (String member : members) {
            LOGGER.log(Level.INFO, "Adding Hazelcast node to {0} with ip {1}.", new Object[]{config.getInstanceName(), member});
            tcpIpConfig.addMember(member);
        }
    }

    HazelcastConfigBuilder withPort(final int port) {
        this.port = port;
        return this;
    }

    HazelcastConfigBuilder withProtocol(@NotNull final Protocol protocol) {
        this.protocol = protocol;
        return this;
    }

    HazelcastConfigBuilder withConnectionTimeout(final int timeoutSeconds) {
        this.tcpIpConnectionTimeout = timeoutSeconds;
        return this;
    }

    HazelcastConfigBuilder withGroupPassword(@NotNull final String groupPassword) {
        this.groupPassword = groupPassword;
        return this;
    }

    /**
     * @param members ip addresses of the other cluster members
     */
    HazelcastConfigBuilder withTcpIpMembers(Iterable<String> members) {
        this.members = members;
        return this;
    }

    Config build() {
        setDefaultConfig();
        setDefaultGroupConfig();
        setDefaultNetworkConfig();

        config.getNetworkConfig().setPort(port);
        config.addListenerConfig(new ListenerConfig(this.membershipListener));

        if (protocol == null) {
            throw new IllegalStateException("Protocol must be set");
        }
        configureForProtocol(protocol);
        if (TCPIP == protocol) {
            if (tcpIpConnectionTimeout == null) {
                throw new IllegalStateException("Connection timeout must be set");
            }
            TcpIpConfig tcpIpConfig = config.getNetworkConfig().getJoin().getTcpIpConfig();
            tcpIpConfig.setConnectionTimeoutSeconds(tcpIpConnectionTimeout);

            addTcpIpClusterMembers();
        }

        config.getGroupConfig().setPassword(groupPassword);

        return config;
    }

    HazelcastConfigBuilder withMembershipListener(@NotNull MembershipListener clusterMembershipListener) {
        this.membershipListener = clusterMembershipListener;
        return this;
    }
}
