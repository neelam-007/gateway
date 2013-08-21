package com.l7tech.objectmodel;

import org.jetbrains.annotations.Nullable;

/**
 * Indicates that the implementing class can have a reference to a SecurityZone by its goid.
 */
public interface HasSecurityZoneId {
    /**
     * @return the goid of the referenced SecurityZone.
     */
    @Nullable
    public Goid getSecurityZoneId();

    /**
     * Set the goid of the referenced SecurityZone.
     *
     * @param securityZoneGoid the goid of the referenced SecurityZone.
     */
    public void setSecurityZoneId(@Nullable final Goid securityZoneGoid);
}
