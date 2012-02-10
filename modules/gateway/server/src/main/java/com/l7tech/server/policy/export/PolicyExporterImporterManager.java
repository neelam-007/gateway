package com.l7tech.server.policy.export;

import com.l7tech.gateway.common.export.ExternalReferenceFactory;

import java.util.Set;

/**
 * @author ghuang
 */
public interface PolicyExporterImporterManager {
    /**
     * Find all ExternalReferenceFactory's, which have been registered when the gateway loads modular assertions.
     * @return a set of ExternalReferenceFactory's
     */
    Set<ExternalReferenceFactory> findAllExternalReferenceFactories();

    /**
     * To register a ExternalReferenceFactory
     * @param factory: a factory to be registered
     */
    void register(ExternalReferenceFactory factory);

    /**
     * To unregister a ExternalReferenceFactory
     * @param factory: a factory to be unregistered
     */
    void unregister(ExternalReferenceFactory factory);
}