package com.l7tech.server.extension.registry.lifecycle;

import com.ca.apim.gateway.extension.lifecycle.LifecycleAwareExtension;
import com.l7tech.server.extension.registry.AbstractRegistryImpl;

import java.util.logging.Logger;

/**
 * Concrete implementation of {@link AbstractRegistryImpl} to manage {@link LifecycleAwareExtension} instances.
 */
public class LifecycleExtensionRegistry extends AbstractRegistryImpl<LifecycleAwareExtension> {

    private static final Logger LOGGER = Logger.getLogger(LifecycleExtensionRegistry.class.getName());

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
