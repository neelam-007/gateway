package com.l7tech.objectmodel;

/**
 * Interface implemented by entities that can be placed into a {@link SecurityZone}.
 */
public interface ZoneableEntity extends Entity {
    /**
     * @return the security zone this entity lives in, or null if not set.
     */
    SecurityZone getSecurityZone();

    /**
     * @param securityZone a new security zone to assign to this entity, or null to take it out of all security zones.
     */
    void setSecurityZone(SecurityZone securityZone);
}
