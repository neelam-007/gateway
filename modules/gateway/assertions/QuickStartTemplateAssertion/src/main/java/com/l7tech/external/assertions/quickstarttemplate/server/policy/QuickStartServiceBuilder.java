package com.l7tech.external.assertions.quickstarttemplate.server.policy;

/**
 * Common interface for building a service.  Implementations:
 *      1. build using restman bundle
 *      2. build using com.l7tech.server.service.ServiceManager
 */
public interface QuickStartServiceBuilder {
    /**
     * TODO now that we set the service bundle as a context variable perhaps we can remove this method
      */
    void createService() throws Exception;

    <T> T createServiceBundle(final Class<T> resType) throws Exception;
}
