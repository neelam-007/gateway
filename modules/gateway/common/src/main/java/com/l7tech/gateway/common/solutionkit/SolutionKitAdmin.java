package com.l7tech.gateway.common.solutionkit;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SignatureException;
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
     * Find a list of Solution Kits by a given GUID.
     * @param solutionKitGuid Solution Kit's globally unique identifier (author specified)
     * @return the list of Solution Kits, whose GUID matches the given GUID.
     * @throws FindException
     */
    @NotNull
    @Transactional(readOnly = true)
    @Secured(stereotype = MethodStereotype.FIND_ENTITIES)
    Collection<SolutionKit> findBySolutionKitGuid(@NotNull final String solutionKitGuid) throws FindException;

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
     * @return a Goid of the saved solution kit
     * @throws SaveException
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.SAVE)
    Goid saveSolutionKit(@NotNull SolutionKit solutionKit) throws SaveException;

    /**
     * Update a solution kit
     * @param solutionKit: a solution kit to be updated.
     * @throws UpdateException
     */
    @Secured(stereotype = MethodStereotype.UPDATE)
    void updateSolutionKit(@NotNull SolutionKit solutionKit) throws UpdateException;

    /**
     * Delete a solution kit by Goid
     * @param goid: the Goid of the solution kit to be deleted.
     * @throws FindException
     * @throws DeleteException
     */
    @Secured(stereotype = MethodStereotype.DELETE_BY_ID)
    void deleteSolutionKit(@NotNull Goid goid) throws FindException, DeleteException;

    /**
     * Checks signature and also verifies that signer cert is trusted.
     *
     * @param digest                 SHA-256 digest of the raw input material (i.e. Skar file raw bytes).  Required and cannot be {@code null}.
     *                               Note: this MUST NOT just be the value claimed by the sender -- it must be a freshly
     *                               computed value from hashing the information covered by the signature.
     * @param signatureProperties    Signature properties reader, holding ASN.1 encoded X.509 certificate as Base64 string
     *                               and ASN.1 encoded signature value as Base64 string.
     * @throws SignatureException if signature cannot be validated or signer cert is not trusted.
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    @Administrative(licensed=false, background = true)
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    void verifySkarSignature(@NotNull final byte[] digest, @Nullable final String signatureProperties) throws SignatureException;

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
    List<SolutionKit> getSolutionKitsToUpgrade(@Nullable SolutionKit solutionKit) throws FindException;
}