package com.l7tech.objectmodel;

/**
 * The goid entity is similar to the persistent entity and will replace it.
 * A goid entity is one that will be stored in the database.
 *
 * @author Victor Kazakov
 */
public interface PersistentEntity extends Entity {
    //This is the default Goid
    public static final Goid DEFAULT_GOID = Goid.DEFAULT_GOID;

    /**
     * Returns the entities goid
     *
     * @return The goid of this entity
     */
    Goid getGoid();

    /**
     * Sets the goid of the entity
     *
     * @param goid The new goid of the entity
     */
    void setGoid(Goid goid);

    /**
     * Check if this entity is using a GOID equivalent to {@link #DEFAULT_GOID}, indicating that it
     * has never been saved.
     *
     * @return true if DEFAULT_GOID.equals(getGoid()).
     */
    boolean isUnsaved();

    /**
     * return the version of the entity
     *
     * @return the version of the entity
     */
    int getVersion();

    /**
     * set the version of the entity
     *
     * @param version the new version of the entity
     */
    void setVersion(int version);
}
