package com.l7tech.server.extension.registry;

import com.ca.apim.gateway.extension.Extension;
import com.ca.apim.gateway.extension.ExtensionAccess;
import com.ca.apim.gateway.extension.ExtensionRegistry;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract implementation for ExtensionRegistry and ExtensionAccess
 *
 * @param <E> Extension
 */
public abstract class AbstractRegistryImpl<E extends Extension> implements ExtensionRegistry<E>, ExtensionAccess<E> {

    private final ConcurrentMap<String, E> extensionsMap;
    private final ConcurrentMap<String, Collection<E>> tagExtensionMap;

    /**
     * Constructor
     */
    protected AbstractRegistryImpl() {
        extensionsMap = new ConcurrentHashMap<>();
        tagExtensionMap = new ConcurrentHashMap<>();
    }

    /**
     * Get logger.  The logger to be used to log messages in this class
     *
     * @return Logger
     */
    protected abstract Logger getLogger();

    /**
     * Get the registered extension
     *
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
     * Get all extensions registered with the given tags.
     *
     * @param tags The tags to return extensions with
     * @return The extensions with the given tags. An empty collection if no extensions exist with the given tags
     */
    @Override
    public Collection<E> getTaggedExtensions(final String... tags) {
        return Arrays.stream(tags)
                .flatMap(tag -> tagExtensionMap.getOrDefault(tag, Collections.emptySet()).stream())
                .collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableCollection));
    }

    /**
     * Get all registered extensions
     *
     * @return a Collection of registered extensions
     */
    @Override
    public Collection<E> getAllExtensions() {
        return Collections.unmodifiableCollection(extensionsMap.values());
    }

    /**
     * Register the extension.  If the key is already registered in the registry, the new Extension provided will
     * overwrite the currently registered extension.
     *
     * @param key       the key
     * @param extension the extension
     * @param tags      A list of tags to tag the extension with
     */
    @Override
    public void register(final String key, final E extension, final String... tags) {
        E existingExtension = extensionsMap.put(key, extension);

        for (final String tag : tags) {
            tagExtensionMap.merge(tag, new HashSet<>(Collections.singleton(extension)), (n, o) ->
                    Stream.concat(n.stream(), o.stream()).collect(Collectors.toSet()));
        }

        if (existingExtension != null) {
            tagExtensionMap.values().forEach(s -> s.remove(existingExtension));
            getLogger().log(Level.WARNING, "Overwriting already registered extension with key {0}.", key);
        }

        getLogger().log(Level.INFO, "Registered extension {0} with key {1}",
                new Object[]{extension.getClass().getName(), key});
    }

    /**
     * @see ExtensionRegistry#unregister(String)
     */
    @Override
    public void unregister(final String key) {
        final E extension = extensionsMap.remove(key);
        if (extension != null) {
            tagExtensionMap.values().forEach(s -> s.remove(extension));
        }
        getLogger().log(Level.INFO, "Unregistered extension with key {0}", key);
    }
}
