package com.l7tech.server.extension.registry;

import com.ca.apim.gateway.extension.Extension;
import com.ca.apim.gateway.extension.ExtensionAccess;
import com.ca.apim.gateway.extension.ExtensionRegistry;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract implementation for ExtensionRegistry and ExtensionAccess
 * @param <E> Extension
 */
public abstract class AbstractRegistryImpl<E extends Extension> implements ExtensionRegistry<E>, ExtensionAccess<E> {

    private final ConcurrentMap<String, E> extensionsMap;

    /**
     * Constructor
     */
    protected AbstractRegistryImpl() {
        extensionsMap = new ConcurrentHashMap<>();
    }

    /**
     * Get logger.  The logger to be used to log messages in this class
     * @return Logger
     */
    protected abstract Logger getLogger();

    /**
     * Get the registered extension
     * @param key the key used to register the extension
     * @return the extension if it is registered; null is returned if no such key is found
     */
    @Override
    public E getExtension(final String key) {
        E extension = extensionsMap.get(key);

        if (extension == null) {
            getLogger().log(Level.INFO, "Extension with key {0} is unregistered.", key);
        }

        return extension;
    }

    /**
     * Get all registered extensions
     * @return a Collection of registered extensions
     */
    @Override
    public Collection<E> getAllExtensions() {
        return Collections.unmodifiableCollection(extensionsMap.values());
    }

    /**
     * Register the extension.  If the key is already registered in the registry, the new Extension provided will
     * overwrite the currently registered extension.
     * @param key the key
     * @param extension the extension
     */
    @Override
    public void register(final String key, final E extension) {
        E existingExtension = extensionsMap.put(key, extension);

        if (existingExtension != null) {
            getLogger().log(Level.WARNING, "Overwriting already registered extension with key {0}.");
        }

        getLogger().log(Level.INFO, "Registered extension {0} with key {1}",
                new Object[]{extension.getClass().getName(), key});
    }

    /**
     * @see ExtensionRegistry#unregister(String)
     */
    @Override
    public void unregister(final String key) {
        extensionsMap.remove(key);
        getLogger().log(Level.INFO, "Unregistered extension with key {0}", key);
    }
}
