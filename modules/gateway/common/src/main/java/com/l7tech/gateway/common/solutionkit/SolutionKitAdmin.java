package com.l7tech.gateway.common.solutionkit;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * Remote admin interface for managing {@link com.l7tech.gateway.common.solutionkit.SolutionKit}.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types=EntityType.SOLUTION_KIT)
@Administrative
public interface SolutionKitAdmin extends AsyncAdminMethods {

    /**
     * Retrieve all solution kit entity headers.
     *
     * @return a collection of solution kit entity headers
     * @throws FindException
     */
    @NotNull
    @Transactional(readOnly = true)
    @Secured(stereotype = MethodStereotype.FIND_HEADERS)
    Collection<SolutionKitHeader> findSolutionKits() throws FindException;

    /**
     * Retrieve all child solution kits, whose parent's GOID is the same as a given parentGoid.
     *
     * @return a collection of child solution kits associated with parentGoid.
     * @throws FindException
     */
    @NotNull
    @Transactional(readOnly = true)
    @Secured(stereotype = MethodStereotype.FIND_HEADERS)
    Collection<SolutionKitHeader> findAllChildrenByParentGoid(Goid parentGoid) throws FindException;

    /**
     * Retrieve all solution kits except child solution kits.
     *
     * @return a collection of solution kits except child solution kits.
     * @throws FindException
     */
    @NotNull
    @Transactional(readOnly = true)
    @Secured(stereotype = MethodStereotype.FIND_HEADERS)
    Collection<SolutionKitHeader> findAllExcludingChildren() throws FindException;

    /**
     * Retrieve all solution kits, which have child solution kit(s).
     *
     * @return a collection of solution kits having child solution kit(s).
     * @throws FindException
     */
    @NotNull
    @Transactional(readOnly = true)
    @Secured(stereotype = MethodStereotype.FIND_HEADERS)
    Collection<SolutionKitHeader> findParentSolutionKits() throws FindException;

    /**
     * Retrieve solution kit entity with the given ID.
     *
     * @param goid the ID of solution kit entity
     * @return the solution kit entity
     * @throws FindException
     */
    @Transactional(readOnly = true)
    @Secured(stereotype = MethodStereotype.FIND_ENTITY)
    SolutionKit get(@NotNull Goid goid) throws FindException;

    /**
     * Test installation for the given bundle.
     *
     * @param solutionKit the solution kit to test
     * @param bundle the bundle XML to test
     * @return the resulting mapping XML
     */
    @NotNull
    @Transactional(readOnly = true)
    @Secured(stereotype = MethodStereotype.TEST_CONFIGURATION)
    JobId<String> testInstall(@NotNull SolutionKit solutionKit, @NotNull String bundle);

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
    JobId<Goid> install(@NotNull SolutionKit solutionKit, @NotNull String bundle, boolean isUpgrade);

    /**
     *  Uninstall solution kit identified by the given ID.
     *
     * @param goid the ID of solution kit entity
     * @return an empty string
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.DELETE_BY_ID)
    JobId<String> uninstall(@NotNull Goid goid);

    /**
     * Save a solution kit
     * @param solutionKit: a solution kit to be saved.
     * @return a Goid of the saved sollution kit
     * @throws SaveException
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.SAVE)
    Goid saveSolutionKit(@NotNull SolutionKit solutionKit) throws SaveException;

    /**
     * Delete a solution kit by Goid
     * @param goid: the Goid of the solution kit to be deleted.
     * @throws FindException
     * @throws DeleteException
     */
    @Secured(stereotype = MethodStereotype.DELETE_BY_ID)
    void deleteSolutionKit(@NotNull Goid goid) throws FindException, DeleteException;
}