package com.l7tech.gateway.common.solutionkit;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * Remote admin interface for managing {@link com.l7tech.gateway.common.solutionkit.SolutionKit}.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types=EntityType.SOLUTION_KIT)
@Administrative
public interface SolutionKitAdmin extends AsyncAdminMethods {

    /**
     * Get a list of solution kits for upgrade, depending on the following three cases:
     * Case 1: if the selected solution kit is a child, then add the parent and the selected child into the return list.
     * Case 2: if the selected solution kit is a neither parent nor child, then add only the selected solution kit into the return list.
     * Case 3: if the selected solution kit is a parent, then add the parent and all children into the return list.
     *
     * @param solutionKit: the selected solution kit, which user selects to upgrade.
     * @return a list of solution kits for upgrade
     */
    @NotNull
    @Transactional(readOnly = true)
    @Secured(stereotype = MethodStereotype.FIND_ENTITIES)
    List<SolutionKit> getSolutionKitsToUpgrade(@Nullable SolutionKit solutionKit) throws FindException;   // TODO move to SkUpgrader?  rename method?

    /**
     * Test installation for the given bundle.
     *
     * @param solutionKit the solution kit to test
     * @param bundle the bundle XML to test
     * @param isUpgrade indicate if the solution kit is to be upgraded or installed.
     * @return the resulting mapping XML
     */
    @NotNull
    @Transactional(readOnly = true)
    @Secured(stereotype = MethodStereotype.TEST_CONFIGURATION)
    public String testInstall(@NotNull final SolutionKit solutionKit, @NotNull final String bundle, final boolean isUpgrade) throws Exception;

    /**
     * Test installation for the given bundle.
     *
     * @param solutionKit the solution kit to test
     * @param bundle the bundle XML to test
     * @param isUpgrade indicate if the solution kit is to be upgraded or installed.
     * @return the resulting mapping XML
     */
    @NotNull
    @Transactional(readOnly = true)
    @Secured(stereotype = MethodStereotype.TEST_CONFIGURATION)
    JobId<String> testInstallAsync(@NotNull SolutionKit solutionKit, @NotNull String bundle, boolean isUpgrade);

    /**
     * Install the given solution kit.
     *
     * @param solutionKit the solution kit to install
     * @param bundle the bundle XML to install
     * @param isUpgrade true if this is a upgrade install; false for new first time install
     * @return the saved solution kit entity ID
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.SAVE)
    public Goid install(@NotNull final SolutionKit solutionKit, @NotNull final String bundle, final boolean isUpgrade) throws Exception;

    /**
     * Install the given solution kit.
     *
     * @param solutionKit the solution kit to install
     * @param bundle the bundle XML to install
     * @param isUpgrade true if this is a upgrade install; false for new first time install
     * @return the saved solution kit entity ID
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.SAVE)
    JobId<Goid> installAsync(@NotNull SolutionKit solutionKit, @NotNull String bundle, boolean isUpgrade);

    /**
     * Uninstall solution kit identified by the given ID.
     *
     * @param goid the ID of solution kit entity
     * @return an empty string
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.DELETE_BY_ID)
    public String uninstall(@NotNull final Goid goid) throws Exception;

    /**
     *  Uninstall solution kit identified by the given ID.
     *
     * @param goid the ID of solution kit entity
     * @return an empty string
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.DELETE_BY_ID)
    JobId<String> uninstallAsync(@NotNull Goid goid);


    /* When to use "get" and "find"? "get" a single solution kit; "find" a bunch of solution kits. */

    /**
     * Retrieve all solution kit entity headers.
     *
     * @return a collection of solution kit entity headers
     * @throws FindException
     */
    @NotNull
    @Transactional(readOnly = true)
    @Secured(stereotype = MethodStereotype.FIND_HEADERS)
    public Collection<SolutionKitHeader> findHeaders() throws FindException;

    /**
     * Retrieve all child solution kit headers, whose parent's GOID is the same as a given parentGoid.
     * @param parentGoid parent GOID
     * @return a collection of child solution kit headers associated with parentGoid.
     * @throws FindException
     */
    @NotNull
    @Transactional(readOnly = true)
    @Secured(stereotype = MethodStereotype.FIND_HEADERS)
    public Collection<SolutionKitHeader> findHeaders(@NotNull final Goid parentGoid) throws FindException;

    /**
     * Find a list of Solution Kits by a given GUID.
     * @param solutionKitGuid Solution Kit's globally unique identifier (author specified)
     * @return the list of Solution Kits, whose GUID matches the given GUID.
     * @throws FindException
     */
    @NotNull
    @Transactional(readOnly = true)
    @Secured(stereotype = MethodStereotype.FIND_ENTITIES)
    public Collection<SolutionKit> find(@NotNull final String solutionKitGuid) throws FindException;

    /**
     * Retrieve all child solution kits, whose parent's GOID is the same as a given parentGoid.
     * @param parentGoid parent GOID
     * @return the list of Solution Kits, whose GUID matches the given GUID.
     * @throws FindException
     */
    @NotNull
    @Transactional(readOnly = true)
    @Secured(stereotype = MethodStereotype.FIND_ENTITIES)
    public Collection<SolutionKit> find(@NotNull final Goid parentGoid) throws FindException;


    /**
     * Retrieve solution kit entity with the given ID.
     *
     * @param goid the ID of solution kit entity
     * @return the solution kit entity
     * @throws FindException
     */
    @Transactional(readOnly = true)
    @Secured(stereotype = MethodStereotype.FIND_ENTITY)
    public  SolutionKit get(@NotNull Goid goid) throws FindException;

    /**
     * Save a solution kit
     * @param solutionKit: a solution kit to be saved.
     * @return a Goid of the saved solution kit
     * @throws SaveException
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.SAVE)
    public Goid save(@NotNull SolutionKit solutionKit) throws SaveException;

    /**
     * Update a solution kit
     * @param solutionKit: a solution kit to be updated.
     * @throws UpdateException
     */
    @Secured(stereotype = MethodStereotype.UPDATE)
    void update(@NotNull SolutionKit solutionKit) throws UpdateException;

    /**
     * Delete a solution kit by Goid
     * @param goid: the Goid of the solution kit to be deleted.
     * @throws FindException
     * @throws DeleteException
     */
    @Secured(stereotype = MethodStereotype.DELETE_BY_ID)
    void delete(@NotNull Goid goid) throws FindException, DeleteException;
}