package com.l7tech.server.export;

import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.server.policy.export.PolicyExporterImporterManager;

import java.util.Set;

/**
 * @author ghuang
 */
public class PolicyExporterImporterManagerStub implements PolicyExporterImporterManager {
    @Override
    public Set<ExternalReferenceFactory> findAllExternalReferenceFactories() {
        return null;
    }

    @Override
    public void register(ExternalReferenceFactory factory) {
    }

    @Override
    public void unregister(ExternalReferenceFactory factory) {
    }
}