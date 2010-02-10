package com.l7tech.objectmodel;

/**
 * Interface implemented by EntityManagers that support roles.
 *
 * TODO this should probably just be a side effect of saving an Entity
 */
public interface RoleAwareEntityManager<PET extends PersistentEntity> {

    /**
     * Create any required roles for the given entity.
     *
     * @param entity The entity whose roles should be created.
     * @throws SaveException If an error occurs
     */
    void createRoles( PET entity ) throws SaveException;

    /**
     * Delete any roles for the given entity identifier.
     *
     * @param entityOid The OID for the entity whose roles should be deleted.
     * @throws DeleteException If an error occurs.
     */
    void deleteRoles( long entityOid ) throws DeleteException;
}
