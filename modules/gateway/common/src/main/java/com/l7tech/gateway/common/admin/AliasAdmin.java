package com.l7tech.gateway.common.admin;

import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import org.springframework.transaction.annotation.Transactional;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.DELETE_BY_ID;
import static com.l7tech.gateway.common.security.rbac.MethodStereotype.SAVE_OR_UPDATE;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * Admin interface for managing aliased entities
 *
 * @author darmstrong
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types= {EntityType.SERVICE_ALIAS, EntityType.POLICY_ALIAS})
public interface AliasAdmin<HT extends Alias> {

    /**
     * Store the specified new or existing alias. If the specified {@link com.l7tech.objectmodel.Alias} contains a
     * unique object ID that already exists, this will replace the objects current configuration with the new configuration.
     * Otherwise, a new object will be created.
     *
     * <p>The only reason to save an alias is if you are creating a new alias.</p>
     *
     * @param alias
     * @return
     * @throws com.l7tech.objectmodel.SaveException   if the requested information could not be saved
     * @throws com.l7tech.objectmodel.UpdateException if the requested information could not be updated
     * @throws com.l7tech.objectmodel.VersionException if the service version conflict is detected
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    Goid saveAlias(HT alias)
            throws UpdateException, SaveException, VersionException;

    /**
     * Find an Alias based on the original entities oid and the folderGoid in which the alias currently resides
     * @param entityGoid goid of the real entity
     * @param folderGoid folder goid of the folder in which the alias currently resides
     * @return The alias of the entityGoid supplied if found, null otherwise
     * @throws com.l7tech.objectmodel.FindException
     */
    @Transactional(readOnly=true)
    @Secured(stereotype= MethodStereotype.FIND_ENTITY)
    @Administrative(licensed=false)
    public HT findAliasByEntityAndFolder(final Goid entityGoid, final Goid folderGoid) throws FindException;

    /**
     * Delete an {@link com.l7tech.objectmodel.Alias} by its unique identifier.

     * @param id the unique identifier of the {@link com.l7tech.objectmodel.Alias} to delete.
     * @throws com.l7tech.objectmodel.DeleteException if the requested alias could not be deleted
     */
    @Secured(stereotype= DELETE_BY_ID, types= {EntityType.SERVICE_ALIAS, EntityType.POLICY_ALIAS})
    void deleteEntityAlias(String id) throws DeleteException;
}
