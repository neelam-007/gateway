package com.l7tech.gateway.common.service;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

/**
 * Extension of EntityHeader with some service information.
 *
 * @author Steve Jones
 */
public class ServiceHeader extends EntityHeader {

    //- PUBLIC

    public ServiceHeader(final PublishedService svc) {
        this( svc.isSoap(),
              svc.isDisabled(),
              svc.displayName(),
              svc.getId(),
              svc.getName(),
              svc.getName());
    }

    public ServiceHeader(final boolean isSoap,
                         final boolean isDisabled,
                         final String displayName,
                         final String serviceId,
                         final String name,
                         final String description) {
        super(serviceId, EntityType.SERVICE, name, description);

        this.isSoap = isSoap;
        this.isDisabled = isDisabled;
        this.displayName = displayName;
    }

    public boolean isSoap() {
        return isSoap;
    }

    public boolean isDisabled() {
        return isDisabled;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String toString() {
        return getDisplayName();
    }

    //- PRIVATE

    private final boolean isSoap;
    private final boolean isDisabled;
    private final String displayName;
}
