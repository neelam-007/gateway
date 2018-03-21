package com.l7tech.server.extension.registry.sharedstate;

import com.ca.apim.gateway.extension.sharedstate.SharedCounterProvider;
import com.l7tech.server.extension.provider.sharedstate.LocalCounterProvider;
import com.l7tech.server.extension.registry.AbstractRegistryImpl;
import com.l7tech.util.Config;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * SharedCounterProviderRegistry handles registrations of SharedCounterProvider
 */
public class SharedCounterProviderRegistry extends AbstractRegistryImpl<SharedCounterProvider> {
    private static final Logger LOGGER = Logger.getLogger(SharedCounterProviderRegistry.class.getName());

    private static final String DEFAULT_EXTENSION = LocalCounterProvider.class.getName();
    private static final String DEFAULT_EXTENSION_PROPERTY = "com.ca.apim.gateway.extension.distributedcounter.default";

    private Config config;

    public SharedCounterProviderRegistry(Config config) {
        super();
        Objects.requireNonNull(config, "Config cannot be null");
        this.config = config;

        register(DEFAULT_EXTENSION, new LocalCounterProvider());
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
