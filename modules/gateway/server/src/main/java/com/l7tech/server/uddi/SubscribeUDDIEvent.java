package com.l7tech.server.uddi;

/**
 * UDDI Event for subscription updates
 */
class SubscribeUDDIEvent extends UDDIEvent {

    //- PACKAGE

    enum Type { SUBSCRIBE, UNSUBSCRIBE }

    SubscribeUDDIEvent( final long registryOid,
                        final Type type  ) {
        this( registryOid, type, false );
    }

    SubscribeUDDIEvent( final long registryOid,
                        final Type type,
                        final boolean expiredOnly ) {
        this.registryOid = registryOid;
        this.type = type;
        this.expiredOnly = expiredOnly;
    }

    long getRegistryOid() {
        return registryOid;
    }

    Type getType() {
        return type;
    }

    boolean isExpiredOnly() {
        return expiredOnly;
    }

    //- PRIVATE

    private final long registryOid;
    private final Type type;
    private final boolean expiredOnly;
}
