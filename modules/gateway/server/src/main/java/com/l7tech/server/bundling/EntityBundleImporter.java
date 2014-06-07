package com.l7tech.server.bundling;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The bundle importer will import an entity bundle to this gateway. Entities in the bundle will be mapped to existing
 * entities on the gateway using the bundle mappings
 */
public interface EntityBundleImporter {

    /**
     * This will import the given entity bundle. If test is true or there is an error during bundle import nothing is
     * committed and all changes that were made are rolled back
     *
     * @param bundle The bundle to import
     * @param test   if true the bundle import will be performed but rolled back afterwards and the results of the
     *               import will be returned. If false the bundle import will be committed it if is successful.
     * @param active True to activate the updated services and policies.
     * @param versionComment The comment to set for updated/created services and policies
     * @return The mapping results of the bundle import.
     */
    @NotNull
    public List<EntityMappingResult> importBundle(@NotNull final EntityBundle bundle, final boolean test, final boolean active, @Nullable final String versionComment);
}
