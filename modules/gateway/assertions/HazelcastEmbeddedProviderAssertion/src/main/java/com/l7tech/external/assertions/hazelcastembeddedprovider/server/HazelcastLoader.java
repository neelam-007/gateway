package com.l7tech.external.assertions.hazelcastembeddedprovider.server;

import com.ca.apim.gateway.extension.lifecycle.LifecycleAwareExtension;
import com.ca.apim.gateway.extension.lifecycle.LifecycleException;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.GatewayState;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.extension.registry.lifecycle.LifecycleExtensionRegistry;
import com.l7tech.server.extension.registry.sharedstate.SharedKeyValueStoreProviderRegistry;
import com.l7tech.server.security.sharedkey.SharedKeyManager;
import com.l7tech.server.service.FirewallRulesManager;
import org.springframework.context.ApplicationContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.hazelcastembeddedprovider.server.HazelcastConfigParams.*;
import static com.l7tech.util.ExceptionUtils.getDebugException;
import static com.l7tech.util.ExceptionUtils.getMessage;
import static com.l7tech.util.InetAddressUtil.isValidIpv4Address;
import static com.l7tech.util.InetAddressUtil.isValidIpv6Address;
import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static java.util.logging.Level.*;

/**
 * HazelcastLoader provides implementation to initialize the HazelcastEmbeddedProviderAssertion module including
 * starting and shutting-down Hazelcast instance and registering Embedded Hazelcast implementations of Gateway Extensions
 */
public final class HazelcastLoader {

    private static final Logger LOGGER = Logger.getLogger(HazelcastLoader.class.getName());
    private static final String LIFECYCLE_EXTENSION_KEY = HazelcastLoader.class.getName();
    private static final String SHARED_STATE_PROVIDER_REGISTRY_KEY = "embeddedhazelcast";

    private static HazelcastLoader instance;

    private final SharedKeyValueStoreProviderRegistry sharedKeyValueStoreProviderRegistry;
    private final LifecycleExtensionRegistry lifecycleExtensionRegistry;
    private final FirewallRulesManager firewallRulesManager;
    private final SharedKeyManager sharedKeyManager;
    private final ClusterInfoManager clusterInfoManager;
    private final ServerConfig serverConfig;

    private Config hzConfig;
    private HazelcastEmbeddedSharedStateProvider hzProvider;

