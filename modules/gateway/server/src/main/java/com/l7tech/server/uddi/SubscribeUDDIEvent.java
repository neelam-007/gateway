package com.l7tech.server.uddi;

import com.l7tech.objectmodel.Goid;

/**
 * UDDI Event for subscription updates
 */
class SubscribeUDDIEvent extends UDDIEvent {

    //- PACKAGE

    enum Type { SUBSCRIBE, UNSUBSCRIBE }

    SubscribeUDDIEvent( final Goid registryGoid,
                        final Type type  ) {
        this( registryGoid, type, false );
    }

    SubscribeUDDIEvent( final Goid registryGoid,
                        final Type type,
                        final boolean expiredOnly ) {
        super(false);
        this.registryGoid = registryGoid;
        this.type = type;
        this.expiredOnly = expiredOnly;
    }

    Goid getRegistryGoid() {
        return registryGoid;
    }

    Type getType() {
        return type;
    }

    boolean isExpiredOnly() {
        return expiredOnly;
    }

    //- PRIVATE

    private final Goid registryGoid;
    private final Type type;
    private final boolean expiredOnly;
}
