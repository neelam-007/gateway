package com.l7tech.external.assertions.quickstarttemplate.server.policy;

/**
 * Common interface for building a service.  Implementations:
 *      1. build using restman bundle
 *      2. build using com.l7tech.server.service.ServiceManager
 */
public interface QuickStartServiceBuilder {
    <T> T createServiceBundle(final Class<T> resType) throws Exception;
}
