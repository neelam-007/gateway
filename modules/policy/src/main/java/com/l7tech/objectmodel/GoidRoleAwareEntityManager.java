package com.l7tech.objectmodel;

/**
 * Interface implemented by EntityManagers that support roles.
 *
 * TODO this should probably just be a side effect of saving an Entity
 */
public interface GoidRoleAwareEntityManager<E extends GoidEntity> {

    /**
     * Create any required roles for the given entity.
     *
     * @param entity The entity whose roles should be created.
     * @throws com.l7tech.objectmodel.SaveException If an error occurs
     */
    void createRoles(E entity) throws SaveException;

    /**
     * Update any roles for the given entity.
     *
     * @param entity The entity whose roles should be updated.
     * @throws com.l7tech.objectmodel.UpdateException If an error occurs
     */
    void updateRoles(E entity) throws UpdateException;

    /**
     * Delete any roles for the given entity identifier.
     *
     * @param entityGoid The GOID for the entity whose roles should be deleted.
     * @throws com.l7tech.objectmodel.DeleteException If an error occurs.
     */
    void deleteRoles(Goid entityGoid) throws DeleteException;
}
