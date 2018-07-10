package com.l7tech.server.bundling;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The bundle importer will import entity bundles to this gateway. Entities in the bundles will be mapped to existing
 * entities on the gateway using the bundle mappings
 */
public interface EntityBundleImporter {

    /**
     * This will import the given entity bundles. If test is true or there is an error during bundles import nothing is
     * committed and all changes that were made are rolled back
     *
     * @param bundles The bundles to import
     * @param test   if true the bundles import will be performed but rolled back afterwards and the results of the
     *               import will be returned. If false the bundles import will be committed it if is successful.
     * @param active True to activate the updated services and policies.
     * @param versionComment The comment to set for updated/created services and policies
     * @return A list of mapping results of the bundles import.
     */
    @NotNull
    public List<List<EntityMappingResult>> importBundles(@NotNull final List<EntityBundle> bundles, final boolean test, final boolean active, @Nullable final String versionComment);
}
