package com.l7tech.server.uddi;

import com.l7tech.objectmodel.Goid;

/**
 * UDDIEvent for scheduled event.
 */
class TimerUDDIEvent extends UDDIEvent {

    //- PACKAGE

    enum Type { METRICS_PUBLISH, METRICS_CLEANUP, SUBSCRIPTION_POLL }

    TimerUDDIEvent( final Goid registryGoid,
                    final Type type ) {
        this.registryGoid = registryGoid;
        this.type = type;
    }

    Goid getRegistryGoid() {
        return registryGoid;
    }

    Type getType() {
        return type;
    }

    //- PRIVATE

    private final Goid registryGoid;
    private final Type type;

}
