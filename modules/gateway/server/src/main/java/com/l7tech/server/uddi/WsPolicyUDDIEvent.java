package com.l7tech.server.uddi;

/**
 * 
 */
class WsPolicyUDDIEvent extends UDDIEvent {

    //- PACKAGE

    WsPolicyUDDIEvent( final long registryOid ) {
        this.registryOid = registryOid;
    }

    long getRegistryOid() {
        return registryOid;
    }

    //- PRIVATE

    private final long registryOid;
    
}
