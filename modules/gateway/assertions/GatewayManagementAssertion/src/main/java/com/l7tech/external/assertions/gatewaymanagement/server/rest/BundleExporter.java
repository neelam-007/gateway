package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.BundleTransformer;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.bundling.EntityBundle;
import com.l7tech.server.bundling.EntityBundleExporter;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Properties;

/**
 * This is used to create a bundle export.
 */
public class BundleExporter {
    @Inject
    private EntityBundleExporter entityBundleExporter;
    @Inject
    private BundleTransformer bundleTransformer;


    //DEFAULTS and options
    public static final String IncludeRequestFolderOption = "IncludeRequestFolder";
    public static final String DefaultMappingActionOption = "DefaultMappingAction";
    public static final String DefaultMapByOption = "DefaultMapBy";
    public static final String IgnoredEntityIdsOption = "IgnoredEntityIds";

    /**
     * Creates a bundle export given the export options
     *
     * @param bundleExportOptions A map of export options. Can be null to use the defaults
     * @param headers             The list of headers to create the export from.
     * @return The bundle generated from the headers given
     * @throws FindException
     */
    @NotNull
    public Bundle exportBundle(@Nullable Properties bundleExportOptions, @NotNull EntityHeader... headers) throws FindException, CannotRetrieveDependenciesException {
        EntityBundle entityBundle = entityBundleExporter.exportBundle(bundleExportOptions == null ? new Properties() : bundleExportOptions, headers);
        return bundleTransformer.convertToMO(entityBundle);
    }
}
