package com.l7tech.server.uddi;

/**
 * UDDI Event for subscription updates
 */
class SubscribeUDDIEvent extends UDDIEvent {

    //- PACKAGE

    enum Type { SUBSCRIBE, UNSUBSCRIBE }

    SubscribeUDDIEvent( final long registryOid,
                        final Type type ) {
        this.registryOid = registryOid;
        this.type = type;
    }

    long getRegistryOid() {
        return registryOid;
    }

    Type getType() {
        return type;
    }

    //- PRIVATE

    private final long registryOid;
    private final Type type;
}
