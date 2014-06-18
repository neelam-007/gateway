package com.l7tech.objectmodel;

/**
 * An entity manager that can look up entities based on a GUID, rather than a name or OID.
 */
public interface GuidBasedEntityManager <ET extends GuidEntity> {
    /**
     * Returns the the entity that matches the provided GUID
     * @param guid The GUID to search with
     * @return The entity that matches the provided GUID
     * @throws FindException If no entity could be found
     */
    ET findByGuid(String guid) throws FindException;
}
