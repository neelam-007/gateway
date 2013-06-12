package com.l7tech.objectmodel;

import org.jetbrains.annotations.Nullable;

/**
 * Indicates that the implementing class can have a reference to a SecurityZone by its oid.
 */
public interface HasSecurityZoneOid {
    /**
     * @return the oid of the referenced SecurityZone.
     */
    @Nullable
    public Long getSecurityZoneOid();

    /**
     * Set the oid of the referenced SecurityZone.
     *
     * @param securityZoneOid the oid of the referenced SecurityZone.
     */
    public void setSecurityZoneOid(@Nullable final Long securityZoneOid);
}
