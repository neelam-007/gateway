package com.l7tech.external.assertions.whichmodule;

import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;
import static com.l7tech.objectmodel.EntityType.GENERIC;

/**
 * Admin interface for SSM code to perform server-side CRUD on DemoGenericEntity instances.
 */
@Secured(types=GENERIC)
public interface DemoGenericEntityAdmin {
    /**
     * Find all DemoGenericEntity instances.
     *
     * @return a collection of instances.  May be empty but never null.
     * @throws FindException if there is a problem reading the database
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    Collection<DemoGenericEntity> findAll() throws FindException;

    /**
     * Save a new or changed DemoGenericEntity.
     *
     * @param entity the entity to save.  Required.
     * @return the oid assigned to the entity (useful when saving a new one)
     * @throws SaveException if there is a problem saving the entity
     * @throws UpdateException if there is a problem updating an existing entity
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    Goid save(DemoGenericEntity entity) throws SaveException, UpdateException;

    /**
     * Delete a DemoGenericEntity.
     *
     * @param entity the entity to delete.  Required.
     * @throws DeleteException if entity cannot be deleted
     * @throws FindException if entity cannot be located before deletion
     */
    @Secured(stereotype=DELETE_ENTITY)
    void delete(DemoGenericEntity entity) throws DeleteException, FindException;
}
