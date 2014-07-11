package com.l7tech.external.assertions.websocket;

import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 6/4/12
 * Time: 11:41 AM
 * To change this template use File | Settings | File Templates.
 */
@Secured
public interface WebSocketConnectionEntityAdmin {
    /**
     * Find all WebSocketConnectionEntity instances.
     *
     * @return a collection of instances.  May be empty but never null.
     * @throws com.l7tech.objectmodel.FindException if there is a problem reading the database
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    Collection<WebSocketConnectionEntity> findAll() throws FindException;

    /**
     * Save a new or changed WebSocketConnectionEntity.
     *
     * @param entity the entity to save.  Required.
     * @return the oid assigned to the entity (useful when saving a new one)
     * @throws com.l7tech.objectmodel.SaveException if there is a problem saving the entity
     * @throws com.l7tech.objectmodel.UpdateException if there is a problem updating an existing entity
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    Goid save(WebSocketConnectionEntity entity) throws SaveException, UpdateException;

    /**
     * Delete a WebSocketConnectionEntity.
     *
     * @param entity the entity to delete.  Required.
     * @throws com.l7tech.objectmodel.DeleteException if entity cannot be deleted
     * @throws com.l7tech.objectmodel.FindException if entity cannot be located before deletion
     */
    @Secured(stereotype=DELETE_ENTITY)
    void delete(WebSocketConnectionEntity entity) throws DeleteException, FindException;

    @Secured(stereotype=FIND_ENTITY)
    WebSocketConnectionEntity findByPrimaryKey(Goid key) throws FindException;
}
