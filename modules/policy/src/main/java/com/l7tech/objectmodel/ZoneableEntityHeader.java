package com.l7tech.objectmodel;

import org.jetbrains.annotations.Nullable;

/**
 * EntityHeader for an entity that has a SecurityZone.
 */
public class ZoneableEntityHeader extends EntityHeader {
    protected Long securityZoneOid;

    public ZoneableEntityHeader(long oid, EntityType type, String name, String description, Integer version) {
        super(oid, type, name, description, version);
    }

    @Nullable
    public Long getSecurityZoneOid() {
        return securityZoneOid;
    }

    public void setSecurityZoneOid(@Nullable Long securityZoneOid) {
        this.securityZoneOid = securityZoneOid;
    }
}
