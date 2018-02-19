package com.ca.apim.gateway.extension;


import java.util.Collection;

/**
 * ExtensionAccess allows for retrieval of registered Extensions
 * @param <E> Extension
 */
public interface ExtensionAccess <E extends Extension> {
    /**
     * Get the registered extension with the specified key
     * @param key the key used to register the extension
     * @return the Extension
     */
     E getExtension(final String key);

    /**
     * Get all extensions that are registered
     * @return an immutable collection of extensions registered
     */
    Collection<E> getAllExtensions();
}
