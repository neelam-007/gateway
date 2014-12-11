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
     * Installs specified bundle. Does not save solution kit entity.
     *
     * @param solutionKit
     * @param bundle
     * @param isTest
     * @return
     * @throws SaveException
     * @throws SolutionKitException
     */
    @NotNull
    String installBundle(@NotNull SolutionKit solutionKit, @NotNull String bundle, boolean isTest) throws SaveException, SolutionKitException;

    /**
     * Uninstalled bundle associated with the specified solution kit GOID. Does delete solution kit entity.
     * @param goid
     * @throws FindException
     * @throws DeleteException
     * @throws SolutionKitException
     */
    void uninstallBundle(@NotNull Goid goid) throws FindException, DeleteException, SolutionKitException;
}