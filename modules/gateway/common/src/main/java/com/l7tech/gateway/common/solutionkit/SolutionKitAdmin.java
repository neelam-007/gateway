package com.l7tech.gateway.common.solutionkit;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
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
     * @param bundle the bundle XML to test
     * @return the resulting mapping XML
     */
    @NotNull
    @Transactional(readOnly = true)
    @Secured(stereotype = MethodStereotype.TEST_CONFIGURATION)
    JobId<String> testInstall(@NotNull String bundle);

    /**
     * Install the given solution kit.
     *
     * @param solutionKit the solution kit to install
     * @param bundle the bundle XML to install
     * @return the saved solution kit entity ID
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.SAVE)
    JobId<Goid> install(@NotNull SolutionKit solutionKit, @NotNull String bundle);

    /**
     *  Uninstall solution kit identified by the given ID.
     *
     * @param goid the ID of solution kit entity
     * @return an empty string
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.DELETE_BY_ID)
    JobId<String> uninstall(@NotNull Goid goid);
}