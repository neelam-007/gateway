package com.l7tech.gateway.common.admin;

import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;
import static com.l7tech.objectmodel.EntityType.ENCAPSULATED_ASSERTION;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * Remote admin interface for managing {@link com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig} instances
 * and related configuration on the Gateway.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types=ENCAPSULATED_ASSERTION)
@Administrative
public interface EncapsulatedAssertionAdmin {

    /**
     * Find all registered encapsulated assertion templates.
     *
     * @return a collection of EncapsulatedAssertionConfig instances.  Never null but may be empty.
     * @throws FindException if there was a problem reading configs from the database.
     */
    @NotNull
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    Collection<EncapsulatedAssertionConfig> findAllEncapsulatedAssertionConfigs() throws FindException;

    /**
     * Find specified encapsulated assertion template by its goid.
     *
     * @param goid encapsulated assertion GOID to look up.
     * @return the requested config.  Never null.
     * @throws FindException if not found, or there was an error performing the lookup
     */
    @NotNull
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITY)
    EncapsulatedAssertionConfig findByPrimaryKey(Goid goid) throws FindException;

    /**
     * Find specified encapsulated assertion template by its GUID.
     *
     * @param guid GUID to look up.  Required.
     * @return the requested config.  Never null.
     * @throws FindException if not found, or there was an error performing the lookup
     */
    @NotNull
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITY)
    EncapsulatedAssertionConfig findByGuid(@NotNull String guid) throws FindException;

    /**
     * Find all active/enabled encapsulated assertion configs that reference the specified policy GOID as the
     * backing policy.
     *
     * @param policyGoid the policy GOID to check.
     * @return a collection of encapsulated assertion configs that reference this policy GOID.  May be empty but never null.
     * @throws FindException if there is a problem accessing the database
     */
    @NotNull
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    Collection<EncapsulatedAssertionConfig> findByPolicyGoid(Goid policyGoid) throws FindException;

    /**
     * Find an EncapsulatedAssertionConfig by name.
     *
     * @param name the name of the EncapsulatedAssertionConfig to find.
     * @return the EncapsulatedAssertionConfig with the given name or null if none found.
     * @throws FindException if an error occurs accessing the database.
     */
    @Nullable
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITY)
    EncapsulatedAssertionConfig findByUniqueName(@NotNull final String name) throws FindException;

    /**
     * Saves a new or existing encapsulated assertion templates.
     * @param config the {@link EncapsulatedAssertionConfig} to save.  Required.
     * @return the global object id (goid) of the newly saved config
     * @throws com.l7tech.objectmodel.SaveException if there was a server-side problem saving the config
     * @throws com.l7tech.objectmodel.UpdateException if there was a server-side problem updating the cconfig
     * @throws com.l7tech.objectmodel.VersionException if the updated config was not up-to-date (updating an old version)
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    public Goid saveEncapsulatedAssertionConfig(@NotNull EncapsulatedAssertionConfig config) throws SaveException, UpdateException, VersionException;

    /**
     * Removes the specified {@link EncapsulatedAssertionConfig} from the database.
     * @param goid the goid of the config to be deleted
     * @throws FindException if the config cannot be found
     * @throws DeleteException if the config cannot be deleted
     * @throws ConstraintViolationException if the config cannot be deleted
     */
    @Secured(stereotype= DELETE_BY_ID)
    public void deleteEncapsulatedAssertionConfig(Goid goid) throws FindException, DeleteException, ConstraintViolationException;

}
