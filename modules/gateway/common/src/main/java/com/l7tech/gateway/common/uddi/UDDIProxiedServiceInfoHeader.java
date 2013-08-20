package com.l7tech.gateway.common.uddi;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;

/**
 * EntityHeader for UDDIProxiedServiceInfo.
 */
public class UDDIProxiedServiceInfoHeader extends ReferencesZoneAndServiceHeader {
    public UDDIProxiedServiceInfoHeader(final Goid goid, final String name, final String description, final Integer version,
                                        final Goid securityZoneGoid, final Goid publishedServiceGoid) {
        super(goid, EntityType.UDDI_PROXIED_SERVICE_INFO, name, description, version, securityZoneGoid, publishedServiceGoid);
    }
}
