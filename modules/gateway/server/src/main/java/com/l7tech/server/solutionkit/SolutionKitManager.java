package com.l7tech.server.solutionkit;

import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.gateway.common.solutionkit.SolutionKitInfo;
import com.l7tech.objectmodel.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
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
     * @param metadata solution kit metadata, including the instance modifier (prefix used to distinguish solution kit instances).
     * @param isTest true if this is a test installation, no changes will be persisted; false otherwise
     * @return the resulting mappings XML
     * @throws SaveException
     * @throws SolutionKitException
     */
    @NotNull
    String importBundle(@NotNull final String bundle, @NotNull final SolutionKit metadata, final boolean isTest) throws Exception;

    /**
     * Import the given bundles. This method will persist entities that are installed / upgraded / uninstalled, but will not
     * persist solution kit entity to the database.
     *
     * @param solutionKitInfo The information needed for multi bundle import such as delete bundles and install bundles.
     *                        Also holds solution kits metadata
     * @param isTest true if this is a test installation, no changes will be persisted; false otherwise
     * @return the resulting mappings XML
     * @throws Exception A variety of exceptions from solution kits and restman
     */
    @NotNull
    String importBundles(@NotNull final SolutionKitInfo solutionKitInfo, final boolean isTest) throws Exception;
    /**
     * Find a list of Solution Kits by a given GUID.
     * @param solutionKitGuid Solution Kit's globally unique identifier (author specified)
     * @return the list of Solution Kits, whose GUID matches the given GUID.
     * @throws FindException
     */
    List<SolutionKit> findBySolutionKitGuid(@NotNull final String solutionKitGuid) throws FindException;

    SolutionKit findBySolutionKitGuidAndIM(@NotNull final String solutionKitGuid, @Nullable String instanceModifier) throws FindException;


    // TODO: ghuang; consider to deprecate this method and use {@link #findHeaders(com.l7tech.objectmodel.Goid)}
    // TODO: as in most of the cases the caller will get the {@link com.l7tech.gateway.common.solutionkit.SolutionKit} object anyways
    // TODO: which eventually will produce way more DB traffic and transactions

     /**
     * Find all child solution kit headers of a parent solution kit given by a parent Goid.
     *
     * @param parentGoid: the Goid of a parent solution kit
     * @return a list of child solution kits
     * @throws FindException
     */
    List<SolutionKitHeader> findAllChildrenHeadersByParentGoid(@NotNull final Goid parentGoid) throws FindException;

    /**
     * Find all child solution kit headers of a parent solution kit given by a parent Goid.
     * @param parentGoid: the Goid of a parent solution kit
     * @return a list of child solution kits
     * @throws FindException
     */
    @NotNull
    Collection<SolutionKit> findAllChildrenByParentGoid(@NotNull final Goid parentGoid) throws FindException;

    /**
     * Updates entities readonly-ness. Entities are specified by {@code entityIds} and owned by other kits then the specified {@code solutionKit}.<br/>
     * This is done by decrementing version_stamp for all specified entities owned by other solution kits (as this {@code solutionKit} now takes over readonly-ness).
     * <p/>
     * This is a workaround for an edge case where an entity owned by solution kit 1 (aka. sk1) is deleted and recreated by solution kit 2 (aka. sk2),
     * thus taking over entity readonly-ness (i.e. {@link com.l7tech.server.security.rbac.ProtectedEntityTracker ProtectedEntityTracker}
     * would prioritize entity readonly flag from sk2 over sk1).<br/>
     * This allows entity readonly flag to be updated from both sk1 and sk2, therefore {@code ProtectedEntityTracker} would prioritize
     * entity readonly flag from the last solution kit that made the update (of course including recreation of the entity).<br/>
     * Traditional timestamp is not used here, as in a cluster environment there is no guarantee that the nodes clocks would be in sync.<br/>
     * In addition entity ownership is not removed from sk1 as sk2 might be deleted in the future (without removing the entity itself)
     * thus preventing the only entity owner sk1 to update entity readonly flag.
     *
     * @param entityIds      Collection of entity-ids to update.  Required and cannot be {@code null}.
     * @param solutionKit    SolutionKit that takes over readonly-ness for the specified entities.  Required and cannot be {@code null}.
     * @throws com.l7tech.objectmodel.UpdateException  if a DB error happens while updating the entities.
     */
    void decrementEntitiesVersionStamp(@NotNull final Collection<String> entityIds, @NotNull final Goid solutionKit) throws UpdateException;
}