package com.l7tech.server.policy.export;

import com.l7tech.gateway.common.export.ExternalReferenceFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author ghuang
 */
public class PolicyExporterImporterManagerImpl implements PolicyExporterImporterManager {
    private final Set<ExternalReferenceFactory> factories = new ConcurrentSkipListSet<ExternalReferenceFactory>();

    @Override
    public Set<ExternalReferenceFactory> findAllExternalReferenceFactories() {
        return Collections.unmodifiableSet(new HashSet<ExternalReferenceFactory>(factories));
    }

    @Override
    public void register(ExternalReferenceFactory factory) {
        factories.add(factory);
    }

    @Override
    public void unregister(ExternalReferenceFactory factory) {
        factories.remove(factory);
    }
}