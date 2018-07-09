package com.l7tech.external.assertions.hazelcastembeddedprovider.server;

import com.ca.apim.gateway.extension.sharedstate.Configuration;
import com.ca.apim.gateway.extension.sharedstate.SharedKeyValueStore;
import com.ca.apim.gateway.extension.sharedstate.SharedKeyValueStoreProvider;
import com.hazelcast.config.Config;
import com.hazelcast.config.ConfigurationException;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static com.l7tech.util.ExceptionUtils.getDebugException;
import static com.l7tech.util.ExceptionUtils.getMessage;
import static java.text.MessageFormat.format;
import static java.util.Objects.requireNonNull;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;

/**
 * HazelcastEmbeddedSharedStateProvider is the hazelcast implementation of the SharedKeyValueStoreProvider.
 */
public class HazelcastEmbeddedSharedStateProvider implements SharedKeyValueStoreProvider {

    private static final Logger LOGGER = Logger.getLogger(HazelcastEmbeddedSharedStateProvider.class.getName());
    private final HazelcastInstance hazelcastInstance;

    private final Map<String, List<String>> partitionLostListenerIdMap = new ConcurrentHashMap<>();
    private final Map<String, HazelcastKeyValueStore> keyValueStores = new ConcurrentHashMap<>();

    /**
     * @param hazelcastInstance hazelcast instance; cannot be null
     */
    HazelcastEmbeddedSharedStateProvider(@NotNull HazelcastInstance hazelcastInstance) {
        requireNonNull(hazelcastInstance, "hazelcastInstance cannot be null");
        this.hazelcastInstance = hazelcastInstance;
    }

    @SuppressWarnings("unchecked")
    @Override
    public SharedKeyValueStore getKeyValueStore(final String name, final Configuration config) {
        return keyValueStores.computeIfAbsent(name, s -> {
            addMapConfig(name, config);
            HazelcastKeyValueStore store = new HazelcastKeyValueStore(hazelcastInstance.getMap(name));
            postConstruct(store);
            return store;
        });
    }

    /**
     * Dynamically add map configuration to hazelcast if the map does not exist yet in the cluster; otherwise it is a
     * no-op
     *
     * @param name the map name
     * @param config the configuration
     */
    private void addMapConfig(final String name, final Configuration config) {
        MapConfig mapConfig = new MapConfig(name);

        // TODO: read from config and set configuration

        if (null == hazelcastInstance.getConfig().getMapConfigOrNull(name)) {
            try {
                hazelcastInstance.getConfig().addMapConfig(mapConfig);
            } catch (ConfigurationException e) {
                LOGGER.log(WARNING, getDebugException(e), () -> "Failed to add new map configuration for " + name + ". " + getMessage(e));
            }
        } else {
            LOGGER.log(INFO, () -> format("Map configuration is already defined for {0}.  Reusing existing map configuration for the cluster.", name));
        }
    }

    private void postConstruct(HazelcastKeyValueStore store) {
        // add listener to log lost partitions (i.e. loss of data)
        String partitionLostListenerId = store.addPartitionLostListener(
                event -> LOGGER.log(WARNING, "Partition loss detected for map {0} : {1}", new Object[]{store.getName(), event})
        );
        List<String> partitionLostListenerIds = partitionLostListenerIdMap.getOrDefault(store.getName(), new ArrayList<>());
        partitionLostListenerIds.add(partitionLostListenerId);
    }

    void shutdown() {
        for (Map.Entry<String, HazelcastKeyValueStore> entry : keyValueStores.entrySet()) {
            if (partitionLostListenerIdMap.containsKey(entry.getKey())) {
                for (String id : partitionLostListenerIdMap.get(entry.getKey())) {
                    entry.getValue().removePartitionLostListener(id);
                }
            }
        }
        hazelcastInstance.shutdown();
    }

    boolean isRunning() {
        return null != hazelcastInstance && hazelcastInstance.getLifecycleService().isRunning();
    }

    Config getHazelcastConfig() {
        return null != hazelcastInstance ? hazelcastInstance.getConfig() : null;
    }
}
