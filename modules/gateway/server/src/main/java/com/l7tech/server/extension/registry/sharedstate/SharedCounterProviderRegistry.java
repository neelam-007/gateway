package com.l7tech.server.extension.registry.sharedstate;

import com.ca.apim.gateway.extension.sharedstate.counter.SharedCounterProvider;
import com.l7tech.server.extension.provider.sharedstate.counter.LocalCounterProvider;
import com.l7tech.server.extension.registry.AbstractRegistryImpl;

import java.util.logging.Logger;

/**
 * SharedCounterProviderRegistry handles registrations of SharedCounterProvider
 */
public class SharedCounterProviderRegistry extends AbstractRegistryImpl<SharedCounterProvider> {
    private static final Logger LOGGER = Logger.getLogger(SharedCounterProviderRegistry.class.getName());
    public static final String SYSPROP_COUNTER_PROVIDER = "com.l7tech.server.extension.sharedCounterProvider";

    public SharedCounterProviderRegistry() {
        super();
        this.register(LocalCounterProvider.KEY, new LocalCounterProvider(), LocalCounterProvider.KEY);
    }

    @Override
    public SharedCounterProvider getExtension(String key) {
        if (key == null || key.isEmpty()) {
            return super.getExtension(LocalCounterProvider.KEY);
        }
        return super.getExtension(key);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
