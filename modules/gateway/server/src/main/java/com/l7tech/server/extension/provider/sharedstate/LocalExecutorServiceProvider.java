package com.l7tech.server.extension.provider.sharedstate;

import com.ca.apim.gateway.extension.sharedstate.Configuration;
import com.ca.apim.gateway.extension.sharedstate.SharedExecutorServiceProvider;
import com.ca.apim.gateway.extension.sharedstate.SharedScheduledExecutorService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * LocalExecutorServiceProvider provides an in-memory implementation of the SharedExecutorServiceProvider.
 * Note: The executor services only work within the single node as data will not be shared between the cluster.
 */
public class LocalExecutorServiceProvider implements SharedExecutorServiceProvider {
    private final ConcurrentMap<String, SharedScheduledExecutorService> executorServices;

    public LocalExecutorServiceProvider() {
        executorServices = new ConcurrentHashMap<>();
    }

    /**
     * @see SharedExecutorServiceProvider#getScheduledExecutorService(String, Configuration)
     */
    @Override
    public SharedScheduledExecutorService getScheduledExecutorService(String name, Configuration config) {
        return executorServices.computeIfAbsent(name, key -> new LocalScheduledExecutorService(
                key, Integer.parseInt(config.get(Configuration.Param.SCHEDULED_EXECUTOR_CORE_POOL_SIZE))));
    }
}
