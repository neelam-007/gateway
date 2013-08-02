package com.l7tech.objectmodel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * EntityHeader for an entity that has a SecurityZone.
 */
public class ZoneableEntityHeader extends EntityHeader implements HasSecurityZoneGoid {
    protected Goid securityZoneGoid;

    public ZoneableEntityHeader() {
        super();
    }

    public ZoneableEntityHeader(@NotNull final EntityHeader headerToCopy) {
        this(headerToCopy.getStrId(), headerToCopy.getType(), headerToCopy.getName(), headerToCopy.getDescription(), headerToCopy.getVersion());
    }

    @Deprecated
    public ZoneableEntityHeader(final long oid, final EntityType type, final String name, final String description, final Integer version) {
        super(oid, type, name, description, version);
    }

    public ZoneableEntityHeader(final Goid goid, final EntityType type, final String name, final String description, final Integer version) {
        super(goid, type, name, description, version);
    }

    public ZoneableEntityHeader(final String oid, final EntityType type, final String name, final String description, final Integer version) {
        super(oid, type, name, description, version);
    }

    @Nullable
    public Goid getSecurityZoneGoid() {
        return securityZoneGoid;
    }

    public void setSecurityZoneGoid(@Nullable Goid securityZoneGoid) {
        this.securityZoneGoid = securityZoneGoid;
    }
}
