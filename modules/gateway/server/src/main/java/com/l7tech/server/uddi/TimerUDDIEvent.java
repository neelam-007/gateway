package com.l7tech.server.uddi;

/**
 * UDDIEvent for scheduled event.
 */
class TimerUDDIEvent extends UDDIEvent {

    //- PACKAGE

    enum Type { METRICS_PUBLISH, METRICS_CLEANUP, SUBSCRIPTION_POLL }

    TimerUDDIEvent( final long registryOid,
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
