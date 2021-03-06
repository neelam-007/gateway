package com.l7tech.server.extension.registry.sharedstate;

import com.ca.apim.gateway.extension.sharedstate.SharedKeyValueStoreProvider;
import com.l7tech.server.extension.provider.sharedstate.LocalKeyValueStoreProvider;
import com.l7tech.server.extension.registry.AbstractRegistryImpl;
import com.l7tech.util.Config;

import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Concrete implementation of {@link AbstractRegistryImpl} to manage {@link SharedKeyValueStoreProvider} instances.
 */
public class SharedKeyValueStoreProviderRegistry extends AbstractRegistryImpl<SharedKeyValueStoreProvider> {

    private static final Logger LOGGER = Logger.getLogger(SharedKeyValueStoreProviderRegistry.class.getName());

    public static final String SYSTEM_PROPERTY_KEY_VALUE_STORE_PROVIDER = "com.l7tech.server.extension.sharedKeyValueStoreProvider";

    private final Config config;

    public SharedKeyValueStoreProviderRegistry(Config config) {
        super();

        requireNonNull(config, "Config cannot be null");
        this.config = config;

        LocalKeyValueStoreProvider localKeyValueStoreProvider = new LocalKeyValueStoreProvider();
        register(localKeyValueStoreProvider.getRegistryKey(), localKeyValueStoreProvider);
    }

    /**
     * @return the configured SharedKeyValueStoreProvider. If it is not configured, return the default provider.
     */
    public SharedKeyValueStoreProvider getExtension() {
        String defaultExt = config.getProperty(SYSTEM_PROPERTY_KEY_VALUE_STORE_PROVIDER);
        if (isBlank(defaultExt)) {
            defaultExt = LocalKeyValueStoreProvider.REGISTRY_KEY;
        }

        return getExtension(defaultExt.trim());
    }

    /**
     * Overriden to prevent the default extension from being unregistered
     *
     * @param key the extension key
     * @throws IllegalArgumentException if the provided key is the LOCAL_KEY_VALUE_STORE_PROVIDER_NAME
     */
    @Override
    public void unregister(final String key) {
        // Prevent default extension from being unregistered
        if (LocalKeyValueStoreProvider.REGISTRY_KEY.equals(key)) {
            throw new IllegalArgumentException("Default extension '" + LocalKeyValueStoreProvider.REGISTRY_KEY + "' cannot be unregistered");
        }

        super.unregister(key);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
