package com.l7tech.server.extension.registry.sharedstate;

import com.ca.apim.gateway.extension.sharedstate.SharedExecutorServiceProvider;
import com.l7tech.server.extension.provider.sharedstate.LocalExecutorServiceProvider;
import com.l7tech.server.extension.registry.AbstractRegistryImpl;
import com.l7tech.server.extension.registry.sharedstate.SharedStateProviderRegistry;
import com.l7tech.util.Config;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * SharedExecutorProviderRegistry handles registrations of SharedExecutorServiceProvider
 */
public class SharedExecutorProviderRegistry
        extends AbstractRegistryImpl<SharedExecutorServiceProvider> {

    private static final Logger LOGGER = Logger.getLogger(SharedStateProviderRegistry.class.getName());

    private static final String DEFAULT_EXTENSION = LocalExecutorServiceProvider.class.getName();
    private static final String DEFAULT_EXTENSION_PROPERTY = "com.ca.apim.gateway.extension.distributedexecutor.default";

    private Config config;

    public SharedExecutorProviderRegistry(Config config) {
        super();
        Objects.requireNonNull(config, "Config cannot be null");
        this.config = config;

        register(DEFAULT_EXTENSION, new LocalExecutorServiceProvider());
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