    @SuppressWarnings({"UnusedDeclaration"})
    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        if (instance == null) {
            instance = new HazelcastLoader(context);
        }
    }

    /**
     * When onModuleUnloaded completes, it MUST ensure all HazelcastInstances are shutdown and ports opened for
     * Hazelcast are closed.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public static synchronized void onModuleUnloaded() {
        if (instance != null) {
            instance.stopHazelcastInstance();
            instance = null;
        }
    }

    /**
     *
     * Only call constructor once to keep this as a singleton.
     *
     */
    private HazelcastLoader(final ApplicationContext context) {
        sharedKeyValueStoreProviderRegistry = context.getBean("sharedKeyValueStoreProviderRegistry", SharedKeyValueStoreProviderRegistry.class);
        lifecycleExtensionRegistry = context.getBean("lifecycleExtensionRegistry", LifecycleExtensionRegistry.class);
        firewallRulesManager = context.getBean("ssgFirewallManager", FirewallRulesManager.class);
        sharedKeyManager = context.getBean("sharedKeyManager", SharedKeyManager.class);
        clusterInfoManager = context.getBean("clusterInfoManager", ClusterInfoManager.class);
        serverConfig = context.getBean("serverConfig", ServerConfig.class);

        LifecycleAwareExtension hazelcastLoaderExtension = new LifecycleAwareExtension() {
            @Override
            public void start() throws LifecycleException {
                try {
                    createHazelcastInstance();
                } catch (Exception e) {
                    LOGGER.log(WARNING, getDebugException(e),
                            () -> "Failed to initialize Hazelcast cluster. " +  getMessage(e));
                    stopHazelcastInstance();
                    throw new LifecycleException("Could not initialize Hazelcast cluster", e);
                }
            }

            @Override
            public void stop() {
                stopHazelcastInstance();
            }

            @Override
            public String getName() {
                return "HazelcastEmbeddedProviderExtension";
            }
        };


        lifecycleExtensionRegistry.register(LIFECYCLE_EXTENSION_KEY, hazelcastLoaderExtension);
        final GatewayState gatewayState = context.getBean("gatewayState", GatewayState.class);
        if (gatewayState.isReadyForMessages()) {
            // This module was loaded to an already running Gateway.
            try {
                hazelcastLoaderExtension.start();
            } catch (LifecycleException e) {
                // log in the lowest level just in case, but this exception can only be an outcome of handling the hazelcast initialization failure
                LOGGER.log(FINEST, e.getMessage(), e);
            }
        }
    }

    private void createHazelcastInstance() throws FindException {
        LOGGER.log(FINE, "Loading HazelcastEmbeddedProvider...");

        if (isRunning()) {
            throw new IllegalStateException("Hazelcast instance " + hzConfig.getInstanceName() + " is already running.");
        }

        final Set<String> clusterMembers = this.getClusterMembers();
        final String groupPassword = new String(sharedKeyManager.getSharedKey());

        this.hzConfig = new HazelcastConfigBuilder()
                .withPort(serverConfig.getIntProperty(NETWORK_PORT_SYS_PROP, NETWORK_PORT_DEFAULT))
                .withProtocol(PROTOCOL)
                .withConnectionTimeout(serverConfig.getIntProperty(DATA_GRID_TCPIP_CONNECTION_TIMEOUT_SYS_PROP, TCPIP_CONNECTION_TIMEOUT_DEFAULT))
                .withGroupPassword(groupPassword)
                .withTcpIpMembers(clusterMembers)
                .withMembershipListener(new ClusterMembershipListener())
                .build();

        LOGGER.log(FINE, () -> format("Shared Data Provider added Hazelcast members: {0}", clusterMembers));

        // ensure the firewall is open so that traffic can be sent in
        final int inboundPort = hzConfig.getNetworkConfig().getPort();
        firewallRulesManager.openPort(hzConfig.getInstanceName(), inboundPort);
        LOGGER.log(INFO, "Created firewall open port rule for Hazelcast on port {0,number,#}", inboundPort);

        try {
            LOGGER.log(INFO, () -> format("Creating Hazelcast instance {0}", hzConfig.getInstanceName()));

            HazelcastInstance hzInstance = Hazelcast.newHazelcastInstance(hzConfig);
            hzInstance.getCluster().addMembershipListener(new ClusterMembershipListener());
            LOGGER.log(INFO, () -> format("Running Hazelcast instance {0}", hzConfig.getInstanceName()));

            hzProvider = new HazelcastEmbeddedSharedStateProvider(hzInstance);
            this.sharedKeyValueStoreProviderRegistry.register(SHARED_STATE_PROVIDER_REGISTRY_KEY, hzProvider);
        } catch (Exception e) {
            LOGGER.log(WARNING, getDebugException(e), () -> "Could not create Hazelcast instance " + hzConfig.getInstanceName() + " : " + getMessage(e));
            throw new IllegalStateException(e);
        }

        LOGGER.log(INFO, "Loaded HazelcastEmbeddedProvider.");
    }

    private Set<String> getClusterMembers() throws FindException {
        Set<String> clusterMembers = new HashSet<>();
        String clusterAddressesSysProp = serverConfig.getProperty(CLUSTER_ADDRESSES_SYS_PROP);
        if (clusterAddressesSysProp == null) {
            // Property not set, read from cluster info manager
            LOGGER.log(FINE, () -> format("No value found for system property {0}", CLUSTER_ADDRESSES_SYS_PROP));
            try {
                clusterInfoManager.retrieveClusterStatus().forEach(nodeInfo -> clusterMembers.add(nodeInfo.getAddress()));
            } catch (FindException e) {
                LOGGER.log(WARNING, getDebugException(e),
                        () -> "Cannot retrieve list of cluster nodes to connect. " +  getMessage(e));
                throw e;
            }
        } else if (!clusterAddressesSysProp.isEmpty()) {
            List<String> ipAddressArray = asList(clusterAddressesSysProp.split(","));
            if (checkValidityIpAddress(ipAddressArray)) {
                clusterMembers.addAll(ipAddressArray);
            } else {
                throw new IllegalArgumentException("Invalid IP address(s) found. " + clusterAddressesSysProp);
            }
        }
        return clusterMembers;
    }

    private void stopHazelcastInstance() {
        LOGGER.log(FINE, "Unloading HazelcastEmbeddedProvider...");

        // Unregister all extensions added
        lifecycleExtensionRegistry.unregister(LIFECYCLE_EXTENSION_KEY);
        sharedKeyValueStoreProviderRegistry.unregister(SHARED_STATE_PROVIDER_REGISTRY_KEY);

        try {
            if (!isRunning()) {
                LOGGER.log(INFO, "Hazelcast instance is already stopped.");
            } else {
                LOGGER.log(INFO, "Shutting down Hazelcast instance.");
                if (null != hzProvider) {
                    hzProvider.shutdown();
                    hzProvider = null;
                }
                LOGGER.log(INFO, "Shutdown completed for Hazelcast instance.");
            }
        } catch (Exception e) {
            LOGGER.log(WARNING, getDebugException(e), () -> "Could not shutdown Hazelcast instance: " +  getMessage(e));
        } finally {
            int instancesToClose = Hazelcast.getAllHazelcastInstances().size();
            if (instancesToClose > 0) {
                LOGGER.log(INFO, () -> format("There are {0} Hazelcast instances not closed. Forcing them to shutdown.", instancesToClose));
                Hazelcast.shutdownAll(); // this is safe since the only Hazelcast instance is created in this class.
            }
        }

        LOGGER.log(INFO, "Unloaded HazelcastEmbeddedProvider.");
    }

    /**
     *
     * Checks if array has valid ip addresses, returns true if all addresses are valid, and false otherwise
     *
     * @param ipAddressArray String of systemProperty that is split through commas for a list of ip addresses
     */
    private static boolean checkValidityIpAddress(List<String> ipAddressArray){
        for (String ipAddress : ipAddressArray) {
            ipAddress = ipAddress.trim();
            boolean valid = isValidIpv4Address(ipAddress) || isValidIpv6Address(ipAddress);
            if (!valid) {
                return false;
            }
        }
        return true;
    }

    boolean isRunning() {
        return hzProvider != null && hzProvider.isRunning();
    }

    static HazelcastLoader getInstance() {
        return instance;
    }

    Config getHazelcastConfig() {
        return hzProvider.getHazelcastConfig();
    }
}
