package com.l7tech.objectmodel;

import org.jetbrains.annotations.Nullable;

/**
 * A GuidEntityHeader which also has a reference to a SecurityZone.
 */
public class ZoneableGuidEntityHeader extends GuidEntityHeader implements HasSecurityZoneId {
    protected Goid securityZoneGoid;

    protected ZoneableGuidEntityHeader(final Goid goid, final EntityType type, final String name, final String description, final int version) {
        super(goid, type, name, description, version);
    }

    public ZoneableGuidEntityHeader(final String id, final EntityType type, final String name, final String description, final Integer version) {
        super(id, type, name, description, version);
    }

    @Nullable
    @Override
    public Goid getSecurityZoneId() {
        return securityZoneGoid;
    }

    @Override
    public void setSecurityZoneId(@Nullable final Goid securityZoneGoid) {
        this.securityZoneGoid = securityZoneGoid;
    }
}
