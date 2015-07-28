package com.l7tech.server.solutionkit;

import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.objectmodel.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Entity manager for {@link com.l7tech.gateway.common.solutionkit.SolutionKit}.
 */
public interface SolutionKitManager extends EntityManager<SolutionKit, SolutionKitHeader> {

    /**
     * Import the given bundle. This method will persist entities that are installed / upgraded / uninstalled, but will not
     * persist solution kit entity to the database.
     *
     * @param bundle the bundle XML to install
     * @param instanceModifier the prefix used to distinguish solution kit instances.
     * @param isTest true if this is a test installation, no changes will be persisted; false otherwise
     * @return the resulting mappings XML
     * @throws SaveException
     * @throws SolutionKitException
     */
    @NotNull
    String importBundle(@NotNull final String bundle, @Nullable final String instanceModifier, final boolean isTest) throws SaveException, SolutionKitException;

    /**
     * Find a list of Solution Kits by a given GUID.
     * @param solutionKitGuid Solution Kit's globally unique identifier (author specified)
     * @return the list of Solution Kits, whose GUID matches the given GUID.
     * @throws FindException
     */
    List<SolutionKit> findBySolutionKitGuid(@NotNull final String solutionKitGuid) throws FindException;

    /**
     * Reads and enables entity protection for all Solution Kit-owned entities. This method should be invoked
     * after any EntityOwnershipDescriptors have been added e.g. in the process of Solution Kit installation/upgrade/removal.
     * @throws FindException
     */
    void updateProtectedEntityTracking() throws FindException;
}