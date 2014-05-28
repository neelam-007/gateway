package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.BundleTransformer;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.server.bundling.EntityBundle;
import com.l7tech.server.bundling.EntityBundleImporter;
import com.l7tech.server.bundling.EntityMappingResult;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.List;

/**
 * Imports a bundle. This transforms the bundle to an entity bundle and used the entity bundle importer to import the
 * bundle.
 */
public class BundleImporter {
    @Inject
    private EntityBundleImporter entityBundleImporter;
    @Inject
    private BundleTransformer bundleTransformer;

    /**
     * Imports the bundle
     * @param bundle The bundle to import
     * @param test if true the bundle import is tested, no changes will be persisted
     * @param active True to activate the updated services and policies.
     * @param versionComment The comment to set for updated/created services and policies
     * @return Returns the resulting mappings for the bundle import
     * @throws ResourceFactory.InvalidResourceException
     */
    @NotNull
    public List<Mapping> importBundle(@NotNull final Bundle bundle, final boolean test, final boolean active, final String versionComment) throws ResourceFactory.InvalidResourceException {
        EntityBundle entityBundle = bundleTransformer.convertFromMO(bundle).getEntity();
        List<EntityMappingResult> mappingsPerformed = entityBundleImporter.importBundle(entityBundle, test, active, versionComment);
        return bundleTransformer.updateMappings(bundle.getMappings(), mappingsPerformed);
    }
}
