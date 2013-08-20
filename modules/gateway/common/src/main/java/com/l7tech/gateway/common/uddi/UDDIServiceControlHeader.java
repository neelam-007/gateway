package com.l7tech.gateway.common.uddi;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;

/**
 * EntityHeader for UDDIServiceControl.
 */
public class UDDIServiceControlHeader extends ReferencesZoneAndServiceHeader {
    public UDDIServiceControlHeader(final Goid goid, final String name, final String description, final Integer version,
                                    final Goid securityZoneGoid, final Goid publishedServiceGoid) {
        super(goid, EntityType.UDDI_SERVICE_CONTROL, name, description, version, securityZoneGoid, publishedServiceGoid);
    }
}
