package com.l7tech.external.assertions.xmppassertion;

import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;

/**
 * User: cirving
 * Date: 2/28/12
 * Time: 9:58 AM
 */
@Secured
public interface XMPPConnectionEntityAdmin {
    /**
     * Find all XMPPConnectionEntity instances.
     *
     * @return a collection of instances.  May be empty but never null.
     * @throws com.l7tech.objectmodel.FindException if there is a problem reading the database
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    Collection<XMPPConnectionEntity> findAll() throws FindException;

    /**
     * Save a new or changed URIMappingEntity.
     *
     * @param entity the entity to save.  Required.
     * @return the oid assigned to the entity (useful when saving a new one)
     * @throws com.l7tech.objectmodel.SaveException if there is a problem saving the entity
     * @throws com.l7tech.objectmodel.UpdateException if there is a problem updating an existing entity
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    Goid save(XMPPConnectionEntity entity) throws SaveException, UpdateException;

    /**
     * Delete a XMPPConnectionEntity.
     *
     * @param entity the entity to delete.  Required.
     * @throws com.l7tech.objectmodel.DeleteException if entity cannot be deleted
     * @throws com.l7tech.objectmodel.FindException if entity cannot be located before deletion
     */
    @Secured(stereotype=DELETE_ENTITY)
    void delete(XMPPConnectionEntity entity) throws DeleteException, FindException;

    @Secured(stereotype=FIND_ENTITY)
    XMPPConnectionEntity findByUniqueName(String name) throws FindException;

    @Secured(stereotype=FIND_ENTITY)
    public XMPPConnectionEntity find(Goid goid) throws FindException;
}
