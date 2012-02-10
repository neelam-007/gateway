package com.l7tech.server.policy.export;

import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.gateway.common.export.PolicyExporterImporterAdmin;

import java.util.Set;

/**
 * @author ghuang
 */
public class PolicyExporterImporterAdminImpl implements PolicyExporterImporterAdmin {
    private PolicyExporterImporterManager policyExporterImporterManager;

    public PolicyExporterImporterAdminImpl(PolicyExporterImporterManager policyExporterImporterManager) {
        this.policyExporterImporterManager = policyExporterImporterManager;
    }

    @Override
    public Set<ExternalReferenceFactory> findAllExternalReferenceFactories() {
        return policyExporterImporterManager.findAllExternalReferenceFactories();
    }
}