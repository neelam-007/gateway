package com.l7tech.server.uddi;

/**
 * UDDIEvent for notification of an updated (or deleted) UDDI business service. 
 */
class BusinessServiceUpdateUDDIEvent extends UDDIEvent {

    //- PACKAGE

    BusinessServiceUpdateUDDIEvent( final long registryOid,
                                    final String serviceKey,
                                    final boolean deleted ) {
        this.registryOid = registryOid;
        this.serviceKey = serviceKey;
        this.deleted = deleted;
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

    //- PRIVATE

    private final long registryOid;
    private final String serviceKey;
    private final boolean deleted;
}
