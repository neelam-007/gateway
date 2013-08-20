package com.l7tech.gateway.common.uddi;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ZoneableEntityHeader;

/**
 * EntityHeader which references a SecurityZone and a PublishedService.
 */
public abstract class ReferencesZoneAndServiceHeader extends ZoneableEntityHeader {
    private Goid publishedServiceGoid;

    public ReferencesZoneAndServiceHeader(final Goid goid, final EntityType entityType, final String name, final String description,
                                          final Integer version, final Goid securityZoneGoid, final Goid publishedServiceGoid) {
        super(goid, entityType, name, description, version);
        this.publishedServiceGoid = publishedServiceGoid;
        this.securityZoneGoid = securityZoneGoid;
    }

    public Goid getPublishedServiceGoid() {
        return publishedServiceGoid;
    }
}
