package com.l7tech.uddi;

/**
 * Interface for UDDI entities operational information.
 */
public interface UDDIOperationalInfo {

    /**
     * Get the key for the entity.
     *
      * @return The entity key
     */
    public String getEntityKey();

    /**
     * Get the name of the entity owner.
     *
     * @return The owner name
     */
    public String getAuthorizedName();

    /**
     * Get the created date for the entity.
     *
     * @return The created date
     */
    public long getCreatedTime();

    /**
     * Get the last modified time for the entity.
     *
     * @return The last modified time.
     */
    public long getModifiedTime();

    /**
     * Get the last modified time for the entity hierarchy.
     *
     * @return The last modified time.
     */
    public long getModifiedIncludingChildrenTime();
}
