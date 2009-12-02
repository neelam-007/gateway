package com.l7tech.server.uddi;

/**
 * UDDIEvent for notification of an updated (or deleted) UDDI business service. 
 */
class BusinessServiceUpdateUDDIEvent extends UDDIEvent {

    //- PACKAGE

    BusinessServiceUpdateUDDIEvent(final long registryOid,
                                   final String serviceKey,
                                   final boolean deleted,
                                   boolean forceUpdate) {
        super(false);
        this.registryOid = registryOid;
        this.serviceKey = serviceKey;
        this.deleted = deleted;
        this.forceUpdate = forceUpdate;
    }

    long getRegistryOid() {
        return registryOid;
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

    private final long registryOid;
    private final String serviceKey;
    private final boolean deleted;
    private final boolean forceUpdate;
}
