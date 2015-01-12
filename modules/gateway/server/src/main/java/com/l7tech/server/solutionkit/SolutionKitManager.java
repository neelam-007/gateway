package com.l7tech.server.solutionkit;

import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.objectmodel.*;
import org.jetbrains.annotations.NotNull;

/**
 * Entity manager for {@link com.l7tech.gateway.common.solutionkit.SolutionKit}.
 */
public interface SolutionKitManager extends EntityManager<SolutionKit, SolutionKitHeader> {

    /**
     * Install the given bundle. This method will persist entities that are installed, but will not
     * persist solution kit entity to the database.
     *
     * @param bundle the bundle XML to install
     * @param isTest true if this is a test installation, no changes will be persisted; false otherwise
     * @return the resulting mappings XML
     * @throws SaveException
     * @throws SolutionKitException
     */
    @NotNull
    String installBundle(@NotNull String bundle, boolean isTest) throws SaveException, SolutionKitException;

    /**
     * Uninstall the given bundle. This method will delete entities that are uninstalled, but will not
     * delete solution kit entity from the database.
     *
     * @param goid the ID of solution kit entity
     * @throws FindException
     * @throws DeleteException
     * @throws SolutionKitException
     */
    void uninstallBundle(@NotNull Goid goid) throws FindException, DeleteException, SolutionKitException;
}