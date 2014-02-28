package com.l7tech.server.bundling;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;

/**
 * The bundle exporter will export an entity bundle from this gateway.
 */
public interface EntityBundleExporter {

    /**
     * Exports a bundle given a list of entity headers to export. This will find all the dependencies needed to import
     * the bundle.
     *
     * @param bundleExportProperties Properties to export the bundle with.
     * @param headers                The entity headers to export
     * @return The entity bundle containing all entities in the bundle and the mappings required to import them.
     * @throws FindException
     */
    @NotNull
    public EntityBundle exportBundle(@NotNull final Properties bundleExportProperties, @NotNull final EntityHeader... headers) throws FindException;
}
