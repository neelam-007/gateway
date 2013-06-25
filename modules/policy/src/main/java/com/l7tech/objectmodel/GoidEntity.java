package com.l7tech.objectmodel;

/**
 * This was created: 6/25/13 as 9:39 AM
 *
 * @author Victor Kazakov
 */
public interface GoidEntity extends Entity {
    //This is the default Goid
    public static final Goid DEFAULT_GOID = new Goid(0, -1);

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
