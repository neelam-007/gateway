package com.l7tech.server.uddi;

import com.l7tech.objectmodel.Goid;

/**
 * UDDIEvent for notification of an updated (or deleted) UDDI business service. 
 */
class BusinessServiceUpdateUDDIEvent extends UDDIEvent {

    //- PACKAGE

    BusinessServiceUpdateUDDIEvent(final Goid registryGoid,
                                   final String serviceKey,
                                   final boolean deleted,
                                   boolean forceUpdate) {
        super(false);
        this.registryGoid = registryGoid;
        this.serviceKey = serviceKey;
        this.deleted = deleted;
        this.forceUpdate = forceUpdate;
    }

    Goid getRegistryGoid() {
        return registryGoid;
    }

    String getServiceKey() {
        return serviceKey;
    }

    boolean isDeleted() {
        return deleted;
    }

    /**
     * If true, then any monitoring configuration should be ignored. Any found updates should be applied
     * @return
     */
    public boolean isForceUpdate() {
        return forceUpdate;
    }

    //- PRIVATE

    private final Goid registryGoid;
    private final String serviceKey;
    private final boolean deleted;
    private final boolean forceUpdate;
}
