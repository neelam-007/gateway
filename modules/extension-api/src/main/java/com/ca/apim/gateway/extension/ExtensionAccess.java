package com.ca.apim.gateway.extension;


import java.util.Collection;

/**
 * ExtensionAccess allows for retrieval of registered Extensions
 *
 * @param <E> Extension
 */
public interface ExtensionAccess<E extends Extension> {
    /**
     * Get the registered extension with the specified key
     *
     * @param key the key used to register the extension
     * @return the Extension
     */
    E getExtension(final String key);

    /**
     * Get all extensions that are registered
     *
     * @return an immutable collection of extensions registered
     */
    Collection<E> getAllExtensions();

    /**
     * Returns all extensions with the given tags
     *
     * @param tags The tags to return extensions with
     * @return The extensions with the given tags. An empty collection if no extensions exist with the given tags
     */
    Collection<E> getTaggedExtensions(final String... tags);
}
