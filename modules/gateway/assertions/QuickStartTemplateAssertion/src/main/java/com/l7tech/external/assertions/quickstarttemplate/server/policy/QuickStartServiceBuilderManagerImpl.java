package com.l7tech.external.assertions.quickstarttemplate.server.policy;

import org.jetbrains.annotations.NotNull;

/**
 * Build service using com.l7tech.server.service.ServiceManager.
 * See ServiceAPIResourceFactory#createResourceInternal or  in com.l7tech.console.panels.AbstractPublishServiceWizard#checkResolutionConflictAndSave for example.
 */
public class QuickStartServiceBuilderManagerImpl implements QuickStartServiceBuilder {
    @Override
    public void createService() {
        // TODO
        throw new UnsupportedOperationException("createService not implemented");
    }

    @Override
    public <T> T createServiceBundle(@NotNull final Class<T> resType) throws Exception {
        // TODO
        throw new UnsupportedOperationException("createServiceBundle not implemented");
    }
}
