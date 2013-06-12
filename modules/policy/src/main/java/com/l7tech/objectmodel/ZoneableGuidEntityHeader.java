package com.l7tech.objectmodel;

import org.jetbrains.annotations.Nullable;

/**
 * A GuidEntityHeader which also has a reference to a SecurityZone.
 */
public class ZoneableGuidEntityHeader extends GuidEntityHeader implements HasSecurityZoneOid {
    protected Long securityZoneOid;

    protected ZoneableGuidEntityHeader(final long oid, final EntityType type, final String name, final String description, final int version) {
        super(oid, type, name, description, version);
    }

    public ZoneableGuidEntityHeader(final String id, final EntityType type, final String name, final String description, final Integer version) {
        super(id, type, name, description, version);
    }

    @Nullable
    @Override
    public Long getSecurityZoneOid() {
        return securityZoneOid;
    }

    @Override
    public void setSecurityZoneOid(@Nullable final Long securityZoneOid) {
        this.securityZoneOid = securityZoneOid;
    }
}
