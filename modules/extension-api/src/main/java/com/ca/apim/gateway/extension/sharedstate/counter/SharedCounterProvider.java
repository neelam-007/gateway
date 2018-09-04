package com.ca.apim.gateway.extension.sharedstate.counter;

import com.ca.apim.gateway.extension.Extension;

/**
 * A SharedCounterProvider is a type of extension that provides specific implementation of SharedCounterStores for
 * sharing counters in a gateway cluster.
 */
public interface SharedCounterProvider extends Extension {

    /**
     * Get the counter store identified by the name or create and return the counter store with the provided configuration
     * May throw an unchecked exception.
     * @param name the name of the counter store
     * @return SharedCounterStore
     */
    SharedCounterStore getCounterStore(String name);

    String getName();
}
