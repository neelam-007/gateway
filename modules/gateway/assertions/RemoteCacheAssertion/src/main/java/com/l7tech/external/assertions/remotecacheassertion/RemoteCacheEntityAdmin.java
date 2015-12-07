package com.l7tech.external.assertions.remotecacheassertion;

import com.l7tech.assertion.base.util.classloaders.ClassLoaderEntityAdmin;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;

/**
 * Created by IntelliJ IDEA.
 * User: cirving
 * Date: 2/28/12
 * Time: 9:58 AM
 * To change this template use File | Settings | File Templates.
 */
@Secured
public interface RemoteCacheEntityAdmin extends ClassLoaderEntityAdmin {
    /**
     * Find all RemoteCacheEntity instances.
     *
     * @return a collection of instances.  May be empty but never null.
     * @throws com.l7tech.objectmodel.FindException if there is a problem reading the database
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    Collection<RemoteCacheEntity> findAll() throws FindException;

    /**
     * Save a new or changed RemoteCacheEntity.
     *
     * @param entity the entity to save.  Required.
     * @return the oid assigned to the entity (useful when saving a new one)
     * @throws com.l7tech.objectmodel.SaveException if there is a problem saving the entity
     * @throws com.l7tech.objectmodel.UpdateException if there is a problem updating an existing entity
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    Goid save(RemoteCacheEntity entity) throws SaveException, UpdateException;

    /**
     * Delete a RemoteCacheEntity.
     *
     * @param entity the entity to delete.  Required.
     * @throws com.l7tech.objectmodel.DeleteException if entity cannot be deleted
     * @throws com.l7tech.objectmodel.FindException if entity cannot be located before deletion
     */
    @Secured(stereotype=DELETE_ENTITY)
    void delete(RemoteCacheEntity entity) throws DeleteException, FindException;

    @Secured(stereotype=FIND_ENTITY)
    RemoteCacheEntity findByUniqueName(String name) throws FindException;

    @Secured(stereotype=FIND_ENTITY)
    public RemoteCacheEntity find(Goid goid) throws FindException;

}
