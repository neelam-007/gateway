package com.l7tech.external.assertions.hazelcastembeddedprovider.server;

import static java.lang.Boolean.FALSE;

/**
 * Hazelcast configuration constants and defaults.
 */
final class HazelcastConfigParams {

    /**
     * Hazelcast system properties (different from Gateway system properties!)
     */
    enum SystemProperty {

        // defaults reduce the delay in merging into cluster after instance startup
        HC_MERGE_FIRST_RUN_DELAY_SECONDS("hazelcast.merge.first.run.delay.seconds", "10"),
        HC_MERGE_NEXT_RUN_DELAY_SECONDS("hazelcast.merge.next.run.delay.seconds", "10"),
        HC_MAX_NO_HEARTBEAT_SEC("hazelcast.max.no.heartbeat.seconds", "10"),
        HC_OPERATION_CALL_TIMEOUT_MILLIS("hazelcast.operation.call.timeout.millis", "10000"),

        // defaults disable REST and memcache features
        HC_REST_ENABLED("hazelcast.rest.enabled", FALSE.toString()),
        HC_MEMCACHE_ENABLED("hazelcast.memcache.enabled", FALSE.toString()),

        // defaults disable phone home
        HC_PHONE_HOME_ENABLED("hazelcast.phone.home.enabled", FALSE.toString());

        private final String propertyName;
        private final String defaultValue;

        SystemProperty(final String propertyName, final String defaultValue) {
            this.propertyName = propertyName;
            this.defaultValue = defaultValue;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public String getDefaultValue() {
            return defaultValue;
        }
    }

    /**
     * Discovery protocols we support for Hazelcast.
     */
    enum Protocol {
        TCPIP("tcpip");

        private final String name;

        Protocol(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    // Default Hazelcast property values.

    static final Protocol PROTOCOL = Protocol.TCPIP;
    static final int TCPIP_CONNECTION_TIMEOUT_DEFAULT = 5;
    static final String NETWORK_PORT_SYS_PROP = "com.l7tech.external.assertions.hazelcastembeddedprovider.network.port";
    static final int NETWORK_PORT_DEFAULT = 8777;
    static final String DEFAULT_GROUP_NAME = "gateway";
    static final String DEFAULT_INSTANCE_NAME = "gateway-hazelcast";
    static final String CLUSTER_ADDRESSES_SYS_PROP =  "com.l7tech.external.assertions.hazelcastembeddedprovider.cluster.addresses";
    static final boolean HC_PORT_AUTO_INCREMENT_DEFAULT = false;
    static final String DATA_GRID_TCPIP_CONNECTION_TIMEOUT_SYS_PROP = "com.l7tech.external.assertions.hazelcastembeddedprovider.tcpip.connection.timeout";

    private HazelcastConfigParams() {}
}
