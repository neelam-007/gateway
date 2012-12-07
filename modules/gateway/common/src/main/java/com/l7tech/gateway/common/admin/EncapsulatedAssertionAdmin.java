package com.l7tech.gateway.common.admin;

import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import org.jetbrains.annotations.NotNull;
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
     * Find specified encapsulated assertion template by its oid.
     *
     * @param oid encapsulated assertion OID to look up.
     * @return the requested OID.  Never null.
     * @throws FindException if not found, or there was an error performing the lookup
     */
    @NotNull
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITY)
    EncapsulatedAssertionConfig findByPrimaryKey(long oid) throws FindException;

    /**
     * Saves a new or existing encapsulated assertion templates.
     * @param config the {@link EncapsulatedAssertionConfig} to save.  Required.
     * @return the object id (oid) of the newly saved config
     * @throws com.l7tech.objectmodel.SaveException if there was a server-side problem saving the config
     * @throws com.l7tech.objectmodel.UpdateException if there was a server-side problem updating the cconfig
     * @throws com.l7tech.objectmodel.VersionException if the updated config was not up-to-date (updating an old version)
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    public long saveEncapsulatedAssertionConfig(@NotNull EncapsulatedAssertionConfig config) throws SaveException, UpdateException, VersionException;

    /**
     * Removes the specified {@link EncapsulatedAssertionConfig} from the database.
     * @param oid the oid of the config to be deleted
     * @throws FindException if the config cannot be found
     * @throws DeleteException if the config cannot be deleted
     * @throws ConstraintViolationException if the config cannot be deleted
     */
    @Secured(stereotype= DELETE_BY_ID)
    public void deleteEncapsulatedAssertionConfig(long oid) throws FindException, DeleteException, ConstraintViolationException;

}
