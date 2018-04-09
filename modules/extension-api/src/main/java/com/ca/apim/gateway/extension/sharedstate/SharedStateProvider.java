package com.ca.apim.gateway.extension.sharedstate;

import com.ca.apim.gateway.extension.Extension;

/**
 * A SharedStateProvider is a type of extension that provides specific implementation of
 * SharedKeyValueStore that shares key value pairs amongst a gateway cluster.
 */
public interface SharedStateProvider extends Extension {

    /**
     * Get the key value store identified by the name or create and return the key value store with the provided configuration
     * @param name the name of the key value store
     * @param config the configuration of the key value store
     * @return SharedKeyValueStore
     */
    SharedKeyValueStore getKeyValueStore(String name, Configuration config);
}
