package com.l7tech.objectmodel;

/**
 * A GuidEntity is an entity that has a guid. This interface defines the getter and setter for the guid.
 */
public interface GuidEntity {
    /**
     * Returns the Guid for this entity
     *
     * @return The entity guid
     */
    public String getGuid();

    /**
     * Sets the guid for this entity
     *
     * @param guid The guid for this entity
     */
    public void setGuid(String guid);
}
