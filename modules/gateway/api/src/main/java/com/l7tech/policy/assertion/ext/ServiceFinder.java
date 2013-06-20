package com.l7tech.policy.assertion.ext;

/**
 * Locates Layer 7 API Services available to an in-process custom assertion.  You would typically get hold of an implementation
 * of this interface by looking up the "serviceFinder" key in the context map.
 */
public interface ServiceFinder {

    /**
     * Obtain an implementation of a Layer 7 API service.  For available services see the Layer 7 API documentation.
     * Not all services may be available based on gateway license, configuration and version.
     *
     * @param serviceInterface the interface class of the desired service, required
     * @param <T> class that implements a Layer 7 API service
     * @return requested service or null
     */
    public <T> T lookupService(Class<T> serviceInterface);
}
