package com.ca.apim.gateway.extension;

/**
 * ExtensionRegistry allows for registering Extension
 * @param <E> Extension
 */
public interface ExtensionRegistry<E extends Extension> {
    /**
     * Register the Extension with key
     * @param key the key
     * @param extension the extension
     */
    void register(final String key, final E extension);

    /**
     * Unregister the Extension
     * @param key the registered key for the Extension
     */
    void unregister(final String key);
}
