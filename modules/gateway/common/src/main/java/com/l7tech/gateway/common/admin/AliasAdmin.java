package com.l7tech.gateway.common.admin;

import org.springframework.transaction.annotation.Transactional;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import static com.l7tech.gateway.common.security.rbac.MethodStereotype.SAVE_OR_UPDATE;
import static com.l7tech.gateway.common.security.rbac.MethodStereotype.FIND_HEADERS;
import static com.l7tech.gateway.common.security.rbac.MethodStereotype.DELETE_BY_ID;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.PolicyAssertionException;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 25, 2008
 * Time: 6:26:17 PM
 * To change this template use File | Settings | File Templates.
 */
/**
 * Admin interface for managing aliased entities
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types= {EntityType.SERVICE_ALIAS, EntityType.POLICY_ALIAS})
public interface AliasAdmin<HT extends Alias> {

    /**
     * Store the specified new or existing alias. If the specified {@link com.l7tech.objectmodel.Alias} contains a
     * unique object ID that already exists, this will replace the objects current configuration with the new configuration.
     * Otherwise, a new object will be created.
     * The only reason to save an alias is if you have changed it's folder attribute.
     *
     * @param alias
     * @return
     * @throws com.l7tech.objectmodel.SaveException   if the requested information could not be saved
     * @throws com.l7tech.objectmodel.UpdateException if the requested information could not be updated
     * @throws com.l7tech.objectmodel.VersionException if the service version conflict is detected
     * @throws com.l7tech.policy.assertion.PolicyAssertionException if the server policy could not be instantiated for this policy
     * @throws IllegalStateException if this save represents anything other than a change to the aliases folder property
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    long saveAlias(HT alias)
            throws UpdateException, SaveException, VersionException, PolicyAssertionException, IllegalStateException;

    /**
     * Find an Alias based on the original entities oid and the folderoid in which the alias currently resides
     * @param entityOid oid of the real entity
     * @param folderOid folder oid of the folder in which the alias currently resides
     * @return The alias of the entityOid supplied if found, null otherwise
     * @throws com.l7tech.objectmodel.FindException
     */
    @Transactional(readOnly=true)
    @Secured(stereotype= MethodStereotype.FIND_ENTITY_BY_ATTRIBUTE)
    @Administrative(licensed=false)
    public HT findAliasByEntityAndFolder(final Long entityOid, final Long folderOid) throws FindException;

    /**
     * Delete an {@link com.l7tech.objectmodel.Alias} by its unique identifier.

     * @param oid the unique identifier of the {@link com.l7tech.objectmodel.Alias} to delete.
     * @throws com.l7tech.objectmodel.DeleteException if the requested alias could not be deleted
     */
    @Secured(stereotype= DELETE_BY_ID, types= {EntityType.SERVICE_ALIAS, EntityType.POLICY_ALIAS})
    void deleteEntityAlias(String oid) throws DeleteException;
}
