package com.l7tech.server.extension.registry.sharedstate;

import com.ca.apim.gateway.extension.sharedstate.SharedStateProvider;
import com.l7tech.server.extension.registry.AbstractRegistryImpl;
import com.l7tech.server.extension.provider.sharedstate.LocalSharedStateProvider;
import com.l7tech.util.Config;
import org.apache.commons.lang.StringUtils;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Shared State Registry handles registrations of SharedStateProviders
 */
public class SharedStateProviderRegistry extends AbstractRegistryImpl<SharedStateProvider> {
    private static final Logger LOGGER = Logger.getLogger(SharedStateProviderRegistry.class.getName());

    private static final String DEFAULT_EXTENSION = LocalSharedStateProvider.class.getName();
    private static final String DEFAULT_EXTENSION_PROPERTY = "com.ca.apim.gateway.extension.sharedState.default";

    private Config config;

    public SharedStateProviderRegistry(Config config) {
        super();
        Objects.requireNonNull(config, "Config cannot be null");
        this.config = config;

        register(DEFAULT_EXTENSION, new LocalSharedStateProvider());
    }

    /**
     * Get the default SharedStateProvider
     * @return the default SharedStateProvider
     */
    public SharedStateProvider getDefaultExtension() {
        String defaultExt = config.getProperty(DEFAULT_EXTENSION_PROPERTY, DEFAULT_EXTENSION);

        if (StringUtils.isBlank(defaultExt)) {
            defaultExt = DEFAULT_EXTENSION;
        }

        return getExtension(defaultExt);
    }

    /**
     * @see com.ca.apim.gateway.extension.ExtensionRegistry#unregister(String) but also prevents the default extension
     * from being unregistered
     * @param key the extension key
     * @throws IllegalArgumentException if the provided key is the DEFAULT_EXTENSION
     */
    @Override
    public void unregister(final String key) {
        // Prevent default extension from being unregistered
        if (DEFAULT_EXTENSION.equals(key)) {
            throw new IllegalArgumentException("Default extension '" + DEFAULT_EXTENSION + "' cannot be unregistered");
        } else {
            super.unregister(key);
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
