package com.l7tech.server.uddi;

import com.l7tech.objectmodel.Goid;

/**
 * 
 */
class WsPolicyUDDIEvent extends UDDIEvent {

    //- PACKAGE

    WsPolicyUDDIEvent( final Goid registryGoid ) {
        this.registryGoid = registryGoid;
    }

    Goid getRegistryGoid() {
        return registryGoid;
    }

    //- PRIVATE

    private final Goid registryGoid;
    
}
